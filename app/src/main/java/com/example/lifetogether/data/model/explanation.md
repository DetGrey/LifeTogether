
# EXAMPLE:
```
package com.example.aca.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long
)
```
