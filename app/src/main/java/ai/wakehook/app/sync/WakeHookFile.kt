package ai.wakehook.app.sync

import ai.wakehook.app.domain.Alarm
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** The decoded contents of a `wakehook.json` file. */
data class WakeHookFile(val version: Int, val alarms: List<Alarm>)

/** dayInt 0=Sun..6=Sat <-> repeatDays bitmask, matching [ai.wakehook.app.domain.dayBit]'s Sun=0..Sat=6 indexing. */
fun repeatDaysToInts(mask: Int): List<Int> =
    (0..6).filter { (mask and (1 shl it)) != 0 }

fun intsToRepeatDays(days: List<Int>): Int =
    days.fold(0) { acc, d -> acc or (1 shl d) }

/**
 * (De)serialization for the `wakehook.json` protocol: a self-describing file an AI agent can
 * read and write directly to add/edit/cancel alarms via Google Drive.
 */
object WakeHookJson {
    private const val PROTOCOL_VERSION = 1
    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private const val INSTRUCTIONS =
        "This file describes the alarms for the WakeHook app. To add an alarm, append an " +
            "object to \"alarms\" with a unique \"id\", \"hour\" (0-23), \"minute\" (0-59), and " +
            "optionally \"label\". Use \"daysOfWeek\" (0=Sun..6=Sat) for a weekly repeating " +
            "alarm, or \"dates\" (list of \"yyyy-MM-dd\" strings) for specific one-off dates; " +
            "omit both for a single one-time alarm. Set \"enabled\": false to disable an alarm " +
            "without deleting it, or remove its object entirely to cancel it. Set \"source\" to " +
            "identify who created the alarm (e.g. \"claude\"). The app writes back an \"ack\" " +
            "object on each alarm it has scheduled, showing its state."

    private fun exampleAlarm(): JSONObject = JSONObject().apply {
        put("id", "example-1")
        put("hour", 6)
        put("minute", 45)
        put("label", "Gym")
        put("daysOfWeek", JSONArray(listOf(1, 2, 3, 4, 5)))
        put("dates", JSONArray())
        put("enabled", true)
        put("source", "claude")
        put("version", 1)
    }

    fun encode(alarms: List<Alarm>): String {
        val root = JSONObject()
        root.put("version", PROTOCOL_VERSION)
        root.put("instructions", INSTRUCTIONS)
        root.put("example", exampleAlarm())

        val array = JSONArray()
        for (alarm in alarms) {
            array.put(encodeAlarm(alarm))
        }
        root.put("alarms", array)

        return root.toString(2)
    }

    private fun encodeAlarm(alarm: Alarm): JSONObject {
        val obj = JSONObject()
        obj.put("id", alarm.id)
        obj.put("hour", alarm.hour)
        obj.put("minute", alarm.minute)
        obj.put("label", alarm.label)
        obj.put("daysOfWeek", JSONArray(repeatDaysToInts(alarm.repeatDays)))
        obj.put("dates", JSONArray(alarm.dates.map { it.format(DATE_FORMAT) }))
        obj.put("enabled", alarm.enabled)
        obj.put("source", alarm.source)
        obj.put("version", alarm.version)
        if (alarm.snoozedUntil != null) {
            obj.put("snoozedUntil", alarm.snoozedUntil)
        } else {
            obj.put("snoozedUntil", JSONObject.NULL)
        }
        if (alarm.ackState.isNotEmpty()) {
            val ack = JSONObject()
            ack.put("state", alarm.ackState)
            ack.put("at", alarm.ackAt)
            ack.put("error", alarm.ackError)
            obj.put("ack", ack)
        }
        return obj
    }

    fun decode(json: String): WakeHookFile {
        val root = if (json.isBlank()) JSONObject() else JSONObject(json)
        val version = root.optInt("version", PROTOCOL_VERSION)
        val array = root.optJSONArray("alarms") ?: JSONArray()

        val alarms = mutableListOf<Alarm>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            alarms.add(decodeAlarm(obj))
        }

        return WakeHookFile(version = version, alarms = alarms)
    }

    private fun decodeAlarm(obj: JSONObject): Alarm {
        val id = if (obj.has("id") && !obj.isNull("id")) obj.getString("id") else java.util.UUID.randomUUID().toString()
        val hour = obj.optInt("hour", 7)
        val minute = obj.optInt("minute", 0)
        val label = obj.optString("label", "")

        val daysArray = obj.optJSONArray("daysOfWeek")
        val days = mutableListOf<Int>()
        if (daysArray != null) {
            for (i in 0 until daysArray.length()) {
                days.add(daysArray.optInt(i))
            }
        }
        val repeatDays = intsToRepeatDays(days)

        val datesArray = obj.optJSONArray("dates")
        val dates = mutableListOf<LocalDate>()
        if (datesArray != null) {
            for (i in 0 until datesArray.length()) {
                val s = datesArray.optString(i, "")
                if (s.isNotEmpty()) {
                    dates.add(LocalDate.parse(s, DATE_FORMAT))
                }
            }
        }

        val enabled = obj.optBoolean("enabled", true)
        val source = obj.optString("source", "local")
        val version = obj.optLong("version", 0)
        val snoozedUntil = if (obj.has("snoozedUntil") && !obj.isNull("snoozedUntil")) {
            obj.optLong("snoozedUntil")
        } else {
            null
        }

        var ackState = ""
        var ackAt = ""
        var ackError = ""
        val ack = obj.optJSONObject("ack")
        if (ack != null) {
            ackState = ack.optString("state", "")
            ackAt = ack.optString("at", "")
            ackError = ack.optString("error", "")
        }

        return Alarm(
            id = id,
            label = label,
            hour = hour,
            minute = minute,
            repeatDays = repeatDays,
            dates = dates,
            enabled = enabled,
            source = source,
            version = version,
            ackState = ackState,
            ackAt = ackAt,
            ackError = ackError,
            snoozedUntil = snoozedUntil,
        )
    }
}
