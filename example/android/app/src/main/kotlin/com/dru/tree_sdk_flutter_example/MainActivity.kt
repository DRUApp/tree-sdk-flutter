package com.dru.tree_sdk_flutter_example

import android.content.Intent
import android.os.Bundle
import com.dru.tree_sdk_flutter.TreeSdkFlutterPlugin
import io.flutter.embedding.android.FlutterActivity

/**
 * Per tree_sdk_flutter's README ("Tapping a reminder notification"): a
 * reminder's PendingIntent targets this launcher Activity (the SDK has no
 * Activity of its own to route to), so we hand the intent to the plugin
 * before it turns into a `TreeSdk.onReminderTapped` event on the Dart side.
 */
class MainActivity : FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate() — that's what creates the Flutter engine
        // and attaches the plugin.
        TreeSdkFlutterPlugin.notePendingReminderIntent(intent)
        super.onCreate(savedInstanceState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        TreeSdkFlutterPlugin.notePendingReminderIntent(intent)
    }
}
