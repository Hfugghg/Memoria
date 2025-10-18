package com.exp.memoria.core.openai;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OpenaiApi {
    // --- 配置信息 (硬编码) ---
    private final String baseUrl = "https://api.deepseek.com";
    // TODO: 请在这里替换为您的真实API密钥
    private final String apiKey = "";
    private final String model = "deepseek-chat";
    private final int max_tokens = 2048;
    private final double temperature = 0.7;
    // -------------------------

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /**
     * 用于流式响应的回调接口
     */
    public interface StreamCallback {
        void onChunkReceived(String chunk);
        void onComplete();
        void onError(Exception e);
    }

    /**
     * 发起流式对话请求
     * @param messages 对话历史消息
     * @param callback 用于处理流式响应的回调
     */
    public void chatStream(List<Message> messages, final StreamCallback callback) {
        ChatRequest chatRequest = new ChatRequest(model, messages, true, max_tokens, temperature);
        String jsonBody = gson.toJson(chatRequest);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(baseUrl)
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    try {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        callback.onError(new IOException("Unexpected code " + response + " with body: " + errorBody));
                    } catch (IOException e) {
                        callback.onError(e);
                    }
                    return;
                }

                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        callback.onError(new IOException("Response body is null"));
                        return;
                    }
                    
                    BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if (data.equals("[DONE]")) {
                                break;
                            }
                            try {
                                ChatCompletionChunk chunk = gson.fromJson(data, ChatCompletionChunk.class);
                                if (chunk != null && chunk.choices != null && !chunk.choices.isEmpty()) {
                                    Delta delta = chunk.choices.get(0).delta;
                                    if (delta != null && delta.content != null) {
                                        callback.onChunkReceived(delta.content);
                                    }
                                }
                            } catch (Exception e) {
                                // 忽略JSON解析错误，继续处理下一行
                            }
                        }
                    }
                } catch (IOException e) {
                    callback.onError(e);
                } finally {
                    callback.onComplete();
                }
            }
        });
    }

    // --- 数据模型内部类 ---

    public static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    private static class ChatRequest {
        public String model;
        public List<Message> messages;
        public boolean stream;
        public int max_tokens;
        public double temperature;

        public ChatRequest(String model, List<Message> messages, boolean stream, int max_tokens, double temperature) {
            this.model = model;
            this.messages = messages;
            this.stream = stream;
            this.max_tokens = max_tokens;
            this.temperature = temperature;
        }
    }

    private static class ChatCompletionChunk {
        public List<Choice> choices;
    }

    private static class Choice {
        public Delta delta;
    }

    private static class Delta {
        public String content;
    }
}
