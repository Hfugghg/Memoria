package com.exp.memoria.core.llmtool.localtools;

import com.exp.memoria.core.novelai.GenerateImage;
import com.exp.memoria.core.novelai.Login;
import com.exp.memoria.core.utils.Argon2Hasher;

import okhttp3.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Contains local tools that can be called by the LLM.
 */
public class LocalTools {

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
     * 执行图像生成的函数
     */
    private String novelaiGenerateImage(){
        // 配置代理
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7897));

        //生成key
        String key = Argon2Hasher.argonHash(System.getenv("NAI_USERNAME"),System.getenv("NAI_PASSWORD"),64,"novelai_data_access_key");//生成访问密钥

        OkHttpClient client = new OkHttpClient.Builder()
                .proxy(proxy)
                .build();
        // Create a Login instance with the custom client
        Login login = new Login(client);
        GenerateImage generator = new GenerateImage(client);
        // Replace with your actual key
        try {
            String accessToken = login.login(key);//生成最终账户令牌
            return "";


        } catch (IOException e) {
            throw new RuntimeException(e);

        } catch (JSONException e) {
            throw new RuntimeException(e);

        }


    }


}
