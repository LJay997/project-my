package com.qq.ijay997;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStringCounterTest {

    @TempDir
    Path tempDir;

    private Path createTestFile(String content) throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    @DisplayName("统计单次出现的字符串")
    void testSingleOccurrence() throws IOException {
        Path file = createTestFile("Hello World");
        assertEquals(1, FileStringCounter.countOccurrences(file.toString(), "Hello"));
    }

    @Test
    @DisplayName("统计多次出现的字符串")
    void testMultipleOccurrences() throws IOException {
        Path file = createTestFile("Java Java Java is great");
        assertEquals(3, FileStringCounter.countOccurrences(file.toString(), "Java"));
    }

    @Test
    @DisplayName("统计不存在的字符串")
    void testNoOccurrence() throws IOException {
        Path file = createTestFile("Hello World");
        assertEquals(0, FileStringCounter.countOccurrences(file.toString(), "Python"));
    }

    @Test
    @DisplayName("统计跨行重复的字符串")
    void testCrossLineOccurrences() throws IOException {
        Path file = createTestFile("Java is\nJava is\nJava is");
        assertEquals(3, FileStringCounter.countOccurrences(file.toString(), "Java"));
    }

    @Test
    @DisplayName("空目标字符串返回0")
    void testEmptyTargetString() throws IOException {
        Path file = createTestFile("Hello World");
        assertEquals(0, FileStringCounter.countOccurrences(file.toString(), ""));
    }

    @Test
    @DisplayName("null目标字符串返回0")
    void testNullTargetString() throws IOException {
        Path file = createTestFile("Hello World");
        assertEquals(0, FileStringCounter.countOccurrences(file.toString(), null));
    }

    @Test
    @DisplayName("null文件名返回0")
    void testNullFileName() {
        assertEquals(0, FileStringCounter.countOccurrences(null, "test"));
    }

    @Test
    @DisplayName("文件不存在返回0")
    void testFileNotExist() {
        assertEquals(0, FileStringCounter.countOccurrences("/nonexistent/path/file.txt", "test"));
    }

    @Test
    @DisplayName("统计空文件")
    void testEmptyFile() throws IOException {
        Path file = createTestFile("");
        assertEquals(0, FileStringCounter.countOccurrences(file.toString(), "test"));
    }

    @Test
    @DisplayName("统计特殊字符")
    void testSpecialCharacters() throws IOException {
        Path file = createTestFile("a.b.c a.b.c");
        assertEquals(2, FileStringCounter.countOccurrences(file.toString(), "a.b.c"));
    }

    @Test
    @DisplayName("统计重复字符重叠情况")
    void testOverlappingMatches() throws IOException {
        Path file = createTestFile("aaa");
        assertEquals(3, FileStringCounter.countOccurrences(file.toString(), "a"));
    }

    @Test
    @DisplayName("统计多字符重叠")
    void testMultiCharOverlap() throws IOException {
        Path file = createTestFile("aaaa");
        assertEquals(3, FileStringCounter.countOccurrences(file.toString(), "aa"));
    }

    @Test
    @DisplayName("区分大小写")
    void testCaseSensitive() throws IOException {
        Path file = createTestFile("Java java JAVA");
        assertEquals(1, FileStringCounter.countOccurrences(file.toString(), "Java"));
    }

    @Test
    @DisplayName("统计中文字符串")
    void testChineseCharacters() throws IOException {
        Path file = createTestFile("你好世界 你好 Java");
        assertEquals(2, FileStringCounter.countOccurrences(file.toString(), "你好"));
    }

    @Test
    @DisplayName("统计只出现在开头的字符串")
    void testMatchAtStart() throws IOException {
        Path file = createTestFile("test rest of file");
        assertEquals(1, FileStringCounter.countOccurrences(file.toString(), "test"));
    }

    @Test
    @DisplayName("统计只出现在结尾的字符串")
    void testMatchAtEnd() throws IOException {
        Path file = createTestFile("start of file test");
        assertEquals(1, FileStringCounter.countOccurrences(file.toString(), "test"));
    }

    @Test
    @DisplayName("多行文件统计")
    void testMultiLineFile() throws IOException {
        Path file = createTestFile("line1\nline2\nline3\nline4\nline1");
        assertEquals(2, FileStringCounter.countOccurrences(file.toString(), "line1"));
    }

    @Test
    @DisplayName("连续出现的字符串")
    void testConsecutiveOccurrences() throws IOException {
        Path file = createTestFile("testtesttest");
        assertEquals(3, FileStringCounter.countOccurrences(file.toString(), "test"));
    }
}