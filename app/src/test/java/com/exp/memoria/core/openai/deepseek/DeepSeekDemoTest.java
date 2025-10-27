package com.exp.memoria.core.openai.deepseek;
import org.json.JSONException;
import org.junit.Test;

import okhttp3.*;
import java.io.IOException;
import org.json.JSONObject;
import org.json.JSONArray;
public class DeepSeekDemoTest {
    private static final String API_KEY =System.getenv("DEEPSEEK_API_KEY"); // 替换为真实 API 密钥
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";

    private final OkHttpClient client;

    public DeepSeekDemoTest() {
        this.client = new OkHttpClient();
    }

    public Response chatWithDeepSeek02(String userMessage) throws IOException {
        String jsonBody = String.format(
                "{" +
                        "\"model\": \"deepseek-chat\"," +
                        "\"messages\": [{" +
                        "\"role\": \"system\"," +
                        "\"content\": \"You are a helpful assistant.\"" +
                        "}, {" +
                        "\"role\": \"user\"," +
                        "\"content\": \"%s\"" +
                        "}]," +
                        "\"stream\": false," +
                        "\"max_tokens\": 2048" +
                        "}",
                escapeJsonString(userMessage)
        );

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(jsonBody, mediaType);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        // 直接返回 Response，让调用者负责关闭
        return client.newCall(request).execute();
    }

    // 转义 JSON 字符串中的特殊字符
    private String escapeJsonString(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Test
    public void test02() {
        DeepSeekDemoTest demo = new DeepSeekDemoTest();

        // 在测试方法中使用 try-with-resources
        try (Response response = demo.chatWithDeepSeek02("你好!")) {
            if (response.isSuccessful()) {
                System.out.println("Response: " + response.body().string());
            } else {
                System.out.println("Request failed: " + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testChat() {
        DeepSeekDemo01 demo = new DeepSeekDemo01();

        try (Response response = demo.chatWithDeepSeek("你好！")) {
            String content = demo.parseResponse(response);
            System.out.println("AI回复: " + content);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }




}