package com.qq.ijay997;

import java.util.EnumSet;
import java.util.Iterator;

/**
 * EnumSet 使用示例
 * EnumSet 是专为枚举类型设计的高性能 Set 实现
 * 
 * @author ijay997
 */
public class EnumSetDemo {

    // ==================== 定义枚举 ====================
    
    /**
     * 星期枚举
     */
    enum DayOfWeek {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }
    
    /**
     * 权限枚举
     */
    enum Permission {
        READ, WRITE, EXECUTE, DELETE, ADMIN
    }
    
    /**
     * 颜色枚举
     */
    enum Color {
        RED, GREEN, BLUE, YELLOW, PURPLE, ORANGE
    }
    
    /**
     * 日志级别枚举
     */
    enum LogLevel {
        DEBUG, INFO, WARN, ERROR, FATAL
    }

    // ==================== 基本用法 ====================
    
    /**
     * 演示 EnumSet 的创建方法
     */
    public static void demoCreation() {
        System.out.println("=== EnumSet 创建方法 ===\n");
        
        // 1. 创建空的 EnumSet
        EnumSet<DayOfWeek> emptySet = EnumSet.noneOf(DayOfWeek.class);
        System.out.println("1. 空集合: " + emptySet);
        
        // 2. 创建包含所有元素的 EnumSet
        EnumSet<DayOfWeek> allDays = EnumSet.allOf(DayOfWeek.class);
        System.out.println("2. 所有元素: " + allDays);
        
        // 3. 使用 of() 创建指定元素的集合
        EnumSet<DayOfWeek> weekdays = EnumSet.of(
            DayOfWeek.MONDAY, 
            DayOfWeek.TUESDAY, 
            DayOfWeek.WEDNESDAY, 
            DayOfWeek.THURSDAY, 
            DayOfWeek.FRIDAY
        );
        System.out.println("3. 工作日: " + weekdays);
        
        // 4. 使用 range() 创建范围集合
        EnumSet<DayOfWeek> workWeek = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        System.out.println("4. 工作周 (range): " + workWeek);
        
        // 5. 使用 complementOf() 创建补集
        EnumSet<DayOfWeek> weekend = EnumSet.complementOf(workWeek);
        System.out.println("5. 周末 (complementOf): " + weekend);
        
        // 6. 使用 copyOf() 复制集合
        EnumSet<DayOfWeek> copiedSet = EnumSet.copyOf(weekdays);
        System.out.println("6. 复制集合: " + copiedSet);
        
        System.out.println();
    }
    
    /**
     * 演示 EnumSet 的基本操作
     */
    public static void demoOperations() {
        System.out.println("=== EnumSet 基本操作 ===\n");
        
        EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
        
        // 添加元素
        permissions.add(Permission.READ);
        permissions.add(Permission.WRITE);
        System.out.println("1. 添加 READ, WRITE: " + permissions);
        
        // 批量添加
        permissions.addAll(EnumSet.of(Permission.EXECUTE, Permission.DELETE));
        System.out.println("2. 批量添加 EXECUTE, DELETE: " + permissions);
        
        // 删除元素
        permissions.remove(Permission.DELETE);
        System.out.println("3. 删除 DELETE: " + permissions);
        
        // 判断包含
        System.out.println("4. 是否包含 READ: " + permissions.contains(Permission.READ));
        System.out.println("5. 是否包含 ADMIN: " + permissions.contains(Permission.ADMIN));
        
        // 清空集合
        permissions.clear();
        System.out.println("6. 清空后: " + permissions);
        
        System.out.println();
    }
    
    /**
     * 演示 EnumSet 的遍历
     */
    public static void demoIteration() {
        System.out.println("=== EnumSet 遍历 ===\n");
        
        EnumSet<Color> colors = EnumSet.of(Color.RED, Color.GREEN, Color.BLUE);
        
        // 1. 增强 for 循环
        System.out.print("1. 增强for循环: ");
        for (Color color : colors) {
            System.out.print(color + " ");
        }
        System.out.println();
        
        // 2. Iterator
        System.out.print("2. Iterator: ");
        Iterator<Color> iterator = colors.iterator();
        while (iterator.hasNext()) {
            System.out.print(iterator.next() + " ");
        }
        System.out.println();
        
        // 3. forEach + Lambda
        System.out.print("3. Lambda: ");
        colors.forEach(color -> System.out.print(color + " "));
        System.out.println();
        
        // 4. Stream API
        System.out.print("4. Stream: ");
        colors.stream()
              .map(Color::toString)
              .forEach(color -> System.out.print(color + " "));
        System.out.println();
        
        System.out.println();
    }

    // ==================== 实际应用场景 ====================
    
    /**
     * 场景1：权限管理
     */
    public static void demoPermissionManagement() {
        System.out.println("=== 场景1：权限管理 ===\n");
        
        // 定义用户权限
        EnumSet<Permission> userPermissions = EnumSet.of(
            Permission.READ, 
            Permission.WRITE
        );
        
        EnumSet<Permission> adminPermissions = EnumSet.allOf(Permission.class);
        
        // 检查权限
        System.out.println("普通用户权限: " + userPermissions);
        System.out.println("管理员权限: " + adminPermissions);
        System.out.println();
        
        // 权限验证
        checkPermission(userPermissions, Permission.READ, "读取文件");
        checkPermission(userPermissions, Permission.WRITE, "写入文件");
        checkPermission(userPermissions, Permission.DELETE, "删除文件");
        checkPermission(userPermissions, Permission.ADMIN, "系统管理");
        
        System.out.println();
        
        // 权限组合
        EnumSet<Permission> editorPermissions = EnumSet.of(
            Permission.READ, 
            Permission.WRITE, 
            Permission.EXECUTE
        );
        
        System.out.println("编辑器权限: " + editorPermissions);
        System.out.println("是否有写权限: " + editorPermissions.contains(Permission.WRITE));
        
        System.out.println();
    }
    
    private static void checkPermission(EnumSet<Permission> permissions, Permission required, String action) {
        if (permissions.contains(required)) {
            System.out.println("✅ 允许: " + action);
        } else {
            System.out.println("❌ 拒绝: " + action);
        }
    }
    
    /**
     * 场景2：日程安排
     */
    public static void demoScheduleManagement() {
        System.out.println("=== 场景2：日程安排 ===\n");
        
        // 工作日会议
        EnumSet<DayOfWeek> meetingDays = EnumSet.range(
            DayOfWeek.MONDAY, 
            DayOfWeek.FRIDAY
        );
        System.out.println("会议日期: " + meetingDays);
        
        // 周末休息
        EnumSet<DayOfWeek> restDays = EnumSet.of(
            DayOfWeek.SATURDAY, 
            DayOfWeek.SUNDAY
        );
        System.out.println("休息日期: " + restDays);
        
        // 检查某天是否有会议
        checkSchedule(meetingDays, DayOfWeek.MONDAY, "周一");
        checkSchedule(meetingDays, DayOfWeek.WEDNESDAY, "周三");
        checkSchedule(meetingDays, DayOfWeek.SATURDAY, "周六");
        
        System.out.println();
        
        // 调整日程：周五改为休息日
        EnumSet<DayOfWeek> adjustedWorkDays = EnumSet.copyOf(meetingDays);
        adjustedWorkDays.remove(DayOfWeek.FRIDAY);
        adjustedWorkDays.addAll(EnumSet.of(DayOfWeek.SATURDAY));
        
        System.out.println("调整后的工作日: " + adjustedWorkDays);
        
        System.out.println();
    }
    
    private static void checkSchedule(EnumSet<DayOfWeek> schedule, DayOfWeek day, String dayName) {
        if (schedule.contains(day)) {
            System.out.println("✅ " + dayName + " 有会议");
        } else {
            System.out.println("❌ " + dayName + " 无会议");
        }
    }
    
    /**
     * 场景3：状态标志位（替代位运算）
     */
    public static void demoStatusFlags() {
        System.out.println("=== 场景3：状态标志位 ===\n");
        
        // 传统方式：使用位运算
        int flags = 0;
        flags |= 1 << 0;  // 设置第0位
        flags |= 1 << 2;  // 设置第2位
        System.out.println("传统位运算: " + Integer.toBinaryString(flags));
        
        // 现代方式：使用 EnumSet
        EnumSet<Color> activeColors = EnumSet.of(Color.RED, Color.BLUE);
        System.out.println("EnumSet: " + activeColors);
        
        System.out.println();
        
        // 添加状态
        activeColors.add(Color.GREEN);
        System.out.println("添加 GREEN 后: " + activeColors);
        
        // 移除状态
        activeColors.remove(Color.RED);
        System.out.println("移除 RED 后: " + activeColors);
        
        // 批量操作
        EnumSet<Color> warmColors = EnumSet.of(Color.RED, Color.YELLOW, Color.ORANGE);
        EnumSet<Color> coolColors = EnumSet.of(Color.BLUE, Color.GREEN, Color.PURPLE);
        
        System.out.println("\n暖色系: " + warmColors);
        System.out.println("冷色系: " + coolColors);
        
        // 交集
        EnumSet<Color> intersection = EnumSet.copyOf(warmColors);
        intersection.retainAll(coolColors);
        System.out.println("交集: " + intersection);
        
        // 并集
        EnumSet<Color> union = EnumSet.copyOf(warmColors);
        union.addAll(coolColors);
        System.out.println("并集: " + union);
        
        // 差集
        EnumSet<Color> difference = EnumSet.copyOf(warmColors);
        difference.removeAll(coolColors);
        System.out.println("差集 (warm - cool): " + difference);
        
        System.out.println();
    }
    
    /**
     * 场景4：配置选项
     */
    public static void demoConfiguration() {
        System.out.println("=== 场景4：配置选项 ===\n");
        
        // 生产环境：只记录警告及以上
        EnumSet<LogLevel> prodLevels = EnumSet.range(LogLevel.WARN, LogLevel.FATAL);
        System.out.println("生产环境日志级别: " + prodLevels);
        
        // 开发环境：记录所有级别
        EnumSet<LogLevel> devLevels = EnumSet.allOf(LogLevel.class);
        System.out.println("开发环境日志级别: " + devLevels);
        
        // 测试环境：记录 info 及以上
        EnumSet<LogLevel> testLevels = EnumSet.range(LogLevel.INFO, LogLevel.FATAL);
        System.out.println("测试环境日志级别: " + testLevels);
        
        System.out.println();
        
        // 动态调整日志级别
        shouldLog(prodLevels, LogLevel.DEBUG, "调试信息");
        shouldLog(prodLevels, LogLevel.ERROR, "错误信息");
        shouldLog(devLevels, LogLevel.DEBUG, "调试信息");
        
        System.out.println();
    }
    
    private static void shouldLog(EnumSet<?> levels, Object level, String message) {
        if (levels.contains(level)) {
            System.out.println("✅ 记录: [" + level + "] " + message);
        } else {
            System.out.println("❌ 忽略: [" + level + "] " + message);
        }
    }

    // ==================== 性能对比 ====================
    
    /**
     * 性能对比：EnumSet vs HashSet
     */
    public static void demoPerformance() {
        System.out.println("=== 性能对比：EnumSet vs HashSet ===\n");
        
        int iterations = 1_000_000;
        
        // EnumSet 性能测试
        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            EnumSet<DayOfWeek> enumSet = EnumSet.of(
                DayOfWeek.MONDAY, 
                DayOfWeek.WEDNESDAY, 
                DayOfWeek.FRIDAY
            );
            enumSet.contains(DayOfWeek.MONDAY);
        }
        long enumSetTime = System.currentTimeMillis() - start;
        System.out.println("EnumSet 耗时: " + enumSetTime + " ms (" + iterations + " 次迭代)");
        
        // HashSet 性能测试
        start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            java.util.HashSet<DayOfWeek> hashSet = new java.util.HashSet<>();
            hashSet.add(DayOfWeek.MONDAY);
            hashSet.add(DayOfWeek.WEDNESDAY);
            hashSet.add(DayOfWeek.FRIDAY);
            hashSet.contains(DayOfWeek.MONDAY);
        }
        long hashSetTime = System.currentTimeMillis() - start;
        System.out.println("HashSet 耗时: " + hashSetTime + " ms (" + iterations + " 次迭代)");
        
        System.out.println("\n性能提升: " + (hashSetTime > 0 ? String.format("%.2f", (double)enumSetTime / hashSetTime * 100) : "N/A") + "%");
        System.out.println("EnumSet 比 HashSet 快约 " + (hashSetTime > 0 ? String.format("%.1f", (double)hashSetTime / enumSetTime) : "N/A") + " 倍");
        
        System.out.println();
    }

    // ==================== 主方法 ====================
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("EnumSet 使用示例");
        System.out.println("========================================\n");
        
        // 基本用法
        demoCreation();
        demoOperations();
        demoIteration();
        
        // 实际应用场景
        demoPermissionManagement();
        demoScheduleManagement();
        demoStatusFlags();
        demoConfiguration();
        
        // 性能对比
        demoPerformance();
        
        System.out.println("========================================");
        System.out.println("EnumSet 优势总结:");
        System.out.println("1. ⚡ 性能优异：内部使用位向量，速度极快");
        System.out.println("2. 💾 内存高效：每个枚举值只用一个 bit");
        System.out.println("3. 🔒 类型安全：编译期检查，只能存储同一种枚举");
        System.out.println("4. 🎯 专用API：提供 range、complementOf 等便捷方法");
        System.out.println("5. ✅ 有序性：按枚举声明顺序遍历");
        System.out.println("========================================");
    }
}
