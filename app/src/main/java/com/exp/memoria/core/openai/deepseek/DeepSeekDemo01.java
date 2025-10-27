package com.exp.memoria.core.openai.deepseek;


import okhttp3.*;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class DeepSeekDemo01 {
    // API 配置常量
    private static final String API_KEY = System.getenv("DEEPSEEK_API_KEY");
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String MODEL = "deepseek-chat";
    private static final String SYSTEM_ROLE_CONTENT = "You are a helpful assistant.";
    private static final int MAX_TOKENS = 2048;
    private static final boolean STREAM = false;

    private final OkHttpClient client;

    public DeepSeekDemo01() {
        this.client = new OkHttpClient();
    }

    public Response chatWithDeepSeek(String userMessage) throws IOException, JSONException {
        // 使用 JSONObject 构建请求体，更安全可靠
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL);
        requestBody.put("stream", STREAM);
        requestBody.put("max_tokens", MAX_TOKENS);

        // 构建消息数组
        JSONArray messages = new JSONArray();

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", SYSTEM_ROLE_CONTENT);
        messages.put(systemMessage);

        JSONObject userMessageObj = new JSONObject();
        userMessageObj.put("role", "user");
        userMessageObj.put("content", userMessage);
        messages.put(userMessageObj);

        requestBody.put("messages", messages);

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

    // 重载方法，支持自定义系统角色
    public Response chatWithDeepSeek(String userMessage, String systemRole) throws IOException, JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL);
        requestBody.put("stream", STREAM);
        requestBody.put("max_tokens", MAX_TOKENS);

        JSONArray messages = new JSONArray();

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemRole != null ? systemRole : SYSTEM_ROLE_CONTENT);
        messages.put(systemMessage);

        JSONObject userMessageObj = new JSONObject();
        userMessageObj.put("role", "user");
        userMessageObj.put("content", userMessage);
        messages.put(userMessageObj);

        requestBody.put("messages", messages);

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


}