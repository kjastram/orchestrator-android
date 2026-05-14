package com.orchestrator.app.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.orchestrator.app.MainActivity
import com.orchestrator.app.R
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

@AndroidEntryPoint
class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TASK_REMINDER -> handleTaskReminder(context, intent)
            Intent.ACTION_BOOT_COMPLETED -> {
                // No-op in v1: tasks store no due times across reboots
            }
        }
    }

    private fun handleTaskReminder(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(taskTitle)
            .setContentText(context.getString(R.string.notification_task_due_today))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }

    companion object {
        const val ACTION_TASK_REMINDER = "com.orchestrator.app.ACTION_TASK_REMINDER"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val CHANNEL_ID = "task_reminders"
        private const val CHANNEL_NAME = "Task Reminders"

        fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming tasks"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

object NotificationScheduler {

    fun scheduleReminder(context: Context, taskId: String, taskTitle: String, dueDate: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val date = try {
            LocalDate.parse(dueDate)
        } catch (e: Exception) {
            return
        }

        val calendar = Calendar.getInstance().apply {
            set(date.year, date.monthValue - 1, date.dayOfMonth, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Don't schedule reminders in the past
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_TASK_REMINDER
            putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_TITLE, taskTitle)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    fun cancelReminder(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_TASK_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}
