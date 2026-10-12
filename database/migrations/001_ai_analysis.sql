-- ============================================================
-- TestPaperGen - AI 解析模块 数据库增量脚本（v0.2）
--
-- 依赖：database/base/test_paper_generation_base.sql（上游原版结构）
-- 变更原则：仅新增字段，不修改、不删除任何既有列；可安全回滚
-- 适用数据库：MySQL 5.7+（utf8mb4）
-- ============================================================

USE `test_paper_generation`;

-- 1、QuestionBank 新增 analysis 字段（AI 生成的试题解析）
SET @db := DATABASE();
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'QuestionBank' AND COLUMN_NAME = 'analysis');
SET @sql := IF(@exists = 0,
  'ALTER TABLE `QuestionBank` ADD COLUMN `analysis` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT ''AI 生成的试题解析''',
  'SELECT ''column analysis already exists, skipped''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2、QuestionBank 新增 analysis_status 字段（0 无解析 / 1 已生成）
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'QuestionBank' AND COLUMN_NAME = 'analysis_status');
SET @sql := IF(@exists = 0,
  'ALTER TABLE `QuestionBank` ADD COLUMN `analysis_status` tinyint NOT NULL DEFAULT 0 COMMENT ''解析状态：0 无解析，1 已生成''',
  'SELECT ''column analysis_status already exists, skipped''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3、QuestionBank 新增 analysis_update_time 字段（解析最近更新时间）
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'QuestionBank' AND COLUMN_NAME = 'analysis_update_time');
SET @sql := IF(@exists = 0,
  'ALTER TABLE `QuestionBank` ADD COLUMN `analysis_update_time` datetime DEFAULT NULL COMMENT ''解析最近更新时间''',
  'SELECT ''column analysis_update_time already exists, skipped''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 回滚方式（如需撤销本次变更，执行以下三条语句）：
-- ALTER TABLE `QuestionBank` DROP COLUMN `analysis`;
-- ALTER TABLE `QuestionBank` DROP COLUMN `analysis_status`;
-- ALTER TABLE `QuestionBank` DROP COLUMN `analysis_update_time`;
-- ============================================================
