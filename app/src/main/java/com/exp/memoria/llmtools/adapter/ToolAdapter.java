package com.exp.memoria.llmtools.adapter;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;

/**
 * An interface for adapting LLM-specific tool call requests.
 * 它负责解析一个 LLM 响应，执行所有请求的工具调用，
 * 并将结果格式化为该 LLM 期望的响应 JSON。
 */
public interface ToolAdapter {

    /**
     * 处理来自 LLM 助手的响应消息中的所有工具调用。
     *
     * @param llmAssistantMessage 包含工具调用请求的完整 JSON 响应消息。
     * @return 一个 JSON 对象列表，每个对象都是一个格式化的工具响应，
     * 准备作为下一轮对话历史发送回 LLM。
     * @throws JSONException 如果工具调用格式无效。
     */
    List<JSONObject> processToolCalls(JSONObject llmAssistantMessage) throws JSONException;
}