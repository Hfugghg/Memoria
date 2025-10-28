package com.exp.memoria.llmtools.adapter;

import com.exp.memoria.llmtools.LlmTool;
import com.exp.memoria.llmtools.tool.ToolRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 实现了 ToolAdapter 接口，用于处理来自 Google Gemini 的工具调用。
 */
public class GeminiToolAdapter implements ToolAdapter {

    private final ToolRegistry toolRegistry;

    public GeminiToolAdapter(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public List<JSONObject> processToolCalls(JSONObject llmAssistantMessage) throws JSONException {
        List<JSONObject> toolResponseMessages = new ArrayList<>();

        // Gemini 在 "parts" 数组中发送一个或多个 "functionCall"
        JSONArray parts = llmAssistantMessage.getJSONArray("parts");

        for (int i = 0; i < parts.length(); i++) {
            JSONObject part = parts.getJSONObject(i);
            if (!part.has("functionCall")) {
                continue;
            }

            JSONObject functionCall = part.getJSONObject("functionCall");
            String functionName = functionCall.getString("name");
            JSONObject arguments = functionCall.getJSONObject("args");

            String result;
            if (toolRegistry.hasTool(functionName)) {
                LlmTool tool = toolRegistry.getTool(functionName);
                result = tool.execute(arguments);
            } else {
                result = "错误: 未知的工具: " + functionName;
            }

            // 将结果格式化为 Gemini 期望的 "function" 角色消息
            JSONObject toolResponseMessage = new JSONObject();
            toolResponseMessage.put("role", "function");

            // Gemini 的响应格式是 parts: [ { "functionResponse": ... } ]
            JSONObject functionResponse = new JSONObject();
            functionResponse.put("name", functionName);
            functionResponse.put("response", new JSONObject().put("result", result)); // Gemini 期望结果在一个 "result" 键中

            JSONArray responseParts = new JSONArray();
            responseParts.put(new JSONObject().put("functionResponse", functionResponse));

            toolResponseMessage.put("parts", responseParts);

            toolResponseMessages.add(toolResponseMessage);
        }

        return toolResponseMessages;
    }
}