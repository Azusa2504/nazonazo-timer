
package com.example.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.util.Log

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("TimerReceiver", "Alarm received!")
        val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("timer_finished", true).apply()
        val mediaPlayer = MediaPlayer.create(context, R.raw.stop)
        mediaPlayer.start()
    }

}
