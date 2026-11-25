-- ============================================================
-- TestPaperGen - 任务包 A：学生端、自动判题与综合报告
-- 迁移脚本 002（本包占用 002 段）
--
-- 依赖：database/base/test_paper_generation_base.sql 与 001_ai_analysis.sql
-- 变更原则：
--   1. 既有列只放宽长度，不删除、不改语义；
--   2. 新表名一律小写，适配 lower_case_table_names=1；
--   3. 可重复执行（幂等）；
--   4. 不新建试卷快照表：题干/分值/题型/章节复用 QuestionGenHistory。
--
-- 适用数据库：MySQL 5.7+ / 8.0（utf8mb4）
-- ============================================================

USE `test_paper_generation`;

-- ------------------------------------------------------------
-- 1、放宽答案长度：多空填空题答案可能超过 255 字符
-- ------------------------------------------------------------
ALTER TABLE `QuestionBank`
  MODIFY COLUMN `answer` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci
  DEFAULT NULL COMMENT '标准答案，多空填空题使用 | 分隔';
ALTER TABLE `QuestionGenHistory`
  MODIFY COLUMN `answer` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci
  DEFAULT NULL COMMENT '标准答案快照，多空填空题使用 | 分隔';

-- ------------------------------------------------------------
-- 2、QuestionBank 答案治理字段（AI 建议与人工确认分离）
--    注意：AI 反推只写 answer_suggest，绝不直接覆盖 answer。
-- ------------------------------------------------------------
SET @db := DATABASE();

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'QuestionBank' AND COLUMN_NAME = 'answer_confirm_status');
SET @sql := IF(@exists = 0,
  'ALTER TABLE `QuestionBank` ADD COLUMN `answer_confirm_status` tinyint NOT NULL DEFAULT 0 COMMENT ''答案确认状态：0 未确认，1 已确认''',
  'SELECT ''column answer_confirm_status already exists, skipped''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'QuestionBank' AND COLUMN_NAME = 'answer_suggest');
SET @sql := IF(@exists = 0,
  'ALTER TABLE `QuestionBank` ADD COLUMN `answer_suggest` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT ''AI 反推建议值（未生效）''',
  'SELECT ''column answer_suggest already exists, skipped''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'QuestionBank' AND COLUMN_NAME = 'answer_confidence');
SET @sql := IF(@exists = 0,
  'ALTER TABLE `QuestionBank` ADD COLUMN `answer_confidence` decimal(5,4) DEFAULT NULL COMMENT ''建议置信度 0~1''',
  'SELECT ''column answer_confidence already exists, skipped''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'QuestionBank' AND COLUMN_NAME = 'answer_suggest_source');
SET @sql := IF(@exists = 0,
  'ALTER TABLE `QuestionBank` ADD COLUMN `answer_suggest_source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT ''建议来源：ai / mock''',
  'SELECT ''column answer_suggest_source already exists, skipped''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'QuestionBank' AND COLUMN_NAME = 'answer_confirmed_by');
SET @sql := IF(@exists = 0,
  'ALTER TABLE `QuestionBank` ADD COLUMN `answer_confirmed_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT ''答案确认人''',
  'SELECT ''column answer_confirmed_by already exists, skipped''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'QuestionBank' AND COLUMN_NAME = 'answer_confirmed_time');
SET @sql := IF(@exists = 0,
  'ALTER TABLE `QuestionBank` ADD COLUMN `answer_confirmed_time` datetime DEFAULT NULL COMMENT ''答案确认时间''',
  'SELECT ''column answer_confirmed_time already exists, skipped''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 3、考试发布
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `exam_publish` (
  `id` int NOT NULL AUTO_INCREMENT,
  `test_paper_uid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '试卷唯一标识（QuestionGenHistory.test_paper_uid）',
  `test_paper_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '试卷名称快照',
  `publisher` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '发布教师用户名',
  `target_scope` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'students' COMMENT '发布范围：students 指定学生 / all 全班',
  `deadline` datetime NOT NULL COMMENT '截止时间',
  `total_score` decimal(10,1) DEFAULT '0.0' COMMENT '卷面总分',
  `question_count` int DEFAULT '0' COMMENT '题目数量',
  `create_time` datetime DEFAULT NULL COMMENT '发布时间',
  PRIMARY KEY (`id`),
  KEY `idx_exam_publish_publisher` (`publisher`),
  KEY `idx_exam_publish_paper` (`test_paper_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='试卷发布记录';

-- ------------------------------------------------------------
-- 4、学生提交状态（发布时即为每个目标学生建一行 assigned）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `exam_submission` (
  `id` int NOT NULL AUTO_INCREMENT,
  `publish_id` int NOT NULL COMMENT 'exam_publish.id',
  `student_username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '学生用户名',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'assigned' COMMENT 'assigned 待完成 / submitted 已提交',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `total_score` decimal(10,1) DEFAULT '0.0' COMMENT '卷面总分',
  `score` decimal(10,1) DEFAULT '0.0' COMMENT '自动判分得分',
  `pending_count` int DEFAULT '0' COMMENT '主观题待评数量',
  `ungraded_count` int DEFAULT '0' COMMENT '答案未确认不计分数量',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_submission` (`publish_id`, `student_username`),
  KEY `idx_exam_submission_student` (`student_username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学生对一次发布的提交状态';

-- ------------------------------------------------------------
-- 5、逐题作答与判分结果
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `exam_answer` (
  `id` int NOT NULL AUTO_INCREMENT,
  `submission_id` int NOT NULL COMMENT 'exam_submission.id',
  `question_history_id` int NOT NULL COMMENT 'QuestionGenHistory.id',
  `question_bank_id` int DEFAULT NULL COMMENT 'QuestionBank.id',
  `answer_raw` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '学生原始作答；多空填空用 | 分隔',
  `judge_result` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'correct / wrong / pending / ungraded',
  `judge_mode` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'auto / pending',
  `score_awarded` decimal(10,1) NOT NULL DEFAULT '0.0' COMMENT '本题得分',
  `judge_time` datetime DEFAULT NULL COMMENT '判分时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_answer` (`submission_id`, `question_history_id`),
  KEY `idx_exam_answer_history` (`question_history_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='逐题作答与判分结果';

-- ------------------------------------------------------------
-- 6、答案治理审计（AI 建议 / 人工确认 / 驳回 / 失败全留痕）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `question_answer_audit` (
  `id` int NOT NULL AUTO_INCREMENT,
  `question_bank_id` int NOT NULL COMMENT 'QuestionBank.id',
  `topic_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '题型快照',
  `old_answer` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '反推前的原值',
  `suggested_answer` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'AI 建议值',
  `confirmed_answer` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '人工确认后的最终值',
  `confidence` decimal(5,4) DEFAULT NULL COMMENT '建议置信度 0~1',
  `source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'ai / mock',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'suggested / confirmed / rejected / failed',
  `fail_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '处理失败或驳回原因',
  `confirm_user` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '确认/驳回人',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认/驳回时间',
  `create_time` datetime DEFAULT NULL COMMENT '记录创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_answer_audit_question` (`question_bank_id`),
  KEY `idx_answer_audit_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='答案治理审计记录';

-- ------------------------------------------------------------
-- 7、教师角色标记（仅改角色标签，不改登录逻辑）
--    既有题库/组卷接口只校验登录，不校验 user_role，故不受影响。
--    admin 天然具备教师权限。新增教师由管理员执行：
--    UPDATE `User` SET user_role='teacher' WHERE username='xxx';
-- ------------------------------------------------------------
UPDATE `User` SET `user_role` = 'teacher'
 WHERE `username` IN ('teacher001', 'teacher002') AND `user_role` = 'user';

-- ============================================================
-- 回滚方式（按需执行）：
-- UPDATE `User` SET user_role='user' WHERE username IN ('teacher001','teacher002') AND user_role='teacher';
-- DROP TABLE IF EXISTS `question_answer_audit`;
-- DROP TABLE IF EXISTS `exam_answer`;
-- DROP TABLE IF EXISTS `exam_submission`;
-- DROP TABLE IF EXISTS `exam_publish`;
-- ALTER TABLE `QuestionBank` DROP COLUMN `answer_confirmed_time`;
-- ALTER TABLE `QuestionBank` DROP COLUMN `answer_confirmed_by`;
-- ALTER TABLE `QuestionBank` DROP COLUMN `answer_suggest_source`;
-- ALTER TABLE `QuestionBank` DROP COLUMN `answer_confidence`;
-- ALTER TABLE `QuestionBank` DROP COLUMN `answer_suggest`;
-- ALTER TABLE `QuestionBank` DROP COLUMN `answer_confirm_status`;
-- ALTER TABLE `QuestionBank` MODIFY COLUMN `answer` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL;
-- ALTER TABLE `QuestionGenHistory` MODIFY COLUMN `answer` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL;
-- ============================================================
