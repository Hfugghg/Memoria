package com.exp.memoria.core.openai.deepseek;

import static org.junit.Assert.*;

import org.junit.Test;

public class DeepSeekCalculatorDemoTest {

    @Test
    public  void test01() {
        DeepSeekCalculatorDemo demo = new DeepSeekCalculatorDemo();

        System.out.println("🧮 DeepSeek 计算器 Function Calling 演示");
        System.out.println("=" .repeat(50));

        // 演示单个计算
        demo.simpleCalculatorExample();

        System.out.println("\n" + "=" .repeat(50));
        System.out.println("🔢 多个计算演示");

        // 演示多个计算
        demo.multipleCalculationsExample();
    }

}