package com.exp.memoria.core.llmtool.localtools;

import org.junit.Test;

public class LocalToolsTest {

    @Test
    public  void test01() {
        LocalTools localTools = new LocalTools();

        // 单个图像生成示例
        System.out.println("🚀 开始单个图像生成示例");
        localTools.imageGenerationExample("请按照我给出的参数生成图片,正向提示词:masterpiece, best quality, anime style, cinematic lighting, dynamic angle, 1girl playing in water at beach, wet clothes, accidental exposure, see-through, wardrobe malfunction, happy expression, splashing water, ocean waves, sunny day, sand, barefoot, youthful, medium breasts, blush, wet hair,反向提示词:lowres, bad quality, worst quality, jpeg artifacts, blurry, watermark, signature, text, cropped, out of frame, ugly, duplicate, morbid, mutilated, extra fingers, mutated hands, poorly drawn hands, poorly drawn face, deformed, bad anatomy, disfigured, malformed limbs, missing arms, missing legs, extra arms, extra legs, fused fingers, too many fingers, long neck .ucPreset:0,种子:0");

        //localTools.imageGenerationExample("请按照我给出的参数生成图片,正向提示词:masterpiece, best quality, anime style, cinematic lighting, close-up, dynamic angle, mature woman, squatting, legs spread, genital exposure, masturbating, wet, aroused expression, blush, detailed anatomy, focused, intimate scene, smooth skin, medium breasts, pubic hair, hands on genitals,反向提示词:lowres, bad quality, worst quality, jpeg artifacts, blurry, watermark, signature, text, cropped, out of frame, ugly, duplicate, morbid, mutilated, extra fingers, mutated hands, poorly drawn hands, poorly drawn face, deformed, bad anatomy, disfigured, malformed limbs, missing arms, missing legs, extra arms, extra legs, fused fingers, too many fingers, long neck, clothing, underwear, bra, pants, skirt.ucPreset设为3,种子随机");

        //localTools.imageGenerationExample("请帮我生成一张樱花盛开的日式庭院图片");
        // 批量图像生成示例
        // System.out.println("\n🚀 开始批量图像生成示例");
        // localTools.multipleImageGenerationExamples();
    }
}
