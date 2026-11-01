package com.teemo.testpapergeneration.services;

import com.teemo.testpapergeneration.entity.QuestionBank;
import freemarker.template.utility.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Export only consumes saved analysis; it never calls the AI service. */
public final class PaperExportContent {
    private PaperExportContent() {}

    public static Map<String, String> model(List<QuestionBank> questions, boolean withAnalysis) {
        Map<String, String> model = new HashMap<>();
        int totalScore = 0;
        int index = 0;
        StringBuilder contents = new StringBuilder();
        for (QuestionBank question : questions) {
            // Keep the existing score calculation and default content unchanged.
            totalScore += question.getScore();
            contents.append(++index).append("、（本题").append(question.getScore())
                    .append("分）").append(question.getTopic());
            if (withAnalysis && hasAnalysis(question)) {
                contents.append("\r解析：").append(question.getAnalysis());
            }
            contents.append("\r\r");
        }
        model.put("total_score", String.valueOf(totalScore));
        model.put("total_count", String.valueOf(index));
        model.put("contents", contents.toString());
        return model;
    }

    /** Fragment inserted inside the legacy template's w:t element. */
    public static String legacyTopicXml(List<QuestionBank> questions) {
        StringBuilder xml = new StringBuilder();
        int index = 0;
        for (QuestionBank question : questions) {
            xml.append(++index).append("、").append(xmlText(question.getTopic()));
            if (hasAnalysis(question)) {
                xml.append(xmlBreak()).append("解析：").append(xmlText(question.getAnalysis()));
            }
            xml.append(xmlBreak());
        }
        return xml.toString();
    }

    private static boolean hasAnalysis(QuestionBank question) {
        return question.getAnalysis() != null && !question.getAnalysis().trim().isEmpty();
    }

    private static String xmlText(String text) {
        return StringUtil.XMLEnc(text == null ? "" : text)
                .replace("\r\n", "\n").replace('\r', '\n').replace("\n", xmlBreak());
    }

    private static String xmlBreak() {
        return "</w:t><w:br/><w:t xml:space=\"preserve\">";
    }
}
