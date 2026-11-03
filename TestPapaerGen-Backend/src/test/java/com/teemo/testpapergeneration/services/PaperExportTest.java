package com.teemo.testpapergeneration.services;

import com.teemo.testpapergeneration.entity.QuestionBank;
import freemarker.template.Configuration;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PaperExportTest {
    private QuestionBank question(String topic, String analysis) {
        QuestionBank question = new QuestionBank();
        question.setTopic(topic);
        question.setAnalysis(analysis);
        question.setScore(5.0);
        return question;
    }

    @Test
    void disabledModelMatchesOriginalContentsExactly() {
        List<QuestionBank> questions = Arrays.asList(question("第一题", "不应出现"), question("第二题", null));
        Map<String, String> model = PaperExportContent.model(questions, false);
        assertEquals("1、（本题5.0分）第一题\r\r2、（本题5.0分）第二题\r\r", model.get("contents"));
        assertEquals("10", model.get("total_score"));
        assertEquals("2", model.get("total_count"));
    }

    @Test
    void disabledDocxMatchesLegacyTemplateFilling() throws Exception {
        Map<String, String> oldModel = new HashMap<>();
        oldModel.put("contents", "1、（本题5.0分）题目\r\r");
        oldModel.put("total_score", "5");
        oldModel.put("total_count", "1");
        File actual = new WordExport(PaperExportContent.model(
                Collections.singletonList(question("题目", "隐藏解析")), false), false).exportTestPaper(1);
        try (InputStream template = new ClassPathResource("templates/test-paper-template.docx").getInputStream();
             XWPFDocument expected = new XWPFDocument(template);
             InputStream input = Files.newInputStream(actual.toPath());
             XWPFDocument result = new XWPFDocument(input)) {
            WordExport.fillTestPaperTemplate(expected, oldModel);
            assertEquals(expected.getDocument().xmlText(), result.getDocument().xmlText());
        } finally {
            Files.deleteIfExists(actual.toPath());
        }
    }

    @Test
    void enabledDocxEscapesAndRendersAnalysisUnderEachQuestion() throws Exception {
        List<QuestionBank> questions = Arrays.asList(
                question("题干 < & >", "第一行 <tag> & \"引号\"\r\n第二行\r第三行\n第四行"),
                question("无解析题", null), question("空白解析题", "  \n "));
        File file = new WordExport(PaperExportContent.model(questions, true), true).exportTestPaper(1);
        try (InputStream input = Files.newInputStream(file.toPath());
             XWPFDocument document = new XWPFDocument(input)) {
            String text = document.getParagraphs().stream().map(p -> p.getText()).collect(Collectors.joining("\n"));
            assertTrue(text.contains("题干 < & >\n解析：第一行 <tag> & \"引号\"\n第二行\n第三行\n第四行"), text);
            assertTrue(text.contains("2、（本题5.0分）无解析题"));
            assertEquals(1, text.split("解析：", -1).length - 1);
            assertFalse(text.contains("${contents}"));
            // XML serializers may keep '>' literal; '<' must be escaped.
            assertTrue(document.getDocument().xmlText().contains("&lt;tag"));
            assertFalse(document.getDocument().xmlText().contains("<tag>"));
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    void legacyTemplateSupportsEscapedAnalysisAndOldModel() throws Exception {
        Configuration config = new Configuration(Configuration.VERSION_2_3_30);
        config.setClassForTemplateLoading(GenWord.class, "/res");
        config.setDefaultEncoding("UTF-8");
        Map<String, Object> model = new HashMap<>();
        model.put("total_score", "5");
        model.put("total_number", "1");
        model.put("topic", "原有题目");
        StringWriter old = new StringWriter();
        config.getTemplate("微机原理试卷模板2.ftl").process(model, old);
        assertTrue(old.toString().contains("原有题目"));
        assertFalse(old.toString().contains("解析："));

        model.put("withAnalysis", true);
        model.put("topicXml", PaperExportContent.legacyTopicXml(
                Collections.singletonList(question("题干 < & >", "推理 <x> &\n下一行"))));
        StringWriter writer = new StringWriter();
        config.getTemplate("微机原理试卷模板2.ftl").process(model, writer);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        String text = factory.newDocumentBuilder().parse(new ByteArrayInputStream(
                writer.toString().getBytes(StandardCharsets.UTF_8))).getDocumentElement().getTextContent();
        assertTrue(text.contains("题干 < & >解析：推理 <x> &下一行"));
        assertTrue(writer.toString().contains("</w:t><w:br/><w:t"));
    }

    @Test
    void emptyListProducesZeroTotals() {
        Map<String, String> model = PaperExportContent.model(Collections.emptyList(), true);
        assertEquals("0", model.get("total_count"));
        assertEquals("0", model.get("total_score"));
        assertEquals("", model.get("contents"));
    }
}
