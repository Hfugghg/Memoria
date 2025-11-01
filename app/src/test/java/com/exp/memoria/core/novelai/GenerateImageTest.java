package com.exp.memoria.core.novelai;

import com.exp.memoria.core.utils.Argon2Hasher;
import com.exp.memoria.core.utils.Byte2Img;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;

public class GenerateImageTest {

    private GenerateImage generator;
    private String key1;

    @Before
    public void setUp() throws JSONException, IOException {
        String hash = Argon2Hasher.argonHash(System.getenv("NAI_USERNAME"), System.getenv("NAI_PASSWORD"), 64, "novelai_data_access_key");
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7897));
        OkHttpClient client = new OkHttpClient.Builder()
                .proxy(proxy)
                .build();
        Login login = new Login(client);
        this.key1 = System.getenv("NAI_ACCESS_KEY");//login.login(hash);
        this.generator = new GenerateImage(client);
    }

    @Test
    public void testGenerateImageWithV3Model() {
        String prompt = "1girl, best quality, amazing quality, very aesthetic, absurdres";
        String negative_prompt = "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry";
        String model = "nai-diffusion-3";
        String action = "generate";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("width", 832);//宽
        parameters.put("height", 1216);//高
        parameters.put("scale", 5.0);//CFG系数
        parameters.put("sampler", "k_euler");//
        parameters.put("steps", 28);//迭代步数
        parameters.put("n_samples", 1);//生成图像数量
        parameters.put("seed", 0);//
        parameters.put("ucPreset", 0);//
        parameters.put("qualityToggle", true);//
        parameters.put("uc", negative_prompt);//

        try {
            byte[] imageBytes = generator.generateImage(key1, prompt, model, action, parameters, 60, 3);

            assert imageBytes != null;
            assert imageBytes.length > 0;

            String outputFilePath = "F:/Project/Memoria/app/generate_image_v3_test.png";
            Byte2Img.saveBytesAsImage(imageBytes, outputFilePath);

            System.out.println("V3 Image saved to: " + outputFilePath);

        } catch (Exception e) {
            e.printStackTrace();
            assert false : "V3 Test failed with exception: " + e.getMessage();
        }
    }

    @Test
    public void testGenerateImageWithV4Model() {
        String prompt = "1girl, best quality, amazing quality, very aesthetic, absurdres";
        String negative_prompt = "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry";
        String model = "nai-diffusion-4-full";
        String action = "generate";

        Map<String, Object> parameters = new HashMap<>();
        // V4模型参数
        parameters.put("params_version", 1); // 参数版本, V4模型固定为1
        parameters.put("width", 832); // 图像宽度
        parameters.put("height", 1216); // 图像高度
        parameters.put("scale", 5.0); // 提示词引导系数 (CFG Scale), 控制与提示词的贴合程度
        parameters.put("sampler", "k_euler"); // 采样器算法
        parameters.put("steps", 28); // 迭代步数, 影响细节和生成时间
        parameters.put("seed", 0); // 随机种子, 相同种子和参数可复现结果
        parameters.put("n_samples", 1); // 生成图像数量
        parameters.put("ucPreset", 3); // 负面内容预设, V4推荐值为3, 用于规避通用负面内容
        parameters.put("qualityToggle", false); // 质量开关, V4中设为false
        parameters.put("sm", false); // 是否启用SMEA采样技术
        parameters.put("sm_dyn", false); // 是否启用SMEA+DYN采样技术
        parameters.put("dynamic_thresholding", false); // 是否启用动态阈值, 防止高CFG时过饱和
        parameters.put("controlnet_strength", 1.0); // ControlNet强度 (未使用)
        parameters.put("legacy", false); // 是否启用旧版兼容模式
        parameters.put("add_original_image", false); // 是否在输出中添加原图(图生图/局部重绘用)
        parameters.put("cfg_rescale", 0.0); // 引导系数重缩放, 0.0为不启用
        parameters.put("noise_schedule", "native"); // 噪声表类型
        parameters.put("legacy_v3_extend", false); // 是否启用V3旧版扩展功能
        parameters.put("uncond_scale", 1.0); // 负面提示词引导系数
        parameters.put("negative_prompt", negative_prompt); // 负面提示词
        parameters.put("prompt", prompt); // 正面提示词
        parameters.put("reference_image_multiple", new ArrayList<>()); // Vibe Transfer 参考图片列表 (未使用)
        parameters.put("reference_information_extracted_multiple", new ArrayList<>()); // Vibe Transfer 提取信息列表 (未使用)
        parameters.put("reference_strength_multiple", new ArrayList<>()); // Vibe Transfer 强度列表 (未使用)
        parameters.put("extra_noise_seed", 0); // 额外的噪声种子, 用于在主种子基础上产生微调

        Map<String, Object> v4_prompt = new HashMap<>();
        v4_prompt.put("use_coords", false);
        v4_prompt.put("use_order", false);
        Map<String, Object> v4_prompt_caption = new HashMap<>();
        v4_prompt_caption.put("base_caption", prompt);
        v4_prompt_caption.put("char_captions", new ArrayList<>());
        v4_prompt.put("caption", v4_prompt_caption);
        parameters.put("v4_prompt", v4_prompt);

        Map<String, Object> v4_negative_prompt = new HashMap<>();
        v4_negative_prompt.put("use_coords", false);
        v4_negative_prompt.put("use_order", false);
        Map<String, Object> v4_negative_prompt_caption = new HashMap<>();
        v4_negative_prompt_caption.put("base_caption", negative_prompt);
        v4_negative_prompt_caption.put("char_captions", new ArrayList<>());
        v4_negative_prompt.put("caption", v4_negative_prompt_caption);
        parameters.put("v4_negative_prompt", v4_negative_prompt);

        try {
            // Increased timeout to 120 seconds
            byte[] imageBytes = generator.generateImage(key1, prompt, model, action, parameters, 120, 3);

            assert imageBytes != null;
            assert imageBytes.length > 0;

            String outputFilePath = "F:/Project/Memoria/app/generate_image_v4_test.png";
            Byte2Img.saveBytesAsImage(imageBytes, outputFilePath);

            System.out.println("V4 Image saved to: " + outputFilePath);

        } catch (Exception e) {
            e.printStackTrace();
            assert false : "V4 Test failed with exception: " + e.getMessage();
        }
    }
}
