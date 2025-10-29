package com.exp.memoria.core.novelai;

import com.exp.memoria.core.utils.Argon2Hasher;
import com.exp.memoria.core.utils.Byte2Img;

import org.json.JSONException;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;

public class GenerateImageTest {

    @Test
    public void testGenerateImageAndSaveToFile() throws JSONException, IOException {

        String hash = Argon2Hasher.argonHash(System.getenv("NAI_USERNAME"),System.getenv("NAI_PASSWORD"),64,"novelai_data_access_key");//生成访问密钥

        // Configure the proxy
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7897));

        // Create an OkHttpClient with the proxy
        OkHttpClient client = new OkHttpClient.Builder()
                .proxy(proxy)
                .build();

        // Create a Login instance with the custom client
        Login login = new Login(client);

        // Replace with your actual key
        String accessToken = login.login(hash);//生成最终账户令牌

        System.out.println(accessToken);


        // Pass the proxy-enabled client to the GenerateImage constructor
        GenerateImage generator = new GenerateImage(client);
       // String accessToken = "YOUR_ACCESS_TOKEN"; // <-- 替换为您的有效 Access Token
        String prompt = "1girl, best quality, amazing quality, very aesthetic, absurdres";
        String model = "nai-diffusion-3";
        String action = "generate";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("width", 832);
        parameters.put("height", 1216);
        parameters.put("scale", 5);
        parameters.put("sampler", "k_euler");
        parameters.put("steps", 28);
        parameters.put("n_samples", 1);
        parameters.put("ucPreset", 0);
        parameters.put("qualityToggle", true);
        parameters.put("uc", "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry");


        try {
            byte[] imageBytes = generator.generateImage(accessToken, prompt, model, action, parameters, 60, 3);

            assert imageBytes != null;
            assert imageBytes.length > 0;

            // Save the image using the utility class
            String outputFilePath = "F:/Project/Memoria/app/generate_image_test.png";
            Byte2Img.saveBytesAsImage(imageBytes, outputFilePath);

            System.out.println("Image saved to: " + outputFilePath);

        } catch (Exception e) {
            // 如果 Access Token 无效或网络请求失败，测试将在此处失败
            e.printStackTrace();
            assert false : "Test failed with exception: " + e.getMessage();
        }
    }
}
