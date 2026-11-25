package com.teemo.testpapergeneration.exam.service;

import com.alibaba.fastjson.JSONObject;
import com.teemo.testpapergeneration.entity.QuestionBank;
import com.teemo.testpapergeneration.entity.QuestionGenHistory;
import com.teemo.testpapergeneration.exam.dto.ResultQuestionView;
import com.teemo.testpapergeneration.exam.dto.StudentPaperView;
import com.teemo.testpapergeneration.exam.dto.StudentQuestionView;
import com.teemo.testpapergeneration.exam.dto.StudentResultView;
import com.teemo.testpapergeneration.exam.entity.ExamAnswer;
import com.teemo.testpapergeneration.exam.entity.ExamPublish;
import com.teemo.testpapergeneration.exam.entity.ExamSubmission;
import com.teemo.testpapergeneration.exam.grading.AnswerJudge;
import com.teemo.testpapergeneration.exam.mapper.*;
import com.teemo.testpapergeneration.exam.ws.ExamNotifier;
import com.teemo.testpapergeneration.mapper.QuestionBankMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 任务包 A：发布、作答、提交、结果与报告。
 */
@Service
public class ExamService {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final ExamPublishMapper publishMapper;
    private final ExamSubmissionMapper submissionMapper;
    private final ExamAnswerMapper answerMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamUserMapper userMapper;
    private final QuestionBankMapper questionBankMapper;
    private final AnswerJudge answerJudge;
    private final ExamNotifier notifier;

    public ExamService(ExamPublishMapper publishMapper,
                       ExamSubmissionMapper submissionMapper,
                       ExamAnswerMapper answerMapper,
                       ExamPaperMapper paperMapper,
                       ExamUserMapper userMapper,
                       QuestionBankMapper questionBankMapper,
                       AnswerJudge answerJudge,
                       ExamNotifier notifier) {
        this.publishMapper = publishMapper;
        this.submissionMapper = submissionMapper;
        this.answerMapper = answerMapper;
        this.paperMapper = paperMapper;
        this.userMapper = userMapper;
        this.questionBankMapper = questionBankMapper;
        this.answerJudge = answerJudge;
        this.notifier = notifier;
    }

    // ------------------------------------------------------------------
    // 教师端：发布与进度
    // ------------------------------------------------------------------

    @Transactional
    public JSONObject publish(String publisher, String paperUid, String scope,
                              List<String> studentUsernames, Date deadline) {
        if (isBlank(paperUid)) {
            throw ExamException.badRequest("请选择要发布的试卷");
        }
        if (deadline == null) {
            throw ExamException.badRequest("请设置截止时间");
        }
        if (!deadline.after(new Date())) {
            throw ExamException.badRequest("截止时间必须晚于当前时间");
        }
        List<QuestionGenHistory> histories = paperMapper.listHistoriesByPaperUid(paperUid);
        if (histories == null || histories.isEmpty()) {
            throw ExamException.badRequest("试卷不存在或没有题目，无法发布");
        }
        String paperName = paperMapper.getPaperName(paperUid);
        if (isBlank(paperName)) {
            paperName = histories.get(0).getTest_paper_name();
        }

        List<String> targets = resolveTargets(scope, studentUsernames);
        if (targets.isEmpty()) {
            throw ExamException.badRequest("没有可发布的学生");
        }

        double totalScore = 0.0;
        for (QuestionGenHistory history : histories) {
            totalScore += history.getScore() == null ? 0.0 : history.getScore();
        }

        Date now = new Date();
        ExamPublish publish = new ExamPublish();
        publish.setTest_paper_uid(paperUid);
        publish.setTest_paper_name(paperName);
        publish.setPublisher(publisher);
        publish.setTarget_scope("all".equalsIgnoreCase(scope) ? "all" : "students");
        publish.setDeadline(deadline);
        publish.setTotal_score(round1(totalScore));
        publish.setQuestion_count(histories.size());
        publish.setCreate_time(now);
        publishMapper.insert(publish);

        List<ExamSubmission> submissions = new ArrayList<>();
        for (String student : targets) {
            ExamSubmission submission = new ExamSubmission();
            submission.setPublish_id(publish.getId());
            submission.setStudent_username(student);
            submission.setStatus(ExamSubmission.STATUS_ASSIGNED);
            submission.setTotal_score(round1(totalScore));
            submission.setScore(0.0);
            submission.setPending_count(0);
            submission.setUngraded_count(0);
            submissions.add(submission);
        }
        submissionMapper.batchInsert(submissions);

        JSONObject event = new JSONObject();
        event.put("publishId", publish.getId());
        event.put("paperName", paperName);
        event.put("deadline", formatDate(deadline));
        event.put("teacher", publisher);
        for (String student : targets) {
            notifier.notifyUser(student, "paper.published", event);
        }

        JSONObject result = new JSONObject();
        result.put("publishId", publish.getId());
        result.put("paperName", paperName);
        result.put("targetCount", targets.size());
        return result;
    }

    public List<JSONObject> listPublishes(String username, boolean admin) {
        List<ExamPublish> publishes = admin
                ? publishMapper.listAll()
                : publishMapper.listByPublisher(username);
        List<JSONObject> result = new ArrayList<>();
        Date now = new Date();
        for (ExamPublish publish : publishes) {
            JSONObject item = publishJson(publish);
            item.put("targetCount", submissionMapper.countTargets(publish.getId()));
            item.put("submittedCount", submissionMapper.countSubmitted(publish.getId()));
            item.put("expired", publish.getDeadline() != null && publish.getDeadline().before(now));
            result.add(item);
        }
        return result;
    }

    public JSONObject listSubmissions(Integer publishId, String username, boolean admin) {
        ExamPublish publish = requirePublish(publishId);
        requireOwnerOrAdmin(publish, username, admin);
        JSONObject result = new JSONObject();
        result.put("publish", publishJson(publish));
        List<JSONObject> students = new ArrayList<>();
        for (ExamSubmission submission : submissionMapper.listByPublish(publishId)) {
            JSONObject item = new JSONObject();
            item.put("studentUsername", submission.getStudent_username());
            item.put("status", submission.getStatus());
            item.put("submitTime", formatDate(submission.getSubmit_time()));
            item.put("totalScore", submission.getTotal_score());
            item.put("score", ExamSubmission.STATUS_SUBMITTED.equals(submission.getStatus())
                    ? submission.getScore() : null);
            item.put("pendingCount", submission.getPending_count());
            item.put("ungradedCount", submission.getUngraded_count());
            students.add(item);
        }
        result.put("students", students);
        return result;
    }

    public JSONObject classReport(Integer publishId, String username, boolean admin) {
        ExamPublish publish = requirePublish(publishId);
        requireOwnerOrAdmin(publish, username, admin);
        List<QuestionGenHistory> histories = paperMapper.listHistoriesByPaperUid(publish.getTest_paper_uid());
        Map<Integer, QuestionGenHistory> historyMap = historyMap(histories);

        Map<String, DimensionAgg> chapters = new LinkedHashMap<>();
        Map<String, DimensionAgg> labels = new LinkedHashMap<>();
        List<JSONObject> students = new ArrayList<>();

        for (ExamSubmission submission : submissionMapper.listByPublish(publishId)) {
            JSONObject item = new JSONObject();
            item.put("studentUsername", submission.getStudent_username());
            item.put("status", submission.getStatus());
            item.put("submitTime", formatDate(submission.getSubmit_time()));
            item.put("score", ExamSubmission.STATUS_SUBMITTED.equals(submission.getStatus())
                    ? submission.getScore() : null);
            item.put("totalScore", submission.getTotal_score());
            item.put("pendingCount", submission.getPending_count());
            item.put("ungradedCount", submission.getUngraded_count());
            students.add(item);

            if (ExamSubmission.STATUS_SUBMITTED.equals(submission.getStatus())) {
                for (ExamAnswer answer : answerMapper.listBySubmission(submission.getId())) {
                    accumulate(chapters, labels, historyMap.get(answer.getQuestion_history_id()), answer);
                }
            }
        }

        JSONObject result = new JSONObject();
        result.put("publish", publishJson(publish));
        result.put("students", students);
        result.put("chapters", dimensionList(chapters));
        result.put("labels", dimensionList(labels));
        return result;
    }

    public List<String> eligibleStudents() {
        List<String> students = userMapper.listStudentUsernames();
        return students == null ? new ArrayList<>() : students;
    }

    // ------------------------------------------------------------------
    // 学生端：列表、答题、提交、结果
    // ------------------------------------------------------------------

    public List<JSONObject> studentPapers(String username) {
        List<ExamSubmission> submissions = submissionMapper.listByStudent(username);
        List<JSONObject> result = new ArrayList<>();
        Date now = new Date();
        for (ExamSubmission submission : submissions) {
            ExamPublish publish = publishMapper.getById(submission.getPublish_id());
            if (publish == null) {
                continue;
            }
            JSONObject item = new JSONObject();
            item.put("publishId", publish.getId());
            item.put("paperName", publish.getTest_paper_name());
            item.put("publisher", publish.getPublisher());
            item.put("deadline", formatDate(publish.getDeadline()));
            item.put("totalScore", publish.getTotal_score());
            item.put("questionCount", publish.getQuestion_count());
            boolean submitted = ExamSubmission.STATUS_SUBMITTED.equals(submission.getStatus());
            boolean expired = publish.getDeadline() != null && publish.getDeadline().before(now);
            item.put("status", submitted ? "submitted" : (expired ? "expired" : "assigned"));
            item.put("submitted", submitted);
            item.put("expired", expired);
            item.put("score", submitted ? submission.getScore() : null);
            item.put("pendingCount", submission.getPending_count());
            item.put("ungradedCount", submission.getUngraded_count());
            result.add(item);
        }
        Collections.sort(result, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject left, JSONObject right) {
                Integer leftId = left.getInteger("publishId");
                Integer rightId = right.getInteger("publishId");
                return rightId == null ? -1 : (leftId == null ? 1 : rightId.compareTo(leftId));
            }
        });
        return result;
    }

    public StudentPaperView paperForAnswering(Integer publishId, String username) {
        ExamSubmission submission = submissionMapper.get(publishId, username);
        if (submission == null) {
            throw ExamException.forbidden("无权访问该试卷");
        }
        ExamPublish publish = requirePublish(publishId);
        if (ExamSubmission.STATUS_SUBMITTED.equals(submission.getStatus())) {
            throw ExamException.badRequest("该试卷已提交，请查看结果");
        }
        if (publish.getDeadline() != null && publish.getDeadline().before(new Date())) {
            throw ExamException.badRequest("该试卷已截止");
        }

        StudentPaperView view = new StudentPaperView();
        view.setPublishId(publish.getId());
        view.setPaperName(publish.getTest_paper_name());
        view.setPublisher(publish.getPublisher());
        view.setDeadline(formatDate(publish.getDeadline()));
        view.setSubmitted(false);
        view.setTotalScore(publish.getTotal_score());
        List<StudentQuestionView> questions = new ArrayList<>();
        List<QuestionGenHistory> histories = paperMapper.listHistoriesByPaperUid(publish.getTest_paper_uid());
        for (QuestionGenHistory history : histories) {
            QuestionBank bank = bankOf(history.getQuestion_bank_id());
            boolean confirmed = isConfirmed(bank);
            StudentQuestionView question = new StudentQuestionView();
            question.setQuestionHistoryId(history.getId());
            question.setTopic(history.getTopic());
            question.setTopicType(history.getTopic_type());
            question.setScore(history.getScore());
            question.setChapter1(history.getChapter_1());
            question.setChapter2(history.getChapter_2());
            question.setLabel1(history.getLabel_1());
            question.setLabel2(history.getLabel_2());
            question.setBlankCount(answerJudge.blankCount(history.getTopic(),
                    bank == null ? null : bank.getAnswer(), confirmed));
            questions.add(question);
        }
        view.setQuestionCount(questions.size());
        view.setQuestions(questions);
        return view;
    }

    @Transactional
    public JSONObject submit(Integer publishId, String username, Map<Integer, String> answerMap) {
        ExamSubmission submission = submissionMapper.get(publishId, username);
        if (submission == null) {
            throw ExamException.forbidden("无权提交该试卷");
        }
        if (ExamSubmission.STATUS_SUBMITTED.equals(submission.getStatus())) {
            throw ExamException.badRequest("该试卷已提交，不可重复提交");
        }
        ExamPublish publish = requirePublish(publishId);
        if (publish.getDeadline() != null && publish.getDeadline().before(new Date())) {
            throw ExamException.badRequest("该试卷已截止，无法提交");
        }

        List<QuestionGenHistory> histories = paperMapper.listHistoriesByPaperUid(publish.getTest_paper_uid());
        Map<Integer, QuestionGenHistory> historyMap = historyMap(histories);
        for (Integer questionId : answerMap.keySet()) {
            if (!historyMap.containsKey(questionId)) {
                throw ExamException.badRequest("提交内容包含不属于本试卷的题目：" + questionId);
            }
        }

        Date now = new Date();
        double earned = 0.0;
        double objectivePossible = 0.0;
        int pendingCount = 0;
        int ungradedCount = 0;
        List<ExamAnswer> rows = new ArrayList<>();

        for (QuestionGenHistory history : histories) {
            String raw = answerMap.get(history.getId());
            String answerText = raw == null ? "" : raw.trim();
            QuestionBank bank = bankOf(history.getQuestion_bank_id());
            boolean confirmed = isConfirmed(bank);
            AnswerJudge.Category category = answerJudge.categoryOf(history.getTopic_type());

            ExamAnswer row = new ExamAnswer();
            row.setSubmission_id(submission.getId());
            row.setQuestion_history_id(history.getId());
            row.setQuestion_bank_id(history.getQuestion_bank_id());
            row.setAnswer_raw(answerText);
            row.setJudge_time(now);

            if (category == AnswerJudge.Category.SUBJECTIVE) {
                row.setJudge_result(ExamAnswer.RESULT_PENDING);
                row.setJudge_mode(ExamAnswer.MODE_PENDING);
                row.setScore_awarded(0.0);
                pendingCount++;
            } else if (!confirmed) {
                row.setJudge_result(ExamAnswer.RESULT_UNGRADED);
                row.setJudge_mode(ExamAnswer.MODE_AUTO);
                row.setScore_awarded(0.0);
                ungradedCount++;
            } else {
                objectivePossible += history.getScore() == null ? 0.0 : history.getScore();
                boolean correct = answerJudge.judge(category, bank.getAnswer(), answerText);
                row.setJudge_mode(ExamAnswer.MODE_AUTO);
                row.setJudge_result(correct ? ExamAnswer.RESULT_CORRECT : ExamAnswer.RESULT_WRONG);
                double awarded = correct && history.getScore() != null ? history.getScore() : 0.0;
                row.setScore_awarded(awarded);
                earned += awarded;
            }
            rows.add(row);
        }

        answerMapper.batchInsert(rows);

        submission.setSubmit_time(now);
        submission.setScore(round1(earned));
        submission.setPending_count(pendingCount);
        submission.setUngraded_count(ungradedCount);
        int updated = submissionMapper.markSubmitted(submission);
        if (updated == 0) {
            throw ExamException.badRequest("该试卷已提交，不可重复提交");
        }

        JSONObject teacherEvent = new JSONObject();
        teacherEvent.put("publishId", publish.getId());
        teacherEvent.put("paperName", publish.getTest_paper_name());
        teacherEvent.put("student", username);
        teacherEvent.put("score", round1(earned));
        teacherEvent.put("submitTime", formatDate(now));
        notifier.notifyUser(publish.getPublisher(), "submission.received", teacherEvent);

        JSONObject studentEvent = new JSONObject();
        studentEvent.put("publishId", publish.getId());
        studentEvent.put("paperName", publish.getTest_paper_name());
        studentEvent.put("score", round1(earned));
        notifier.notifyUser(username, "result.ready", studentEvent);

        JSONObject result = new JSONObject();
        result.put("publishId", publish.getId());
        result.put("score", round1(earned));
        result.put("totalScore", publish.getTotal_score());
        result.put("objectivePossible", round1(objectivePossible));
        result.put("pendingCount", pendingCount);
        result.put("ungradedCount", ungradedCount);
        return result;
    }

    public StudentResultView result(Integer publishId, String username) {
        ExamSubmission submission = submissionMapper.get(publishId, username);
        if (submission == null) {
            throw ExamException.forbidden("无权查看该试卷结果");
        }
        if (!ExamSubmission.STATUS_SUBMITTED.equals(submission.getStatus())) {
            throw ExamException.badRequest("提交后才能查看成绩与解析");
        }
        ExamPublish publish = requirePublish(publishId);
        List<QuestionGenHistory> histories = paperMapper.listHistoriesByPaperUid(publish.getTest_paper_uid());
        Map<Integer, ExamAnswer> answerMap = new HashMap<>();
        for (ExamAnswer answer : answerMapper.listBySubmission(submission.getId())) {
            answerMap.put(answer.getQuestion_history_id(), answer);
        }

        double objectivePossible = 0.0;
        List<ResultQuestionView> questions = new ArrayList<>();
        for (QuestionGenHistory history : histories) {
            QuestionBank bank = bankOf(history.getQuestion_bank_id());
            boolean confirmed = isConfirmed(bank);
            AnswerJudge.Category category = answerJudge.categoryOf(history.getTopic_type());
            ExamAnswer answer = answerMap.get(history.getId());

            ResultQuestionView view = new ResultQuestionView();
            view.setQuestionHistoryId(history.getId());
            view.setTopic(history.getTopic());
            view.setTopicType(history.getTopic_type());
            view.setScore(history.getScore());
            view.setChapter1(history.getChapter_1());
            view.setChapter2(history.getChapter_2());
            view.setLabel1(history.getLabel_1());
            view.setLabel2(history.getLabel_2());
            view.setYourAnswer(answer == null ? "" : answer.getAnswer_raw());
            view.setJudgeResult(answer == null ? ExamAnswer.RESULT_PENDING : answer.getJudge_result());
            view.setScoreAwarded(answer == null ? 0.0 : answer.getScore_awarded());
            if (category != AnswerJudge.Category.SUBJECTIVE) {
                objectivePossible += history.getScore() == null ? 0.0 : history.getScore();
            }
            if (confirmed) {
                view.setCorrectAnswer(bank.getAnswer());
                view.setAnalysis(isBlank(bank.getAnalysis()) ? "暂无解析" : bank.getAnalysis());
            } else {
                view.setCorrectAnswer(null);
                view.setAnalysis("该题答案尚未经教师确认，暂不提供解析");
            }
            if (ExamAnswer.RESULT_PENDING.equals(view.getJudgeResult())) {
                view.setReason("主观题待教师评分，不计入自动评分");
            } else if (ExamAnswer.RESULT_UNGRADED.equals(view.getJudgeResult())) {
                view.setReason("该题答案尚未经教师确认，本题不计分");
            } else if (ExamAnswer.RESULT_WRONG.equals(view.getJudgeResult())) {
                view.setReason("作答与标准答案不一致");
            } else {
                view.setReason("");
            }
            questions.add(view);
        }

        StudentResultView result = new StudentResultView();
        result.setPublishId(publish.getId());
        result.setPaperName(publish.getTest_paper_name());
        result.setScore(submission.getScore());
        result.setTotalScore(publish.getTotal_score());
        result.setObjectivePossible(round1(objectivePossible));
        result.setPendingCount(submission.getPending_count());
        result.setUngradedCount(submission.getUngraded_count());
        result.setQuestions(questions);
        return result;
    }

    // ------------------------------------------------------------------
    // 学习报告：实时聚合，不落表
    // ------------------------------------------------------------------

    public JSONObject studentReport(String username, Integer publishId) {
        List<ExamSubmission> submissions = submissionMapper.listByStudent(username);
        Map<String, DimensionAgg> chapters = new LinkedHashMap<>();
        Map<String, DimensionAgg> labels = new LinkedHashMap<>();
        List<JSONObject> submitSummaries = new ArrayList<>();
        double totalPossible = 0.0;
        double totalEarned = 0.0;
        int pendingTotal = 0;
        int ungradedTotal = 0;

        for (ExamSubmission submission : submissions) {
            if (!ExamSubmission.STATUS_SUBMITTED.equals(submission.getStatus())) {
                continue;
            }
            if (publishId != null && !publishId.equals(submission.getPublish_id())) {
                continue;
            }
            ExamPublish publish = publishMapper.getById(submission.getPublish_id());
            if (publish == null) {
                continue;
            }
            List<QuestionGenHistory> histories = paperMapper.listHistoriesByPaperUid(publish.getTest_paper_uid());
            Map<Integer, QuestionGenHistory> historyMap = historyMap(histories);
            for (ExamAnswer answer : answerMapper.listBySubmission(submission.getId())) {
                accumulate(chapters, labels, historyMap.get(answer.getQuestion_history_id()), answer);
                if (ExamAnswer.RESULT_CORRECT.equals(answer.getJudge_result())
                        || ExamAnswer.RESULT_WRONG.equals(answer.getJudge_result())) {
                    QuestionGenHistory history = historyMap.get(answer.getQuestion_history_id());
                    if (history != null) {
                        totalPossible += history.getScore() == null ? 0.0 : history.getScore();
                        totalEarned += answer.getScore_awarded() == null ? 0.0 : answer.getScore_awarded();
                    }
                }
            }
            pendingTotal += submission.getPending_count() == null ? 0 : submission.getPending_count();
            ungradedTotal += submission.getUngraded_count() == null ? 0 : submission.getUngraded_count();

            JSONObject summary = new JSONObject();
            summary.put("publishId", publish.getId());
            summary.put("paperName", publish.getTest_paper_name());
            summary.put("score", submission.getScore());
            summary.put("totalScore", submission.getTotal_score());
            summary.put("submitTime", formatDate(submission.getSubmit_time()));
            submitSummaries.add(summary);
        }

        JSONObject result = new JSONObject();
        result.put("totalPossible", round1(totalPossible));
        result.put("totalEarned", round1(totalEarned));
        result.put("rate", rate(totalEarned, totalPossible));
        result.put("pendingCount", pendingTotal);
        result.put("ungradedCount", ungradedTotal);
        result.put("submissions", submitSummaries);
        result.put("chapters", dimensionList(chapters));
        result.put("labels", dimensionList(labels));
        return result;
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private List<String> resolveTargets(String scope, List<String> requested) {
        LinkedHashSet<String> targets = new LinkedHashSet<>();
        if ("all".equalsIgnoreCase(scope)) {
            List<String> all = userMapper.listStudentUsernames();
            if (all != null) {
                targets.addAll(all);
            }
            return new ArrayList<>(targets);
        }
        if (requested == null || requested.isEmpty()) {
            throw ExamException.badRequest("请选择至少一名学生");
        }
        for (String username : requested) {
            if (isBlank(username)) {
                continue;
            }
            String trimmed = username.trim();
            if (userMapper.countStudent(trimmed) == 0) {
                throw ExamException.badRequest("学生不存在或未通过审核：" + trimmed);
            }
            targets.add(trimmed);
        }
        return new ArrayList<>(targets);
    }

    private ExamPublish requirePublish(Integer publishId) {
        ExamPublish publish = publishMapper.getById(publishId);
        if (publish == null) {
            throw ExamException.badRequest("发布记录不存在");
        }
        return publish;
    }

    private void requireOwnerOrAdmin(ExamPublish publish, String username, boolean admin) {
        if (!admin && !publish.getPublisher().equals(username)) {
            throw ExamException.forbidden("只有发布教师或管理员可以查看");
        }
    }

    private JSONObject publishJson(ExamPublish publish) {
        JSONObject item = new JSONObject();
        item.put("id", publish.getId());
        item.put("testPaperUid", publish.getTest_paper_uid());
        item.put("paperName", publish.getTest_paper_name());
        item.put("publisher", publish.getPublisher());
        item.put("scope", publish.getTarget_scope());
        item.put("deadline", formatDate(publish.getDeadline()));
        item.put("totalScore", publish.getTotal_score());
        item.put("questionCount", publish.getQuestion_count());
        item.put("createTime", formatDate(publish.getCreate_time()));
        return item;
    }

    private Map<Integer, QuestionGenHistory> historyMap(List<QuestionGenHistory> histories) {
        Map<Integer, QuestionGenHistory> map = new HashMap<>();
        for (QuestionGenHistory history : histories) {
            map.put(history.getId(), history);
        }
        return map;
    }

    private QuestionBank bankOf(Integer bankId) {
        if (bankId == null) {
            return null;
        }
        List<QuestionBank> banks = questionBankMapper.getQuestionBankById(bankId);
        return banks == null || banks.isEmpty() ? null : banks.get(0);
    }

    private boolean isConfirmed(QuestionBank bank) {
        return bank != null && bank.getAnswer_confirm_status() != null
                && bank.getAnswer_confirm_status() == 1;
    }

    private void accumulate(Map<String, DimensionAgg> chapters, Map<String, DimensionAgg> labels,
                            QuestionGenHistory history, ExamAnswer answer) {
        if (history == null) {
            return;
        }
        String chapter = isBlank(history.getChapter_1()) ? "未标注章节" : history.getChapter_1();
        DimensionAgg chapterAgg = chapters.get(chapter);
        if (chapterAgg == null) {
            chapterAgg = new DimensionAgg();
            chapters.put(chapter, chapterAgg);
        }
        chapterAgg.add(history, answer);

        String label = isBlank(history.getLabel_1()) ? "未标注知识点" : history.getLabel_1();
        DimensionAgg labelAgg = labels.get(label);
        if (labelAgg == null) {
            labelAgg = new DimensionAgg();
            labels.put(label, labelAgg);
        }
        labelAgg.add(history, answer);
    }

    private List<JSONObject> dimensionList(Map<String, DimensionAgg> map) {
        List<JSONObject> list = new ArrayList<>();
        for (Map.Entry<String, DimensionAgg> entry : map.entrySet()) {
            DimensionAgg agg = entry.getValue();
            JSONObject item = new JSONObject();
            item.put("name", entry.getKey());
            item.put("possible", round1(agg.possible));
            item.put("earned", round1(agg.earned));
            item.put("rate", rate(agg.earned, agg.possible));
            item.put("questionCount", agg.questionCount);
            item.put("correctCount", agg.correctCount);
            item.put("pendingCount", agg.pendingCount);
            list.add(item);
        }
        Collections.sort(list, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject left, JSONObject right) {
                Double l = left.getDouble("rate");
                Double r = right.getDouble("rate");
                double lv = l == null ? 0.0 : l.doubleValue();
                double rv = r == null ? 0.0 : r.doubleValue();
                int compare = Double.compare(lv, rv);
                if (compare != 0) {
                    return compare;
                }
                return String.valueOf(left.getString("name")).compareTo(String.valueOf(right.getString("name")));
            }
        });
        return list;
    }

    private static class DimensionAgg {
        private double possible;
        private double earned;
        private int questionCount;
        private int correctCount;
        private int pendingCount;

        private void add(QuestionGenHistory history, ExamAnswer answer) {
            questionCount++;
            if (ExamAnswer.RESULT_CORRECT.equals(answer.getJudge_result())) {
                correctCount++;
            }
            if (ExamAnswer.RESULT_PENDING.equals(answer.getJudge_result())
                    || ExamAnswer.RESULT_UNGRADED.equals(answer.getJudge_result())) {
                pendingCount++;
                return;
            }
            possible += history.getScore() == null ? 0.0 : history.getScore();
            earned += answer.getScore_awarded() == null ? 0.0 : answer.getScore_awarded();
        }
    }

    private double rate(double earned, double possible) {
        if (possible <= 0) {
            return 0.0;
        }
        return Math.round(earned / possible * 10000.0) / 100.0;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String formatDate(Date date) {
        return date == null ? null : new SimpleDateFormat(DATE_PATTERN).format(date);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
