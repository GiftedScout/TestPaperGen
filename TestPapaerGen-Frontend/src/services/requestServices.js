import { request } from 'umi';
import { API } from '../config/requestConfig';
import { parseParams } from '../utils/myUtils';

// login
export function login(payload) {
  const url = `${API}/login`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include'
  })
}

// getLoginStatus
export function getLoginStatus() {
  const url = `${API}/getLoginStatus`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include'
  })
}

// logout
export function logout() {
  const url = `${API}/logout`;
  return request(url, {
    method: 'post',
    mode: 'cors',
    credentials: 'include'
  })
}

// registered
export function registered(payload) {
  const url = `${API}/registered`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include'
  })
}

// getAllUser
export function getAllUser() {
  const url = `${API}/getAllUser`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include'
  })
}

// deleteUser
export function deleteUser(payload) {
  const url = `${API}/deleteUser`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    params: payload,
    credentials: 'include'
  })
}

// getApplyUser
export function getApplyUser() {
  const url = `${API}/getApplyUser`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include'
  })
}

// passApply
export function passApply(payload) {
  const url = `${API}/passApply`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    params: payload,
    credentials: 'include'
  })
}

// deleteApply
export function deleteApply(payload) {
  const url = `${API}/deleteApply`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    params: payload,
    credentials: 'include'
  })
}

// questionBank

export function getQuestionBank() {
  const url = `${API}/getAllQuestionBank`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include'
  })
}

export function deleteSingleQuestionBank(payload) {
  const url = `${API}/deleteSingleQuestionBank`;
  return request(url, {
    method: 'get',
    params: payload,
    mode: 'cors',
    credentials: 'include'
  })
}

export function getEachChapterCount() {
  const url = `${API}/getEachChapterCount`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include'
  })
}

export function getEachScoreCount() {
  const url = `${API}/getEachScoreCount`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include'
  })
}


// questionEdit

export function getAllQuestionLabels() {
  const url = `${API}/getAllQuestionLabels`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include'
  })
}

export function insertSingleQuestionBank(payload) {
  const url = `${API}/insertSingleQuestionBank`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include'
  })
}

export function getQuestionBankById(payload) {
  const url = `${API}/getQuestionBankById`;
  return request(url, {
    method: 'get',
    params: payload,
    mode: 'cors',
    credentials: 'include'
  })
}

export function updateQuestionBankById(payload) {
  const url = `${API}/updateQuestionBankById`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include'
  })
}

// questionGenerator
export function questionGen(payload) {
  const url = `${API}/questionGen`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include'
  })
}

export function randomSelect(payload) {
  const url = `${API}/randomSelect`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include'
  })
}

export function geneticSelect(payload) {
  const url = `${API}/geneticSelect`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include'
  })
}

export function downloadFile() {
  // 文件下载
  const a = document.createElement('a');
  document.body.append(a);
  const url = `${API}/getFile`;
  a.href = url;
  a.download = '试卷.docx';
  a.target = '_blank';
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

// questionGenHistory
export function getAllTestPaperGenHistory() {
  const url = `${API}/getAllTestPaperGenHistory`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include'
  })
}

export function getQuestionGenHistoriesByTestPaperUid(payload) {
  const url = `${API}/getQuestionGenHistoriesByTestPaperUid`;
  return request(url, {
    method: 'get',
    params: payload,
    mode: 'cors',
    credentials: 'include'
  })
}

export function deleteQuestionGenHistoryByTestPaperUid(payload) {
  const url = `${API}/deleteQuestionGenHistoryByTestPaperUid`;
  return request(url, {
    method: 'get',
    params: payload,
    mode: 'cors',
    credentials: 'include'
  })
}

export function uploadFile(payload) {
  const url = `${API}/upload`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include'
  })
}

// 新 生成word接口
export function questionGen2(payload) {
  const url = `${API}/questionGen2`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include',
    parseResponse: false // 关闭返回值简化
  })
}

// 重新导出word接口
export function reExportTestPaper(payload) {
  const url = `${API}/reExportTestPaper`;
  return request(url, {
    method: 'get',
    params: payload,
    mode: 'cors',
    credentials: 'include',
    parseResponse: false // 关闭返回值简化
  })
}

// 导出答案接口
export function exportAnswer(payload) {
  const url = `${API}/exportAnswer`;
  return request(url, {
    method: 'get',
    params: payload,
    mode: 'cors',
    credentials: 'include',
    parseResponse: false // 关闭返回值简化
  })
}

// 获取所有题目类型
export function getTopicType(payload) {
  const url = `${API}/getTopicType`;
  return request(url, {
    method: "get",
    params: payload,
    mode: "cors",
    credentials: "include",
  })
}

// 搜索题目
export function searchQuestionByTopic(payload) {
  const url = `${API}/searchQuestionByTopic`;
  return request(url, {
    method: "get",
    params: payload,
    mode: "cors",
    credentials: "include",
  })
}

// 新 更新已经组卷的题目
export function updateQuestionGenHistory(payload) {
  const url = `${API}/updateQuestionGenHistory`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include',
  })
}

// AI 解析模块，详见 docs/05-接口文档.md

// 生成并保存单题解析
export function generateAnalysis(payload) {
  const url = `${API}/aiAnalysis/generate`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include',
  })
}

// 批量生成解析
export function batchGenerateAnalysis(payload) {
  const url = `${API}/aiAnalysis/batchGenerate`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include',
  })
}

// 查询单题解析
export function getAnalysis(payload) {
  const url = `${API}/aiAnalysis/${payload.id}`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include',
  })
}

// 手动修改解析
export function updateAnalysis(payload) {
  const url = `${API}/aiAnalysis/${payload.id}`;
  return request(url, {
    method: 'put',
    data: { analysis: payload.analysis },
    mode: 'cors',
    credentials: 'include',
  })
}

// 查询 AI 模块运行状态（含脱敏 Key 和配置标志，不含明文）
export function getAiStatus() {
  const url = `${API}/aiAnalysis/status`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include',
  })
}

// 管理员运行时配置 AI 参数（密钥只写入后端内存，不持久化）
export function saveAiConfig(payload) {
  const url = `${API}/aiAnalysis/config`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include',
  })
}

// 连通性测试（不保存配置）
export function testAiConnection(payload) {
  const url = `${API}/aiAnalysis/test`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include',
  })
}

// 拉取模型列表
export function listAiModels(payload) {
  const url = `${API}/aiAnalysis/models`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include',
  })
}

// ------------------------------------------------------------------
// 任务包 A：考试发布、作答、结果与报告
// ------------------------------------------------------------------

// 教师/管理员：发布试卷
export function publishExam(payload) {
  const url = `${API}/exam/publish`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include',
  })
}

// 教师/管理员：已发布列表
export function getExamPublishList() {
  const url = `${API}/exam/publish/list`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include',
  })
}

// 教师/管理员：某次发布的提交列表，payload = {id}
export function getExamSubmissions(payload) {
  const url = `${API}/exam/publish/${payload.id}/submissions`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include',
  })
}

// 教师/管理员：班级报告，payload = {id}
export function getClassReport(payload) {
  const url = `${API}/exam/publish/${payload.id}/report`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include',
  })
}

// 教师/管理员：可选学生列表
export function getExamStudents() {
  const url = `${API}/exam/students`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include',
  })
}

// 当前登录身份
export function getExamMe() {
  const url = `${API}/exam/me`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include',
  })
}

// 学生：我的考试列表
export function getStudentPapers() {
  const url = `${API}/exam/student/papers`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include',
  })
}

// 学生：按发布 id 获取答题试卷，payload = {publishId}
export function getStudentPaper(payload) {
  const url = `${API}/exam/student/papers/${payload.publishId}`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include',
  })
}

// 学生：提交作答，payload = {publishId, answers}
export function submitStudentPaper(payload) {
  const url = `${API}/exam/student/papers/${payload.publishId}/submit`;
  return request(url, {
    method: 'post',
    data: {answers: payload.answers},
    mode: 'cors',
    credentials: 'include',
  })
}

// 学生：作答结果，payload = {publishId}
export function getStudentResult(payload) {
  const url = `${API}/exam/student/papers/${payload.publishId}/result`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include',
  })
}

// 学生：学习报告，payload = {publishId?}
export function getStudentReport(payload) {
  const url = `${API}/exam/report/student`;
  return request(url, {
    method: 'get',
    params: payload || {},
    mode: 'cors',
    credentials: 'include',
  })
}

// ------------------------------------------------------------------
// 任务包 A：答案治理
// ------------------------------------------------------------------

// 待确认答案列表，payload = {keyword?, topicType?, lowOnly?}
export function getAnswerPending(payload) {
  const url = `${API}/exam/answers/pending`;
  return request(url, {
    method: 'get',
    params: payload || {},
    mode: 'cors',
    credentials: 'include',
  })
}

// 批量反推答案建议，payload = {questionIds}
export function suggestAnswers(payload) {
  const url = `${API}/exam/answers/suggest`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include',
  })
}

// 采纳/编辑后确认，payload = {auditId, finalAnswer}
export function confirmAnswer(payload) {
  const url = `${API}/exam/answers/confirm`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include',
  })
}

// 驳回建议，payload = {auditId, reason}
export function rejectAnswer(payload) {
  const url = `${API}/exam/answers/reject`;
  return request(url, {
    method: 'post',
    data: payload,
    mode: 'cors',
    credentials: 'include',
  })
}

// 某题的审计记录，payload = {questionId}
export function getAnswerAudits(payload) {
  const url = `${API}/exam/answers/audits/${payload.questionId}`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include',
  })
}

// 按题型分层抽检准确率
export function getAnswerSampling() {
  const url = `${API}/exam/answers/sampling`;
  return request(url, {
    method: 'get',
    mode: 'cors',
    credentials: 'include',
  })
}