package com.qq.ijay997;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SolutionTest {

    @Test
    void moveZeroes() {
        int[] nums = {1, 0, 1};
        new Solution().moveZeroes(nums);
        assertArrayEquals(new int[]{1, 1, 0}, nums);
    }

    @Test
    void lengthOfLongestSubstring() {
        assertEquals(2, new Solution().lengthOfLongestSubstring("abcabcbb"));
    }

    @Test
    void maxSubArray() {
        assertEquals(6, new Solution().maxSubArray(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}));
    }

    @Test
    void twoSum() {
        assertArrayEquals(new int[]{0, 1}, new Solution().twoSum(new int[]{2, 7, 11, 15}, 9));
    }

    @Test
    void twoSum2() {
        assertArrayEquals(new int[]{0, 1}, new Solution().twoSum2(new int[]{2, 7, 11, 15}, 9));
    }

    @Test
    void removeDuplicates() {
        assertEquals(2, new Solution().removeDuplicates(new int[]{1, 1, 2}));
    }

    @Test
    void removeElement() {
        assertEquals(2, new Solution().removeElement(new int[]{3, 2, 2, 3}, 3));
    }

    @Test
    void searchInsert() {
        assertEquals(2, new Solution().searchInsert(new int[]{1, 3, 5, 6}, 5));
    }

    @Test
    void reverseList() {
        ListNode head = new ListNode(1);
        head.next = new ListNode(2);
        head.next.next = new ListNode(3);
        head.next.next.next = new ListNode(4);
        head.next.next.next.next = new ListNode(5);
        ListNode reverse = new Solution().reverseList(head);
    }

    @Test
    void mergeTwoLists() {
        Solution solution = new Solution();

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
        Solution solution = new Solution();
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
        Solution solution = new Solution();
        List<List<Integer>> lists = solution.levelOrder(root);
        System.out.println(lists); // // 预期结果: [[3],[9,20],[15,7]]
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
        Solution solution = new Solution();
        assertEquals(3, solution.maxDepth(root));
    }


}