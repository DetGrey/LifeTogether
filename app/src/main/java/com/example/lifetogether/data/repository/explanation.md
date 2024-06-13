# EXAMPLE:
```
package com.example.aca.data.repository

import com.example.aca.data.model.MessageEntity
import com.example.aca.domain.repository.MessageRepository

class MessageRepositoryImpl : MessageRepository {

    override fun sendMessage(message: MessageEntity) {
    // Code to send message using Firebase Realtime Database
    }
    
    override fun getMessageHistory(userId: String): List<MessageEntity> {
        // Code to retrieve message history from local database or Firebase
        return emptyList()
    }
}
```