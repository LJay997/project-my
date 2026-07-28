package com.qq.ijay997;

import java.util.ArrayDeque;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SolutionTest {

    private Solution solution;

    @BeforeEach
    void setUp() {
        solution = new Solution();
    }

    @Test
    void moveZeroes() {
        int[] nums = {1, 0, 1};
        solution.moveZeroes(nums);
        assertArrayEquals(new int[]{1, 1, 0}, nums);
    }

    @Test
    void lengthOfLongestSubstring() {
        assertEquals(2, solution.lengthOfLongestSubstring("abcabcbb"));
    }

    @Test
    void maxSubArray() {
        assertEquals(6, solution.maxSubArray(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}));
    }

    @Test
    void twoSum() {
        assertArrayEquals(new int[]{0, 1}, solution.twoSum(new int[]{2, 7, 11, 15}, 9));
    }

    @Test
    void twoSum2() {
        assertArrayEquals(new int[]{0, 1}, solution.twoSum2(new int[]{2, 7, 11, 15}, 9));
    }

    @Test
    void removeDuplicates() {
        assertEquals(2, solution.removeDuplicates(new int[]{1, 1, 2}));
    }

    @Test
    void removeElement() {
        assertEquals(2, solution.removeElement(new int[]{3, 2, 2, 3}, 3));
    }

    @Test
    void searchInsert() {
        assertEquals(2, solution.searchInsert(new int[]{1, 3, 5, 6}, 5));
    }

    @Test
    void reverseList() {
        ListNode head = new ListNode(1);
        head.next = new ListNode(2);
        head.next.next = new ListNode(3);
        head.next.next.next = new ListNode(4);
        head.next.next.next.next = new ListNode(5);
        ListNode reverse = solution.reverseList(head);
    }

    @Test
    void mergeTwoLists() {


        // 构建测试数据: list1 = [1,2,4]
        ListNode list1 = new ListNode(1);
        list1.next = new ListNode(2);
        list1.next.next = new ListNode(4);

        // 构建测试数据: list2 = [1,3,4]
        ListNode list2 = new ListNode(1);
        list2.next = new ListNode(3);
        list2.next.next = new ListNode(4);

        // 执行合并操作
        ListNode result = solution.mergeTwoLists(list1, list2);

        // 验证结果
        int[] expected = {1, 1, 2, 3, 4, 4};
        ListNode current = result;
        for (int i = 0; i < expected.length; i++) {
            assertNotNull("Result list is shorter than expected", current.toString());
//            assertEquals(Float.parseFloat("Value mismatch at position " + i), expected[i], current.val);
            current = current.next;
        }
        assertNull("Result list is longer than expected", current.toString());
    }

    @Test
    public void TestMergeKLists() {

        // 测试用例1: 正常情况
        System.out.println("测试用例1:");
        ListNode l1 = createLinkedList(new int[]{1, 4, 5});
        ListNode l2 = createLinkedList(new int[]{1, 3, 4});
        ListNode l3 = createLinkedList(new int[]{2, 6});
        ListNode[] lists1 = {l1, l2, l3};
        ListNode result1 = solution.mergeKLists(lists1);
        printLinkedList(result1); // 预期输出: 1->1->2->3->4->4->5->6

        // 测试用例2: 空数组
        System.out.println("\n测试用例2:");
        ListNode[] lists2 = {};
        ListNode result2 = solution.mergeKLists1(lists2);
        System.out.println(result2 == null ? "null" : printLinkedListToString(result2)); // 预期输出: null

        // 测试用例3: 包含空链表
        System.out.println("\n测试用例3:");
        ListNode l4 = createLinkedList(new int[]{1, 2, 3});
        ListNode l5 = null;
        ListNode l6 = createLinkedList(new int[]{4, 5});
        ListNode[] lists3 = {l4, l5, l6};
        ListNode result3 = solution.mergeKLists1(lists3);
        printLinkedList(result3); // 预期输出: 1->2->3->4->5

        // 测试用例4: 单个链表
        System.out.println("\n测试用例4:");
        ListNode l7 = createLinkedList(new int[]{1, 2, 3});
        ListNode[] lists4 = {l7};
        ListNode result4 = solution.mergeKLists1(lists4);
        printLinkedList(result4); // 预期输出: 1->2->3

        // 测试用例5: 全部为空链表
        System.out.println("\n测试用例5:");
        ListNode l8 = null;
        ListNode l9 = null;
        ListNode[] lists5 = {l8, l9};
        ListNode result5 = solution.mergeKLists1(lists5);
        System.out.println(result5 == null ? "null" : printLinkedListToString(result5)); // 预期输出: null

    }

    // 辅助方法：创建链表
    private static ListNode createLinkedList(int[] values) {
        if (values == null || values.length == 0) {
            return null;
        }

        ListNode head = new ListNode(values[0]);
        ListNode current = head;
        for (int i = 1; i < values.length; i++) {
            current.next = new ListNode(values[i]);
            current = current.next;
        }
        return head;
    }

    // 辅助方法：打印链表
    private static void printLinkedList(ListNode head) {
        String result = printLinkedListToString(head);
        System.out.print(result);
    }

    // 辅助方法：将链表转换为字符串
    private static String printLinkedListToString(ListNode head) {
        StringBuilder sb = new StringBuilder();
        ListNode current = head;
        while (current != null) {
            sb.append(current.val);
            if (current.next != null) {
                sb.append("->");
            }
            current = current.next;
        }
        return sb.toString();
    }

    @Test
    void levelOrder() {
        TreeNode root = new TreeNode(3,
                new TreeNode(9), new TreeNode(20,
                new TreeNode(15), new TreeNode(7)));

        List<List<Integer>> lists = solution.levelOrder(root);
        System.out.println(lists); // // 预期结果: [[3],[9,20],[15,7]]
    }

    @Test
    void levelOrder2() {
        // 测试用例 1: 正常二叉树
        System.out.println("\n=== levelOrder2 测试用例 1: 正常二叉树 ===");
        TreeNode root1 = new TreeNode(3,
                new TreeNode(9), new TreeNode(20,
                new TreeNode(15), new TreeNode(7)));
        List<List<Integer>> result1 = solution.levelOrder2(root1);
        System.out.println("预期结果: [[3],[9,20],[15,7]]");
        System.out.println("实际结果: " + result1);

        // 测试用例 2: 空树
        System.out.println("\n=== levelOrder2 测试用例 2: 空树 ===");
        TreeNode root2 = null;
        List<List<Integer>> result2 = solution.levelOrder2(root2);
        System.out.println("预期结果: null");
        System.out.println("实际结果: " + result2);

        // 测试用例 3: 只有根节点
        System.out.println("\n=== levelOrder2 测试用例 3: 只有根节点 ===");
        TreeNode root3 = new TreeNode(1);
        List<List<Integer>> result3 = solution.levelOrder2(root3);
        System.out.println("预期结果: [[1]]");
        System.out.println("实际结果: " + result3);

        // 测试用例 4: 只有左子树
        System.out.println("\n=== levelOrder2 测试用例 4: 只有左子树 ===");
        TreeNode root4 = new TreeNode(1,
                new TreeNode(2,
                        new TreeNode(3,
                                new TreeNode(4), null), null), null);
        List<List<Integer>> result4 = solution.levelOrder2(root4);
        System.out.println("预期结果: [[1],[2],[3],[4]]");
        System.out.println("实际结果: " + result4);

        // 测试用例 5: 只有右子树
        System.out.println("\n=== levelOrder2 测试用例 5: 只有右子树 ===");
        TreeNode root5 = new TreeNode(1,
                null, new TreeNode(2,
                        null, new TreeNode(3,
                                null, new TreeNode(4))));
        List<List<Integer>> result5 = solution.levelOrder2(root5);
        System.out.println("预期结果: [[1],[2],[3],[4]]");
        System.out.println("实际结果: " + result5);

        // 测试用例 6: 完全二叉树
        System.out.println("\n=== levelOrder2 测试用例 6: 完全二叉树 ===");
        TreeNode root6 = new TreeNode(1,
                new TreeNode(2,
                        new TreeNode(4), new TreeNode(5)),
                new TreeNode(3,
                        new TreeNode(6), new TreeNode(7)));
        List<List<Integer>> result6 = solution.levelOrder2(root6);
        System.out.println("预期结果: [[1],[2,3],[4,5,6,7]]");
        System.out.println("实际结果: " + result6);

        System.out.println("\nlevelOrder2 测试完成");
    }

    /**
     * 测试完全二叉树
     */
    @Test
    public void testMaxDepth_CompleteBinaryTree() {
        //       3
        //      / \
        //     9   20
        //        /  \
        //       15   7
        TreeNode root = new TreeNode(3);
        root.left = new TreeNode(9);
        root.right = new TreeNode(20);
        root.right.left = new TreeNode(15);
        root.right.right = new TreeNode(7);

        assertEquals(3, solution.maxDepth(root));
    }

    @Test
    public void testComplexNonSymmetricTree() {

        // 构建复杂非对称树:
        //           1
        //         /   \
        //        2     2
        //       / \   / \
        //      3   4 4   3
        //     /          /
        //    5          5
        TreeNode root = new TreeNode(1);
        root.left = new TreeNode(2);
        root.right = new TreeNode(2);
        root.left.left = new TreeNode(3);
        root.left.right = new TreeNode(4);
        root.right.left = new TreeNode(4);
        root.right.right = new TreeNode(3);
        root.left.left.left = new TreeNode(5);
        root.right.left.left = new TreeNode(5); // 注意这里的位置

        assertFalse(solution.isSymmetric1(root));
    }

    @Test
    void invertTree() {
        TreeNode root = new TreeNode(4);
        root.left = new TreeNode(2, new TreeNode(1), new TreeNode(3));
        root.right = new TreeNode(7, new TreeNode(6), new TreeNode(9));

        TreeNode treeNode = solution.invertTree1(root);
        System.out.println(treeNode);
    }

    @Test
    void findKthLargest() {

        int[] num = {3, 2, 1, 5, 6, 4};
        int kthLargest = solution.findKthLargest(num, 2);
        System.out.println(kthLargest);
    }

    /**
     * findKthLargest2 方法测试 - 使用最小堆找到数组中第k大的元素
     */
    @Test
    void testFindKthLargest2_Basic() {
        System.out.println("\n=== 测试用例 1: 基本功能测试 ===");

        // 测试用例 1: 正常情况，找第2大的元素
        int[] nums1 = {3, 2, 1, 5, 6, 4};
        assertEquals(5, solution.findKthLargest2(nums1, 2), "第2大的元素应该是 5");

        // 测试用例 2: 找第1大的元素（最大值）
        assertEquals(6, solution.findKthLargest2(nums1, 1), "第1大的元素应该是 6");

        // 测试用例 3: 找最后1大的元素（最小值）
        assertEquals(1, solution.findKthLargest2(nums1, 6), "第6大的元素应该是 1");

        // 测试用例 4: 找中间位置的元素
        assertEquals(3, solution.findKthLargest2(nums1, 4), "第4大的元素应该是 3");

        System.out.println("基本功能测试通过");
    }

    @Test
    void testFindKthLargest2_Duplicates() {
        System.out.println("\n=== 测试用例 2: 包含重复元素 ===");

        // 测试用例 5: 所有元素相同
        int[] nums2 = {5, 5, 5, 5, 5};
        assertEquals(5, solution.findKthLargest2(nums2, 1), "所有元素相同时，第1大应该是 5");
        assertEquals(5, solution.findKthLargest2(nums2, 3), "所有元素相同时，第3大应该是 5");
        assertEquals(5, solution.findKthLargest2(nums2, 5), "所有元素相同时，第5大应该是 5");

        // 测试用例 6: 部分元素重复
        int[] nums3 = {3, 3, 3, 2, 2, 1};
        assertEquals(3, solution.findKthLargest2(nums3, 1), "第1大的元素应该是 3");
        assertEquals(3, solution.findKthLargest2(nums3, 2), "第2大的元素应该是 3");
        assertEquals(3, solution.findKthLargest2(nums3, 3), "第3大的元素应该是 3");
        assertEquals(2, solution.findKthLargest2(nums3, 4), "第4大的元素应该是 2");
        assertEquals(2, solution.findKthLargest2(nums3, 5), "第5大的元素应该是 2");
        assertEquals(1, solution.findKthLargest2(nums3, 6), "第6大的元素应该是 1");

        System.out.println("重复元素测试通过");
    }

    @Test
    void testFindKthLargest2_SingleElement() {
        System.out.println("\n=== 测试用例 3: 单元素数组 ===");

        // 测试用例 7: 只有一个元素
        int[] nums4 = {1};
        assertEquals(1, solution.findKthLargest2(nums4, 1), "单元素数组，第1大应该是 1");

        System.out.println("单元素测试通过");
    }

    @Test
    void testFindKthLargest2_TwoElements() {
        System.out.println("\n=== 测试用例 4: 双元素数组 ===");

        // 测试用例 8: 两个不同元素
        int[] nums5 = {1, 2};
        assertEquals(2, solution.findKthLargest2(nums5, 1), "第1大的元素应该是 2");
        assertEquals(1, solution.findKthLargest2(nums5, 2), "第2大的元素应该是 1");

        // 测试用例 9: 两个相同元素
        int[] nums6 = {3, 3};
        assertEquals(3, solution.findKthLargest2(nums6, 1), "第1大的元素应该是 3");
        assertEquals(3, solution.findKthLargest2(nums6, 2), "第2大的元素应该是 3");

        System.out.println("双元素测试通过");
    }

    @Test
    void testFindKthLargest2_SortedArrays() {
        System.out.println("\n=== 测试用例 5: 已排序数组 ===");

        // 测试用例 10: 升序数组
        int[] nums7 = {1, 2, 3, 4, 5};
        assertEquals(5, solution.findKthLargest2(nums7, 1), "升序数组，第1大应该是 5");
        assertEquals(3, solution.findKthLargest2(nums7, 3), "升序数组，第3大应该是 3");
        assertEquals(1, solution.findKthLargest2(nums7, 5), "升序数组，第5大应该是 1");

        // 测试用例 11: 降序数组
        int[] nums8 = {5, 4, 3, 2, 1};
        assertEquals(5, solution.findKthLargest2(nums8, 1), "降序数组，第1大应该是 5");
        assertEquals(3, solution.findKthLargest2(nums8, 3), "降序数组，第3大应该是 3");
        assertEquals(1, solution.findKthLargest2(nums8, 5), "降序数组，第5大应该是 1");

        System.out.println("已排序数组测试通过");
    }

    @Test
    void testFindKthLargest2_NegativeNumbers() {
        System.out.println("\n=== 测试用例 6: 包含负数 ===");

        // 测试用例 12: 全部负数
        int[] nums9 = {-5, -3, -1, -4, -2};
        assertEquals(-1, solution.findKthLargest2(nums9, 1), "第1大的元素应该是 -1");
        assertEquals(-3, solution.findKthLargest2(nums9, 3), "第3大的元素应该是 -3");
        assertEquals(-5, solution.findKthLargest2(nums9, 5), "第5大的元素应该是 -5");

        // 测试用例 13: 正负数混合
        int[] nums10 = {-2, 1, -3, 4, -1, 2, 1, -5, 4};
        assertEquals(4, solution.findKthLargest2(nums10, 1), "第1大的元素应该是 4");
        assertEquals(4, solution.findKthLargest2(nums10, 2), "第2大的元素应该是 4");
        assertEquals(2, solution.findKthLargest2(nums10, 3), "第3大的元素应该是 2");

        System.out.println("负数测试通过");
    }

    @Test
    void testFindKthLargest2_LargeArray() {
        System.out.println("\n=== 测试用例 7: 较大数组 ===");

        // 测试用例 14: 较大的数组
        int[] nums11 = new int[100];
        for (int i = 0; i < 100; i++) {
            nums11[i] = i + 1;
        }

        assertEquals(100, solution.findKthLargest2(nums11, 1), "第1大的元素应该是 100");
        assertEquals(50, solution.findKthLargest2(nums11, 51), "第51大的元素应该是 50");
        assertEquals(1, solution.findKthLargest2(nums11, 100), "第100大的元素应该是 1");

        System.out.println("大数组测试通过");
    }

    @Test
    void testFindKthLargest2_EdgeCases() {
        System.out.println("\n=== 测试用例 8: 边界情况 ===");

        // 测试用例 15: k 等于数组长度
        int[] nums12 = {7, 10, 4, 3, 20, 15};
        assertEquals(3, solution.findKthLargest2(nums12, 6), "k=数组长度时，应返回最小值 3");

        // 测试用例 16: k=1，找最大值
        assertEquals(20, solution.findKthLargest2(nums12, 1), "k=1 时，应返回最大值 20");

        // 测试用例 17: 经典 LeetCode 示例
        int[] nums13 = {3, 2, 3, 1, 2, 4, 5, 5, 6};
        assertEquals(4, solution.findKthLargest2(nums13, 4), "LeetCode 示例，第4大应该是 4");

        System.out.println("边界情况测试通过");
    }

    /**
     * printListFromTailToHead 方法测试 - 从尾到头打印链表
     */
    @Test
    void testPrintListFromTailToHead_Basic() {
        System.out.println("\n=== 测试用例 1: 基本功能测试 ===");

        // 测试用例 1: 正常链表 [1, 2, 3]
        ListNode head1 = createLinkedList(new int[]{1, 2, 3});
        ArrayList<Integer> result1 = solution.printListFromTailToHead(head1);
        assertEquals(Arrays.asList(3, 2, 1), result1, "应该返回 [3, 2, 1]");
        System.out.println("链表 [1, 2, 3] 从尾到头: " + result1);

        // 测试用例 2: 较长链表 [1, 2, 3, 4, 5]
        ListNode head2 = createLinkedList(new int[]{1, 2, 3, 4, 5});
        ArrayList<Integer> result2 = solution.printListFromTailToHead(head2);
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result2, "应该返回 [5, 4, 3, 2, 1]");
        System.out.println("链表 [1, 2, 3, 4, 5] 从尾到头: " + result2);

        System.out.println("基本功能测试通过");
    }

    @Test
    void testPrintListFromTailToHead_EmptyAndSingle() {
        System.out.println("\n=== 测试用例 2: 空链表和单节点 ===");

        // 测试用例 3: 空链表
        ListNode head3 = null;
        ArrayList<Integer> result3 = solution.printListFromTailToHead(head3);
        assertNotNull(result3, "结果不应为 null");
        assertTrue(result3.isEmpty(), "空链表应返回空列表");
        System.out.println("空链表从尾到头: " + result3);

        // 测试用例 4: 单节点链表
        ListNode head4 = new ListNode(1);
        ArrayList<Integer> result4 = solution.printListFromTailToHead(head4);
        assertEquals(Arrays.asList(1), result4, "单节点应返回 [1]");
        System.out.println("单节点链表 [1] 从尾到头: " + result4);

        System.out.println("空链表和单节点测试通过");
    }

    @Test
    void testPrintListFromTailToHead_TwoNodes() {
        System.out.println("\n=== 测试用例 3: 双节点链表 ===");

        // 测试用例 5: 两个不同值的节点
        ListNode head5 = createLinkedList(new int[]{1, 2});
        ArrayList<Integer> result5 = solution.printListFromTailToHead(head5);
        assertEquals(Arrays.asList(2, 1), result5, "应该返回 [2, 1]");
        System.out.println("链表 [1, 2] 从尾到头: " + result5);

        // 测试用例 6: 两个相同值的节点
        ListNode head6 = createLinkedList(new int[]{5, 5});
        ArrayList<Integer> result6 = solution.printListFromTailToHead(head6);
        assertEquals(Arrays.asList(5, 5), result6, "应该返回 [5, 5]");
        System.out.println("链表 [5, 5] 从尾到头: " + result6);

        System.out.println("双节点测试通过");
    }

    @Test
    void testPrintListFromTailToHead_Duplicates() {
        System.out.println("\n=== 测试用例 4: 包含重复元素 ===");

        // 测试用例 7: 所有元素相同
        ListNode head7 = createLinkedList(new int[]{3, 3, 3, 3});
        ArrayList<Integer> result7 = solution.printListFromTailToHead(head7);
        assertEquals(Arrays.asList(3, 3, 3, 3), result7, "应该返回 [3, 3, 3, 3]");
        System.out.println("链表 [3, 3, 3, 3] 从尾到头: " + result7);

        // 测试用例 8: 部分元素重复
        ListNode head8 = createLinkedList(new int[]{1, 2, 2, 3, 3, 3});
        ArrayList<Integer> result8 = solution.printListFromTailToHead(head8);
        assertEquals(Arrays.asList(3, 3, 3, 2, 2, 1), result8, "应该返回 [3, 3, 3, 2, 2, 1]");
        System.out.println("链表 [1, 2, 2, 3, 3, 3] 从尾到头: " + result8);

        System.out.println("重复元素测试通过");
    }

    @Test
    void testPrintListFromTailToHead_NegativeNumbers() {
        System.out.println("\n=== 测试用例 5: 包含负数 ===");

        // 测试用例 9: 全部负数
        ListNode head9 = createLinkedList(new int[]{-3, -2, -1});
        ArrayList<Integer> result9 = solution.printListFromTailToHead(head9);
        assertEquals(Arrays.asList(-1, -2, -3), result9, "应该返回 [-1, -2, -3]");
        System.out.println("链表 [-3, -2, -1] 从尾到头: " + result9);

        // 测试用例 10: 正负数混合
        ListNode head10 = createLinkedList(new int[]{-2, 0, 3, -1, 5});
        ArrayList<Integer> result10 = solution.printListFromTailToHead(head10);
        assertEquals(Arrays.asList(5, -1, 3, 0, -2), result10, "应该返回 [5, -1, 3, 0, -2]");
        System.out.println("链表 [-2, 0, 3, -1, 5] 从尾到头: " + result10);

        System.out.println("负数测试通过");
    }

    @Test
    void testPrintListFromTailToHead_SortedLists() {
        System.out.println("\n=== 测试用例 6: 已排序链表 ===");

        // 测试用例 11: 升序链表
        ListNode head11 = createLinkedList(new int[]{1, 2, 3, 4, 5});
        ArrayList<Integer> result11 = solution.printListFromTailToHead(head11);
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result11, "升序链表反转后应为降序");
        System.out.println("升序链表 [1, 2, 3, 4, 5] 从尾到头: " + result11);

        // 测试用例 12: 降序链表
        ListNode head12 = createLinkedList(new int[]{5, 4, 3, 2, 1});
        ArrayList<Integer> result12 = solution.printListFromTailToHead(head12);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result12, "降序链表反转后应为升序");
        System.out.println("降序链表 [5, 4, 3, 2, 1] 从尾到头: " + result12);

        System.out.println("已排序链表测试通过");
    }

    @Test
    void testPrintListFromTailToHead_LargeList() {
        System.out.println("\n=== 测试用例 7: 较大链表 ===");

        // 测试用例 13: 较大的链表（100个节点）
        int[] values = new int[100];
        for (int i = 0; i < 100; i++) {
            values[i] = i + 1;
        }
        ListNode head13 = createLinkedList(values);
        ArrayList<Integer> result13 = solution.printListFromTailToHead(head13);

        // 验证结果大小
        assertEquals(100, result13.size(), "结果应该包含 100 个元素");
        // 验证第一个元素（原链表最后一个）
        assertEquals(100, result13.get(0), "第一个元素应该是 100");
        // 验证最后一个元素（原链表第一个）
        assertEquals(1, result13.get(99), "最后一个元素应该是 1");
        // 验证中间元素
        assertEquals(50, result13.get(50), "第51个元素应该是 50");

        System.out.println("大链表测试通过，结果大小: " + result13.size());
    }

    @Test
    void testPrintListFromTailToHead_EdgeCases() {
        System.out.println("\n=== 测试用例 8: 边界情况 ===");

        // 测试用例 14: 单个大数值
        ListNode head14 = new ListNode(Integer.MAX_VALUE);
        ArrayList<Integer> result14 = solution.printListFromTailToHead(head14);
        assertEquals(Arrays.asList(Integer.MAX_VALUE), result14, "应正确处理最大值");
        System.out.println("最大值节点: " + result14);

        // 测试用例 15: 单个小数值
        ListNode head15 = new ListNode(Integer.MIN_VALUE);
        ArrayList<Integer> result15 = solution.printListFromTailToHead(head15);
        assertEquals(Arrays.asList(Integer.MIN_VALUE), result15, "应正确处理最小值");
        System.out.println("最小值节点: " + result15);

        // 测试用例 16: 包含零值
        ListNode head16 = createLinkedList(new int[]{0, 0, 0});
        ArrayList<Integer> result16 = solution.printListFromTailToHead(head16);
        assertEquals(Arrays.asList(0, 0, 0), result16, "应正确处理零值");
        System.out.println("零值链表 [0, 0, 0] 从尾到头: " + result16);

        // 测试用例 17: 交替正负数
        ListNode head17 = createLinkedList(new int[]{-1, 2, -3, 4, -5});
        ArrayList<Integer> result17 = solution.printListFromTailToHead(head17);
        assertEquals(Arrays.asList(-5, 4, -3, 2, -1), result17, "应正确反转交替正负数链表");
        System.out.println("交替正负数链表从尾到头: " + result17);

        System.out.println("边界情况测试通过");
    }

    /**
     * printListFromTailToHeadIterative 方法测试 - 使用头插法（迭代）从尾到头打印链表
     */
    @Test
    void testPrintListFromTailToHeadIterative_Basic() {
        System.out.println("\n=== 测试用例 1: 头插法基本功能测试 ===");

        // 测试用例 1: 正常链表 [1, 2, 3]
        ListNode head1 = createLinkedList(new int[]{1, 2, 3});
        ArrayList<Integer> result1 = solution.printListFromTailToHeadIterative(head1);
        assertEquals(Arrays.asList(3, 2, 1), result1, "应该返回 [3, 2, 1]");
        System.out.println("链表 [1, 2, 3] 从尾到头（头插法）: " + result1);

        // 测试用例 2: 较长链表 [1, 2, 3, 4, 5]
        ListNode head2 = createLinkedList(new int[]{1, 2, 3, 4, 5});
        ArrayList<Integer> result2 = solution.printListFromTailToHeadIterative(head2);
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result2, "应该返回 [5, 4, 3, 2, 1]");
        System.out.println("链表 [1, 2, 3, 4, 5] 从尾到头（头插法）: " + result2);

        System.out.println("基本功能测试通过");
    }

    @Test
    void testPrintListFromTailToHeadIterative_EmptyAndSingle() {
        System.out.println("\n=== 测试用例 2: 头插法空链表和单节点 ===");

        // 测试用例 3: 空链表
        ListNode head3 = null;
        ArrayList<Integer> result3 = solution.printListFromTailToHeadIterative(head3);
        assertNotNull(result3, "结果不应为 null");
        assertTrue(result3.isEmpty(), "空链表应返回空列表");
        System.out.println("空链表从尾到头（头插法）: " + result3);

        // 测试用例 4: 单节点链表
        ListNode head4 = new ListNode(1);
        ArrayList<Integer> result4 = solution.printListFromTailToHeadIterative(head4);
        assertEquals(Arrays.asList(1), result4, "单节点应返回 [1]");
        System.out.println("单节点链表 [1] 从尾到头（头插法）: " + result4);

        System.out.println("空链表和单节点测试通过");
    }

    @Test
    void testPrintListFromTailToHeadIterative_CompareWithRecursive() {
        System.out.println("\n=== 测试用例 3: 对比递归和迭代方法 ===");

        // 测试用例 5: 验证两种方法结果一致
        int[][] testCases = {
            {1, 2, 3},
            {5, 4, 3, 2, 1},
            {1},
            {-1, 2, -3, 4},
            {3, 3, 3},
            {0, 0, 0, 0}
        };

        for (int i = 0; i < testCases.length; i++) {
            ListNode head = createLinkedList(testCases[i]);
            ArrayList<Integer> recursiveResult = solution.printListFromTailToHead(head);
            ArrayList<Integer> iterativeResult = solution.printListFromTailToHeadIterative(head);
            
            assertEquals(recursiveResult, iterativeResult, 
                "测试用例 " + (i + 1) + " 两种方法结果应一致");
            System.out.println("测试用例 " + (i + 1) + ": " + Arrays.toString(testCases[i]) + 
                             " -> " + iterativeResult);
        }

        System.out.println("递归与迭代方法对比测试通过");
    }

    @Test
    void testPrintListFromTailToHeadIterative_Duplicates() {
        System.out.println("\n=== 测试用例 4: 头插法重复元素 ===");

        // 测试用例 6: 所有元素相同
        ListNode head6 = createLinkedList(new int[]{3, 3, 3, 3});
        ArrayList<Integer> result6 = solution.printListFromTailToHeadIterative(head6);
        assertEquals(Arrays.asList(3, 3, 3, 3), result6, "应该返回 [3, 3, 3, 3]");

        // 测试用例 7: 部分元素重复
        ListNode head7 = createLinkedList(new int[]{1, 2, 2, 3, 3, 3});
        ArrayList<Integer> result7 = solution.printListFromTailToHeadIterative(head7);
        assertEquals(Arrays.asList(3, 3, 3, 2, 2, 1), result7, "应该返回 [3, 3, 3, 2, 2, 1]");

        System.out.println("重复元素测试通过");
    }

    @Test
    void testPrintListFromTailToHeadIterative_NegativeNumbers() {
        System.out.println("\n=== 测试用例 5: 头插法负数测试 ===");

        // 测试用例 8: 全部负数
        ListNode head8 = createLinkedList(new int[]{-3, -2, -1});
        ArrayList<Integer> result8 = solution.printListFromTailToHeadIterative(head8);
        assertEquals(Arrays.asList(-1, -2, -3), result8, "应该返回 [-1, -2, -3]");

        // 测试用例 9: 正负数混合
        ListNode head9 = createLinkedList(new int[]{-2, 0, 3, -1, 5});
        ArrayList<Integer> result9 = solution.printListFromTailToHeadIterative(head9);
        assertEquals(Arrays.asList(5, -1, 3, 0, -2), result9, "应该返回 [5, -1, 3, 0, -2]");

        System.out.println("负数测试通过");
    }

    @Test
    void testPrintListFromTailToHeadIterative_LargeList() {
        System.out.println("\n=== 测试用例 6: 头插法较大链表 ===");

        // 测试用例 10: 较大的链表（100个节点）
        int[] values = new int[100];
        for (int i = 0; i < 100; i++) {
            values[i] = i + 1;
        }
        ListNode head10 = createLinkedList(values);
        ArrayList<Integer> result10 = solution.printListFromTailToHeadIterative(head10);

        // 验证结果大小
        assertEquals(100, result10.size(), "结果应该包含 100 个元素");
        // 验证第一个元素（原链表最后一个）
        assertEquals(100, result10.get(0), "第一个元素应该是 100");
        // 验证最后一个元素（原链表第一个）
        assertEquals(1, result10.get(99), "最后一个元素应该是 1");
        // 验证中间元素
        assertEquals(50, result10.get(50), "第51个元素应该是 50");

        System.out.println("大链表测试通过，结果大小: " + result10.size());
    }

    @Test
    void topKFrequent() {
        int[] nums = {4, 1, -1, 2, -1, 2, 3};

        int[] result = solution.topKFrequent(nums, 2);
        System.out.println(result);
    }

    @Test
    void getIntersectionNode1() {
        // 创建测试链表
        ListNode common = new ListNode(8);
        common.next = new ListNode(4);
        common.next.next = new ListNode(5);

        // 链表 A: 4 -> 1 -> 8 -> 4 -> 5
        ListNode headA = new ListNode(4);
        headA.next = new ListNode(1);
        headA.next.next = common;

        // 链表 B: 5 -> 6 -> 1 -> 8 -> 4 -> 5
        ListNode headB = new ListNode(5);
        headB.next = new ListNode(6);
        headB.next.next = new ListNode(1);
        headB.next.next.next = common;

        // 调用方法测试

        ListNode result = solution.getIntersectionNode1(headA, headB);

        // 输出结果
        if (result != null) {
            System.out.println("相交节点的值为: " + result.val); // 应输出 8
        } else {
            System.out.println("无相交节点");
        }

        // 边界测试：两个链表完全不相交
        ListNode headC = new ListNode(1);
        headC.next = new ListNode(2);
        ListNode headD = new ListNode(3);
        headD.next = new ListNode(4);

        ListNode result2 = solution.getIntersectionNode1(headC, headD);
        if (result2 != null) {
            System.out.println("相交节点的值为: " + result2.val);
        } else {
            System.out.println("无相交节点"); // 应输出此信息
        }
    }

    @Test
    void lowestCommonAncestor() {
        // 构造测试用的二叉树
        TreeNode root = new TreeNode(3);
        root.left = new TreeNode(5);
        root.right = new TreeNode(1);
        root.left.left = new TreeNode(6);
        root.left.right = new TreeNode(2);
        root.right.left = new TreeNode(0);
        root.right.right = new TreeNode(8);
        root.left.right.left = new TreeNode(7);
        root.left.right.right = new TreeNode(4);


        // 测试用例1: p = 5, q = 1 -> 预期结果: 3
        TreeNode p1 = root.left; // 节点5
        TreeNode q1 = root.right; // 节点1
        TreeNode result1 = solution.lowestCommonAncestor(root, p1, q1);
        System.out.println("Test Case 1: Expected 3, Got " + result1.val);

        // 测试用例2: p = 5, q = 4 -> 预期结果: 5
        TreeNode p2 = root.left; // 节点5
        TreeNode q2 = root.left.right.right; // 节点4
        TreeNode result2 = solution.lowestCommonAncestor(root, p2, q2);
        System.out.println("Test Case 2: Expected 5, Got " + result2.val);

        // 测试用例3: p = 6, q = 2 -> 预期结果: 5
        TreeNode p3 = root.left.left; // 节点6
        TreeNode q3 = root.left.right; // 节点2
        TreeNode result3 = solution.lowestCommonAncestor(root, p3, q3);
        System.out.println("Test Case 3: Expected 5, Got " + result3.val);

    }

    @Test
    void isPalindrome1() {
        // 测试用例1: 回文链表 [1, 2, 2, 1]
        ListNode head1 = new ListNode(1);
        head1.next = new ListNode(2);
        head1.next.next = new ListNode(2);
        head1.next.next.next = new ListNode(1);
        System.out.println("Test Case 1: " + solution.isPalindrome1(head1)); // 预期输出: true

        // 测试用例2: 非回文链表 [1, 2]
        ListNode head2 = new ListNode(1);
        head2.next = new ListNode(2);
        System.out.println("Test Case 2: " + solution.isPalindrome1(head2)); // 预期输出: false

        // 测试用例3: 单节点链表 [1]
        ListNode head3 = new ListNode(1);
        System.out.println("Test Case 3: " + solution.isPalindrome1(head3)); // 预期输出: true

        // 测试用例4: 空链表
        ListNode head4 = null;
        System.out.println("Test Case 4: " + solution.isPalindrome1(head4)); // 预期输出: false

        // 测试用例5: 回文链表 [1, 2, 3, 2, 1]
        ListNode head5 = new ListNode(1);
        head5.next = new ListNode(2);
        head5.next.next = new ListNode(3);
        head5.next.next.next = new ListNode(2);
        head5.next.next.next.next = new ListNode(1);
        System.out.println("Test Case 5: " + solution.isPalindrome1(head5)); // 预期输出: true
    }

    @Test
    void testGroupAnagrams() {
        Solution solution = new Solution();

        // 测试用例1：正常情况
        String[] strs1 = {"eat", "tea", "tan", "ate", "nat", "bat"};
        List<List<String>> result1 = solution.groupAnagrams1(strs1);
        System.out.println("结果1: " + result1);
        // 预期输出: [[bat], [nat, tan], [ate, eat, tea]] （顺序可能不同）

        // 测试用例2：包含空字符串
        String[] strs2 = {"", ""};
        List<List<String>> result2 = solution.groupAnagrams1(strs2);
        System.out.println("结果2: " + result2);
        // 预期输出: [["", ""]]

        // 测试用例3：单个字符
        String[] strs3 = {"a"};
        List<List<String>> result3 = solution.groupAnagrams1(strs3);
        System.out.println("结果3: " + result3);
        // 预期输出: [["a"]]
    }

    @Test
    void longestConsecutive() {
        int[] nums = {100, 4, 200, 1, 3, 2};
        System.out.println(solution.longestConsecutive(nums));
    }

    @Test
    void moveZeroes1() {
        int[] nums = {0, 1, 0, 3, 12};
        solution.moveZeroes1(nums);
        System.out.println(Arrays.toString(nums));
    }

    @Test
    void maxArea() {
        int[] height = {1, 8, 6, 2, 5, 4, 8, 3, 7};
        System.out.println(solution.maxArea(height));
    }

    @Test
    void isSubsequence() {
        String s = "abc";
        String t = "ahbgdc";
        System.out.println(solution.isSubsequence(s, t));
    }

    @Test
    void middleNode() {
        ListNode linkedList = createLinkedList(new int[]{1, 2, 3, 4, 5, 6});
        ListNode middleNode = solution.middleNode(linkedList);
        System.out.println("Middle node value: " + middleNode.val);
    }

    @Test
    void rotateString() {
        System.out.println(solution.rotateString("abcde", "cdeab"));
        System.out.println(solution.rotateString("abcde", "abced"));
    }

    @Test
    void rotateString3() {
        ArrayDeque<Character> objects = new ArrayDeque<>();
        objects.add('a');
        objects.add('b');
        objects.add('c');
        objects.add('d');
        System.out.println(objects.toArray().toString());

        System.out.println(solution.rotateString3("abcde", "cdeab"));
    }

    @Test
    void validateStackSequences() {
        int[] pushed = {1, 2, 3, 4, 5};
        int[] popped = {4, 5, 3, 2, 1};
        System.out.println(solution.validateStackSequences(pushed, popped));
    }

    @Test
    void climbStairs() {
        // 测试用例 1: n = 0，有 1 种方法（不跳）
        assertEquals(1, solution.climbStairs(0), "n=0 时应该有 1 种方法");

        // 测试用例 2: n = 1，只有 1 种方法（跳 1 级）
        assertEquals(1, solution.climbStairs(1), "n=1 时应该有 1 种方法");

        // 测试用例 3: n = 2，有 2 种方法（1+1 或 2）
        assertEquals(2, solution.climbStairs(2), "n=2 时应该有 2 种方法");

        // 测试用例 4: n = 3，有 3 种方法（1+1+1、1+2、2+1）
        assertEquals(3, solution.climbStairs(3), "n=3 时应该有 3 种方法");

        // 测试用例 5: n = 4，有 5 种方法
        assertEquals(5, solution.climbStairs(4), "n=4 时应该有 5 种方法");

        // 测试用例 6: n = 5，有 8 种方法
        assertEquals(8, solution.climbStairs(5), "n=5 时应该有 8 种方法");

        // 测试用例 7: n = 10，验证斐波那契数列
        assertEquals(89, solution.climbStairs(10), "n=10 时应该有 89 种方法");

        // 测试用例 8: 打印结果便于观察
        System.out.println("n=6 时的跳法数：" + solution.climbStairs(6));
        System.out.println("n=7 时的跳法数：" + solution.climbStairs(7));
        System.out.println("n=8 时的跳法数：" + solution.climbStairs(8));
    }

    @Test
    void canPermutePalindrome() {
        // 测试用例 1: 基本 false 情况
        assertFalse(solution.canPermutePalindrome("code"), "'code' 无法排列成回文");

        // 测试用例 2: 基本 true 情况
        assertTrue(solution.canPermutePalindrome("carerac"), "'carerac' 可以排列成回文");

        // 测试用例 3: 所有字符都出现偶数次
        assertTrue(solution.canPermutePalindrome("aabbccdd"), "'aabbccdd' 可以排列成回文");

        // 测试用例 4: 有一个字符出现奇数次
        assertTrue(solution.canPermutePalindrome("aab"), "'aab' 可以排列成回文 'aba'");

        // 测试用例 5: 单个字符
        assertTrue(solution.canPermutePalindrome("a"), "单个字符可以形成回文");

        // 测试用例 6: 空字符串
        assertFalse(solution.canPermutePalindrome(""), "空字符串不能形成回文");

        // 测试用例 7: null 值
        assertFalse(solution.canPermutePalindrome(null), "null 不能形成回文");

        // 测试用例 8: 多个不同字符出现奇数次
        assertFalse(solution.canPermutePalindrome("aabbcccddd"), "'aabbcccddd' 有多个奇数字符，无法形成回文");

        // 测试用例 9: 经典回文串
        assertTrue(solution.canPermutePalindrome("racecar"), "'racecar' 本身就是回文");

        // 测试用例 10: 两个相同字符
        assertTrue(solution.canPermutePalindrome("aa"), "'aa' 可以形成回文");

        // 测试用例 11: 复杂情况 - 偶数长度但有多个奇数字符
        assertFalse(solution.canPermutePalindrome("codeco"), "'codeco' 有 2 个奇数字符 (d,e)，无法形成回文");

        // 测试用例 12: 正确的复杂情况
        assertTrue(solution.canPermutePalindrome("carrac"), "'carrac' 可以形成回文");

        // 打印测试结果
        System.out.println("canPermutePalindrome 测试完成");
    }

    @Test
    void search() {
        int[] nums = {-1, 0, 3, 5, 9, 12};
        System.out.println(solution.search(nums, 9));
    }

    /**
     * 测试第一个错误版本
     * 注意：需要重写 isBadVersion 方法来模拟实际场景
     */
    @Test
    void firstBadVersion() {
//        // 测试用例 1: 第一个版本就是错误版本
//        Solution solution1 = new Solution() {
//            @Override
//            public boolean isBadVersion(int version) {
//                return version >= 4;
//            }
//        };
//        assertEquals(4, solution1.firstBadVersion(5), "第一个版本就应该是错误版本");
//
//        // 测试用例 2: 中间版本是第一个错误版本
//        Solution solution2 = new Solution() {
//            @Override
//            public boolean isBadVersion(int version) {
//                return version >= 4;
//            }
//        };
//        assertEquals(4, solution2.firstBadVersion(5), "第 4 个版本应该是第一个错误版本");
//
//        // 测试用例 3: 最后一个版本是第一个错误版本
//        Solution solution3 = new Solution() {
//            @Override
//            public boolean isBadVersion(int version) {
//                return version >= 10;
//            }
//        };
//        assertEquals(10, solution3.firstBadVersion(10), "最后一个版本应该是第一个错误版本");
//
//        // 测试用例 4: 只有一个版本且是错误版本
//        Solution solution4 = new Solution() {
//            @Override
//            public boolean isBadVersion(int version) {
//                return version >= 1;
//            }
//        };
//        assertEquals(1, solution4.firstBadVersion(1), "唯一版本应该是错误版本");
//
//        // 测试用例 5: n <= 0 的情况
//        Solution solution5 = new Solution() {
//            @Override
//            public boolean isBadVersion(int version) {
//                return version >= 1;
//            }
//        };
//        assertEquals(-1, solution5.firstBadVersion(0), "n=0 应该返回 -1");
//        assertEquals(-1, solution5.firstBadVersion(-1), "n<0 应该返回 -1");

        // 测试用例 6: 大数测试（防止整数溢出）
        Solution solution6 = new Solution() {
            @Override
            public boolean isBadVersion(int version) {
                return version >= 1702766719;
            }
        };
        assertEquals(1702766719, solution6.firstBadVersion(2126753390),
                "大数情况下应正确找到第一个错误版本");

        System.out.println("firstBadVersion 测试完成");
    }

    @Test
    void selectSort() {
        int[] arr = {5, 3, 8, 4, 2};
        solution.sort2(arr);
        System.out.println(Arrays.toString(arr));
    }

    @Test
    void quickSort() {
        int[] arr = {5, 3, 8, 77, 2, 42, 46};
        solution.quickSort(arr);
        System.out.println(Arrays.toString(arr));
    }

    @Test
    void preOrder() {
        // 测试用例 1: 空树
        System.out.println("测试用例 1: 空树");
        solution.preOrder(null);

        // 测试用例 2: 单节点树
        System.out.println("\n测试用例 2: 单节点树");
        TreeNode root2 = new TreeNode(1);
        solution.preOrder(root2);

        // 测试用例 3: 只有左子树
        System.out.println("\n测试用例 3: 只有左子树");
        TreeNode root3 = new TreeNode(1);
        root3.left = new TreeNode(2);
        root3.left.left = new TreeNode(3);
        solution.preOrder(root3);

        // 测试用例 4: 只有右子树
        System.out.println("\n测试用例 4: 只有右子树");
        TreeNode root4 = new TreeNode(1);
        root4.right = new TreeNode(2);
        root4.right.right = new TreeNode(3);
        solution.preOrder(root4);

        // 测试用例 5: 完整的二叉树
        System.out.println("\n测试用例 5: 完整的二叉树");
        TreeNode root5 = new TreeNode(1);
        root5.left = new TreeNode(2);
        root5.right = new TreeNode(3);
        root5.left.left = new TreeNode(4);
        root5.left.right = new TreeNode(5);
        root5.right.left = new TreeNode(6);
        root5.right.right = new TreeNode(7);
        solution.preOrder(root5);

        // 测试用例 6: 复杂的非对称树
        System.out.println("\n测试用例 6: 复杂的非对称树");
        TreeNode root6 = new TreeNode(5);
        root6.left = new TreeNode(3);
        root6.right = new TreeNode(8);
        root6.left.left = new TreeNode(1);
        root6.left.right = new TreeNode(4);
        root6.right.right = new TreeNode(10);
        root6.right.right.left = new TreeNode(9);
        root6.right.right.right = new TreeNode(11);
        solution.preOrder(root6);

        System.out.println("\npreOrder 测试完成");
    }

    @Test
    void testIsPerfectBinaryTree_ValidTrees() {
        PerfectBinaryTreeFromArray tree = new PerfectBinaryTreeFromArray();
        // 测试用例 11: 验证完美二叉树
        System.out.println("\n测试用例 11: 验证完美二叉树");

        // 单节点
        TreeNode single = new TreeNode(1);
        assertTrue(tree.isPerfectBinaryTree(single), "单节点应该是完美二叉树");

        // 3 个节点
        TreeNode threeNodes = tree.buildPerfectBinaryTree(new int[]{1, 2, 3});
        assertTrue(tree.isPerfectBinaryTree(threeNodes), "3 个节点的树应该是完美二叉树");

        // 7 个节点
        TreeNode sevenNodes = tree.buildPerfectBinaryTree(new int[]{1, 2, 3, 4, 5, 6, 7});
        assertTrue(tree.isPerfectBinaryTree(sevenNodes), "7 个节点的树应该是完美二叉树");

        // 15 个节点
        TreeNode fifteenNodes = tree.buildPerfectBinaryTree(new int[15]);
        assertTrue(tree.isPerfectBinaryTree(fifteenNodes), "15 个节点的树应该是完美二叉树");
    }

    @Test
    void testInsert() {
        System.out.println("\n=== insert 方法测试 ===");

        // 测试用例 1: 向空树插入（注意：由于方法是 void 且参数传递问题，这个测试主要用于演示）
        System.out.println("\n测试用例 1: 向空树插入单个节点");
        TreeNode root1 = null;
        solution.insert(5, root1);
        System.out.println("向空树插入 5 (注意：Java 值传递，实际不会改变 root1)");

        // 测试用例 2: 构建二叉搜索树并验证
        System.out.println("\n测试用例 2: 构建二叉搜索树");
        TreeNode root2 = new TreeNode(5,
                new TreeNode(3, new TreeNode(2), new TreeNode(4)),
                new TreeNode(7, new TreeNode(6), null));

//        solution.insert(3, root2);
//        solution.insert(7, root2);
//        solution.insert(2, root2);
//        solution.insert(4, root2);
//        solution.insert(6, root2);
        solution.insert(8, root2);

        // 验证树的结构
        assertEquals(5, root2.val, "根节点应为 5");
        assertEquals(3, root2.left.val, "左子节点应为 3");
        assertEquals(7, root2.right.val, "右子节点应为 7");
        assertEquals(2, root2.left.left.val, "左左子节点应为 2");
        assertEquals(4, root2.left.right.val, "左右子节点应为 4");
        assertEquals(6, root2.right.left.val, "右左子节点应为 6");
        assertEquals(8, root2.right.right.val, "右右子节点应为 8");

        System.out.println("构建的二叉搜索树:");
        System.out.println("       5");
        System.out.println("      / \\");
        System.out.println("     3   7");
        System.out.println("    / \\ / \\");
        System.out.println("   2  4 6  8");

        // 测试用例 3: 插入重复值
        System.out.println("\n测试用例 3: 插入重复值");
        int originalLeftVal = root2.left.val;
        solution.insert(3, root2); // 插入已存在的值
        assertEquals(originalLeftVal, root2.left.val, "插入重复值不应改变树结构");
        System.out.println("插入重复值 3，树结构未改变");

        // 测试用例 4: 插入递增序列
        System.out.println("\n测试用例 4: 插入递增序列");
        TreeNode root4 = new TreeNode(1);
        solution.insert(2, root4);
        solution.insert(3, root4);
        solution.insert(4, root4);

        assertEquals(1, root4.val);
        assertEquals(2, root4.right.val);
        assertEquals(3, root4.right.right.val);
        assertEquals(4, root4.right.right.right.val);
        assertNull(root4.left, "递增序列应全部在右子树");
        System.out.println("递增序列形成右斜树：1->2->3->4");

        // 测试用例 5: 插入递减序列
        System.out.println("\n测试用例 5: 插入递减序列");
        TreeNode root5 = new TreeNode(5);
        solution.insert(4, root5);
        solution.insert(3, root5);
        solution.insert(2, root5);

        assertEquals(5, root5.val);
        assertEquals(4, root5.left.val);
        assertEquals(3, root5.left.left.val);
        assertEquals(2, root5.left.left.left.val);
        assertNull(root5.right, "递减序列应全部在左子树");
        System.out.println("递减序列形成左斜树：5->4->3->2");

        // 测试用例 6: 混合插入
        System.out.println("\n测试用例 6: 混合正负数");
        TreeNode root6 = new TreeNode(0);
        solution.insert(-5, root6);
        solution.insert(5, root6);
        solution.insert(-2, root6);
        solution.insert(2, root6);

        assertEquals(0, root6.val);
        assertEquals(-5, root6.left.val);
        assertEquals(5, root6.right.val);
        assertEquals(-2, root6.left.right.val);
        assertEquals(2, root6.right.left.val);
        System.out.println("混合正负数插入成功");

        System.out.println("\n=== insert 方法测试完成 ===");
    }

    /**
     * binarySearchLeftEdge 方法测试 - 二分查找最左边的 target
     */
    @Test
    void testBinarySearchLeftEdge_Basic() {
        System.out.println("\n=== 测试用例 1: 基本查找 ===");
        int[] nums1 = {1, 2, 3, 4, 5};

        // 查找存在的元素
        assertEquals(2, solution.binarySearchLeftEdge(nums1, 3), "应该找到索引 2");
        assertEquals(0, solution.binarySearchLeftEdge(nums1, 1), "应该找到索引 0");
        assertEquals(4, solution.binarySearchLeftEdge(nums1, 5), "应该找到索引 4");

        // 查找不存在的元素
        assertEquals(-1, solution.binarySearchLeftEdge(nums1, 6), "不存在的元素应返回 -1");
        assertEquals(-1, solution.binarySearchLeftEdge(nums1, 0), "不存在的元素应返回 -1");

        System.out.println("基本查找测试通过");
    }

    @Test
    void testBinarySearchLeftEdge_Duplicates() {
        System.out.println("\n=== 测试用例 2: 重复元素（查找最左边）===");
        int[] nums2 = {1, 2, 2, 2, 3, 4, 5};

        // 查找有重复的元素，应该返回最左边的索引
        assertEquals(1, solution.binarySearchLeftEdge(nums2, 2), "应该返回最左边的索引 1");

        // 查找唯一元素
//        assertEquals(0, solution.binarySearchLeftEdge(nums2, 1), "应该找到索引 0");
        assertEquals(4, solution.binarySearchLeftEdge(nums2, 3), "应该找到索引 4");
        assertEquals(6, solution.binarySearchLeftEdge(nums2, 5), "应该找到索引 6");

        System.out.println("重复元素查找测试通过");
        System.out.println("数组：[1, 2, 2, 2, 3, 4, 5]");
        System.out.println("查找 2，返回索引：" + solution.binarySearchLeftEdge(nums2, 2));
    }

    @Test
    void testBinarySearchLeftEdge_AllSame() {
        System.out.println("\n=== 测试用例 3: 所有元素相同 ===");
        int[] nums3 = {5, 5, 5, 5, 5};

        // 查找唯一的值，应该返回最左边的索引 0
        assertEquals(0, solution.binarySearchLeftEdge(nums3, 5), "应该返回最左边的索引 0");
        assertEquals(-1, solution.binarySearchLeftEdge(nums3, 3), "不存在的元素应返回 -1");

        System.out.println("全相同元素查找测试通过");
    }

    @Test
    void testBinarySearchLeftEdge_SingleElement() {
        System.out.println("\n=== 测试用例 4: 单元素数组 ===");
        int[] nums4 = {1};

        assertEquals(0, solution.binarySearchLeftEdge(nums4, 1), "应该找到索引 0");
        assertEquals(-1, solution.binarySearchLeftEdge(nums4, 2), "不存在的元素应返回 -1");

        System.out.println("单元素查找测试通过");
    }

    @Test
    void testBinarySearchLeftEdge_TwoElements() {
        System.out.println("\n=== 测试用例 5: 双元素数组 ===");
        int[] nums5 = {1, 3};

        assertEquals(0, solution.binarySearchLeftEdge(nums5, 1), "应该找到索引 0");
        assertEquals(1, solution.binarySearchLeftEdge(nums5, 3), "应该找到索引 1");
        assertEquals(-1, solution.binarySearchLeftEdge(nums5, 2), "不存在的元素应返回 -1");

        System.out.println("双元素查找测试通过");
    }

    @Test
    void testBinarySearchLeftEdge_EmptyAndNull() {
        System.out.println("\n=== 测试用例 6: 空数组和 null ===");

        // 空数组
        int[] emptyNums = {};
        assertEquals(-1, solution.binarySearchLeftEdge(emptyNums, 1), "空数组应返回 -1");

        // null 数组
        assertEquals(-1, solution.binarySearchLeftEdge(null, 1), "null 数组应返回 -1");

        System.out.println("空数组和 null 测试通过");
    }

    @Test
    void testBinarySearchLeftEdge_NegativeNumbers() {
        System.out.println("\n=== 测试用例 7: 包含负数 ===");
        int[] nums7 = {-5, -3, -1, 0, 2, 4};

        assertEquals(0, solution.binarySearchLeftEdge(nums7, -5), "应该找到索引 0");
        assertEquals(2, solution.binarySearchLeftEdge(nums7, -1), "应该找到索引 2");
        assertEquals(3, solution.binarySearchLeftEdge(nums7, 0), "应该找到索引 3");
        assertEquals(-1, solution.binarySearchLeftEdge(nums7, -2), "不存在的元素应返回 -1");

        System.out.println("负数查找测试通过");
    }

    @Test
    void testBinarySearchLeftEdge_LargeArray() {
        System.out.println("\n=== 测试用例 8: 较大数组 ===");
        int[] nums8 = new int[100];
        for (int i = 0; i < 100; i++) {
            nums8[i] = i;
        }

        // 查找第一个元素
        assertEquals(0, solution.binarySearchLeftEdge(nums8, 0), "应该找到索引 0");
        // 查找中间元素
        assertEquals(50, solution.binarySearchLeftEdge(nums8, 50), "应该找到索引 50");
        // 查找最后一个元素
        assertEquals(99, solution.binarySearchLeftEdge(nums8, 99), "应该找到索引 99");
        // 查找不存在的元素
        assertEquals(-1, solution.binarySearchLeftEdge(nums8, 100), "不存在的元素应返回 -1");

        System.out.println("大数组查找测试通过");
    }

    @Test
    void testBinarySearchLeftEdge_MultipleDuplicates() {
        System.out.println("\n=== 测试用例 9: 多个重复元素组 ===");
        int[] nums9 = {1, 1, 1, 2, 2, 3, 3, 3, 3, 4, 5, 5, 5};

        // 查找不同组的重复元素
        assertEquals(0, solution.binarySearchLeftEdge(nums9, 1), "应该返回 1 的最左索引 0");
        assertEquals(3, solution.binarySearchLeftEdge(nums9, 2), "应该返回 2 的最左索引 3");
        assertEquals(5, solution.binarySearchLeftEdge(nums9, 3), "应该返回 3 的最左索引 5");
        assertEquals(9, solution.binarySearchLeftEdge(nums9, 4), "应该返回 4 的索引 9");
        assertEquals(10, solution.binarySearchLeftEdge(nums9, 5), "应该返回 5 的最左索引 10");

        System.out.println("多组重复元素查找测试通过");
        System.out.println("数组：[1,1,1,2,2,3,3,3,3,4,5,5,5]");
        System.out.println("查找 3，返回索引：" + solution.binarySearchLeftEdge(nums9, 3));
    }

    @Test
    void testBinarySearchLeftEdge_EdgeCases() {
        System.out.println("\n=== 测试用例 10: 边界情况 ===");

        // 只查找比所有元素都小的值
        int[] nums10 = {10, 20, 30};
        assertEquals(-1, solution.binarySearchLeftEdge(nums10, 5), "小于所有元素应返回 -1");

        // 只查找比所有元素都大的值
        assertEquals(-1, solution.binarySearchLeftEdge(nums10, 50), "大于所有元素应返回 -1");

        // 查找两个元素之间的值
        assertEquals(-1, solution.binarySearchLeftEdge(nums10, 15), "不存在的元素应返回 -1");

        System.out.println("边界情况测试通过");
    }

    @Test
    void twoSumBruteForce() {
        int[] ints = {2, 7, 11, 15};
        int[] ints1 = solution.twoSumBruteForce(ints, 9);
    }

    @Test
    void twoSumHashTable() {
        int[] ints = {2, 7, 11, 15};
        int[] ints1 = solution.twoSumHashTable(ints, 9);
        System.out.println(ints1[0] + " " + ints1[1]);
    }

    @Test
    void selectionSort() {
        int[] ints = {5, 2, 4, 6, 1, 3};
        solution.selectionSort(ints);
        System.out.println(ints);
    }

    @Test
    void bubbleSort() {
        int[] ints = {5, 1, 2, 3, 4};
        solution.bubbleSort(ints);
        System.out.println(ints);
    }

    @Test
    void quickSort2() {
        int[] ints = {5, 1, 2, 3, 4};
        solution.quickSort2(ints);
        System.out.println(ints);
    }

    @Test
    void test(){
        int f1 =129;
        Integer f2 =129;
        double f3 =1d;
        Double f4 =1d;
        Float f5 =1f;
        long f6 = 1L;

        System.out.println(f1==f2);
        System.out.println(f1==f3);
        System.out.println(f1==f4);
        System.out.println(f1==f5);
        System.out.println(f1==f6);
    }

    /**
     * buildTree 方法测试 - 根据前序和中序遍历构建二叉树
     */
    @Test
    void testBuildTree_Basic() {
        System.out.println("\n=== 测试用例 1: buildTree 基本测试 ===");

        // 构建测试数据
        //     3
        //    / \
        //   9  20
        //      / \
        //     15  7
        int[] preorder1 = {3, 9, 20, 15, 7};
        int[] inorder1 = {9, 3, 15, 20, 7};

        TreeNode root1 = solution.buildTree1(preorder1, inorder1);

        assertNotNull(root1, "根节点不应为 null");
        assertEquals(3, root1.val, "根节点值应为 3");
        assertEquals(9, root1.left.val, "左子节点值应为 9");
        assertEquals(20, root1.right.val, "右子节点值应为 20");
        assertEquals(15, root1.right.left.val, "右左子节点值应为 15");
        assertEquals(7, root1.right.right.val, "右右子节点值应为 7");

        System.out.println("构建的二叉树:");
        System.out.println("    3");
        System.out.println("   / \\");
        System.out.println("  9  20");
        System.out.println("     / \\");
        System.out.println("    15  7");

        int[] preorder2 = {1, 2, 4, 5, 3};
        int[] inorder2 = {4, 2, 5, 1, 3};

        TreeNode root2 = solution.buildTree(preorder2, inorder2);
        System.out.println(1);
    }

    @Test
    void testBuildTree1() {
        int[] preorder1 = {3,9,20,15,7};
        int[] inorder1 = {9,3,15,20,7};

        TreeNode root1 = solution.buildTree1(preorder1, inorder1);
        System.out.println(1);
    }

    /**
     * findPathToTarget 方法测试 - 在二叉树中搜索所有值为 target 的节点，返回路径
     */
    @Test
    void testFindPathToTarget_Basic() {
        System.out.println("\n=== 测试用例 1: 基本查找 ===");

        // 构建测试二叉树:
        //       1
        //      / \
        //     2   3
        //    / \   \
        //   4   2   5
        TreeNode root = new TreeNode(1);
        root.left = new TreeNode(2);
        root.right = new TreeNode(3);
        root.left.left = new TreeNode(4);
        root.left.right = new TreeNode(2);
        root.right.right = new TreeNode(5);

        List<List<Integer>> paths = solution.findPathToTarget(root, 2);

        System.out.println("查找目标值: 2");
        System.out.println("找到的路径数: " + paths.size());
        for (List<Integer> path : paths) {
            System.out.println(path);
        }

        assertEquals(2, paths.size(), "应该找到 2 条路径");

        // 验证路径内容
        List<Integer> expectedPath1 = Arrays.asList(1, 2);
        List<Integer> expectedPath2 = Arrays.asList(1, 2, 2);

        assertTrue(paths.contains(expectedPath1), "应该包含路径 [1, 2]");
        assertTrue(paths.contains(expectedPath2), "应该包含路径 [1, 2, 2]");
    }
}