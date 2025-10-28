package com.exp.memoria.llmtools.adapter;

import com.exp.memoria.llmtools.tool.ToolRegistry;
import java.util.HashMap;
import java.util.Map;

/**
 * 工厂类，用于根据 LLM 供应商创建并返回适当的 ToolAdapter。
 * 这是您架构中的主要“路由”点。
 */
public class ToolAdapterFactory {

    private final Map<String, ToolAdapter> adapters = new HashMap<>();

    /**
     * @param toolRegistry 包含所有已注册工具的注册表。
     */
    public ToolAdapterFactory(ToolRegistry toolRegistry) {
        // 在此处初始化所有支持的适配器
        adapters.put("openai", new OpenAiToolAdapter(toolRegistry));
        adapters.put("gemini", new GeminiToolAdapter(toolRegistry));
        adapters.put("deepseek", new OpenAiToolAdapter(toolRegistry));
        // ... 在此添加 AnthropicToolAdapter, GroqToolAdapter 等
    }

    /**
     * 获取指定供应商的适配器。
     * @param provider 供应商名称 (例如 "openai", "gemini")
     * @return 对应的 ToolAdapter
     * @throws IllegalArgumentException 如果供应商不受支持。
     */
    public ToolAdapter getAdapter(String provider) {
        ToolAdapter adapter = adapters.get(provider.toLowerCase());
        if (adapter == null) {
            throw new IllegalArgumentException("不支持的 LLM 供应商: " + provider);
        }
        return adapter;
    }
}
