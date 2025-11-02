package com.exp.memoria.core.llmtool.localtools;

import com.exp.memoria.core.novelai.GenerateImage;
import com.exp.memoria.core.novelai.Login;
import com.exp.memoria.core.utils.Argon2Hasher;
import com.exp.memoria.core.utils.Byte2Img;
import com.exp.memoria.core.utils.RandomUtils;

import okhttp3.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Contains local tools that can be called by the LLM.
 */
public class LocalTools {

    // API 配置
    private static final String DEEPSEEK_API_KEY = System.getenv("DEEPSEEK_API_KEY");
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String DEEPSEEK_MODEL = "deepseek-chat";

    // NovelAI 配置
    private String accessKey = System.getenv("NAI_ACCESS_KEY");
    private String novelaiName = System.getenv("NAI_USERNAME");
    private String novelaiPassword = System.getenv("NAI_PASSWORD");
    private String proxyhostname = "127.0.0.1";
    private int proxyport = 7897;
    private String domain = "novelai_data_access_key";

    private final OkHttpClient client;
    private final Login login;
    private final GenerateImage generator;

    // 图像生成工具定义
    private static final JSONArray IMAGE_GENERATION_TOOLS;

    static {
        try {
            IMAGE_GENERATION_TOOLS = new JSONArray("""
                    [
                        {
                            "type": "function",
                            "function": {
                                "name": "novelai3_generate_image",
                                "description": "使用 NovelAI V3模型生成图像，支持配置图片尺寸、采样参数、质量设置等",
                                "parameters": {
                                    "type": "object",
                                    "properties": {
                                        "prompt": {
                                            "type": "string",
                                            "description": "正面提示词，描述想要生成的图像内容"
                                        },
                                        "model": {
                                            "type": "string",
                                            "description": "模型名称，默认使用 nai-diffusion-3",
                                            "enum": ["nai-diffusion-3"]
                                        },
                                        "width": {
                                            "type": "integer",
                                            "description": "图片宽度，范围 1-2048，默认 832"
                                        },
                                        "height": {
                                            "type": "integer",\s
                                            "description": "图片高度，范围 1-2048，默认 1216"
                                        },
                                        "scale": {
                                            "type": "integer",
                                            "description": "提示词引导系数 (CFG Scale)，控制与提示词的贴合程度，范围 1-10，默认 5"
                                        },
                                        "sampler": {
                                            "type": "string",
                                            "description": "采样器算法，默认 k_euler",
                                            "enum": ["k_euler", "k_euler_ancestral", "k_dpmpp_2m", "k_dpmpp_sde", "ddim"]
                                        },
                                        "steps": {
                                            "type": "integer",
                                            "description": "迭代步数，影响细节和生成时间，范围 1-50，默认 28"
                                        },
                                        "n_samples": {
                                            "type": "integer",
                                            "description": "生成图像数量，范围 1-4，默认 1"
                                        },
                                        "seed": {
                                            "type": "integer",
                                            "description": "随机种子，相同种子和参数可复现结果，范围 0-2147483647，默认 0 (随机) "
                                        },
                                        "ucPreset": {
                                            "type": "integer",
                                            "description": "负面内容预设，用于规避通用负面内容，范围 0-3，默认 0",
                                            "enum": [0, 1, 2, 3]
                                        },
                                        "qualityToggle": {
                                            "type": "boolean",
                                            "description": "质量开关，开启可提升图像质量，默认 true"
                                        },
                                        "uc": {
                                            "type": "string",
                                            "description": "负面提示词，描述不希望出现在图像中的内容"
                                        }
                                    },
                                    "required": ["prompt"],
                                    "additionalProperties": false
                                }
                            }
                        }
                    ]
                """);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public LocalTools() {
        // 初始化代理和HTTP客户端
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyhostname, proxyport));
        this.client = new OkHttpClient.Builder()
                .proxy(proxy)
                .build();

        this.login = new Login(client);
        this.generator = new GenerateImage(client);
    }

    // Setter 方法
    public void setDomain(String domain) { this.domain = domain; }
    public void setProxyhostname(String proxyhostname) { this.proxyhostname = proxyhostname; }
    public void setProxyport(int proxyport) { this.proxyport = proxyport; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public void setNovelaiName(String novelaiName) { this.novelaiName = novelaiName; }
    public void setNovelaiPassword(String novelaiPassword) { this.novelaiPassword = novelaiPassword; }

    /**
     * 主要的图像生成演示方法
     */
    public void imageGenerationExample(String userPrompt) {
        try {
            List<JSONObject> messages = new ArrayList<>();

            System.out.println("👤 用户: " + userPrompt);

            // 添加用户消息
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            messages.add(userMessage);

            // 第一步：发送请求，模型识别需要调用图像生成工具
            System.out.println("🔄 发送到DeepSeek...");
            JSONObject firstResponse = sendMessages(messages, IMAGE_GENERATION_TOOLS);

            // 解析响应
            JSONObject assistantMessage = firstResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message");

            messages.add(assistantMessage);

            // 检查是否有工具调用
            if (assistantMessage.has("tool_calls")) {
                JSONArray toolCalls = assistantMessage.getJSONArray("tool_calls");
                JSONObject toolCall = toolCalls.getJSONObject(0);

                // 执行图像生成工具
                System.out.println("🎨 调用图像生成工具...");
                String generationResult = executeImageGeneration(toolCall);
                System.out.println("✅ 图像生成完成: " + generationResult);

                // 添加工具执行结果
                JSONObject toolMessage = new JSONObject();
                toolMessage.put("role", "tool");
                toolMessage.put("tool_call_id", toolCall.getString("id"));
                toolMessage.put("content", generationResult);
                messages.add(toolMessage);

                // 第二步：发送生成结果给模型，获得最终回答
                JSONObject finalResponse = sendMessages(messages, IMAGE_GENERATION_TOOLS);
                String finalAnswer = finalResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");

                System.out.println("🤖 AI助手: " + finalAnswer);
            } else {
                System.out.println("❌ 模型没有调用图像生成工具");
                String content = assistantMessage.getString("content");
                System.out.println("🤖 AI助手: " + content);
            }

        } catch (Exception e) {
            System.err.println("❌ 图像生成过程出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 执行图像生成工具
     */
    private String executeImageGeneration(JSONObject toolCall) throws JSONException {
        JSONObject function = toolCall.getJSONObject("function");
        String functionName = function.getString("name");
        JSONObject arguments = new JSONObject(function.getString("arguments"));

        if ("novelai3_generate_image".equals(functionName)) {
            String prompt = arguments.getString("prompt");
            String model = arguments.optString("model", null);
            int width = arguments.optInt("width", 0);
            int height = arguments.optInt("height", 0);
            int scale = arguments.optInt("scale", 0);
            String sampler = arguments.optString("sampler", null);
            int steps = arguments.optInt("steps", 0);
            int n_samples = arguments.optInt("n_samples", 0);
            int seed = arguments.optInt("seed", 0);
            int ucPreset = arguments.optInt("ucPreset", 0);
            boolean qualityToggle = arguments.optBoolean("qualityToggle", true);
            String uc = arguments.optString("uc", null);

            return novelai3GenerateImage(prompt, model, width, height, scale, sampler,
                    steps, n_samples, seed, ucPreset, qualityToggle, uc);
        }

        return "未知的图像生成函数";
    }

    /**
     * v3模型图像生成的函数
     */
    private String novelai3GenerateImage(String prompt, String model, int width, int height,
                                         int scale, String sampler, int steps, int n_samples,
                                         int seed, int ucPreset, boolean qualityToggle, String uc) {
        // 参数验证和默认值设置
          model=( model != null )? model : "nai-diffusion-3";
//        if (model == null) {
//            model = "nai-diffusion-3";
//        }
//        if ((width == 0) || (width > 2048)) {
//            width = 832;
//        }
//        if ((height == 0) || (height > 2048)) {
//            height = 1216;
//        }
//        if ((scale == 0) || (scale > 10)) {
//            scale = 5;
//        }
//        if (sampler == null) {
//            sampler = "k_euler";
//        }
//        if ((steps == 0) || (steps > 50)) {
//            steps = 28;
//        }
//        if ((n_samples == 0) || (n_samples > 4)) {
//            n_samples = 1;
//        }
        if ((seed <= 0) || (seed > 2147483647)) {
            seed = RandomUtils.generateRandomInt();
        }
//        if ((ucPreset == 0) || (ucPreset > 3)) {
//            ucPreset = 0;
//        }
//        if (uc == null) {
//            uc = "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry";
//        }

        // 必需参数检查

        Map<String, Object> parameters = new HashMap<>();
//        parameters.put("width", width);
//        parameters.put("height", height);
//        parameters.put("scale", scale);
//        parameters.put("sampler", sampler);
//        parameters.put("steps", steps);
//        parameters.put("n_samples", n_samples);
//        parameters.put("seed", seed);
//        parameters.put("ucPreset", ucPreset);
//        parameters.put("qualityToggle", qualityToggle);
//        parameters.put("uc", uc);

        // 设置默认值
       // parameters.put("prompt", prompt.trim());
       // parameters.put("model", model != null ? model : "nai-diffusion-3");
        parameters.put("width", (width <= 0 || width > 2048) ? 832 : width);
        parameters.put("height", (height <= 0 || height > 2048) ? 1216 : height);
        parameters.put("scale", (scale <= 0 || scale > 10) ? 5 : scale);
        parameters.put("sampler", sampler != null ? sampler : "k_euler");
        parameters.put("steps", (steps <= 0 || steps > 50) ? 28 : steps);
        parameters.put("n_samples", (n_samples <= 0 || n_samples > 4) ? 1 : n_samples);
        parameters.put("seed", seed);
        //  parameters.put("seed", (seed <= 0 || seed > 2147483647) ? RandomUtils.generateRandomInt() : seed);
        parameters.put("ucPreset", (ucPreset < 0 || ucPreset > 3) ? 0 : ucPreset);
        parameters.put("qualityToggle", qualityToggle);
        parameters.put("uc", uc != null ? uc : "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry");

        try {
            String accessToken = accessKeyoraccessToken();
            byte[] imageBytes = generator.generateImage(accessToken, prompt, model, "generate", parameters, 300, 3);

            if (imageBytes == null || imageBytes.length == 0) {
                return "图像生成失败：返回的图像数据为空";
            }

            System.out.println("ucPreset :"+ucPreset);
            System.out.println("seed :"+seed);
            String outputFilePath = "F:/Project/Memoria/app/generated_image.png";
            Byte2Img.saveBytesAsImage(imageBytes, outputFilePath);
            return "图像生成成功！保存路径: " + outputFilePath + " | 提示词: " + prompt;

        } catch (IOException | JSONException e) {
            return "图像生成过程中出现错误: " + e.getMessage();
        }
    }

    /**
     * 获取访问密钥或令牌
     */
    private String accessKeyoraccessToken() throws JSONException, IOException {
        if (accessKey == null || accessKey.trim().isEmpty()) {
            String keyhash = Argon2Hasher.argonHash(novelaiName, novelaiPassword, 64, domain);
            return login.login(keyhash);
        } else {
            return accessKey;
        }
    }

    /**
     * 发送消息到DeepSeek API
     */
    private JSONObject sendMessages(List<JSONObject> messages, JSONArray tools) throws IOException, JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", DEEPSEEK_MODEL);
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
                .url(DEEPSEEK_API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + DEEPSEEK_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API请求失败: " + response.code() + " - " + response.message());
            }

            String responseBody = response.body().string();
            return new JSONObject(responseBody);
        }
    }

    /**
     * 批量图像生成演示
     */
    public void multipleImageGenerationExamples() {
        String[] prompts = {
                "生成一张美丽的日落海滩风景图",
        };

        for (String prompt : prompts) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("处理新的图像生成请求...");
            imageGenerationExample(prompt);
            try {
                // 添加延迟避免API限制
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 获取工具定义（供外部调用）
     */
    public static JSONArray getImageGenerationTools() {
        return IMAGE_GENERATION_TOOLS;
    }
}