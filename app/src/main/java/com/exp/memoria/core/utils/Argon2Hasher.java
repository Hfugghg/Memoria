package com.exp.memoria.core.utils;

import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.util.encoders.UrlBase64;

import java.nio.charset.StandardCharsets;

/**
 * 使用 Argon2 算法生成安全的哈希值
 */
public class Argon2Hasher {

    public static String argonHash(String email, String password, int size, String domain) {
        String passwordPart = password.length() > 6 ? password.substring(0, 6) : password;
        String preSalt = passwordPart + email + domain;
        byte[] preSaltBytes = preSalt.getBytes(StandardCharsets.UTF_8);

        Blake2bDigest blake2b = new Blake2bDigest(128);
        blake2b.update(preSaltBytes, 0, preSaltBytes.length);
        byte[] salt = new byte[16];
        blake2b.doFinal(salt, 0);

        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(2)
                .withMemoryAsKB(2000000 / 1024)
                .withParallelism(1)
                .withSalt(salt);

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(builder.build());

        byte[] rawHash = new byte[size];
        generator.generateBytes(password.getBytes(StandardCharsets.UTF_8), rawHash, 0, rawHash.length);

        String hashed = new String(UrlBase64.encode(rawHash), StandardCharsets.UTF_8);

        if (hashed.length() > 64) {
            return hashed.substring(0, 64);
        }

        return hashed;
    }
}
