package com.exp.memoria.core.openai.deepseek;

import org.junit.Test;

public class DeepSeekCalculatorDemoTest {

    @Test
    public void test01() throws Exception {
        // 从环境变量或安全的地方获取 API 密钥
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("警告: DEEPSEEK_API_KEY 环境变量未设置，跳过测试。");
            return; // 如果没有密钥，则跳过测试
        }

        DeepSeekCalculatorDemo demo = new DeepSeekCalculatorDemo(apiKey);

        System.out.println("🧮 DeepSeek 计算器 Function Calling 演示");
        System.out.println("=".repeat(50));

        // 演示单个计算
        demo.simpleCalculatorExample();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("🔢 多个计算演示");

        // 演示多个计算
        demo.multipleCalculationsExample();
    }
}