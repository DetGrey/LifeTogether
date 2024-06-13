# EXAMPLE:
```
package com.example.aca.domain.repository

import com.example.aca.domain.model.Message

interface MessageRepository {

    fun sendMessage(message: Message)
    fun getMessageHistory(userId: String): List<Message>

}
```
