package com.teemo.testpapergeneration.exam.service;

/**
 * 任务包 A 的业务异常：code 为对外返回码（403 越权 / 500 业务冲突）。
 */
public class ExamException extends RuntimeException {

    private final int code;

    public ExamException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ExamException forbidden(String message) {
        return new ExamException(403, message);
    }

    public static ExamException badRequest(String message) {
        return new ExamException(500, message);
    }
}
