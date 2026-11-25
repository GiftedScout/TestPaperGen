package com.teemo.testpapergeneration.exam.grading;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnswerJudgeTest {

    private final AnswerJudge judge = new AnswerJudge();

    @Test
    void classifiesQuestionTypes() {
        assertEquals(AnswerJudge.Category.CHOICE, judge.categoryOf("选择题"));
        assertEquals(AnswerJudge.Category.CHOICE, judge.categoryOf("选择题"));
        assertEquals(AnswerJudge.Category.JUDGE, judge.categoryOf("判断题"));
        assertEquals(AnswerJudge.Category.FILL, judge.categoryOf("填空题"));
        assertEquals(AnswerJudge.Category.SUBJECTIVE, judge.categoryOf("简答题"));
        assertEquals(AnswerJudge.Category.SUBJECTIVE, judge.categoryOf("计算题"));
        assertEquals(AnswerJudge.Category.SUBJECTIVE, judge.categoryOf(null));
        assertTrue(judge.isObjective("选择题"));
        assertFalse(judge.isObjective("论述题"));
    }

    @Test
    void normalizeConvertsFullWidthAndCollapsesSpaces() {
        assertEquals("A B", judge.normalize("\u3000Ａ\u3000Ｂ "));
        assertEquals("", judge.normalize(null));
    }

    @Test
    void choiceIgnoresOrderCaseAndFullWidth() {
        assertTrue(judge.judgeChoice("AC", "c,a"));
        assertTrue(judge.judgeChoice("A", "ａ"));
        assertTrue(judge.judgeChoice("A", " A "));
        assertFalse(judge.judgeChoice("AC", "A"));
        assertFalse(judge.judgeChoice("AC", "ABC"));
        assertFalse(judge.judgeChoice("A", ""));
        assertFalse(judge.judgeChoice("A", "甲"));
    }

    @Test
    void trueFalseAcceptsSynonyms() {
        assertTrue(judge.judgeTrueFalse("对", "√"));
        assertTrue(judge.judgeTrueFalse("正确", "TRUE"));
        assertTrue(judge.judgeTrueFalse("错", "×"));
        assertTrue(judge.judgeTrueFalse("错误", "false"));
        assertFalse(judge.judgeTrueFalse("对", "错"));
        assertFalse(judge.judgeTrueFalse("对", "不确定"));
    }

    @Test
    void fillRequiresAllBlanksAndToleratesFormatting() {
        assertTrue(judge.judgeFill("北京|上海", "北京；上海"));
        assertTrue(judge.judgeFill("2|3", " 2 ; 3 "));
        assertTrue(judge.judgeFill("Hello|World", "hello|world"));
        assertTrue(judge.judgeFill("3.0", "3"));
        assertFalse(judge.judgeFill("北京|上海", "北京"));
        assertFalse(judge.judgeFill("北京|上海", "北京|"));
        assertFalse(judge.judgeFill("北京|上海", "上海|北京"));
        assertFalse(judge.judgeFill("3", "3.1"));
    }

    @Test
    void blankCountUsesConfirmedAnswerThenUnderlines() {
        assertEquals(3, judge.blankCount("题干", "a|b|c", true));
        assertEquals(2, judge.blankCount("____ 与 ____", null, false));
        assertEquals(1, judge.blankCount("没有下划线", null, false));

        StringBuilder manyBlanks = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            manyBlanks.append("___ ");
        }
        assertEquals(10, judge.blankCount(manyBlanks.toString(), null, false));
    }

    @Test
    void suggestionValidationNormalizesAndRejectsBadInput() {
        assertEquals("CA", judge.validateSuggestion("选择题", "c,a"));
        assertEquals("对", judge.validateSuggestion("判断题", "TRUE"));
        assertEquals("错", judge.validateSuggestion("判断题", "×"));
        assertEquals("x|y", judge.validateSuggestion("填空题", "x；y"));

        assertThrows(IllegalArgumentException.class, () -> judge.validateSuggestion("选择题", "123"));
        assertThrows(IllegalArgumentException.class, () -> judge.validateSuggestion("判断题", "也许"));
        assertThrows(IllegalArgumentException.class, () -> judge.validateSuggestion("填空题", "   "));
        assertThrows(IllegalArgumentException.class, () -> judge.validateSuggestion("简答题", "任意"));
    }

    @Test
    void equivalenceCoversSamplingStatistics() {
        assertTrue(judge.equivalent("选择题", "AC", "ca"));
        assertFalse(judge.equivalent("选择题", "AC", "A"));
        assertTrue(judge.equivalent("判断题", "对", "√"));
        assertTrue(judge.equivalent("填空题", "2|3", "2；3"));
        assertFalse(judge.equivalent("简答题", "a", "a"));
    }
}
