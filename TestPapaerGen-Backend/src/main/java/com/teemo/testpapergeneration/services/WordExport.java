package com.teemo.testpapergeneration.services;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class WordExport {

    Map<String, String> map;
    private final boolean withAnalysis;
    public WordExport(Map<String, String> map) {
        this(map, false);
    }

    public WordExport(Map<String, String> map, boolean withAnalysis) {
        this.map = map;
        this.withAnalysis = withAnalysis;
    }

    public File exportTestPaper(Integer templateType) {
        String templatePath = "/templates/test-paper-template.docx";
        switch (templateType) {
            case 1:
                templatePath = "/templates/test-paper-template.docx";
                break;
            case 2:
                templatePath = "/templates/answer-template.docx";
                break;
        }
        try {
            // 试卷的模板路径
            ClassPathResource classPathResource = new ClassPathResource(templatePath);
            try (InputStream is = classPathResource.getInputStream();
                 XWPFDocument doc = new XWPFDocument(is)) {
                // 充填模板
                fillTestPaperTemplate(doc, map, withAnalysis);
                // 输出文件
                File file = File.createTempFile("TestPaperExport" + "_" + UUID.randomUUID().toString().replace("-", "").toLowerCase(), ".docx");
                try (FileOutputStream out = new FileOutputStream(file)) {
                    doc.write(out);
                }
                return file;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {

    }

    /**
     * 替换段落文本
     *
     * @param document docx解析对象
     * @param map  需要替换的信息
     */
    public static void fillTestPaperTemplate(XWPFDocument document, Map<String, String> map) {
        fillTestPaperTemplate(document, map, false);
    }

    private static void fillTestPaperTemplate(XWPFDocument document, Map<String, String> map, boolean withAnalysis) {
        // 获取段落集合
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        for (XWPFParagraph paragraph : paragraphs) {
            // 判断此段落中是否有需要进行替换的模板标记 ${key}
            if (paragraph.getText().indexOf("${") < paragraph.getText().indexOf("}")) {
                List<XWPFRun> runs = paragraph.getRuns();
                for (XWPFRun run : runs) {
                    String str = changeValue(run.toString(), map);
                    if (str != null) {
                        if (withAnalysis && run.toString().contains("${contents}")) {
                            // Explicit Word line breaks; POI escapes XML-sensitive characters.
                            String[] lines = str.split("\\r\\n|\\r|\\n", -1);
                            run.setText(lines[0], 0);
                            for (int i = 1; i < lines.length; i++) {
                                run.addBreak();
                                run.setText(lines[i]);
                            }
                        } else {
                            run.setText(str, 0);
                        }
                    }
                }
            }
        }
    }

    /**
     * 匹配传入信息集合与模板
     *
     * @param originalVal 模板需要替换的区域
     * @param map 传入信息集合
     * @return 模板需要替换区域信息集合对应值
     */
    private static String changeValue(String originalVal, Map<String, String> map) {
        String res = null;
        Set<Map.Entry<String, String>> mapSets = map.entrySet();
        for (Map.Entry<String, String> item : mapSets) {
            // 匹配模板与替换值 格式 ${key}
            String key = "${" + item.getKey() + "}";
            if (originalVal.contains(key)) {
                res = item.getValue();
            }
        }
        return res;
    }

    /**
     * 将documents对象另存为文件
     *
     * @param document 文档对象
     * @return 包含 true 成功 或 false 失败
     */
    public static boolean documentSave(XWPFDocument document, File file) {
        try {
            FileOutputStream out = new FileOutputStream(file);
            document.write(out);
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }



}
