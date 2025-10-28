package com.exp.memoria.llmtools.tool;

import android.util.Log;

import com.exp.memoria.llmtools.LlmTool;

import java.util.HashMap;
import java.util.Map;

/**
 * 负责注册和管理所有 LlmTool 实例的单例。
 * Adapter 将使用此类来按名称查找和执行工具。
 */
public class ToolRegistry {

    private static final String TAG = "ToolRegistry";
    private static ToolRegistry instance;
    private final Map<String, LlmTool> tools = new HashMap<>();

    /**
     * 私有构造函数，在此处注册所有工具。
     * 在 Android 中，如果某个工具需要 Context，您可以传入 Application Context。
     */
    private ToolRegistry() {
        // 在此处注册所有可用的工具
        registerTool(new CalculatorTool());
        // 示例: registerTool(new WeatherTool(applicationContext));
    }

    /**
     * 获取 ToolRegistry 的单例实例。
     */
    public static synchronized ToolRegistry getInstance() {
        if (instance == null) {
            instance = new ToolRegistry();
        }
        return instance;
    }

    public void registerTool(LlmTool tool) {
        tools.put(tool.getToolName(), tool);
        Log.d(TAG, "已注册工具: " + tool.getToolName());
    }

    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }

    public LlmTool getTool(String toolName) {
        return tools.get(toolName);
    }
}