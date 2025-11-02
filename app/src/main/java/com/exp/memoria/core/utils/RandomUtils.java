package com.exp.memoria.core.utils;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机数生成工具类
 * 可以生成0-2147483647（包含边界）的随机数
 */
public class RandomUtils {

    /**
     * 生成一个在0到2147483647（包含边界）的随机整数。
     *
     * @return 0到2147483647之间的随机整数。
     */
    public static int generateRandomInt() {
        return ThreadLocalRandom.current().nextInt() & Integer.MAX_VALUE;
    }
}
