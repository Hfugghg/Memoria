package com.exp.memoria.ui.settings.settingsscreen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.exp.memoria.ui.settings.SystemInstruction
import com.exp.memoria.ui.settings.SystemInstructionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemInstructionSection(
    systemInstruction: SystemInstruction,
    systemInstructionViewModel: SystemInstructionViewModel
) {
    val context = LocalContext.current
    var showAddInstructionDialog by remember { mutableStateOf(false) }
    var showEditInstructionDialog by remember { mutableStateOf(false) }
    var editingInstructionIndex by remember { mutableStateOf<Int?>(null) }
    var draftInstructionText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("系统指令", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (systemInstruction.parts.isEmpty()) {
            Text("当前没有系统指令。点击下方按钮添加。", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp) // Limit height to avoid excessive scrolling
            ) {
                items(systemInstruction.parts.size) { index ->
                    val part = systemInstruction.parts[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = part.text,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                editingInstructionIndex = index
                                draftInstructionText = part.text
                                showEditInstructionDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑指令")
                            }
                            IconButton(onClick = {
                                systemInstructionViewModel.removeSystemInstructionPart(index)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除指令")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                draftInstructionText = "" // Clear draft for new instruction
                showAddInstructionDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加指令")
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加系统指令")
        }
    }

    // Add Instruction Dialog
    if (showAddInstructionDialog) {
        AlertDialog(
            onDismissRequest = { showAddInstructionDialog = false },
            title = { Text("添加系统指令") },
            text = {
                OutlinedTextField(
                    value = draftInstructionText,
                    onValueChange = { draftInstructionText = it },
                    label = { Text("指令内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (draftInstructionText.isNotBlank()) {
                            systemInstructionViewModel.addSystemInstructionPart(draftInstructionText)
                            showAddInstructionDialog = false
                        } else {
                            Toast.makeText(context, "指令内容不能为空", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddInstructionDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Edit Instruction Dialog
    if (showEditInstructionDialog) {
        AlertDialog(
            onDismissRequest = { showEditInstructionDialog = false },
            title = { Text("编辑系统指令") },
            text = {
                OutlinedTextField(
                    value = draftInstructionText,
                    onValueChange = { draftInstructionText = it },
                    label = { Text("指令内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        editingInstructionIndex?.let { index ->
                            if (draftInstructionText.isNotBlank()) {
                                systemInstructionViewModel.updateSystemInstructionPart(index, draftInstructionText)
                                showEditInstructionDialog = false
                                editingInstructionIndex = null
                            } else {
                                Toast.makeText(context, "指令内容不能为空", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditInstructionDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
