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
        this.key1 = login.login(hash);
        this.generator = new GenerateImage(client);
    }

    @Test
    public void testGenerateImageWithV3Model() {
        String prompt = "1girl, best quality, amazing quality, very aesthetic, absurdres";
        String negative_prompt = "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry";
        String model = "nai-diffusion-3";
        String action = "generate";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("width", 832);
        parameters.put("height", 1216);
        parameters.put("scale", 5.0);
        parameters.put("sampler", "k_euler");
        parameters.put("steps", 28);
        parameters.put("n_samples", 1);
        parameters.put("seed", 0);
        parameters.put("ucPreset", 0);
        parameters.put("qualityToggle", true);
        parameters.put("uc", negative_prompt);

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
        parameters.put("params_version", 1);
        parameters.put("width", 832);
        parameters.put("height", 1216);
        parameters.put("scale", 5.0);
        parameters.put("sampler", "k_euler");
        parameters.put("steps", 28);
        parameters.put("seed", 0);
        parameters.put("n_samples", 1);
        parameters.put("ucPreset", 3);
        parameters.put("qualityToggle", false);
        parameters.put("sm", false);
        parameters.put("sm_dyn", false);
        parameters.put("dynamic_thresholding", false);
        parameters.put("controlnet_strength", 1.0);
        parameters.put("legacy", false);
        parameters.put("add_original_image", false);
        parameters.put("cfg_rescale", 0.0);
        parameters.put("noise_schedule", "native");
        parameters.put("legacy_v3_extend", false);
        parameters.put("uncond_scale", 1.0);
        parameters.put("negative_prompt", negative_prompt);
        parameters.put("prompt", prompt);
        parameters.put("reference_image_multiple", new ArrayList<>());
        parameters.put("reference_information_extracted_multiple", new ArrayList<>());
        parameters.put("reference_strength_multiple", new ArrayList<>());
        parameters.put("extra_noise_seed", 0);

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
