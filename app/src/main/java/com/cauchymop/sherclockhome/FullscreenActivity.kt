package com.cauchymop.sherclockhome

import android.Manifest.permission
import android.Manifest.permission.READ_CALENDAR
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.provider.BaseColumns
import android.provider.CalendarContract.Events.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat.checkSelfPermission
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_fullscreen.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val MY_PERMISSIONS_REQUEST_READ_CONTACTS = 123
private val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
private val dateFormat = DateFormat.getDateInstance(DateFormat.LONG)
private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

class FullscreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen)
    }

    override fun onStart() {
        super.onStart()
        updateTime()
        requestPermissionIfNeeded()
    }

    private fun updateTime() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        val now = Date()
        val today = dayFormat.format(now).capitalize()
        day.text = today
        date.text = dateFormat.format(now)
        time.text = timeFormat.format(now)
        Handler().postDelayed({ updateTime() }, TimeUnit.SECONDS.toMillis(5))

        if (checkSelfPermission(this, permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            updateCalendar(now, today)
        }
    }

    private fun requestPermissionIfNeeded() {
        if (checkSelfPermission(this, READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(READ_CALENDAR), MY_PERMISSIONS_REQUEST_READ_CONTACTS)
        }
    }

    private fun updateCalendar(now: Date, today: String) {
        try {
            val startDay = now.time
            val endDay = startDay + TimeUnit.DAYS.toMillis(1)
            val projection = arrayOf(BaseColumns._ID, TITLE, DTSTART)
            val selection = "$DTSTART >= ? AND $DTSTART<= ?"
            val selectionArgs = arrayOf(java.lang.Long.toString(startDay), java.lang.Long.toString(endDay))
            contentResolver.query(CONTENT_URI, projection, selection, selectionArgs, "$DTSTART ASC").use {
                if (it.moveToFirst()) {
                    val eventTitle = it.getString(it.getColumnIndex(TITLE))
                    val eventStart = it.getLong(it.getColumnIndex(DTSTART))
                    val eventDay = dayFormat.format(eventStart).capitalize()
                    val eventTime = timeFormat.format(eventStart)
                    val formatId = if (eventDay == today) R.string.today_format else R.string.tomorrow_format
                    calendarEvent.text = getString(formatId, eventTime, eventTitle)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
