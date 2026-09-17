package com.dru.tree_sdk_flutter

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dru.treesdk.TreeSDK
import com.dru.treesdk.modules.reminders.ReminderDeepLink
import com.dru.treesdk.ui.TreeRootView
import com.dru.treesdk.ui.TreeScreen

/**
 * Hosts `TreeRootView` full-screen — the Android counterpart to the iOS side's
 * `UIHostingController` presented modally over the Flutter view.
 *
 * A dedicated Activity, not a Fragment or an overlay inside the host's own Activity, so the SDK
 * gets its own lifecycle and back-stack entry — "the SDK owns the whole screen" from this
 * package's README applies just as much on Android.
 *
 * Reads `TreeSDK.shared` directly rather than threading the SDK instance through the launching
 * Intent (Intents can't carry it — no `Serializable`/`Parcelable` session object exists, and
 * shouldn't): [TreeSdkFlutterPlugin.start] only calls [launch] after `TreeSDK.initialize` has
 * already set that singleton.
 */
class TreeSdkFlutterActivity : ComponentActivity() {

    private var reminderDeepLink by mutableStateOf<ReminderDeepLink?>(null)

    /**
     * Which presentation this Activity is, as handed out by the plugin.
     *
     * Reported back when the presentation ends so the plugin can tell a real close from a stale
     * one - see [TreeSdkFlutterPlugin.notifyClosed]. -1 means "not launched by the plugin", which
     * only happens if the system recreates this Activity from a saved instance state.
     */
    private var presentationID: Int = -1

    /** Set once the close has been reported, so onDestroy does not report it twice. */
    private var reportedClosed = false

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // A relaunch is a new presentation as far as the plugin is concerned, so take its id -
        // otherwise this Activity would keep reporting under an id the plugin has retired.
        intent.takePresentationID()?.let { presentationID = it }
        ReminderDeepLink.from(intent)?.let { reminderDeepLink = it }
    }

    /**
     * The presentation is over, however it ended.
     *
     * A reminder notification opens the host's launcher activity with `FLAG_ACTIVITY_CLEAR_TOP`,
     * which finishes this Activity outright: [onClose] never runs, so the plugin went on
     * believing the SDK was on screen and ignored every later `start` as a duplicate. The user
     * was left on the host's connect page with a Start button that did nothing.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) reportClosed()
    }

    private fun reportClosed() {
        if (reportedClosed) return
        reportedClosed = true
        TreeSdkFlutterPlugin.activePlugin?.notifyClosed(presentationID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )

        val sdk = TreeSDK.shared
        if (sdk == null) {
            // Process death and restart landed directly on this Activity with no SDK session left
            // to show (TreeSDK.shared is in-memory only, same as the Swift side keeps no on-disk
            // presentation state either). Nothing sensible to present — back out to the host.
            finish()
            return
        }

        val plugin = TreeSdkFlutterPlugin.activePlugin
        presentationID = intent.takePresentationID() ?: -1
        val initial = intent.getStringExtra(EXTRA_INITIAL)
            ?.let { runCatching { TreeScreen.valueOf(it) }.getOrNull() }
            ?: TreeScreen.EXPLORE
        reminderDeepLink = ReminderDeepLink.from(intent)

        setContent {
            // Fires once, on the first composition — the closest Compose equivalent to the Swift
            // side's `host.present(hosting, animated: true) { result(nil) }` completion, which is
            // what resolves the Dart `start()` call.
            LaunchedEffect(Unit) { plugin?.notifyPresented(initial) }

            TreeRootView(
                sdk = sdk,
                initial = initial,
                reminderDeepLink = reminderDeepLink,
                onReminderDeepLinkHandled = {
                    reminderDeepLink = null
                    ReminderDeepLink.clear(intent)
                },
                onClose = {
                    reportClosed()
                    finish()
                },
            )
        }
    }

    /**
     * The launch id this Intent carries, if it came from the plugin.
     *
     * Deliberately left on the Intent rather than consumed: a configuration change recreates this
     * Activity and re-reads the same Intent, and it has to come back as the same presentation or
     * its eventual close would be dismissed as stale.
     */
    private fun Intent.takePresentationID(): Int? =
        getIntExtra(EXTRA_PRESENTATION_ID, -1).takeIf { it >= 0 }

    companion object {
        private const val EXTRA_INITIAL = "initial"
        private const val EXTRA_PRESENTATION_ID = "presentation_id"

        internal fun launch(
            context: Context,
            initial: TreeScreen,
            deepLinkExtras: Bundle? = null,
            presentationID: Int = -1,
        ) {
            val intent = Intent(context, TreeSdkFlutterActivity::class.java)
                .putExtra(EXTRA_INITIAL, initial.name)
                .putExtra(EXTRA_PRESENTATION_ID, presentationID)
            deepLinkExtras?.let { intent.putExtras(it) }
            // singleTop in the manifest plus this flag: a second launch is delivered to the
            // instance already on screen (onNewIntent, which re-reads the deep link) instead of
            // stacking another copy of the SDK behind or in front of it.
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (context !is ComponentActivity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
