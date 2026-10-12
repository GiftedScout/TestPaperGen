package com.teemo.testpapergeneration.ai;

/**
 * AI 解析模块统一异常。
 * 调用方（Controller）捕获后转换为友好提示返回前端，不影响原有组卷主流程。
 */
public class AiException extends Exception {

    public AiException(String message) {
        super(message);
    }

    public AiException(String message, Throwable cause) {
        super(message, cause);
    }
}
