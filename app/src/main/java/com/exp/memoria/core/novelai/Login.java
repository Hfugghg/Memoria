package com.exp.memoria.core.novelai;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class Login {

    private OkHttpClient client;

    public Login() {
        this.client = new OkHttpClient();
    }

    public Login(OkHttpClient client) {
        this.client = client;
    }

    public String login(String key) throws IOException, JSONException {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", key);
        String json = jsonObject.toString();

        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url("https://api.novelai.net/user/login")
                .post(body)
                .build();

        try (Response response = this.client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            JSONObject responseJson = new JSONObject(responseBody);
            System.out.println(responseJson);
            return responseJson.getString("accessToken");
        }
    }


}
