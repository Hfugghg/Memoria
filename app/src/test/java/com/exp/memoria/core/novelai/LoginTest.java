package com.exp.memoria.core.novelai;

import static org.junit.Assert.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

import okhttp3.OkHttpClient;
import okhttp3.Response;

public class LoginTest {
    @Test
    public void testLogin() throws IOException, JSONException {
        // Configure the proxy
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7897));

        // Create an OkHttpClient with the proxy
        OkHttpClient client = new OkHttpClient.Builder()
                .proxy(proxy)
                .build();
        // Create a Login instance with the custom client
        Login login = new Login(client);

        // Replace with your actual key
        String accessToken = login.login("");

        System.out.println(accessToken);
    }

    @Test
    public void test02(){
        Login login = new Login();

        System.out.println(login.getTest());
    }

    @Test
    public void test03(){
        Login login = new Login();




        System.out.println(login.test02());
    }
    int test;
    int test12;
    @Test
    public void test04() {
        if ((test == 0 )&&(true)) {
            test = 1;
        }
        if ((test12 == 0 )&&(true)) {
            test12 = 2;
        }
        System.out.println(test+" "+test12);

    }









}