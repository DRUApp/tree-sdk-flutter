package com.dru.tree_sdk_flutter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import com.dru.treesdk.TreeSDK
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sdk = TreeSDK.shared
        if (sdk == null) {
            // Process death and restart landed directly on this Activity with no SDK session left
            // to show (TreeSDK.shared is in-memory only, same as the Swift side keeps no on-disk
            // presentation state either). Nothing sensible to present — back out to the host.
            finish()
            return
        }

        val plugin = TreeSdkFlutterPlugin.activePlugin
        val initial = intent.getStringExtra(EXTRA_INITIAL)
            ?.let { runCatching { TreeScreen.valueOf(it) }.getOrNull() }
            ?: TreeScreen.EXPLORE

        setContent {
            // Fires once, on the first composition — the closest Compose equivalent to the Swift
            // side's `host.present(hosting, animated: true) { result(nil) }` completion, which is
            // what resolves the Dart `start()` call.
            LaunchedEffect(Unit) { plugin?.notifyPresented(initial) }

            TreeRootView(
                sdk = sdk,
                initial = initial,
                onClose = {
                    plugin?.notifyClosed()
                    finish()
                },
            )
        }
    }

    companion object {
        private const val EXTRA_INITIAL = "initial"

        internal fun launch(context: Context, initial: TreeScreen) {
            val intent = Intent(context, TreeSdkFlutterActivity::class.java)
                .putExtra(EXTRA_INITIAL, initial.name)
            if (context !is ComponentActivity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
