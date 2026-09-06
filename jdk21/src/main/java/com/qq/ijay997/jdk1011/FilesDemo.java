package com.qq.ijay997.jdk1011;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JDK 11 —— Files 便捷读写 Demo。
 *
 * <p>新增 {@code Files.writeString / readString / mismatch}，
 * 使读写小文本文件不再依赖一大堆 Stream/Reader 样板代码。</p>
 *
 * <p>本 Demo 在系统临时目录生成临时文件用于演示。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk1011.FilesDemo</p>
 *
 * @version JDK 11+
 */
public class FilesDemo {

    public static void main(String[] args) throws IOException {

        FileWriter fileWriter = new FileWriter("aaa.txt");
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        try(bufferedWriter; fileWriter){
            fileWriter.write("This is anamy");
            fileWriter.write("This is anamy");
            fileWriter.write("This is anamy");
        }

        // 在临时目录创建文件路径
        Path tmp = Files.createTempFile("jdk11-demo-", ".txt");

        // writeString：一行写文本
        String content = "Hello Files API\n第二行内容";
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        System.out.println("写入文件: " + tmp);

        // readString：一行读文本
        String readBack = Files.readString(tmp, StandardCharsets.UTF_8);
        System.out.println("读取内容:\n" + readBack);

        // mismatch：比较两个文件内容，返回第一个不同字节的下标，相同返回 -1
        Path tmp2 = Files.createTempFile("jdk11-demo-", ".txt");
        Files.writeString(tmp2, "Hello Files APIx第二行内容", StandardCharsets.UTF_8);
        long diff = Files.mismatch(tmp, tmp2);
        System.out.println("mismatch(两个文件首处不同字节索引) = " + diff);

        // 清理临时文件
        Files.deleteIfExists(tmp);
        Files.deleteIfExists(tmp2);
        System.out.println("已清理临时文件。");
    }
}
