package com.example.volumeharmanwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class VolumeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_VOLUME_UP, ACTION_VOLUME_DOWN -> {
                val widgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                val direction = if (intent.action == ACTION_VOLUME_UP) "up" else "down"
                // Запускаем сервис для изменения громкости
                val serviceIntent = Intent(context, VolumeChangeService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    putExtra("direction", direction)
                }
                context.startService(serviceIntent)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // Создаём PendingIntent для кнопки "+"
        val intentUp = Intent(context, VolumeWidgetProvider::class.java).apply {
            action = ACTION_VOLUME_UP
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val pendingUp = PendingIntent.getBroadcast(
            context, appWidgetId, intentUp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.button_plus, pendingUp)

        // Создаём PendingIntent для кнопки "-"
        val intentDown = Intent(context, VolumeWidgetProvider::class.java).apply {
            action = ACTION_VOLUME_DOWN
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val pendingDown = PendingIntent.getBroadcast(
            context, appWidgetId + 1000, intentDown,  // другой requestCode
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.button_minus, pendingDown)

        // Устанавливаем начальное значение громкости (будет обновлено позже)
        views.setTextViewText(R.id.volume_text, "?")

        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Запускаем сервис, чтобы сразу получить актуальную громкость и обновить виджет
        val initIntent = Intent(context, VolumeChangeService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra("init", true)
        }
        context.startService(initIntent)
    }

    companion object {
        const val ACTION_VOLUME_UP = "com.example.volumeharmanwidget.VOLUME_UP"
        const val ACTION_VOLUME_DOWN = "com.example.volumeharmanwidget.VOLUME_DOWN"
    }
}