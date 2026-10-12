package com.teemo.testpapergeneration.ai;

import com.teemo.testpapergeneration.entity.QuestionBank;

/**
 * 大模型客户端接口。
 * <p>
 * 当前实现为 {@link OpenAiCompatibleClient}（兼容 OpenAI chat/completions 协议）。
 * 后续如需接入其他厂商（文心、通义、DeepSeek 等），新增实现类即可，上层 Service 无感知。
 */
public interface AiClient {

    /**
     * 调用大模型生成单题试题解析。
     *
     * @param question 题目（至少包含 topic / topic_type / answer）
     * @return 解析纯文本
     * @throws AiException 网络异常、超时、返回格式非法等
     */
    String generateAnalysis(QuestionBank question) throws AiException;
}
