package com.exp.memoria.domain.usecase

import com.exp.memoria.data.repository.MemoryRepository
import javax.inject.Inject

class GetChatResponseUseCase @Inject constructor(
    private val repository: MemoryRepository
) {
    suspend operator fun invoke(query: String): String {
        return repository.getChatResponse(query)
    }
}