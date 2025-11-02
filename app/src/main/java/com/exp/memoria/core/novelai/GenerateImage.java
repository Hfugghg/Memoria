package com.exp.memoria.core.novelai;

import androidx.annotation.NonNull;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GenerateImage {

    private String base_url = "https://image.novelai.net";


    public void setBase_url(String base_url) {
        this.base_url = base_url;
    }
    public String getBase_url() {
        return base_url;
    }

    //private static final String BASE_URL = "https://image.novelai.net";
    private  OkHttpClient client;

    public GenerateImage(OkHttpClient client) {
        this.client = client;
    }


    public byte[] generateImage(String accessToken, String prompt, String model, String action, Map<String, Object> parameters, Integer timeout, Integer retryCount) throws IOException, JSONException {
        JSONObject data = new JSONObject();
        data.put("input", prompt);
        data.put("model", model);
        data.put("action", action);
        data.put("parameters", new JSONObject(parameters));

        OkHttpClient.Builder clientBuilder = this.client.newBuilder();

        if (timeout != null) {
            clientBuilder.callTimeout(timeout, TimeUnit.SECONDS);
        }

        if (retryCount != null && retryCount > 1) {
            clientBuilder.addInterceptor(new RetryInterceptor(retryCount));
        }

        OkHttpClient finalClient = clientBuilder.build();

        RequestBody body = RequestBody.create(data.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(base_url + "/ai/generate-image")
                .post(body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = finalClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.out.println("RAW ERROR BODY: " + response.body().string());
                throw new IOException("Unexpected code " + response);
            }
            if (response.body() == null) {
                return null;
            }

            // NovelAI API returns a zip file, we need to extract the first entry which is the image.
            byte[] responseBytes = response.body().bytes();
            try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(responseBytes))) {
                ZipEntry entry = zipInputStream.getNextEntry();
                if (entry != null) {
                    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zipInputStream.read(buffer)) > 0) {
                        byteOut.write(buffer, 0, len);
                    }
                    zipInputStream.closeEntry();
                    return byteOut.toByteArray();
                } else {
                    String errorBody = new String(responseBytes);
                    throw new IOException("No zip entry found in the response. Body: " + errorBody);
                }
            }
        }
    }

    private static class RetryInterceptor implements Interceptor {
        private final int maxRetry;

        RetryInterceptor(int maxRetry) {
            this.maxRetry = maxRetry;
        }

        @NonNull
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = chain.proceed(request);

            int tryCount = 0;
            List<Integer> retryableCodes = Arrays.asList(429, 500, 502, 503, 504);

            while (!response.isSuccessful() && tryCount < maxRetry && retryableCodes.contains(response.code())) {
                tryCount++;
                try {
                    long backoff = (long) (Math.pow(2, tryCount - 1));
                    Thread.sleep(backoff * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", e);
                }
                response.close();
                response = chain.proceed(request);
            }

            return response;
        }
    }
}
