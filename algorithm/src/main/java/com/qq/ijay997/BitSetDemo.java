package com.qq.ijay997;

import java.util.BitSet;
import java.util.PriorityQueue;

/**
 * BitSet 使用示例
 * BitSet 是一个高效的位向量实现，用于存储和操作二进制位
 * 
 * @author ijay997
 */
public class BitSetDemo {

    // ==================== 基本用法 ====================
    
    /**
     * 演示 BitSet 的创建和基本操作
     */
    public static void demoBasicOperations() {
        System.out.println("=== BitSet 基本操作 ===\n");
        
        // 1. 创建 BitSet
        BitSet bitSet = new BitSet();
        System.out.println("1. 创建空 BitSet: " + bitSet);
        System.out.println("   大小: " + bitSet.size() + " bits");
        System.out.println("   长度: " + bitSet.length() + " bits");
        System.out.println("   基数（设置的位数）: " + bitSet.cardinality());
        
        // 2. 设置位（索引从0开始）
        bitSet.set(0);
        bitSet.set(2);
        bitSet.set(4);
        bitSet.set(6);
        System.out.println("\n2. 设置位 0, 2, 4, 6: " + bitSet);
        System.out.println("   二进制表示: " + toBinaryString(bitSet, 8));
        
        // 3. 检查位
        System.out.println("\n3. 检查位:");
        System.out.println("   位0: " + bitSet.get(0));  // true
        System.out.println("   位1: " + bitSet.get(1));  // false
        System.out.println("   位2: " + bitSet.get(2));  // true
        
        // 4. 清除位
        bitSet.clear(2);
        System.out.println("\n4. 清除位2: " + bitSet);
        System.out.println("   二进制表示: " + toBinaryString(bitSet, 8));
        
        // 5. 翻转位
        bitSet.flip(1);
        System.out.println("\n5. 翻转位1: " + bitSet);
        System.out.println("   二进制表示: " + toBinaryString(bitSet, 8));
        
        // 6. 批量操作
        BitSet batchSet = new BitSet();
        batchSet.set(0, 5);  // 设置位 0-4
        System.out.println("\n6. 批量设置位 0-4: " + batchSet);
        System.out.println("   二进制表示: " + toBinaryString(batchSet, 8));
        
        System.out.println();
    }
    
    /**
     * 演示 BitSet 的集合运算
     */
    public static void demoSetOperations() {
        System.out.println("=== BitSet 集合运算 ===\n");
        
        BitSet set1 = new BitSet();
        set1.set(0);
        set1.set(1);
        set1.set(2);
        set1.set(3);
        System.out.println("集合1: " + toBinaryString(set1, 8));
        
        BitSet set2 = new BitSet();
        set2.set(2);
        set2.set(3);
        set2.set(4);
        set2.set(5);
        System.out.println("集合2: " + toBinaryString(set2, 8));
        
        // 1. 并集（OR）
        BitSet union = (BitSet) set1.clone();
        union.or(set2);
        System.out.println("\n1. 并集 (OR): " + toBinaryString(union, 8));

        // 2. 交集（AND）
        BitSet intersection = (BitSet) set1.clone();
        intersection.and(set2);
        System.out.println("2. 交集 (AND): " + toBinaryString(intersection, 8));
        
        // 3. 差集（AND NOT）
        BitSet difference = (BitSet) set1.clone();
        difference.andNot(set2);
        System.out.println("3. 差集 (AND NOT): " + toBinaryString(difference, 8));
        
        // 4. 异或（XOR）
        BitSet xor = (BitSet) set1.clone();
        xor.xor(set2);
        System.out.println("4. 异或 (XOR): " + toBinaryString(xor, 8));
        
        // 5. 判断关系
        System.out.println("\n5. 关系判断:");
        System.out.println("   set1 与 set2 是否有交集: " + set1.intersects(set2));
        
        BitSet subset = new BitSet();
        subset.set(0);
        subset.set(1);
        System.out.println("   subset 是否是 set1 的子集: " + isSubset(subset, set1));
        
        System.out.println();
    }
    
    /**
     * 判断是否为子集
     */
    private static boolean isSubset(BitSet subset, BitSet superset) {
        BitSet temp = (BitSet) subset.clone();
        temp.andNot(superset);
        return temp.isEmpty();
    }

    // ==================== 实际应用场景 ====================
    
    /**
     * 场景1：素数筛选（埃拉托斯特尼筛法）
     */
    public static void demoPrimeSieve() {
        System.out.println("=== 场景1：素数筛选（埃拉托斯特尼筛法）===\n");
        
        int limit = 50;
        BitSet primes = sieveOfEratosthenes(limit);
        
        System.out.println(limit + " 以内的素数:");
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i <= limit; i++) {
            if (primes.get(i)) {
                sb.append(i).append(", ");
            }
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);  // 删除最后的逗号和空格
        }
        System.out.println(sb.toString());
        System.out.println();
    }
    
    /**
     * 埃拉托斯特尼筛法
     */
    private static BitSet sieveOfEratosthenes(int limit) {
        BitSet primes = new BitSet(limit + 1);
        
        // 初始化：假设所有数都是素数
        primes.set(2, limit + 1);
        
        // 筛选
        for (int i = 2; i * i <= limit; i++) {
            if (primes.get(i)) {
                // 将 i 的倍数标记为非素数
                for (int j = i * i; j <= limit; j += i) {
                    primes.clear(j);
                }
            }
        }
        
        return primes;
    }
    
    /**
     * 场景2：权限管理（位标志）
     */
    public static void demoPermissionManagement() {
        System.out.println("=== 场景2：权限管理（位标志）===\n");
        
        // 定义权限位
        final int READ = 0;
        final int WRITE = 1;
        final int EXECUTE = 2;
        final int DELETE = 3;
        final int ADMIN = 4;
        
        // 用户权限
        BitSet userPermissions = new BitSet();
        userPermissions.set(READ);
        userPermissions.set(WRITE);
        System.out.println("用户权限: " + permissionToString(userPermissions));
        
        // 管理员权限
        BitSet adminPermissions = new BitSet();
        adminPermissions.set(READ);
        adminPermissions.set(WRITE);
        adminPermissions.set(EXECUTE);
        adminPermissions.set(DELETE);
        adminPermissions.set(ADMIN);
        System.out.println("管理员权限: " + permissionToString(adminPermissions));
        
        // 权限检查
        System.out.println("\n权限检查:");
        checkPermission(userPermissions, READ, "读取");
        checkPermission(userPermissions, WRITE, "写入");
        checkPermission(userPermissions, EXECUTE, "执行");
        checkPermission(userPermissions, DELETE, "删除");
        
        // 权限组合
        BitSet editorPermissions = (BitSet) userPermissions.clone();
        editorPermissions.set(EXECUTE);
        System.out.println("\n编辑器权限（用户+执行）: " + permissionToString(editorPermissions));
        
        // 权限移除
        editorPermissions.clear(WRITE);
        System.out.println("移除写权限后: " + permissionToString(editorPermissions));
        
        System.out.println();
    }
    
    private static String permissionToString(BitSet permissions) {
        StringBuilder sb = new StringBuilder("[");
        if (permissions.get(0)) sb.append("READ ");
        if (permissions.get(1)) sb.append("WRITE ");
        if (permissions.get(2)) sb.append("EXECUTE ");
        if (permissions.get(3)) sb.append("DELETE ");
        if (permissions.get(4)) sb.append("ADMIN ");
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 1);  // 删除最后的空格
        }
        sb.append("]");
        return sb.toString();
    }
    
    private static void checkPermission(BitSet permissions, int permission, String action) {
        if (permissions.get(permission)) {
            System.out.println("  ✅ 允许: " + action);
        } else {
            System.out.println("  ❌ 拒绝: " + action);
        }
    }
    
    /**
     * 场景3：布隆过滤器简化版
     */
    public static void demoBloomFilter() {
        System.out.println("=== 场景3：布隆过滤器（简化版）===\n");
        
        SimpleBloomFilter bloomFilter = new SimpleBloomFilter(100);
        
        // 添加元素
        bloomFilter.add("apple");
        bloomFilter.add("banana");
        bloomFilter.add("orange");
        System.out.println("已添加: apple, banana, orange");
        
        // 检查元素
        System.out.println("\n检查结果:");
        System.out.println("  apple 可能存在: " + bloomFilter.mightContain("apple"));
        System.out.println("  banana 可能存在: " + bloomFilter.mightContain("banana"));
        System.out.println("  grape 可能存在: " + bloomFilter.mightContain("grape"));
        System.out.println("  orange 可能存在: " + bloomFilter.mightContain("orange"));
        
        System.out.println("\n注意：布隆过滤器可能产生误判（false positive），但不会漏判（false negative）");
        System.out.println();
    }
    
    /**
     * 简化版布隆过滤器
     */
    static class SimpleBloomFilter {
        private BitSet bitSet;
        private int size;
        
        public SimpleBloomFilter(int size) {
            this.size = size;
            this.bitSet = new BitSet(size);
        }
        
        public void add(String value) {
            int hash1 = Math.abs(value.hashCode()) % size;
            int hash2 = Math.abs(value.hashCode() * 31) % size;
            bitSet.set(hash1);
            bitSet.set(hash2);
        }
        
        public boolean mightContain(String value) {
            int hash1 = Math.abs(value.hashCode()) % size;
            int hash2 = Math.abs(value.hashCode() * 31) % size;
            return bitSet.get(hash1) && bitSet.get(hash2);
        }
    }
    
    /**
     * 场景4：位图索引（Bitmap Index）
     */
    public static void demoBitmapIndex() {
        System.out.println("=== 场景4：位图索引（Bitmap Index）===\n");
        
        // 模拟数据库中的性别字段索引
        // 假设有10条记录，索引位置对应记录ID
        BitSet maleIndex = new BitSet();
        BitSet femaleIndex = new BitSet();
        
        // 记录数据：ID -> 性别
        // 0:男, 1:女, 2:男, 3:男, 4:女, 5:女, 6:男, 7:女, 8:男, 9:女
        maleIndex.set(0);
        maleIndex.set(2);
        maleIndex.set(3);
        maleIndex.set(6);
        maleIndex.set(8);
        
        femaleIndex.set(1);
        femaleIndex.set(4);
        femaleIndex.set(5);
        femaleIndex.set(7);
        femaleIndex.set(9);
        
        System.out.println("男性索引: " + toBinaryString(maleIndex, 10));
        System.out.println("女性索引: " + toBinaryString(femaleIndex, 10));
        
        // 查询：找出所有男性
        System.out.println("\n查询所有男性记录ID:");
        printSetBits(maleIndex, "男性");
        
        // 查询：找出所有女性
        System.out.println("\n查询所有女性记录ID:");
        printSetBits(femaleIndex, "女性");
        
        // 复合查询：男性 AND 其他条件（这里简化为直接返回）
        System.out.println("\n位图索引优势:");
        System.out.println("  1. 存储空间小（每个记录只需1 bit）");
        System.out.println("  2. 支持快速的位运算（AND, OR, NOT）");
        System.out.println("  3. 适合低基数字段（如性别、状态等）");
        
        System.out.println();
    }
    
    private static void printSetBits(BitSet bitSet, String label) {
        StringBuilder sb = new StringBuilder();
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
            sb.append(i).append(", ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);
        }
        System.out.println("  " + label + ": [" + sb.toString() + "]");
    }
    
    /**
     * 场景5：访客统计（去重计数）
     */
    public static void demoVisitorCounting() {
        System.out.println("=== 场景5：访客统计（去重计数）===\n");
        
        // 假设用户ID范围是 0-999
        BitSet visitedUsers = new BitSet(1000);
        
        // 模拟访客访问
        int[] visitorIds = {100, 200, 300, 100, 400, 200, 500, 600, 300, 700};
        
        System.out.println("访客ID序列: ");
        for (int id : visitorIds) {
            System.out.print(id + " ");
            visitedUsers.set(id);
        }
        System.out.println();
        
        // 统计独立访客数（UV）
        int uniqueVisitors = visitedUsers.cardinality();
        System.out.println("\n总访问次数: " + visitorIds.length);
        System.out.println("独立访客数（UV）: " + uniqueVisitors);
        System.out.println("重复访问: " + (visitorIds.length - uniqueVisitors));
        
        System.out.println("\n去重后的访客ID:");
        StringBuilder sb = new StringBuilder();
        for (int i = visitedUsers.nextSetBit(0); i >= 0; i = visitedUsers.nextSetBit(i + 1)) {
            sb.append(i).append(", ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);
        }
        System.out.println("  [" + sb.toString() + "]");
        
        System.out.println();
    }

    // ==================== 高级功能 ====================
    
    /**
     * 演示 BitSet 的高级功能
     */
    public static void demoAdvancedFeatures() {
        System.out.println("=== BitSet 高级功能 ===\n");
        
        BitSet bitSet = new BitSet();
        bitSet.set(1);
        bitSet.set(3);
        bitSet.set(5);
        bitSet.set(7);
        bitSet.set(9);
        
        System.out.println("BitSet: " + toBinaryString(bitSet, 12));
        
        // 1. nextSetBit - 查找下一个设置的位
        System.out.println("\n1. nextSetBit():");
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
            System.out.print(i + " ");
        }
        System.out.println();
        
        // 2. nextClearBit - 查找下一个未设置的位
        System.out.println("\n2. nextClearBit():");
        for (int i = bitSet.nextClearBit(0); i < 12; i = bitSet.nextClearBit(i + 1)) {
            System.out.print(i + " ");
            if (i > 10) break;  // 限制输出
        }
        System.out.println();
        
        // 3. length() - 最高设置位的索引+1
        System.out.println("\n3. length(): " + bitSet.length());
        
        // 4. cardinality() - 设置的位数
        System.out.println("4. cardinality(): " + bitSet.cardinality());
        
        // 5. isEmpty() - 是否为空
        System.out.println("5. isEmpty(): " + bitSet.isEmpty());
        
        // 6. intersects() - 是否有交集
        BitSet other = new BitSet();
        other.set(5);
        other.set(10);
        System.out.println("6. intersects(other): " + bitSet.intersects(other));
        
        // 7. toLongArray() - 转换为 long 数组
        long[] longs = bitSet.toLongArray();
        System.out.print("7. toLongArray(): [");
        for (long l : longs) {
            System.out.print(Long.toBinaryString(l) + " ");
        }
        System.out.println("]");
        
        System.out.println();
    }
    
    /**
     * 性能对比：BitSet vs boolean[]
     */
    public static void demoPerformance() {
        System.out.println("=== 性能对比：BitSet vs boolean[] ===\n");
        
        int size = 1_000_000;
        int iterations = 1000;
        
        // BitSet 性能测试
        long start = System.currentTimeMillis();
        for (int iter = 0; iter < iterations; iter++) {
            BitSet bitSet = new BitSet(size);
            for (int i = 0; i < size; i += 100) {
                bitSet.set(i);
            }
            int count = bitSet.cardinality();
        }
        long bitSetTime = System.currentTimeMillis() - start;
        System.out.println("BitSet 耗时: " + bitSetTime + " ms");
        
        // boolean[] 性能测试
        start = System.currentTimeMillis();
        for (int iter = 0; iter < iterations; iter++) {
            boolean[] array = new boolean[size];
            for (int i = 0; i < size; i += 100) {
                array[i] = true;
            }
            int count = 0;
            for (boolean b : array) {
                if (b) count++;
            }
        }
        long arrayTime = System.currentTimeMillis() - start;
        System.out.println("boolean[] 耗时: " + arrayTime + " ms");
        
        System.out.println("\n内存对比:");
        System.out.println("  BitSet: 约 " + (size / 8 / 1024) + " KB (" + size + " bits)");
        System.out.println("  boolean[]: 约 " + (size / 1024) + " KB (" + size + " bytes)");
        System.out.println("  节省空间: " + ((1 - 1.0/8) * 100) + "%");
        
        System.out.println();
    }

    // ==================== 工具方法 ====================
    
    /**
     * 将 BitSet 转换为二进制字符串（便于观察）
     */
    private static String toBinaryString(BitSet bitSet, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = length - 1; i >= 0; i--) {
            sb.append(bitSet.get(i) ? '1' : '0');
        }
        return sb.toString();
    }

    // ==================== 主方法 ====================
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("BitSet 使用示例");
        System.out.println("========================================\n");
        
        // 基本用法
        demoBasicOperations();
        demoSetOperations();
        
        // 实际应用场景
        demoPrimeSieve();
        demoPermissionManagement();
        demoBloomFilter();
        demoBitmapIndex();
        demoVisitorCounting();
        
        // 高级功能
        demoAdvancedFeatures();
        
        // 性能对比
        demoPerformance();
        
        System.out.println("========================================");
        System.out.println("BitSet 优势总结:");
        System.out.println("1. 💾 内存高效：每个元素只用 1 bit");
        System.out.println("2. ⚡ 性能优异：位运算速度极快");
        System.out.println("3. 🎯 适用场景：素数筛选、权限管理、布隆过滤器");
        System.out.println("4. 🔧 丰富API：支持各种集合运算");
        System.out.println("5. 📊 空间优化：比 boolean[] 节省 87.5% 空间");
        System.out.println("========================================");
    }
}
