package com.exp.memoria.llmtools.tool; // 移动到 'tool' 子包

import android.util.Log; // 导入 Android Log

import com.exp.memoria.llmtools.LlmTool;

import org.json.JSONException;
import org.json.JSONObject;

// 实现新的 LlmTool 接口
public class CalculatorTool implements LlmTool {

    private static final String TAG = "CalculatorTool";
    public static final String TOOL_NAME = "calculate";

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public String execute(JSONObject arguments) throws JSONException {
        String expression = arguments.getString("expression");
        return calculateExpression(expression);
    }

    private String calculateExpression(String expression) {
        try {
            // 简单的表达式计算（实际项目中可以使用 ScriptEngine 等更安全的方式）
            expression = expression.replace(" ", "").toLowerCase();
            expression = expression.replace("×", "*");
            expression = expression.replace("÷", "/");

            if (expression.contains("+")) {
                String[] parts = expression.split("\\+");
                double result = Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
                Log.d(TAG, "执行了加法: " + expression); // 使用 Log.d
                return String.valueOf(result);
            } else if (expression.contains("-")) {
                String[] parts = expression.split("-");
                double result = Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
                return String.valueOf(result);
            } else if (expression.contains("*")) {
                String[] parts = expression.split("\\*");
                double result = Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
                Log.d(TAG, "执行了乘法: " + expression); // 使用 Log.d
                return String.valueOf(result);
            } else if (expression.contains("/")) {
                String[] parts = expression.split("/");
                if (Double.parseDouble(parts[1]) == 0) {
                    return "错误：除数不能为零";
                }
                double result = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
                return String.valueOf(result);
            } else if (expression.contains("^")) {
                String[] parts = expression.split("\\^");
                double base = Double.parseDouble(parts[0]);
                double exponent = Double.parseDouble(parts[1]);
                double result = Math.pow(base, exponent);
                return String.valueOf(result);
            }

            return "无法计算的表达式: " + expression;

        } catch (Exception e) {
            return "计算错误: " + e.getMessage();
        }
    }
}