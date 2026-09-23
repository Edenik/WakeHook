package ai.wakehook.app.ui

import android.content.Context
import ai.wakehook.app.alarm.AlarmScheduler
import ai.wakehook.app.data.AlarmDatabase
import ai.wakehook.app.data.AlarmRepository
import ai.wakehook.app.data.RoomAlarmRepository

class AppContainer(context: Context) {
    val repository: AlarmRepository = RoomAlarmRepository(AlarmDatabase.get(context).alarmDao())
    val scheduler: AlarmScheduler = AlarmScheduler(context)
}
