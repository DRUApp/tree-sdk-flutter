package com.dru.tree_sdk_flutter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.dru.treesdk.TreeSDK
import com.dru.treesdk.config.TreeAuth
import com.dru.treesdk.config.TreeEnvironment
import com.dru.treesdk.config.TreeModule
import com.dru.treesdk.config.TreeSDKConfig
import com.dru.treesdk.core.TreeFailure
import com.dru.treesdk.core.TreeResult
import com.dru.treesdk.modules.reminders.ReminderDeepLink
import com.dru.treesdk.ui.TreeScreen
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "TreeSdkFlutter"

/**
 * Presents the native SDK over the Flutter view — the Android counterpart to
 * `TreeSdkFlutterPlugin.swift`.
 *
 * One method in, `start`: build a `TreeSDKConfig` from the Dart-supplied map, initialize
 * `TreeSDK`, then launch [TreeSdkFlutterActivity] to present `TreeRootView`. `TreeRootView` owns
 * navigation from there and dismisses itself back to Flutter.
 *
 * Two calls out: `getToken`, when the host chose token-provider auth and the SDK needs a bearer
 * token — called on *every* request, so the Dart side is expected to cache — and `onClose`, when
 * the user leaves the SDK's UI.
 */
class TreeSdkFlutterPlugin : FlutterPlugin, ActivityAware, MethodChannel.MethodCallHandler {

    private lateinit var channel: MethodChannel
    private var activityBinding: ActivityPluginBinding? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Set while the SDK is on screen, so a second `start` doesn't stack a duplicate presentation. */
    @Volatile
    private var presented = false

    /**
     * A start that has been asked for but has not put anything on screen yet.
     *
     * [presented] only turns true once the session is established, so two calls arriving inside
     * that window both passed the guard below and launched an Activity each. The first consumed
     * the reminder deep link, leaving the second to open plain Explore on top of it.
     */
    @Volatile
    private var starting = false

    /** The `start` call's result, resolved once [TreeSdkFlutterActivity] is actually on screen. */
    private var pendingStartResult: MethodChannel.Result? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "tree_sdk_flutter")
        channel.setMethodCallHandler(this)
        activePlugin = this
        Log.d(TAG, "registered")
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        scope.cancel()
        if (activePlugin === this) activePlugin = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityBinding = binding
        if (pendingReminderExtras != null) notifyReminderTapped()
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activityBinding = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activityBinding = binding
    }

    override fun onDetachedFromActivity() {
        activityBinding = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "start" -> {
                val arguments = call.arguments as? Map<*, *>
                if (arguments == null) {
                    Log.e(TAG, "start FAILED — arguments were not a map")
                    result.error("configuration", "start expects a map of arguments.", null)
                    return
                }
                start(arguments, result)
            }
            "isPresented" -> result.success(presented)
            // Asked by Dart as it starts up. `onReminderTapped` is a push, and on a COLD start
            // this plugin attaches - and fires it - long before the Dart side has subscribed, so
            // the event is dropped and the user is left staring at the host's connect screen.
            // Deliberately does not consume the extras: `start` still needs them to build the
            // deep link that routes to the log screen.
            "hasPendingReminderTap" -> result.success(pendingReminderExtras != null)
            else -> {
                Log.e(TAG, "unhandled method ${call.method}")
                result.notImplemented()
            }
        }
    }

    // MARK: - start

    private fun start(arguments: Map<*, *>, result: MethodChannel.Result) {
        if (presented || starting) {
            Log.i(TAG, "start ignored — the SDK is already on screen or on its way")
            result.success(null)
            return
        }
        val activity = activityBinding?.activity
        if (activity == null) {
            Log.e(TAG, "start FAILED — no activity to present from")
            result.error("no_window", "No activity to present from.", null)
            return
        }

        val config = when (val parsed = parseConfig(arguments)) {
            is ParsedConfig.Failure -> {
                Log.e(TAG, "start FAILED at config — ${parsed.message}")
                result.error("configuration", parsed.message, null)
                return
            }
            is ParsedConfig.Success -> parsed.config
        }
        val initial = screenFrom(arguments)

        Log.i(
            TAG,
            "start: env=${config.environment.name} baseURL=${config.baseURL} " +
                "user=${config.treeUserID} lang=${config.languageCode} " +
                "modules=${config.enabledModules.joinToString(",") { it.name }}",
        )

        starting = true
        pendingStartResult = result
        val appContext = activity.applicationContext
        scope.launch {
            Log.d(TAG, "initialize: establishing the session")
            when (val initialized = TreeSDK.initialize(appContext, config)) {
                is TreeResult.Err -> {
                    val code = code(initialized.failure)
                    Log.e(TAG, "start FAILED at initialize — code=$code message=${initialized.failure.message}")
                    starting = false
                    pendingStartResult = null
                    result.error(code, initialized.failure.message, null)
                }
                is TreeResult.Ok -> {
                    Log.i(TAG, "initialize ok — session established")
                    starting = false
                    presented = true
                    val deepLinkExtras = pendingReminderExtras
                    pendingReminderExtras = null
                    TreeSdkFlutterActivity.launch(activity, initial, deepLinkExtras)
                }
            }
        }
    }

    /** Called by [TreeSdkFlutterActivity] once its content is actually composed on screen. */
    internal fun notifyPresented(initial: TreeScreen) {
        Log.i(TAG, "presented on ${initial.name.lowercase()}")
        pendingStartResult?.success(null)
        pendingStartResult = null
    }

    /** Called by [TreeSdkFlutterActivity] when the user leaves the SDK's UI. */
    internal fun notifyClosed() {
        Log.i(TAG, "closed by the user")
        starting = false
        presented = false
        channel.invokeMethod("onClose", null)
    }

    /** Tells Dart's `TreeSdk.onReminderTapped` stream to fire — see [notePendingReminderIntent]. */
    private fun notifyReminderTapped() {
        Log.i(TAG, "notifying Dart of a pending reminder deep link")
        channel.invokeMethod("onReminderTapped", null)
    }

    // MARK: - Token provider

    /** Bridges `TreeAuth.TokenProvider` to a `getToken` call on the Dart side. */
    internal suspend fun hostToken(): String = withContext(Dispatchers.Main.immediate) {
        // Platform channels are main-thread only; TreeSDK's session code may call this provider
        // from a background dispatcher, so hop back explicitly (the Swift side does the same with
        // DispatchQueue.main.async).
        suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "getToken: asking the host")
            channel.invokeMethod(
                "getToken",
                null,
                object : MethodChannel.Result {
                    override fun success(response: Any?) {
                        val token = response as? String
                        if (token.isNullOrEmpty()) {
                            Log.e(TAG, "getToken FAILED — no usable token in the response")
                            continuation.resumeWithException(
                                IllegalStateException("The token provider returned no usable token."),
                            )
                        } else {
                            Log.d(TAG, "getToken ok — ${token.length} chars")
                            continuation.resume(token)
                        }
                    }

                    override fun error(code: String, message: String?, details: Any?) {
                        Log.e(TAG, "getToken FAILED — code=$code message=${message ?: "none"}")
                        continuation.resumeWithException(
                            IllegalStateException("$code: ${message ?: "token provider failed"}"),
                        )
                    }

                    override fun notImplemented() {
                        // Also covers the sentinel Flutter returns when Dart has no handler
                        // installed.
                        Log.e(TAG, "getToken FAILED — no handler installed on the Dart side")
                        continuation.resumeWithException(
                            IllegalStateException("The token provider returned no usable token."),
                        )
                    }
                },
            )
        }
    }

    // MARK: - Argument parsing

    private sealed class ParsedConfig {
        data class Success(val config: TreeSDKConfig) : ParsedConfig()
        data class Failure(val message: String) : ParsedConfig()
    }

    private fun parseConfig(arguments: Map<*, *>): ParsedConfig {
        val treeUserID = arguments["treeUserId"] as? String
            ?: return ParsedConfig.Failure("treeUserId is required.")

        val auth: TreeAuth = when (arguments["auth"] as? String) {
            "tokenProvider" -> TreeAuth.TokenProvider { hostToken() }
            "clientCredentials" -> {
                val clientId = arguments["clientId"] as? String
                val clientSecret = arguments["clientSecret"] as? String
                if (clientId == null || clientSecret == null) {
                    return ParsedConfig.Failure("clientCredentials auth requires clientId and clientSecret.")
                }
                TreeAuth.ClientCredentials(clientID = clientId, clientSecret = clientSecret)
            }
            else -> return ParsedConfig.Failure("auth must be 'tokenProvider' or 'clientCredentials'.")
        }

        val environmentName = arguments["environment"] as? String ?: "production"
        val baseUrl = arguments["baseUrl"] as? String
        val environment: TreeEnvironment = when (environmentName) {
            "production" -> TreeEnvironment.Production
            "staging" -> TreeEnvironment.Staging
            "development" -> TreeEnvironment.Development
            "custom" -> {
                if (baseUrl.isNullOrEmpty()) {
                    return ParsedConfig.Failure("environment 'custom' requires baseUrl.")
                }
                TreeEnvironment.Custom(baseUrl)
            }
            else -> return ParsedConfig.Failure("Unknown environment '$environmentName'.")
        }

        val wireModules = (arguments["modules"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val modules = wireModules.mapNotNull(::moduleFromWire).toSet()
        if (modules.size != wireModules.size) {
            return ParsedConfig.Failure("Unknown module in $wireModules.")
        }

        val userName = arguments["userName"] as? String
        val (firstName, lastName) = splitName(userName)

        return ParsedConfig.Success(
            TreeSDKConfig(
                auth = auth,
                treeUserID = treeUserID,
                environment = environment,
                enabledModules = modules.ifEmpty { TreeModule.entries.toSet() },
                languageCode = arguments["languageCode"] as? String ?: "en",
                userName = userName,
                firstName = firstName,
                lastName = lastName,
                gender = arguments["gender"] as? String,
            ),
        )
    }

    private fun screenFrom(arguments: Map<*, *>): TreeScreen = when (arguments["initial"] as? String) {
        "diary" -> TreeScreen.DIARY
        "medications" -> TreeScreen.MEDICATIONS
        "reminders" -> TreeScreen.REMINDERS
        // "explore", "chat" (Dart already rejects chat on Android before this is reached — see
        // lib/tree_sdk_flutter.dart), and anything unrecognized all fall back to Explore, same as
        // the Swift side's `.flatMap(TreeScreen.init(rawValue:)) ?? .explore`.
        else -> TreeScreen.EXPLORE
    }

    companion object {
        /**
         * The plugin instance whose Activity/channel are currently live, for
         * [TreeSdkFlutterActivity] to call back into. A single static slot — an Android Intent
         * can't carry the plugin instance itself, and a Flutter app hosts at most one engine using
         * this plugin at a time in practice — mirrors how `TreeSDK.shared` is a single
         * process-wide instance too.
         */
        @Volatile
        internal var activePlugin: TreeSdkFlutterPlugin? = null


        @Volatile
        private var pendingReminderExtras: Bundle? = null

        
        fun notePendingReminderIntent(intent: Intent) {
            if (ReminderDeepLink.from(intent) == null) return
            pendingReminderExtras = intent.extras?.let { Bundle(it) }
            // The warm case: the plugin/channel already exist (the app was already running when
            // the notification was tapped), so tell Dart right away instead of waiting for
            // onAttachedToActivity, which won't fire again for an already-attached Activity.
            activePlugin?.notifyReminderTapped()
        }

        internal fun moduleFromWire(wire: String): TreeModule? = when (wire) {
            "nawafAI" -> TreeModule.NAWAF
            "diary" -> TreeModule.DIARY
            "medications" -> TreeModule.MEDICATIONS
            else -> null
        }

        
        internal fun splitName(name: String?): Pair<String?, String?> {
            val trimmed = name?.trim()
            if (trimmed.isNullOrEmpty()) return null to null
            val spaceIndex = trimmed.indexOf(' ')
            return if (spaceIndex < 0) {
                trimmed to null
            } else {
                trimmed.substring(0, spaceIndex) to trimmed.substring(spaceIndex + 1).trim().ifEmpty { null }
            }
        }

        /** Maps the SDK's typed failure onto the `PlatformException.code` Dart sees. */
        internal fun code(failure: TreeFailure): String = when (failure) {
            is TreeFailure.Network -> "network"
            is TreeFailure.Timeout -> "timeout"
            is TreeFailure.Unauthorized -> "unauthorized"
            is TreeFailure.Forbidden -> "forbidden"
            is TreeFailure.NotFound -> "not_found"
            is TreeFailure.Validation -> "validation"
            is TreeFailure.Server -> "server"
            is TreeFailure.Cancelled -> "cancelled"
            is TreeFailure.Configuration -> "configuration"
            is TreeFailure.Unknown -> "unknown"
        }
    }
}
