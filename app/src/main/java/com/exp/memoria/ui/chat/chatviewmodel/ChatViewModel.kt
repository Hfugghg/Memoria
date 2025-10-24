package com.exp.memoria.ui.chat.chatviewmodel

import android.app.Application
import android.net.Uri // 导入 Uri 类
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exp.memoria.data.local.entity.MessageFile
import com.exp.memoria.data.model.FileAttachment
import com.exp.memoria.data.repository.MemoryRepository
import com.exp.memoria.data.repository.SettingsRepository
import com.exp.memoria.domain.usecase.GetChatResponseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    getChatResponseUseCase: GetChatResponseUseCase,
    private val memoryRepository: MemoryRepository,
    settingsRepository: SettingsRepository,
    private val application: Application,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: String = savedStateHandle.get<String>("conversationId") ?: UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow(ChatState())
    val uiState = _uiState.asStateFlow()

    private val _totalTokenCount = MutableStateFlow<Int?>(null)
    val totalTokenCount = _totalTokenCount.asStateFlow()

    private var currentPage = 0
    private val pageSize = 50

    private val responseHandler: ChatResponseHandler

    init {
        responseHandler = ChatResponseHandler(
            getChatResponseUseCase = getChatResponseUseCase,
            memoryRepository = memoryRepository,
            settingsRepository = settingsRepository,
            application = application,
            conversationId = conversationId,
            _uiState = _uiState,
            _totalTokenCount = _totalTokenCount,
            coroutineScope = viewModelScope
        )

        Log.d("ChatViewModel", "使用 conversationId 初始化: $conversationId")
        loadMoreMessages()
        viewModelScope.launch {
            val header = memoryRepository.getConversationHeaderById(conversationId)
            header?.let {
                _totalTokenCount.value = it.totalTokenCount
            }
        }
    }

    fun loadMoreMessages() {
        viewModelScope.launch {
            Log.d("ChatViewModel", "[诊断] 为 conversationId: $conversationId, 页面: $currentPage 加载更多消息")
            val memories = memoryRepository.getRawMemories(conversationId, pageSize, currentPage * pageSize)
            Log.d(
                "ChatViewModel",
                "[诊断] 从数据库为 conversationId: $conversationId 获取了 ${memories.size} 条原始记忆。"
            )

            val chatMessages = memories.reversed().map { memory ->
                val attachments = memory.id?.let { memoryId ->
                    val messageFiles = memoryRepository.getMessageFilesForMemory(memoryId)
                    messageFiles.mapNotNull { file ->
                        convertBase64ToUri(file.fileContentBase64, file.fileName)
                    }
                } ?: emptyList()

                ChatMessage(
                    id = UUID.nameUUIDFromBytes(memory.id.toString().toByteArray()),
                    text = memory.text,
                    isFromUser = memory.sender == "user",
                    memoryId = memory.id,
                    attachments = attachments
                )
            }
            Log.d("ChatViewModel", "[诊断] 转换后，准备将 ${chatMessages.size} 条新消息添加到UI。")

            _uiState.update { currentState ->
                val newMessages = chatMessages + currentState.messages
                Log.d(
                    "ChatViewModel",
                    "[诊断] 更新UI状态。之前消息数: ${currentState.messages.size}，之后总消息数: ${newMessages.size}。"
                )
                currentState.copy(messages = newMessages)
            }
            currentPage++
        }
    }

    fun updateMessage(messageId: UUID, newText: String) {
        viewModelScope.launch {
            var memoryIdToUpdate: Long? = null
            var originalText: String? = null

            _uiState.update { currentState ->
                val updatedMessages = currentState.messages.map { message ->
                    if (message.id == messageId) {
                        memoryIdToUpdate = message.memoryId
                        originalText = message.text
                        message.copy(text = newText)
                    } else {
                        message
                    }
                }
                currentState.copy(messages = updatedMessages)
            }

            memoryIdToUpdate?.let { memoryId ->
                Log.d("ChatViewModel", "准备更新数据库... Memory ID: $memoryId, 新文本: $newText")
                try {
                    memoryRepository.updateMemoryText(memoryId, newText)
                    Log.d("ChatViewModel", "数据库更新成功。")
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "数据库更新失败。", e)
                    _uiState.update { currentState ->
                        val restoredMessages = currentState.messages.map { message ->
                            if (message.id == messageId) {
                                message.copy(text = originalText ?: newText)
                            } else {
                                message
                            }
                        }
                        currentState.copy(messages = restoredMessages)
                    }
                }
            }
        }
    }

    fun regenerateResponse(aiMessageId: UUID) {
        viewModelScope.launch {
            val messages = _uiState.value.messages
            val aiMessageIndex = messages.indexOfFirst { it.id == aiMessageId }

            if (aiMessageIndex > 0) {
                val aiMessage = messages[aiMessageIndex]
                val userMessage = messages[aiMessageIndex - 1]

                if (aiMessage.memoryId != null && userMessage.isFromUser) {
                    Log.d(
                        "ChatViewModel",
                        "regenerateResponse: 准备从数据库删除 memoryId >= ${aiMessage.memoryId} 的消息"
                    )
                    memoryRepository.deleteFrom(conversationId, aiMessage.memoryId)
                    Log.d("ChatViewModel", "regenerateResponse: 数据库删除完成")

                    _uiState.update { currentState ->
                        val messagesAfterDeletion = currentState.messages.take(aiMessageIndex)
                        Log.d(
                            "ChatViewModel",
                            "regenerateResponse: UI状态更新，保留 ${messagesAfterDeletion.size} 条消息"
                        )
                        currentState.copy(
                            messages = messagesAfterDeletion,
                            isLoading = true
                        )
                    }

                    Log.d("ChatViewModel", "regenerateResponse: 重新发送用户消息: ${userMessage.text}")
                    responseHandler.generateAiResponse(
                        queryForLlm = null,
                        userQueryForSaving = userMessage.text,
                        attachments = emptyList()
                    )
                } else {
                    Log.w(
                        "ChatViewModel",
                        "regenerateResponse: 无法重说。AI消息没有memoryId或前一条不是用户消息。 memoryId is ${aiMessage.memoryId}, isFromUser is ${userMessage.isFromUser}"
                    )
                }
            } else {
                Log.w("ChatViewModel", "regenerateResponse: 无法重说。未找到AI消息或它是第一条消息。")
            }
        }
    }

    fun sendMessage(query: String) {
        val currentUiState = _uiState.value
        if (query.isBlank() && currentUiState.selectedFiles.isEmpty()) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID(),
            text = query,
            isFromUser = true,
            attachments = currentUiState.selectedFiles
        )

        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + userMessage,
                isLoading = true
            )
        }

        _totalTokenCount.value = null

        viewModelScope.launch {
            // 1. 将URI转换为附件
            val attachments = if (currentUiState.selectedFiles.isNotEmpty()) {
                convertUrisToAttachments(currentUiState.selectedFiles)
            } else {
                emptyList()
            }

            // 2. 准备用于保存到数据库的用户消息文本 (核心修复点：不再附加文件名)
            val userQueryForSaving = query

            // 3. 调用 responseHandler 处理响应生成和数据保存
            responseHandler.generateAiResponse(
                queryForLlm = query,
                userQueryForSaving = userQueryForSaving, // 使用原始查询文本
                attachments = attachments
            )

            // 4. 发送成功后，清空selectedFiles列表
            _uiState.update { it.copy(selectedFiles = emptyList()) }
        }
    }

    /**
     * 处理图片选择的URI。
     * @param uri 选择的图片URI，可能为null。
     */
    fun handleImageSelection(uri: Uri?) {
        uri?.let {
            _uiState.update { currentState ->
                currentState.copy(selectedFiles = currentState.selectedFiles + it)
            }
            Log.d("ChatViewModel", "已添加图片 URI: $it")
        }
    }

    /**
     * 处理文件选择的URI。
     * @param uri 选择的文件URI，可能为null。
     */
    fun handleFileSelection(uri: Uri?) {
        uri?.let {
            _uiState.update { currentState ->
                currentState.copy(selectedFiles = currentState.selectedFiles + it)
            }
            Log.d("ChatViewModel", "已添加文件 URI: $it")
        }
    }

    fun deleteAttachment(messageId: UUID, uri: Uri) {
        viewModelScope.launch {
            val message = _uiState.value.messages.find { it.id == messageId } ?: return@launch
            val memoryId = message.memoryId ?: return@launch

            val fileName = getFileNameFromUri(uri)
            val files = memoryRepository.getMessageFilesForMemory(memoryId)
            val fileToDelete = files.find { it.fileName == fileName }

            fileToDelete?.let {
                memoryRepository.deleteMessageFile(it)
                _uiState.update { currentState ->
                    val updatedMessages = currentState.messages.map { msg ->
                        if (msg.id == messageId) {
                            msg.copy(attachments = msg.attachments.filter { it != uri })
                        } else {
                            msg
                        }
                    }
                    currentState.copy(messages = updatedMessages)
                }
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        application.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        return name
    }

    private suspend fun convertUrisToAttachments(uris: List<Uri>): List<FileAttachment> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            try {
                var fileName = "unknown"
                application.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                }
                val fileType = application.contentResolver.getType(uri)

                val inputStream = application.contentResolver.openInputStream(uri) ?: return@mapNotNull null
                val bytes = inputStream.use { it.readBytes() }
                val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)

                FileAttachment(base64Data, fileName, fileType)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "无法将URI转换为附件: $uri", e)
                null
            }
        }
    }

    private suspend fun convertBase64ToUri(base64Data: String, fileName: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val file = File(application.cacheDir, fileName)
            val decodedBytes = Base64.decode(base64Data, Base64.NO_WRAP)
            file.writeBytes(decodedBytes)
            FileProvider.getUriForFile(application, "${application.packageName}.provider", file)
        } catch (e: Exception) {
            Log.e("ChatViewModel", "无法将Base64转换为URI: $fileName", e)
            null
        }
    }
}
