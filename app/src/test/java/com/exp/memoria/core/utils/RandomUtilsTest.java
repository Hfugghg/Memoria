package com.exp.memoria.core.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class RandomUtilsTest {

    @Test
    public void testGenerateRandomInt() {
        // 调用 generateRandomInt 方法一百次，确保每次生成的值都在范围内
        for (int i = 0; i < 100; i++) {
            int randomInt = RandomUtils.generateRandomInt();
            assertTrue("生成的随机数 " + randomInt + " 应该大于等于 0", randomInt >= 0);
            assertTrue("生成的随机数 " + randomInt + " 应该小于等于 " + Integer.MAX_VALUE, randomInt <= Integer.MAX_VALUE);
        }
    }

    @Test
    public void testGenerateRandomInt01() {
        System.out.println(RandomUtils.generateRandomInt());
    }
}
