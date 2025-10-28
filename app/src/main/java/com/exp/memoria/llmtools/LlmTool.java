package com.exp.memoria.llmtools;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 所有本地工具都必须实现的通用接口。
 */
public interface LlmTool {

    /**
     * 获取此工具的唯一名称，LLM 将使用此名称来调用它。
     * @return 工具名称 (例如 "calculate")
     */
    String getToolName();

    /**
     * 执行工具。
     * @param arguments LLM 提供的 JSON 格式的参数。
     * @return 一个代表执行结果的字符串（例如 "15.0"）。
     * @throws JSONException 如果参数格式不正确。
     */
    String execute(JSONObject arguments) throws JSONException;
}
