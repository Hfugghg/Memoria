package com.exp.memoria.core.openai.deepseek;

import com.exp.memoria.llmtools.adapter.ToolAdapter;
import com.exp.memoria.llmtools.adapter.ToolAdapterFactory;
import com.exp.memoria.llmtools.tool.ToolRegistry;
import okhttp3.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import android.util.Log;

public class DeepSeekCalculatorDemo {
    private static final String TAG = "DeepSeekDemo";
    // API 配置常量
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String MODEL = "deepseek-chat";

    private final String apiKey;
    private final OkHttpClient client;
    private final ToolAdapter toolAdapter;

    // 计算器工具定义 - 非常简单直观
    private static final JSONArray CALCULATOR_TOOLS;

    static {
        try {
            CALCULATOR_TOOLS = new JSONArray("""
                [
                    {
                        "type": "function",
                        "function": {
                            "name": "calculate",
                            "description": "Perform basic mathematical calculations",
                            "parameters": {
                                "type": "object",
                                "properties": {
                                    "expression": {
                                        "type": "string",
                                        "description": "The mathematical expression to calculate, e.g. 2+2, 10*5, 15/3"
                                    }
                                },
                                "required": ["expression"]
                            }
                        }
                    }
                ]
                """);
        } catch (JSONException e) {
            Log.e(TAG, "Error initializing CALCULATOR_TOOLS", e);
            throw new RuntimeException(e);
        }
    }

    public DeepSeekCalculatorDemo(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient();
        // 使用 ToolRegistry 和 ToolAdapterFactory 来获取适配器
        ToolRegistry toolRegistry = ToolRegistry.getInstance();
        ToolAdapterFactory factory = new ToolAdapterFactory(toolRegistry);
        this.toolAdapter = factory.getAdapter("deepseek");
    }

    /**
     * 简单的计算器 Function Calling 演示
     */
    public void simpleCalculatorExample() throws IOException, JSONException {
        try {
            // 对话消息列表
            List<JSONObject> messages = new ArrayList<>();

            // 用户提问数学问题
            String userQuestion = "请帮我计算一下 0.9乘以0.518 等于多少？";
            Log.d(TAG, "👤 用户: " + userQuestion);

            // 添加用户消息
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", userQuestion);
            messages.add(userMessage);

            // 第一步：发送请求，模型识别需要调用计算器
            Log.d(TAG, "🔄 发送到DeepSeek...");
            JSONObject firstResponse = sendMessages(messages, CALCULATOR_TOOLS);

            // 解析响应
            JSONObject assistantMessage = firstResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message");

            messages.add(assistantMessage);

            // 检查是否有工具调用
            if (assistantMessage.has("tool_calls")) {
                // 使用 ToolAdapter 的 processToolCalls 方法处理整个 assistantMessage
                List<JSONObject> toolResponseMessages = toolAdapter.processToolCalls(assistantMessage);

                // 将每个工具响应添加到消息列表中
                for (JSONObject toolResponseMessage : toolResponseMessages) {
                    // 提取内容用于日志输出
                    String calculationResult = toolResponseMessage.getString("content");
                    Log.d(TAG, "🧮 计算器执行: " + calculationResult);

                    // 将格式化后的工具响应消息添加到对话历史中
                    messages.add(toolResponseMessage);
                }

                // 第二步：发送计算结果给模型，获得最终回答
                JSONObject finalResponse = sendMessages(messages, CALCULATOR_TOOLS);
                String finalAnswer = finalResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");

                Log.d(TAG, "🤖 AI助手: " + finalAnswer);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in simpleCalculatorExample", e);
            throw e; // 重新抛出异常，让调用者处理
        }
    }

    /**
     * 支持多种数学问题的演示
     */
    public void multipleCalculationsExample() throws IOException, JSONException {
        String[] questions = {
                "0.5125456 乘以 0.8865495 等于多少？",
                "100 减去 45 是多少？",
                "12 乘以 15 等于多少？",
                "144 除以 12 等于多少？",
                "计算 2 的 10 次方"
        };

        for (String question : questions) {
            Log.d(TAG, "\n=== 新问题 ===");
            processCalculation(question);
        }
    }

    /**
     * 处理单个计算问题
     */
    private void processCalculation(String question) throws IOException, JSONException {
        try {
            List<JSONObject> messages = new ArrayList<>();

            Log.d(TAG, "👤 用户: " + question);

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", question);
            messages.add(userMessage);

            // 第一次调用
            JSONObject firstResponse = sendMessages(messages, CALCULATOR_TOOLS);
            JSONObject assistantMessage = firstResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message");

            messages.add(assistantMessage);

            if (assistantMessage.has("tool_calls")) {
                // 使用 ToolAdapter 的 processToolCalls 方法处理整个 assistantMessage
                List<JSONObject> toolResponseMessages = toolAdapter.processToolCalls(assistantMessage);

                // 将每个工具响应添加到消息列表中
                for (JSONObject toolResponseMessage : toolResponseMessages) {
                    // 提取内容用于日志输出
                    String result = toolResponseMessage.getString("content");
                    Log.d(TAG, "🧮 计算: " + result);

                    // 将格式化后的工具响应消息添加到对话历史中
                    messages.add(toolResponseMessage);
                }

                // 第二次调用
                JSONObject finalResponse = sendMessages(messages, CALCULATOR_TOOLS);
                String finalAnswer = finalResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");

                Log.d(TAG, "🤖 AI助手: " + finalAnswer);
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ 处理失败: " + e.getMessage(), e);
            throw e; // 重新抛出异常，让调用者处理
        }
    }

    /**
     * 发送消息到DeepSeek API
     */
    private JSONObject sendMessages(List<JSONObject> messages, JSONArray tools) throws IOException, JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL);
        requestBody.put("stream", false);

        JSONArray messagesArray = new JSONArray();
        for (JSONObject message : messages) {
            messagesArray.put(message);
        }
        requestBody.put("messages", messagesArray);
        requestBody.put("tools", tools);

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(requestBody.toString(), mediaType);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("API请求失败: " + response.code() + " - " + response.message());
        }

        String responseBody = response.body().string();
        return new JSONObject(responseBody);
    }
}
