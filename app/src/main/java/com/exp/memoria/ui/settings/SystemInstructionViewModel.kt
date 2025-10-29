package com.exp.memoria.ui.settings

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exp.memoria.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class SystemInstructionViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _conversationId = MutableStateFlow<String?>(null)

    private val _systemInstruction = MutableStateFlow(SystemInstruction(parts = emptyList()))
    val systemInstruction = _systemInstruction.asStateFlow()

    init {
        viewModelScope.launch {
            _conversationId.value = savedStateHandle.get<String>("conversationId")
            _conversationId.value?.let { loadSystemInstruction(it) }
        }
    }

    private fun loadSystemInstruction(conversationId: String) {
        viewModelScope.launch {
            val header = memoryRepository.getConversationHeaderById(conversationId)
            if (header != null) {
                _systemInstruction.value = parseSystemInstructionJson(header.systemInstruction)
            }
        }
    }

    private fun parseSystemInstructionJson(jsonString: String?): SystemInstruction {
        if (jsonString.isNullOrBlank()) return SystemInstruction(parts = emptyList())
        return try {
            Json.decodeFromString<SystemInstruction>(jsonString)
        } catch (e: Exception) {
            Log.e("SystemInstructionViewModel", "Failed to parse System Instruction JSON: ${e.message}")
            SystemInstruction(parts = emptyList())
        }
    }

    fun addSystemInstructionPart(text: String) {
        if (text.isBlank()) return
        val newPart = Part(text = text)
        val updatedParts = _systemInstruction.value.parts + newPart
        _systemInstruction.value = SystemInstruction(parts = updatedParts)
        updateSystemInstructionInRepository()
    }

    fun updateSystemInstructionPart(index: Int, newText: String) {
        if (index < 0 || index >= _systemInstruction.value.parts.size) return
        if (newText.isBlank()) return
        val updatedParts = _systemInstruction.value.parts.toMutableList().apply {
            this[index] = this[index].copy(text = newText)
        }
        _systemInstruction.value = SystemInstruction(parts = updatedParts)
        updateSystemInstructionInRepository()
    }

    fun removeSystemInstructionPart(index: Int) {
        if (index < 0 || index >= _systemInstruction.value.parts.size) return
        val updatedParts = _systemInstruction.value.parts.toMutableList().apply {
            removeAt(index)
        }
        _systemInstruction.value = SystemInstruction(parts = updatedParts)
        updateSystemInstructionInRepository()
    }

    private fun updateSystemInstructionInRepository() {
        viewModelScope.launch {
            _conversationId.value?.let { conversationId ->
                val jsonString = Json.encodeToString(_systemInstruction.value)
                memoryRepository.updateSystemInstruction(conversationId, jsonString)
            }
        }
    }
}
