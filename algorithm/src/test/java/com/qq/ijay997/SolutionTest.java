package com.qq.ijay997;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}