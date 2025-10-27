package com.exp.memoria.core.openai.deepseek;
import okhttp3.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class DeepSeekDemo02 {
    // API 配置常量
    private static final String API_KEY = System.getenv("DEEPSEEK_API_KEY");
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String MODEL = "deepseek-chat";
    private static final String SYSTEM_ROLE_CONTENT = "You are a helpful assistant.";
    private static final int MAX_TOKENS = 2048;
    private static final boolean STREAM = false;

    private final OkHttpClient client;

    // 工具定义
    private static final JSONArray TOOLS;

    static {
        try {
            TOOLS = new JSONArray("""
                [
                    {
                        "type": "function",
                        "function": {
                            "name": "get_weather",
                            "description": "Get weather of a location, the user should supply a location first.",
                            "parameters": {
                                "type": "object",
                                "properties": {
                                    "location": {
                                        "type": "string",
                                        "description": "The city and state, e.g. San Francisco, CA"
                                    }
                                },
                                "required": ["location"]
                            }
                        }
                    }
                ]
                """);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public DeepSeekDemo02() {
        this.client = new OkHttpClient();
    }

    // 原有的基础聊天方法
    public Response chatWithDeepSeek(String userMessage) throws IOException, JSONException {
        return chatWithDeepSeek(userMessage, SYSTEM_ROLE_CONTENT, false);
    }

    // 重载方法，支持自定义系统角色
    public Response chatWithDeepSeek(String userMessage, String systemRole) throws IOException, JSONException {
        return chatWithDeepSeek(userMessage, systemRole, false);
    }

    // 私有方法，支持工具调用
    private Response chatWithDeepSeek(String userMessage, String systemRole, boolean useTools) throws IOException, JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL);
        requestBody.put("stream", STREAM);
        requestBody.put("max_tokens", MAX_TOKENS);

        JSONArray messages = new JSONArray();

        if (systemRole != null && !systemRole.isEmpty()) {
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemRole);
            messages.put(systemMessage);
        }

        JSONObject userMessageObj = new JSONObject();
        userMessageObj.put("role", "user");
        userMessageObj.put("content", userMessage);
        messages.put(userMessageObj);

        requestBody.put("messages", messages);

        // 如果使用工具调用，添加工具定义
        if (useTools) {
            requestBody.put("tools", TOOLS);
        }

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(requestBody.toString(), mediaType);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        return client.newCall(request).execute();
    }

    // 解析响应内容的方法
    public String parseResponse(Response response) throws IOException, JSONException {
        if (!response.isSuccessful()) {
            throw new IOException("Request failed: " + response.code() + " - " + response.message());
        }

        String responseBody = response.body().string();
        JSONObject jsonResponse = new JSONObject(responseBody);
        JSONArray choices = jsonResponse.getJSONArray("choices");

        if (choices.length() > 0) {
            JSONObject firstChoice = choices.getJSONObject(0);
            JSONObject message = firstChoice.getJSONObject("message");
            return message.getString("content");
        }

        throw new IOException("No choices in response");
    }

    // 新的 Function Calling 示例方法
    public void weatherFunctionCallingExample() {
        try {
            List<JSONObject> messages = new ArrayList<>();

            // 用户消息
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", "How's the weather in Hangzhou, Zhejiang?");
            messages.add(userMessage);

            System.out.println("User>\t " + userMessage.getString("content"));

            // 第一次调用 - 发送用户消息并期待工具调用
            JSONObject firstResponse = sendMessagesWithTools(messages);
            JSONObject assistantMessage = firstResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message");

            messages.add(assistantMessage);

            // 检查是否有工具调用
            if (assistantMessage.has("tool_calls") && !assistantMessage.isNull("tool_calls")) {
                JSONArray toolCalls = assistantMessage.getJSONArray("tool_calls");
                if (toolCalls.length() > 0) {
                    JSONObject toolCall = toolCalls.getJSONObject(0);
                    String toolCallId = toolCall.getString("id");

                    // 模拟工具执行 - 在实际应用中这里会调用真实的天气API
                    String weatherResult = executeWeatherTool(toolCall);

                    // 添加工具执行结果到消息列表
                    JSONObject toolMessage = new JSONObject();
                    toolMessage.put("role", "tool");
                    toolMessage.put("tool_call_id", toolCallId);
                    toolMessage.put("content", weatherResult);
                    messages.add(toolMessage);

                    // 第二次调用 - 发送工具执行结果给模型
                    JSONObject secondResponse = sendMessagesWithTools(messages);
                    String finalResponse = secondResponse.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    System.out.println("Model>\t " + finalResponse);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 发送带工具调用的消息
    private JSONObject sendMessagesWithTools(List<JSONObject> messages) throws IOException, JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL);
        requestBody.put("stream", STREAM);
        requestBody.put("max_tokens", MAX_TOKENS);

        JSONArray messagesArray = new JSONArray();
        for (JSONObject message : messages) {
            messagesArray.put(message);
        }
        requestBody.put("messages", messagesArray);
        requestBody.put("tools", TOOLS);

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(requestBody.toString(), mediaType);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("Request failed: " + response.code() + " - " + response.message());
        }

        String responseBody = response.body().string();
        return new JSONObject(responseBody);
    }

    // 执行天气工具（模拟实现）
    private String executeWeatherTool(JSONObject toolCall) throws JSONException {
        JSONObject function = toolCall.getJSONObject("function");
        String functionName = function.getString("name");
        JSONObject arguments = new JSONObject(function.getString("arguments"));

        if ("get_weather".equals(functionName)) {
            String location = arguments.getString("location");
            // 模拟根据位置返回天气信息
            // 实际应用中这里会调用真实的天气API
            System.out.println("Tool>\t Getting weather for: " + location);
            return "24℃, Sunny";
        }

        return "Tool execution failed";
    }

    // 主方法用于测试
//    public static void main(String[] args) {
//        DeepSeekDemo02 demo = new DeepSeekDemo02();
//        System.out.println("=== DeepSeek Function Calling Demo ===");
//        demo.weatherFunctionCallingExample();
//    }
}