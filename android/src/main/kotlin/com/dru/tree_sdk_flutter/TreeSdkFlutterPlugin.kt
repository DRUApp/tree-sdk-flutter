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
     * Which presentation [presented] refers to, or null when nothing is on screen.
     *
     * The flag alone was not enough. A reminder notification opens the *host's* launcher activity
     * with `FLAG_ACTIVITY_CLEAR_TOP`, which finishes [TreeSdkFlutterActivity] without routing
     * through its own close callback - so `presented` stayed true over an empty screen and every
     * later `start` was ignored as a duplicate. That is the "tapping a reminder lands on the
     * connect page and Start does nothing" bug.
     *
     * An id rather than a boolean because the teardown is racy: the finished Activity's
     * `onDestroy` can arrive *after* the deep link has already opened a new presentation, and a
     * bare flag would let the dying Activity switch the live one off. Each launch takes the next
     * id and only the Activity holding the current one may clear it.
     */
    @Volatile
    private var presentationID: Int? = null

    private var nextPresentationID = 0

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
        val activity = activityBinding?.activity
        if (presented || starting) {
            // One exception to "already on screen": a reminder tap. The notification opens the
            // host's launcher activity, which may have just finished the SDK's - and even when it
            // has not, the deep link still has to be delivered. Re-launching is safe either way:
            // the Activity is singleTop, so this reaches the live instance through onNewIntent or
            // creates a fresh one if CLEAR_TOP took it down.
            // Not while a start is still in flight: that one has not read the extras yet and
            // will deliver them itself. Taking them here would leave it to open a plain Explore
            // on top of the screen the deep link had just reached.
            val extras = pendingReminderExtras?.takeUnless { starting }
            // And only for the session already running: a start asking for a different Tree user
            // has to go through initialize, which is also what disarms the previous user's
            // reminders. Re-using the live session there would open somebody else's diary.
            val live = TreeSDK.shared?.takeIf { it.config.treeUserID == arguments["treeUserId"] }
            if (extras != null && activity != null && live != null) {
                Log.i(TAG, "start while presented — re-delivering the reminder deep link")
                pendingReminderExtras = null
                presented = true
                TreeSdkFlutterActivity.launch(
                    activity,
                    screenFrom(arguments),
                    extras,
                    takePresentationID(),
                )
                result.success(null)
                return
            }
            Log.i(TAG, "start ignored — the SDK is already on screen or on its way")
            result.success(null)
            return
        }
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
            // Every failure has to come back as a Dart error. An exception escaping this
            // coroutine reaches the Main dispatcher's handler and takes the whole host app down
            // with it - the SDK failing to start is never a reason for the app to die.
            val initialized = try {
                TreeSDK.initialize(appContext, config)
            } catch (e: Exception) {
                Log.e(TAG, "start FAILED at initialize — threw ${e.javaClass.simpleName}", e)
                starting = false
                pendingStartResult = null
                result.error("unknown", e.message ?: "The SDK failed to start.", null)
                return@launch
            }
            when (initialized) {
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
                    TreeSdkFlutterActivity.launch(activity, initial, deepLinkExtras, takePresentationID())
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

    /**
     * Called by [TreeSdkFlutterActivity] when its presentation ends - whether the user closed it
     * or the system finished it, which is what a reminder notification's CLEAR_TOP does.
     *
     * Ignores an id that is not the current presentation: that is a previous Activity being torn
     * down after a newer one has already taken the screen, and it must not turn the new one off.
     * Idempotent, so the close callback and the Activity's own teardown can both report.
     */
    internal fun notifyClosed(id: Int) {
        if (presentationID != id) {
            Log.i(TAG, "ignoring a close from presentation $id; current is ${presentationID ?: "none"}")
            return
        }
        Log.i(TAG, "presentation $id ended")
        presentationID = null
        starting = false
        presented = false
        channel.invokeMethod("onClose", null)
    }

    /** Reserves the id for a presentation about to be launched. */
    @Synchronized
    private fun takePresentationID(): Int {
        nextPresentationID += 1
        presentationID = nextPresentationID
        return nextPresentationID
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
            "clientCredentials" ->
                // The SDK is consumed as its **release** AAR, where
                // `TreeAuth.ClientCredentials` has an internal constructor and the debug-only
                // factory that reaches it is not published - so this branch cannot build one in
                // any host configuration. Word for word what the iOS plugin answers, because the
                // Dart API is shared and the two must not disagree about the same call.
                return ParsedConfig.Failure(
                    "clientCredentials auth is not available: the SDK ships without it " +
                        "so a partner secret cannot be embedded. Use tokenProvider.",
                )
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
