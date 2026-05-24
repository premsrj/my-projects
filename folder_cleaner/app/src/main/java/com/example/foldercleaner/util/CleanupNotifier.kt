package com.example.foldercleaner.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.foldercleaner.MainActivity
import com.example.foldercleaner.R
import com.example.foldercleaner.data.CleanupSummary
import com.example.foldercleaner.data.CleanupTrigger

object CleanupNotifier {
    private const val CHANNEL_ID = "cleanup_results"
    private const val CHANNEL_NAME = "Cleanup Results"
    private const val CHANNEL_DESCRIPTION = "Results for automatic and manual folder cleanup"

    fun showCleanupResult(
        context: Context,
        summary: CleanupSummary,
        trigger: CleanupTrigger
    ) {
        if (!NotificationPermissionHelper.hasPermission(context)) {
            return
        }

        createChannelIfNeeded(context)

        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (trigger == CleanupTrigger.Automatic) {
            "Automatic cleanup complete"
        } else {
            "Manual cleanup complete"
        }

        val text = "Deleted ${summary.deletedCount} files from ${summary.scannedCount} scanned. " +
            "Reclaimed ${SizeFormatter.formatBytes(summary.reclaimedBytes)}. " +
            "Skipped ${summary.skippedCount}, failed ${summary.failedCount}."
        val appLargeIcon = loadAppIconBitmap(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setLargeIcon(appLargeIcon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(trigger.ordinal + 2000, notification)
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESCRIPTION
        }

        manager.createNotificationChannel(channel)
    }

    private fun loadAppIconBitmap(context: Context): Bitmap? {
        val drawable = try {
            context.packageManager.getApplicationIcon(context.packageName)
        } catch (_: Throwable) {
            null
        } ?: return null

        return drawableToBitmap(drawable)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 128
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 128
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
