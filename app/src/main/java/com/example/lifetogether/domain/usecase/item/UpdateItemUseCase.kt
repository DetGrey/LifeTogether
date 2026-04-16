package com.example.lifetogether.domain.usecase.item

import android.util.Log
import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.result.Result
import javax.inject.Inject

class UpdateItemUseCase @Inject constructor(
    private val remoteListRepository: RemoteListRepositoryImpl,
) {
    private companion object {
        const val TAG = "UpdateItemUseCase"
    }

    suspend operator fun invoke(
        item: Item,
        listName: String,
    ): ResultListener {
        Log.d(TAG, "invoke listName=$listName id=${item.id} type=${item::class.simpleName}")
        return when (val result = remoteListRepository.updateItem(item, listName)) {
            is Result.Success -> {
                Log.d(TAG, "invoke success listName=$listName id=${item.id}")
                ResultListener.Success
            }
            is Result.Failure -> {
                Log.e(
                    TAG,
                    "invoke failure listName=$listName id=${item.id} message=${result.error}"
                )
                ResultListener.Failure(result.error)
            }
        }
    }
}
