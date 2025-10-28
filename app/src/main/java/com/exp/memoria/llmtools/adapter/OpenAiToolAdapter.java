package com.exp.memoria.llmtools.adapter;

import com.exp.memoria.llmtools.LlmTool;
import com.exp.memoria.llmtools.tool.ToolRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OpenAiToolAdapter implements ToolAdapter {

    // 依赖注入 ToolRegistry，而不是 CalculatorTool
    private final ToolRegistry toolRegistry;

    public OpenAiToolAdapter(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public List<JSONObject> processToolCalls(JSONObject llmAssistantMessage) throws JSONException {
        List<JSONObject> toolResponseMessages = new ArrayList<>();

        // OpenAI 在 'tool_calls' 数组中发送工具调用
        JSONArray toolCalls = llmAssistantMessage.getJSONArray("tool_calls");

        for (int i = 0; i < toolCalls.length(); i++) {
            JSONObject toolCall = toolCalls.getJSONObject(i);

            String toolCallId = toolCall.getString("id");
            JSONObject function = toolCall.getJSONObject("function");
            String functionName = function.getString("name");
            JSONObject arguments = new JSONObject(function.getString("arguments"));

            String result;
            if (toolRegistry.hasTool(functionName)) {
                // 从注册表查找并执行工具
                LlmTool tool = toolRegistry.getTool(functionName);
                result = tool.execute(arguments);
            } else {
                result = "错误: 未知的工具: " + functionName;
            }

            // 将结果格式化为 OpenAI 期望的 "tool" 角色消息
            JSONObject toolResponseMessage = new JSONObject();
            toolResponseMessage.put("role", "tool");
            toolResponseMessage.put("tool_call_id", toolCallId);
            toolResponseMessage.put("name", functionName); // 某些实现需要 name
            toolResponseMessage.put("content", result);

            toolResponseMessages.add(toolResponseMessage);
        }

        return toolResponseMessages;
    }
}