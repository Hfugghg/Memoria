package com.exp.memoria.core.openai.deepseek;

import static org.junit.Assert.*;

import org.junit.Test;

public class DeepSeekDemo02Test {




    @Test
    public void test01() {
        DeepSeekDemo02 demo = new DeepSeekDemo02();
        System.out.println("=== DeepSeek Function Calling Demo ===");
        demo.weatherFunctionCallingExample();
    }

}