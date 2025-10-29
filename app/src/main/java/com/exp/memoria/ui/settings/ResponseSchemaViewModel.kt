package com.exp.memoria.ui.settings

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exp.memoria.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import javax.inject.Inject

@HiltViewModel
class ResponseSchemaViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _conversationId = MutableStateFlow<String?>(null)

    private val _responseSchema = MutableStateFlow("")
    val responseSchema = _responseSchema.asStateFlow()

    private val _isGraphicalSchemaMode = MutableStateFlow(false)
    val isGraphicalSchemaMode = _isGraphicalSchemaMode.asStateFlow()

    private val _graphicalSchemaProperties = MutableStateFlow<List<JsonSchemaProperty>>(emptyList())
    val graphicalSchemaProperties = _graphicalSchemaProperties.asStateFlow()

    private val _draftProperty = MutableStateFlow(JsonSchemaProperty(id = System.currentTimeMillis()))
    val draftProperty = _draftProperty.asStateFlow()

    init {
        viewModelScope.launch {
            _conversationId.value = savedStateHandle.get<String>("conversationId")
            _conversationId.value?.let { loadResponseSchema(it) }
        }

        viewModelScope.launch {
            _isGraphicalSchemaMode.collect { isGraphicalMode ->
                if (isGraphicalMode) {
                    _draftProperty.value = JsonSchemaProperty(id = System.currentTimeMillis())
                }
            }
        }
    }

    private fun loadResponseSchema(conversationId: String) {
        viewModelScope.launch {
            val header = memoryRepository.getConversationHeaderById(conversationId)
            if (header != null) {
                _responseSchema.value = header.responseSchema ?: ""
                _graphicalSchemaProperties.value = parseJsonToGraphicalSchema(header.responseSchema)
            }
        }
    }

    fun onResponseSchemaChange(responseSchema: String) {
        _responseSchema.value = responseSchema
        _graphicalSchemaProperties.value = parseJsonToGraphicalSchema(responseSchema)
        viewModelScope.launch {
            _conversationId.value?.let { conversationId ->
                memoryRepository.updateResponseSchema(conversationId, responseSchema)
            }
        }
    }

    fun onToggleGraphicalSchemaMode() {
        _isGraphicalSchemaMode.update { !it }
        _draftProperty.value = JsonSchemaProperty(id = System.currentTimeMillis())
    }

    fun addGraphicalSchemaProperty() {
        val propertyToAdd = _draftProperty.value
        if (propertyToAdd.name.isNotBlank() && !_graphicalSchemaProperties.value.any { it.name == propertyToAdd.name }) {
            val newList = _graphicalSchemaProperties.value + propertyToAdd.copy(id = System.currentTimeMillis())
            _graphicalSchemaProperties.value = newList
            updateResponseSchemaFromGraphical()
            _draftProperty.value = JsonSchemaProperty(id = System.currentTimeMillis())
        } else if (propertyToAdd.name.isNotBlank()) {
            Log.w("ResponseSchemaViewModel", "Attempted to add a property with an existing name: ${propertyToAdd.name}")
        } else {
            Log.w("ResponseSchemaViewModel", "Attempted to add a property with an empty name.")
        }
    }

    fun updateGraphicalSchemaProperty(updatedProperty: JsonSchemaProperty) {
        val newList = _graphicalSchemaProperties.value.map { if (it.id == updatedProperty.id) updatedProperty else it }
        _graphicalSchemaProperties.value = newList
        updateResponseSchemaFromGraphical()
    }

    fun onDraftPropertyChange(updatedProperty: JsonSchemaProperty) {
        _draftProperty.value = updatedProperty
    }

    fun removeGraphicalSchemaProperty(propertyId: Long) {
        val newList = _graphicalSchemaProperties.value.filter { it.id != propertyId }
        _graphicalSchemaProperties.value = newList
        updateResponseSchemaFromGraphical()
    }

    private fun updateResponseSchemaFromGraphical() {
        val newSchema = convertGraphicalSchemaToJson(_graphicalSchemaProperties.value)
        _responseSchema.value = newSchema
        viewModelScope.launch {
            _conversationId.value?.let { conversationId ->
                memoryRepository.updateResponseSchema(conversationId, newSchema)
            }
        }
    }

    private fun convertGraphicalSchemaToJson(properties: List<JsonSchemaProperty>): String {
        if (properties.isEmpty()) {
            return ""
        }

        val propertiesMap = properties.associate { prop ->
            prop.name to buildJsonObject {
                if (prop.name.isBlank()) return@associate "" to buildJsonObject {}

                put("type", prop.type.name.lowercase())
                if (prop.description.isNotBlank()) {
                    put("description", prop.description)
                }
                when (prop.type) {
                    JsonSchemaPropertyType.STRING -> {
                        if (prop.stringFormat != StringFormat.NONE) {
                            put("format", prop.stringFormat.name.lowercase())
                        }
                    }

                    JsonSchemaPropertyType.NUMBER -> {
                        prop.numberMinimum?.let { put("minimum", it) }
                        prop.numberMaximum?.let { put("maximum", it) }
                    }

                    else -> {
                    }
                }
            }
        }.filterKeys { it.isNotBlank() }

        val rootSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                propertiesMap.forEach { (name, jsonObject) ->
                    put(name, jsonObject)
                }
            })
        }

        return Json { prettyPrint = true }.encodeToString(rootSchema)
    }

    private fun parseJsonToGraphicalSchema(jsonString: String?): List<JsonSchemaProperty> {
        if (jsonString.isNullOrBlank()) return emptyList()
        return try {
            val root = Json.decodeFromString<JsonObject>(jsonString)
            if (root["type"]?.jsonPrimitive?.content != "object" || !root.contains("properties")) {
                return emptyList()
            }
            val properties = root["properties"]?.jsonObject ?: return emptyList()

            properties.map { (name, element) ->
                val propObj = element.jsonObject
                val typeStr = propObj["type"]?.jsonPrimitive?.content?.uppercase()
                val type = JsonSchemaPropertyType.entries.find { it.name == typeStr } ?: JsonSchemaPropertyType.STRING
                JsonSchemaProperty(
                    id = System.currentTimeMillis() + name.hashCode(),
                    name = name,
                    type = type,
                    description = propObj["description"]?.jsonPrimitive?.content ?: "",
                    stringFormat = StringFormat.entries.find {
                        it.name.equals(
                            propObj["format"]?.jsonPrimitive?.content,
                            ignoreCase = true
                        )
                    } ?: StringFormat.NONE,
                    numberMinimum = propObj["minimum"]?.jsonPrimitive?.doubleOrNull,
                    numberMaximum = propObj["maximum"]?.jsonPrimitive?.doubleOrNull
                )
            }
        } catch (e: Exception) {
            Log.e("ResponseSchemaViewModel", "Failed to parse Response Schema JSON: ${e.message}")
            emptyList()
        }
    }
}
