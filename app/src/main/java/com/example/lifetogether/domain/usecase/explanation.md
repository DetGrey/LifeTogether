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