package com.example.volumeharmanwidget

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.IBinder
import android.provider.Settings
import android.widget.RemoteViews
import android.appwidget.AppWidgetManager

class VolumeChangeService : Service() {

    private lateinit var prefs: SharedPreferences
    private val maxVolume = 39
    private val minVolume = 0

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("volume_prefs", Context.MODE_PRIVATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val direction = intent?.getStringExtra("direction") // "up" or "down"
        val isInit = intent?.getBooleanExtra("init", false) ?: false

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Получаем текущую громкость
        val currentVolume = getCurrentVolume()

        var newVolume = currentVolume
        if (!isInit && direction != null) {
            newVolume = when (direction) {
                "up" -> (currentVolume + 1).coerceAtMost(maxVolume)
                "down" -> (currentVolume - 1).coerceAtLeast(minVolume)
                else -> currentVolume
            }

            // Если значение изменилось, отправляем Intent в Harman
            if (newVolume != currentVolume) {
                sendVolumeToHarman(newVolume)
                // Сохраняем последнее установленное значение на всякий случай
                prefs.edit().putInt("last_volume", newVolume).apply()
            }
        }

        // Обновляем виджет с новым значением
        updateWidget(widgetId, newVolume)

        stopSelf()
        return START_NOT_STICKY
    }

    private fun getCurrentVolume(): Int {
        // 1. Пробуем прочитать из Settings.Global
        try {
            val volume = Settings.Global.getInt(contentResolver, "android.car.VOLUME_MUSIC", -1)
            if (volume in 0..maxVolume) {
                return volume
            }
        } catch (e: SecurityException) {
            // нет прав — игнорируем
        }

        // 2. Используем AudioManager
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        // Предполагаем, что шкала AudioManager совпадает с 0..39, но может быть другой.
        // Если нужно масштабировать, добавим логику.
        return volume.coerceIn(minVolume, maxVolume)

        // 3. Если совсем ничего не вышло, берём последнее сохранённое
        // (как запасной вариант)
        // return prefs.getInt("last_volume", 20)
    }

    private fun sendVolumeToHarman(volume: Int) {
        val intent = Intent().apply {
            `package` = "com.harman.gwm.hmi.setting"
            component = ComponentName("com.harman.gwm.hmi.setting", "com.harman.gwm.hmi.setting.service.SettingService")
            putExtra("keyCommand", "adjustVolume")
            putExtra("adjustVolume", volume.toString()) // или volume как Int? В макросе строка.
        }
        try {
            startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateWidget(widgetId: Int, volume: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val views = RemoteViews(packageName, R.layout.widget_layout)
        views.setTextViewText(R.id.volume_text, volume.toString())
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}