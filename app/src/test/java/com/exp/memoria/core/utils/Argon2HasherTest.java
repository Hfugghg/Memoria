package com.exp.memoria.core.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class Argon2HasherTest {

    @Test
    public void test01(){


        String hash = Argon2Hasher.argonHash(System.getenv("NAI_USERNAME"),System.getenv("NAI_PASSWORD"),64,"novelai_data_access_key");
        System.out.println(hash);
    }



}