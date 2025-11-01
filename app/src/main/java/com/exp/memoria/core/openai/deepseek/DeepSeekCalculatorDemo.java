package com.exp.memoria.core.openai.deepseek;

import okhttp3.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class DeepSeekCalculatorDemo {
    // API 配置常量
    private static final String API_KEY = System.getenv("DEEPSEEK_API_KEY");
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String MODEL = "deepseek-chat";

    private final OkHttpClient client;

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
            throw new RuntimeException(e);
        }
    }

    public DeepSeekCalculatorDemo() {
        this.client = new OkHttpClient();
    }

    /**
     * 简单的计算器 Function Calling 演示
     */
    public void simpleCalculatorExample() {
        try {
            // 对话消息列表
            List<JSONObject> messages = new ArrayList<>();

            // 用户提问数学问题
            String userQuestion = "请帮我计算一下 0.9乘以0.518 等于多少？";
            System.out.println("👤 用户: " + userQuestion);

            // 添加用户消息
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", userQuestion);
            messages.add(userMessage);

            // 第一步：发送请求，模型识别需要调用计算器
            System.out.println("🔄 发送到DeepSeek...");
            JSONObject firstResponse = sendMessages(messages, CALCULATOR_TOOLS);

            // 解析响应
            JSONObject assistantMessage = firstResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message");

            messages.add(assistantMessage);

            // 检查是否有工具调用
            if (assistantMessage.has("tool_calls")) {
                JSONArray toolCalls = assistantMessage.getJSONArray("tool_calls");
                JSONObject toolCall = toolCalls.getJSONObject(0);

                // 执行计算器工具
                String calculationResult = executeCalculator(toolCall);
                System.out.println("🧮 计算器执行: " + calculationResult);

                // 添加工具执行结果
                JSONObject toolMessage = new JSONObject();
                toolMessage.put("role", "tool");
                toolMessage.put("tool_call_id", toolCall.getString("id"));
                toolMessage.put("content", calculationResult);
                messages.add(toolMessage);

                // 第二步：发送计算结果给模型，获得最终回答
                JSONObject finalResponse = sendMessages(messages, CALCULATOR_TOOLS);
                String finalAnswer = finalResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");

                System.out.println("🤖 AI助手: " + finalAnswer);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 支持多种数学问题的演示
     */
    public void multipleCalculationsExample() {
        String[] questions = {
                "0.5125456 乘以 0.8865495 等于多少？",
                "100 减去 45 是多少？",
                "12 乘以 15 等于多少？",
                "144 除以 12 等于多少？",
                "计算 2 的 10 次方",
                "计算 0 除以 10等于多少",
                "计算 10 除以 0等于多少"
        };

        for (String question : questions) {
            System.out.println("\n=== 新问题 ===");
            processCalculation(question);
        }
    }

    /**
     * 处理单个计算问题
     */
    private void processCalculation(String question) {
        try {
            List<JSONObject> messages = new ArrayList<>();

            System.out.println("👤 用户: " + question);

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
                JSONArray toolCalls = assistantMessage.getJSONArray("tool_calls");
                JSONObject toolCall = toolCalls.getJSONObject(0);

                String result = executeCalculator(toolCall);
                System.out.println("🧮 计算: " + result);

                // 添加工具结果
                JSONObject toolMessage = new JSONObject();
                toolMessage.put("role", "tool");
                toolMessage.put("tool_call_id", toolCall.getString("id"));
                toolMessage.put("content", result);
                messages.add(toolMessage);

                // 第二次调用
                JSONObject finalResponse = sendMessages(messages, CALCULATOR_TOOLS);
                String finalAnswer = finalResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");

                System.out.println("🤖 AI助手: " + finalAnswer);
            }

        } catch (Exception e) {
            System.out.println("❌ 处理失败: " + e.getMessage());
        }
    }

    /**
     * 执行计算器工具
     */
    private String executeCalculator(JSONObject toolCall) throws JSONException {
        JSONObject function = toolCall.getJSONObject("function");
        String functionName = function.getString("name");
        JSONObject arguments = new JSONObject(function.getString("arguments"));

        if ("calculate".equals(functionName)) {
            String expression = arguments.getString("expression");
            return calculateExpression(expression);
        }

        return "未知的计算函数";
    }

    /**
     * 实际执行数学计算
     */
    private String calculateExpression(String expression) {
        try {
            // 简单的表达式计算（实际项目中可以使用 ScriptEngine 等更安全的方式）
            expression = expression.replace(" ", "").toLowerCase();

            // 处理基本运算
            if (expression.contains("+")) {
                String[] parts = expression.split("\\+");
                double result = Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
                System.out.println("执行了加法");
                return String.valueOf(result);
            } else if (expression.contains("-")) {
                String[] parts = expression.split("-");
                double result = Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
                return String.valueOf(result);
            } else if (expression.contains("*") || expression.contains("×")) {
                expression = expression.replace("×", "*");
                String[] parts = expression.split("\\*");
                double result = Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
                System.out.println("执行了乘法");
                return String.valueOf(result);
            } else if (expression.contains("/") || expression.contains("÷")) {
                expression = expression.replace("÷", "/");
                String[] parts = expression.split("/");
                if (Double.parseDouble(parts[1]) == 0) {
                    return "错误：除数不能为零";
                }
                double result = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
                return String.valueOf(result);
            } else if (expression.contains("^")) {
                String[] parts = expression.split("\\^");
                double base = Double.parseDouble(parts[0]);
                double exponent = Double.parseDouble(parts[1]);
                double result = Math.pow(base, exponent);
                return String.valueOf(result);
            }

            return "无法计算的表达式: " + expression;

        } catch (Exception e) {
            return "计算错误: " + e.getMessage();
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
                .addHeader("Authorization", "Bearer " + API_KEY)
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