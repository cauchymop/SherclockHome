package com.cauchymop.sherclockhome

import android.Manifest.permission
import android.Manifest.permission.READ_CALENDAR
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.CalendarContract.Events.TITLE
import android.provider.CalendarContract.Instances
import android.provider.CalendarContract.Instances.BEGIN
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

    private val timer = Timer()
    private lateinit var timeUpdaterTask: TimerTask
    private lateinit var calendarUpdaterTask: TimerTask
    private val updateTimeFunction = { updateTime() }
    private val updateCalendarFunction = {
        if (checkSelfPermission(this, permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            updateCalendar()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen)
    }

    override fun onResume() {
        super.onResume()
        timeUpdaterTask = object : TimerTask() {
            override fun run() {
                runOnUiThread(updateTimeFunction)
            }
        }
        calendarUpdaterTask = object : TimerTask() {
            override fun run() {
                runOnUiThread(updateCalendarFunction)
            }
        }
        timer.schedule(timeUpdaterTask, Date(), TimeUnit.SECONDS.toMillis(5))
        timer.schedule(calendarUpdaterTask, Date(), TimeUnit.MINUTES.toMillis(30))
    }

    override fun onPause() {
        super.onPause()
        timeUpdaterTask.cancel()
        calendarUpdaterTask.cancel()
        timer.purge()
    }

    override fun onStart() {
        super.onStart()
        requestPermissionIfNeeded()
    }


    private fun updateTime() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        val now = Date()
        val today = dayFormat.format(now).capitalize()
        day.text = today
        val dateString = dateFormat.format(now)
        val timeString = timeFormat.format(now)
        date.text = dateString
        time.text = timeString
    }

    private fun requestPermissionIfNeeded() {
        if (checkSelfPermission(this, READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(READ_CALENDAR), MY_PERMISSIONS_REQUEST_READ_CONTACTS)
        }
    }

    private fun updateCalendar() {
        try {
            val now = Date()
            val today = dayFormat.format(now).capitalize()
            val startMillis = now.time
            val endMillis = startMillis + TimeUnit.DAYS.toMillis(1)
            val projection = arrayOf(BaseColumns._ID, TITLE, BEGIN)
            val selection = "$BEGIN >= ?"
            // Construct the query with the desired date range.
            val builder = Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startMillis)
            ContentUris.appendId(builder, endMillis)
            val selectionArgs = arrayOf("$startMillis")

            contentResolver.query(builder.build(), projection, selection, selectionArgs, "$BEGIN ASC").use {
                if (it.moveToFirst()) {
                    val eventTitle = it.getString(it.getColumnIndex(TITLE))
                    val eventStart = it.getLong(it.getColumnIndex(BEGIN))
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
