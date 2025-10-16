package com.exp.memoria.ui.settings

/**
 * 安全等级
 */
object HarmBlockThreshold {
    /**
     * 未指定阈值
     */
    const val HARM_BLOCK_THRESHOLD_UNSPECIFIED = "HARM_BLOCK_THRESHOLD_UNSPECIFIED"

    /**
     * 屏蔽极低风险及以上的内容
     */
    const val BLOCK_LOW_AND_ABOVE = "BLOCK_LOW_AND_ABOVE"

    /**
     * 屏蔽中等风险及以上的内容
     */
    const val BLOCK_MEDIUM_AND_ABOVE = "BLOCK_MEDIUM_AND_ABOVE"

    /**
     * 仅屏蔽高风险内容
     */
    const val BLOCK_ONLY_HIGH = "BLOCK_ONLY_HIGH"

    /**
     * 不屏蔽任何内容
     */
    const val BLOCK_NONE = "BLOCK_NONE"
}
