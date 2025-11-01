package com.exp.memoria.core.llmtool.localtools;

import com.exp.memoria.core.novelai.GenerateImage;
import com.exp.memoria.core.novelai.Login;
import com.exp.memoria.core.utils.Argon2Hasher;
import com.exp.memoria.core.utils.Byte2Img;

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


    private String accessKey = System.getenv("NAI_ACCESS_KEY");
    private  String novelaiName = System.getenv("NAI_USERNAME");
    private String novelaiPassword =System.getenv("NAI_PASSWORD");
    private String proxyhostname = "127.0.0.1";
    private int proxyport = 7897;

    private  String domain = "novelai_data_access_key";


    public void setDomain(String domain) {
        this.domain = domain;
    }
    public void setProxyhostname(String proxyhostname) {
        this.proxyhostname = proxyhostname;
    }



    public void setProxyport(int proxyport) {
        this.proxyport = proxyport;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setNovelaiName(String novelaiName) {
        this.novelaiName = novelaiName;
    }

    public void setNovelaiPassword(String novelaiPassword) {
        this.novelaiPassword = novelaiPassword;
    }


    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7897));
    String keyhash = Argon2Hasher.argonHash(novelaiName,novelaiPassword,64,domain);//生成访问密钥

    OkHttpClient client = new OkHttpClient.Builder()
            .proxy(proxy)
            .build();

    Login login = new Login(client);
    GenerateImage generator = new GenerateImage(client);


    private String accessKeyoraccessToken() throws JSONException, IOException {
        if (accessKey==null){
            String accessToken = login.login(keyhash);//使用账户和密码生成账户访问令牌
            return accessToken;
        }else{
            return accessKey;
        }
    }
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

    /**
     * v3模型图像生成的函数
     */
    private String novelai3GenerateImage(String prompt,String model,int width, int height,int scale,String sampler, int steps,int n_samples,int seed,int ucPreset,boolean qualityToggle,String uc){
        String action = "generate";

        if ((width == 0 )||(width >2048)) {
            width = 832;
        }
        if ((height == 0 )||(height >2048)) {
            height = 1216;
        }
        if ((scale == 0 )||(scale >10)) {
            scale = 5;
        }
        if (sampler ==null){
            sampler = "k_euler";
        }
        if ((steps==0)||(steps>50)) {
            steps = 28;
        }
        if ((n_samples==0)||(n_samples>4)) {
            n_samples = 1;
        }
        if ((seed==0)||(seed>2147483647)) {
            seed = 0;
        }
        if ((ucPreset==0)||(ucPreset>3)) {
            ucPreset = 0;
        }
        if (uc ==null){
            uc = "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry";
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("width",width);
        parameters.put("height",height);
        parameters.put("scale",scale);
        parameters.put("sampler",sampler);
        parameters.put("steps",steps);
        parameters.put("n_samples",n_samples);
        parameters.put("seed",seed);
        parameters.put("ucPreset",ucPreset);
        parameters.put("qualityToggle",qualityToggle);
        parameters.put("uc",uc);
        try {

            byte[] imageBytes = generator.generateImage(accessKeyoraccessToken(), prompt, model, action, parameters, 120, 3);

            assert imageBytes != null;
            assert imageBytes.length > 0;

            String outputFilePath = "F:/Project/Memoria/app/generate_image_v4_test.png";
            Byte2Img.saveBytesAsImage(imageBytes, outputFilePath);


            return "";


        } catch (IOException e) {
            throw new RuntimeException(e);

        } catch (JSONException e) {
            throw new RuntimeException(e);

        }


    }


}
