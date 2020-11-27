package br.pizao.copiloto.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.pizao.copiloto.database.model.ChatMessage

@Database(entities = [ChatMessage::class], version = 1, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {
    abstract val chatDatabaseDAO: ChatDatabaseDAO

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getInstance(context: Context): ChatDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context, ChatDatabase::class.java, "chat_database"
                    ).fallbackToDestructiveMigration()
                        .build()

                }
                return instance
            }
        }
    }
}
