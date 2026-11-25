package com.teemo.testpapergeneration.exam.grading;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 判题引擎：与 Web 层完全解耦，可直接单元测试。
 *
 * 判分口径（对应任务书第 7 节）：
 * 1. 仅客观题（选择 / 判断 / 填空）自动判分，其余题型一律待评；
 * 2. 多选不给部分分（集合完全相等才算对）；
 * 3. 填空全部空正确才得满分，不做按空比例给分；
 * 4. 容错：全角/半角、大小写、多选顺序、数字等价形式、判断题同义写法；
 * 5. 仅使用「人工确认后」的标准答案，未确认题由上层标记为不计分。
 */
@Component
public class AnswerJudge {

    public enum Category {
        CHOICE, JUDGE, FILL, SUBJECTIVE
    }

    private static final Pattern UNDERLINE = Pattern.compile("_{3,}");

    public Category categoryOf(String topicType) {
        if (topicType == null) {
            return Category.SUBJECTIVE;
        }
        if (topicType.contains("选择")) {
            return Category.CHOICE;
        }
        if (topicType.contains("判断")) {
            return Category.JUDGE;
        }
        if (topicType.contains("填空")) {
            return Category.FILL;
        }
        return Category.SUBJECTIVE;
    }

    public boolean isObjective(String topicType) {
        return categoryOf(topicType) != Category.SUBJECTIVE;
    }

    /**
     * 归一化：全角转半角、去首尾空白、连续空白折叠为一个空格。
     */
    public String normalize(String value) {
        if (value == null) {
            return "";
        }
        char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '\u3000') {
                chars[i] = ' ';
            } else if (c >= '\uFF01' && c <= '\uFF5E') {
                chars[i] = (char) (c - 0xFEE0);
            }
        }
        return new String(chars).trim().replaceAll("\\s+", " ");
    }

    private String compact(String value) {
        return normalize(value).replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    public Set<Character> choiceLetters(String value) {
        Set<Character> letters = new LinkedHashSet<>();
        String normalized = normalize(value).toUpperCase(Locale.ROOT);
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c >= 'A' && c <= 'J') {
                letters.add(c);
            }
        }
        return letters;
    }

    public boolean judgeChoice(String expected, String actual) {
        Set<Character> expectedLetters = choiceLetters(expected);
        Set<Character> actualLetters = choiceLetters(actual);
        return !expectedLetters.isEmpty() && expectedLetters.equals(actualLetters);
    }

    public Boolean parseTrueFalse(String value) {
        String v = compact(value);
        if (v.isEmpty()) {
            return null;
        }
        if ("对".equals(v) || "正确".equals(v) || "√".equals(v) || "✓".equals(v) || "✔".equals(v)
                || "t".equals(v) || "true".equals(v) || "1".equals(v) || "是".equals(v)
                || "y".equals(v) || "yes".equals(v) || "真".equals(v) || "对的".equals(v)) {
            return Boolean.TRUE;
        }
        if ("错".equals(v) || "错误".equals(v) || "×".equals(v) || "x".equals(v) || "✗".equals(v)
                || "✘".equals(v) || "f".equals(v) || "false".equals(v) || "0".equals(v)
                || "否".equals(v) || "n".equals(v) || "no".equals(v) || "假".equals(v) || "错的".equals(v)) {
            return Boolean.FALSE;
        }
        return null;
    }

    public boolean judgeTrueFalse(String expected, String actual) {
        Boolean expectedValue = parseTrueFalse(expected);
        Boolean actualValue = parseTrueFalse(actual);
        return expectedValue != null && expectedValue.equals(actualValue);
    }

    /**
     * 多空填空拆分：支持 |、;、；、换行；忽略空段（任一空未作答即失分）。
     */
    public List<String> splitFill(String raw) {
        List<String> parts = new ArrayList<>();
        if (raw == null) {
            return parts;
        }
        String value = raw.replace('\u3000', ' ');
        String[] fragments = value.split("\\s*(?:\\||；|;|\\r?\\n)\\s*");
        for (String fragment : fragments) {
            String trimmed = fragment.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
        return parts;
    }

    public boolean judgeFill(String expected, String actual) {
        List<String> expectedParts = splitFill(expected);
        List<String> actualParts = splitFill(actual);
        if (expectedParts.isEmpty() || actualParts.size() != expectedParts.size()) {
            return false;
        }
        for (int i = 0; i < expectedParts.size(); i++) {
            if (!fillEquals(expectedParts.get(i), actualParts.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean fillEquals(String expected, String actual) {
        String a = compact(expected);
        String b = compact(actual);
        if (a.equals(b)) {
            return true;
        }
        try {
            return new BigDecimal(a).compareTo(new BigDecimal(b)) == 0;
        } catch (NumberFormatException ignore) {
            return false;
        }
    }

    public boolean judge(Category category, String expected, String actual) {
        switch (category) {
            case CHOICE:
                return judgeChoice(expected, actual);
            case JUDGE:
                return judgeTrueFalse(expected, actual);
            case FILL:
                return judgeFill(expected, actual);
            default:
                return false;
        }
    }

    /**
     * 填空题空数：已确认答案按分隔符计算；未确认时按题干下划线数量估算。
     */
    public int blankCount(String topic, String expected, boolean answerConfirmed) {
        if (answerConfirmed) {
            int count = splitFill(expected).size();
            if (count >= 1 && count <= 10) {
                return count;
            }
        }
        int count = 0;
        if (topic != null) {
            Matcher matcher = UNDERLINE.matcher(topic);
            while (matcher.find()) {
                count++;
            }
        }
        if (count < 1) {
            count = 1;
        }
        return Math.min(count, 10);
    }

    /**
     * 校验并规范化 AI 建议值；格式不符直接抛异常，绝不写入脏数据。
     */
    public String validateSuggestion(String topicType, String raw) {
        Category category = categoryOf(topicType);
        if (category == Category.SUBJECTIVE) {
            throw new IllegalArgumentException("主观题不支持自动反推答案");
        }
        if (raw == null || normalize(raw).isEmpty()) {
            throw new IllegalArgumentException("建议答案为空");
        }
        if (category == Category.CHOICE) {
            Set<Character> letters = choiceLetters(raw);
            if (letters.isEmpty()) {
                throw new IllegalArgumentException("选择题建议答案必须包含选项字母（A-J）");
            }
            StringBuilder builder = new StringBuilder();
            for (Character letter : letters) {
                builder.append(letter);
            }
            return builder.toString();
        }
        if (category == Category.JUDGE) {
            Boolean value = parseTrueFalse(raw);
            if (value == null) {
                throw new IllegalArgumentException("判断题建议答案无法识别为对/错");
            }
            return value ? "对" : "错";
        }
        List<String> parts = splitFill(raw);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("填空题建议答案为空");
        }
        if (parts.size() > 10) {
            throw new IllegalArgumentException("填空题空数超过 10 个");
        }
        for (String part : parts) {
            if (normalize(part).isEmpty()) {
                throw new IllegalArgumentException("填空题存在空答案");
            }
        }
        return String.join("|", parts);
    }

    /**
     * 判断两个答案是否等价，用于抽检准确率统计。
     */
    public boolean equivalent(String topicType, String left, String right) {
        Category category = categoryOf(topicType);
        if (category == Category.SUBJECTIVE) {
            return false;
        }
        if (category == Category.CHOICE) {
            return judgeChoice(left, right);
        }
        if (category == Category.JUDGE) {
            return judgeTrueFalse(left, right);
        }
        return judgeFill(left, right);
    }
}
