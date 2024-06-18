# EXAMPLE:
## with DI (Hilt)
```
package com.example.aca.domain.usecase

import com.example.aca.domain.model.Message
import com.example.aca.domain.repository.MessageRepository

class SendMessageUseCase(private val messageRepository: MessageRepository) {

    operator fun invoke(message: Message) {
        messageRepository.sendMessage(message)
    }
}
```
## Without DI
```
class SendMessageUseCase {
    private val messageRepository = MessageRepositoryImpl()

    operator fun invoke(message: Message) {
        messageRepository.sendMessage(message)
    }
}
```

# Separate Repositories for online and offline 
Create separate repository implementations for each data source, like _LocalItemsRepository_ for Room 
and _RemoteItemsRepository_ for Firestore. This keeps the implementations clean and focused on a single data source.

For maintainability and separation of concerns, it’s often better to keep them separate and have 
another layer (like a service or use case) coordinate between them.