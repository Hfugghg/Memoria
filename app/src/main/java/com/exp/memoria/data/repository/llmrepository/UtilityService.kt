package com.exp.memoria.data.repository.llmrepository

import com.exp.memoria.data.remote.api.ModelDetail

interface UtilityService {
    suspend fun getSummary(text: String, responseSchema: String?): String
    suspend fun getEmbedding(text: String): List<Float>
    suspend fun getAvailableModels(apiKey: String, pageToken: String?): Pair<List<ModelDetail>, String?>
}