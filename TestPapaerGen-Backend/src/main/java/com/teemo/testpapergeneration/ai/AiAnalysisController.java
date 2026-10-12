package com.teemo.testpapergeneration.ai;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.teemo.testpapergeneration.entity.QuestionBank;
import com.teemo.testpapergeneration.utils.MyJsonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 解析模块接口。
 * <p>
 * 全部接口位于登录拦截范围内（未登录不可访问），符合安全性需求。
 * 接口风格与上游保持一致：返回 {code, msg, data} JSON 字符串。
 * <p>
 * 详细说明见 docs/05-接口文档.md。
 */
@RestController
@RequestMapping(value = "/aiAnalysis")
public class AiAnalysisController {

    private final MyJsonResponse myJsonResponse = new MyJsonResponse();

    private final AiAnalysisService aiAnalysisService;

    @Autowired
    public AiAnalysisController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    /**
     * 生成并保存单题解析。
     * POST /aiAnalysis/generate   body: {"questionId": 1}
     */
    @PostMapping(value = "/generate", produces = "text/plain;charset=UTF-8")
    public String generate(@RequestBody String payload) {
        try {
            JSONObject json = JSONObject.parseObject(payload);
            Integer questionId = json.getInteger("questionId");
            QuestionBank question = aiAnalysisService.generateAndSave(questionId);
            return myJsonResponse.make200Resp(MyJsonResponse.default_200_response, question);
        } catch (AiException e) {
            return makeAiError(e);
        } catch (Exception e) {
            return myJsonResponse.make500Resp("解析生成失败，请稍后重试", null);
        }
    }

    /**
     * 批量生成解析（顺序执行，单题失败不影响其他题）。
     * POST /aiAnalysis/batchGenerate   body: {"questionIds": [1, 2, 3]}
     */
    @PostMapping(value = "/batchGenerate", produces = "text/plain;charset=UTF-8")
    public String batchGenerate(@RequestBody String payload) {
        try {
            JSONObject json = JSONObject.parseObject(payload);
            JSONArray idArray = json.getJSONArray("questionIds");
            List<Integer> questionIds = new ArrayList<Integer>();
            if (idArray != null) {
                for (int i = 0; i < idArray.size(); i++) {
                    questionIds.add(idArray.getInteger(i));
                }
            }
            if (questionIds.isEmpty()) {
                return myJsonResponse.make500Resp("参数错误：questionIds 不能为空", null);
            }
            List<JSONObject> results = aiAnalysisService.batchGenerate(questionIds);
            return myJsonResponse.make200Resp(MyJsonResponse.default_200_response, results);
        } catch (Exception e) {
            return myJsonResponse.make500Resp("批量解析生成失败，请稍后重试", null);
        }
    }

    /**
     * 查询单题解析。
     * GET /aiAnalysis/{id}
     */
    @GetMapping(value = "/{id}", produces = "text/plain;charset=UTF-8")
    public String getAnalysis(@PathVariable("id") Integer id) {
        try {
            QuestionBank question = aiAnalysisService.getAnalysis(id);
            return myJsonResponse.make200Resp(MyJsonResponse.default_200_response, question);
        } catch (AiException e) {
            return makeAiError(e);
        } catch (Exception e) {
            return myJsonResponse.make500Resp("查询失败，请稍后重试", null);
        }
    }

    /**
     * 手动修改解析。
     * PUT /aiAnalysis/{id}   body: {"analysis": "..."}
     */
    @PutMapping(value = "/{id}", produces = "text/plain;charset=UTF-8")
    public String updateAnalysis(@PathVariable("id") Integer id, @RequestBody String payload) {
        try {
            JSONObject json = JSONObject.parseObject(payload);
            String analysis = json.getString("analysis");
            Integer status = aiAnalysisService.updateAnalysis(id, analysis);
            JSONObject ret = new JSONObject();
            ret.put("updateStatus", status);
            return myJsonResponse.make200Resp(MyJsonResponse.default_200_response, ret);
        } catch (AiException e) {
            return makeAiError(e);
        } catch (Exception e) {
            return myJsonResponse.make500Resp("保存失败，请稍后重试", null);
        }
    }

    /**
     * 查询模块运行状态（不返回 apiKey）。
     * GET /aiAnalysis/status
     */
    @GetMapping(value = "/status", produces = "text/plain;charset=UTF-8")
    public String status() {
        return myJsonResponse.make200Resp(MyJsonResponse.default_200_response, aiAnalysisService.status());
    }

    /** AI 异常 -> 友好提示（HTTP 层仍为 200，前端按 code 判断） */
    private String makeAiError(AiException e) {
        return myJsonResponse.make500Resp(e.getMessage(), null);
    }
}
