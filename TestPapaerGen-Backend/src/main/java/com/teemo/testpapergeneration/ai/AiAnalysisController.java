package com.teemo.testpapergeneration.ai;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.teemo.testpapergeneration.entity.QuestionBank;
import com.teemo.testpapergeneration.utils.MyJsonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 解析模块接口。
 * <p>
 * 全部接口位于登录拦截范围内（未登录不可访问），符合安全性需求。
 * 统一返回 {code, msg, data} 的 JSON 对象（application/json），
 * 避免 text/plain 与浏览器 Accept: application/json 冲突导致的 406。
 * <p>
 * 详细说明见 docs/05-接口文档.md。
 */
@RestController
@RequestMapping(value = "/aiAnalysis", produces = MediaType.APPLICATION_JSON_VALUE)
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    @Autowired
    public AiAnalysisController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    /**
     * 生成并保存单题解析。
     * POST /aiAnalysis/generate   body: {"questionId": 1}
     */
    @PostMapping(value = "/generate")
    public JSONObject generate(@RequestBody String payload) {
        try {
            JSONObject json = JSONObject.parseObject(payload);
            Integer questionId = json.getInteger("questionId");
            QuestionBank question = aiAnalysisService.generateAndSave(questionId);
            return ok(MyJsonResponse.default_200_response, question);
        } catch (AiException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return error("解析生成失败，请稍后重试");
        }
    }

    /**
     * 批量生成解析（顺序执行，单题失败不影响其他题）。
     * POST /aiAnalysis/batchGenerate   body: {"questionIds": [1, 2, 3]}
     */
    @PostMapping(value = "/batchGenerate")
    public JSONObject batchGenerate(@RequestBody String payload) {
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
                return error("参数错误：questionIds 不能为空");
            }
            return ok(MyJsonResponse.default_200_response, aiAnalysisService.batchGenerate(questionIds));
        } catch (Exception e) {
            return error("批量解析生成失败，请稍后重试");
        }
    }

    /**
     * 查询单题解析。
     * GET /aiAnalysis/{id}
     */
    @GetMapping(value = "/{id}")
    public JSONObject getAnalysis(@PathVariable("id") Integer id) {
        try {
            return ok(MyJsonResponse.default_200_response, aiAnalysisService.getAnalysis(id));
        } catch (AiException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return error("查询失败，请稍后重试");
        }
    }

    /**
     * 手动修改解析。
     * PUT /aiAnalysis/{id}   body: {"analysis": "..."}
     */
    @PutMapping(value = "/{id}")
    public JSONObject updateAnalysis(@PathVariable("id") Integer id, @RequestBody String payload) {
        try {
            JSONObject json = JSONObject.parseObject(payload);
            Integer status = aiAnalysisService.updateAnalysis(id, json.getString("analysis"));
            JSONObject ret = new JSONObject();
            ret.put("updateStatus", status);
            return ok(MyJsonResponse.default_200_response, ret);
        } catch (AiException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return error("保存失败，请稍后重试");
        }
    }

    /**
     * 查询模块运行状态（不返回 apiKey 明文）。
     * GET /aiAnalysis/status
     */
    @GetMapping(value = "/status")
    public JSONObject status() {
        return ok(MyJsonResponse.default_200_response, aiAnalysisService.status());
    }

    /**
     * 管理员运行时配置 AI 参数（密钥只写入内存，不落库、不写文件、不回显明文）。
     * POST /aiAnalysis/config   body: {"enabled":true,"apiKey":"sk-...","baseUrl":"...","model":"..."}
     * 仅 session 中 user_role=admin 可调用。
     */
    @PostMapping(value = "/config")
    public JSONObject saveConfig(@RequestBody(required = false) String payload, HttpSession session) {
        Object role = session.getAttribute("user_role");
        if (role == null || !"admin".equals(role.toString())) {
            return forbidden("仅管理员可配置 AI 参数");
        }
        try {
            JSONObject json = parseBody(payload);
            JSONObject status = aiAnalysisService.updateRuntimeConfig(
                    json.getBoolean("enabled"),
                    json.getString("baseUrl"),
                    json.getString("model"),
                    json.getString("apiKey"));
            return ok("AI 配置已生效（重启后恢复文件配置）", status);
        } catch (AiException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return error("配置保存失败，请检查参数格式");
        }
    }

    /**
     * 连通性测试：不保存配置，用传入的地址/模型/密钥做一次最小请求。
     * POST /aiAnalysis/test   body: {"baseUrl":"...","model":"...","apiKey":"..."}
     */
    @PostMapping(value = "/test")
    public JSONObject testConnection(@RequestBody(required = false) String payload) {
        try {
            JSONObject json = parseBody(payload);
            return ok(MyJsonResponse.default_200_response, aiAnalysisService.testConnection(
                    json.getString("baseUrl"), json.getString("model"), json.getString("apiKey")));
        } catch (Exception e) {
            return error("连通性测试失败，请检查参数");
        }
    }

    /**
     * 拉取模型列表（OpenAI 兼容 /models）。
     * POST /aiAnalysis/models   body: {"baseUrl":"...","apiKey":"..."}
     */
    @PostMapping(value = "/models")
    public JSONObject listModels(@RequestBody(required = false) String payload) {
        try {
            JSONObject json = parseBody(payload);
            return ok(MyJsonResponse.default_200_response,
                    aiAnalysisService.listModels(json.getString("baseUrl"), json.getString("apiKey")));
        } catch (AiException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            return error("模型列表获取失败，请稍后重试");
        }
    }

    // ---------- 响应构造 ----------

    private JSONObject ok(String msg, Object data) {
        JSONObject json = new JSONObject();
        json.put("code", 200);
        json.put("msg", msg);
        json.put("data", data);
        return json;
    }

    private JSONObject error(String msg) {
        JSONObject json = new JSONObject();
        json.put("code", 500);
        json.put("msg", msg);
        json.put("data", null);
        return json;
    }

    private JSONObject forbidden(String msg) {
        JSONObject json = new JSONObject();
        json.put("code", 403);
        json.put("msg", msg);
        json.put("data", null);
        return json;
    }

    private JSONObject parseBody(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return new JSONObject();
        }
        try {
            JSONObject json = JSONObject.parseObject(payload);
            return json == null ? new JSONObject() : json;
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}
