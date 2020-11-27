package br.pizao.copiloto.database

import android.content.Context
import android.graphics.PointF
import android.util.Log
import androidx.lifecycle.LiveData
import br.pizao.copiloto.database.model.ChatMessage
import br.pizao.copiloto.database.model.LeftEye
import br.pizao.copiloto.database.model.RightEye
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ChatRepository {
    private lateinit var chatDatabaseDao: ChatDatabaseDAO
    private lateinit var leftEyeDAO: LeftEyeDAO
    private lateinit var rightEyeDAO: RightEyeDAO

    lateinit var messages: LiveData<List<ChatMessage>>; private set

    var controlledId: Long = 0

    fun init(context: Context) {
        chatDatabaseDao = ChatDatabase.getInstance(context).chatDatabaseDAO
        messages = chatDatabaseDao.getChatMessages()

        val eyeDatabase = EyeDatabase.getInstance(context)
        Log.d("CASDEBUG", eyeDatabase.openHelper.writableDatabase.path)
        leftEyeDAO = eyeDatabase.leftEyeDAO
        rightEyeDAO = eyeDatabase.rightEyeDAO
    }

    fun addMessage(chatMessage: ChatMessage) {
        synchronized(this) {
            chatMessage.id = controlledId++
            CoroutineScope(Dispatchers.IO).launch {
                insertMessage(chatMessage)
            }
        }
    }

    fun updateMessage(chatMessage: ChatMessage) {
        CoroutineScope(Dispatchers.IO).launch {
            chatDatabaseDao.update(chatMessage)
        }
    }

    fun clearDatabase() = chatDatabaseDao.clear()

    @Suppress("BlockingMethodInNonBlockingContext")
    fun addPosition(
        time: Long,
        leftPoints: List<PointF>,
        rightPoints: List<PointF>,
        anglex: Float,
        angley: Float,
        anglez: Float
    ) {
        synchronized(this) {
            CoroutineScope(Dispatchers.IO).launch {
                leftPoints.forEachIndexed { index, point ->
                    leftEyeDAO.insert(
                        LeftEye(
                            time = time,
                            index = index,
                            x = point.x,
                            y = point.y,
                            anglex = anglex,
                            angley = angley,
                            anglez = anglez
                        )
                    )
                }
                rightPoints.forEachIndexed { index, point ->
                    rightEyeDAO.insert(
                        RightEye(
                            time = time,
                            index = index,
                            x = point.x,
                            y = point.y,
                            anglex = anglex,
                            angley = angley,
                            anglez = anglez
                        )
                    )
                }
            }
        }
    }

    private suspend fun insertMessage(chatMessage: ChatMessage) =
        chatDatabaseDao.insert(chatMessage)
}
