package com.dru.tree_sdk_flutter_example

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.EditText
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Catches a crash, writes it down, and shows it on the next launch.
 *
 * FOR TESTER BUILDS ONLY - delete this file and its two calls in MainActivity before release.
 *
 * A tester on someone else's phone cannot run logcat, so a crash on their device was a dead end:
 * "it closes itself" is all that ever came back. The handler below stores the stack trace on disk
 * and the next launch offers to share it, which turns that into something they can send.
 */
object CrashReport {

    private const val TAG = "TreeSdkCrash"
    private const val FILE_NAME = "last-crash.txt"

    /** Installs the handler. Call before anything else in onCreate. */
    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(appContext, thread, error) }
            // Hands back to the system handler so the crash still behaves like a crash.
            previous?.uncaughtException(thread, error)
        }
    }

    /** Shows what was caught last time, if anything, and offers to share it. */
    fun showIfPresent(context: Context) {
        val file = file(context)
        if (!file.exists() || file.length() == 0L) return
        val text = runCatching { file.readText() }.getOrNull() ?: return

        // Selectable, so the whole trace can be copied even on a phone with no share target.
        val field = EditText(context).apply {
            setText(text)
            setTextIsSelectable(true)
            isFocusable = false
            setPadding(40, 30, 40, 30)
            textSize = 11f
        }

        AlertDialog.Builder(context)
            .setTitle("The app crashed last time")
            .setView(field)
            .setPositiveButton("Share") { _, _ ->
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Tree SDK crash")
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                runCatching { context.startActivity(Intent.createChooser(send, "Send the crash")) }
                file.delete()
            }
            .setNegativeButton("Dismiss") { _, _ -> file.delete() }
            .setCancelable(false)
            .show()
    }

    private fun write(context: Context, thread: Thread, error: Throwable) {
        val stack = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val when_ = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val report = buildString {
            appendLine("time: $when_")
            appendLine("thread: ${thread.name}")
            appendLine("device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine()
            append(stack)
        }
        Log.e(TAG, report)
        file(context).writeText(report)
    }

    private fun file(context: Context): File =
        File(context.applicationContext.filesDir, FILE_NAME)
}
