package com.exp.memoria.core.mcp;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class NovelAITest {

    private NovelAI novelAI;

    @Before
    public void setUp() {
        novelAI = new NovelAI();
    }
    @Test
    public void test01(){

        novelAI.setPrompt("Test");

    }

    @Test
    public void test02(){

        novelAI.getPrompt();

    }


}