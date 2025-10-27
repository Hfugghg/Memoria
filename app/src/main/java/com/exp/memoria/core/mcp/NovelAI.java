package com.exp.memoria.core.mcp;

public class NovelAI {
    private String prompt;

    public String getPrompt() {
        System.out.println("读取提示"+prompt);

        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
        System.out.println("写入提示"+prompt);

    }

}
