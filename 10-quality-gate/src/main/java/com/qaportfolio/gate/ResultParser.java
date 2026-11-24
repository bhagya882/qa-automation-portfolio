package com.qaportfolio.gate;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

/** Parses TestNG {@code testng-results.xml} and JUnit XML (Newman / surefire) into a {@link ModuleResult}. */
final class ResultParser {

    /** JWTs (e.g. in Set-Cookie assertion messages) must never reach CI logs or step summaries. */
    private static final java.util.regex.Pattern JWT =
            java.util.regex.Pattern.compile("eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}");

    private ResultParser() {
    }

    static void parseTestNg(Path file, ModuleResult into) {
        Document doc = read(file);
        NodeList methods = doc.getElementsByTagName("test-method");
        for (int i = 0; i < methods.getLength(); i++) {
            Element m = (Element) methods.item(i);
            if ("true".equals(m.getAttribute("is-config"))) {
                continue; // @BeforeMethod etc. are not tests
            }
            String name = className(m) + "#" + m.getAttribute("name");
            if ("true".equals(m.getAttribute("retried"))) {
                into.retried++;
                continue;
            }
            switch (m.getAttribute("status")) {
                case "PASS" -> into.passed++;
                case "FAIL" -> {
                    into.failed++;
                    into.failures.add(name + firstLine(m));
                }
                case "SKIP" -> into.skipped++;
                default -> { /* ignore unknown */ }
            }
        }
        into.sources.add(file.toString());
    }

    static void parseJUnit(Path file, ModuleResult into) {
        Document doc = read(file);
        NodeList cases = doc.getElementsByTagName("testcase");
        for (int i = 0; i < cases.getLength(); i++) {
            Element c = (Element) cases.item(i);
            String name = c.getAttribute("classname") + "#" + c.getAttribute("name");
            if (c.getElementsByTagName("failure").getLength() > 0 || c.getElementsByTagName("error").getLength() > 0) {
                into.failed++;
                into.failures.add(name);
            } else if (c.getElementsByTagName("skipped").getLength() > 0) {
                into.skipped++;
            } else {
                into.passed++;
            }
        }
        into.sources.add(file.toString());
    }

    static String redact(String text) {
        return JWT.matcher(text).replaceAll("<redacted-jwt>");
    }

    private static String className(Element method) {
        if (method.getParentNode() instanceof Element cls && "class".equals(cls.getTagName())) {
            String n = cls.getAttribute("name");
            return n.substring(Math.max(n.lastIndexOf('.'), n.lastIndexOf('$')) + 1);
        }
        return "?";
    }

    private static String firstLine(Element method) {
        NodeList msgs = method.getElementsByTagName("message");
        if (msgs.getLength() == 0) {
            return "";
        }
        String text = msgs.item(0).getTextContent().strip();
        // First two non-blank lines, so AssertJ "[description]" headers keep the actual vs expected line
        String line = String.join(" ", text.lines().map(String::strip).filter(l -> !l.isBlank()).limit(2).toList());
        line = redact(line);
        return line.isEmpty() ? "" : " — " + (line.length() > 160 ? line.substring(0, 160) + "…" : line);
    }

    /** Secure DOM parsing: report files come from CI artifacts, so external entities/DTDs are disabled (XXE). */
    private static Document read(Path file) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            return b.parse(file.toFile());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot parse " + file + ": " + e.getMessage(), e);
        }
    }
}
