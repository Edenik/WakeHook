package ai.wakehook.app.data

import ai.wakehook.app.domain.Alarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AlarmRepository {
    fun observeAlarms(): Flow<List<Alarm>>
    suspend fun getAll(): List<Alarm>
    suspend fun get(id: String): Alarm?
    suspend fun upsert(alarm: Alarm)
    suspend fun delete(id: String)
}

class RoomAlarmRepository(private val dao: AlarmDao) : AlarmRepository {
    override fun observeAlarms(): Flow<List<Alarm>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getAll(): List<Alarm> = dao.getAll().map { it.toDomain() }
    override suspend fun get(id: String): Alarm? = dao.getById(id)?.toDomain()
    override suspend fun upsert(alarm: Alarm) = dao.upsert(alarm.toEntity())
    override suspend fun delete(id: String) = dao.deleteById(id)
}
