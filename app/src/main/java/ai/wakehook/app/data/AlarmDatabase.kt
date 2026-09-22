package ai.wakehook.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.WeakHashMap
import java.util.concurrent.Executors

@Database(entities = [AlarmEntity::class], version = 1, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        // Keyed by the application Context instance rather than a single static field.
        // In production there is exactly one application Context per process, so this
        // behaves identically to a plain singleton. Under Robolectric, each test gets its
        // own fresh Application instance (even when Robolectric reuses a classloader
        // sandbox across test classes for performance), so keying by that instance keeps
        // each test's database isolated instead of reusing a Room object whose connection
        // pool/executors were built against a previous, now-torn-down sandbox — which was
        // otherwise crashing a second Robolectric test class that also called get() with
        // "Illegal connection pointer" from Robolectric's SQLite shadow.
        private val INSTANCES = WeakHashMap<Context, AlarmDatabase>()

        fun get(context: Context): AlarmDatabase {
            val appContext = context.applicationContext
            synchronized(this) {
                return INSTANCES.getOrPut(appContext) {
                    Room.databaseBuilder(appContext, AlarmDatabase::class.java, "wakehook.db")
                        .setQueryExecutor(Executors.newSingleThreadExecutor())
                        .setTransactionExecutor(Executors.newSingleThreadExecutor())
                        .build()
                }
            }
        }
    }
}
