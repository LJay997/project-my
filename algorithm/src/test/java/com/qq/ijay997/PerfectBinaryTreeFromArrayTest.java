package com.qq.ijay997;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PerfectBinaryTreeFromArrayTest {

    private PerfectBinaryTreeFromArray solution;

    @BeforeEach
    void setUp() {
        solution = new PerfectBinaryTreeFromArray();
    }

    /**
     * 辅助方法：层序遍历打印二叉树（用于验证）
     */
    private void printTree(TreeNode root) {
        if (root == null) {
            System.out.println("空树");
            return;
        }
        
        java.util.Queue<TreeNode> queue = new java.util.LinkedList<>();
        queue.offer(root);
        
        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                TreeNode node = queue.poll();
                if (node != null) {
                    System.out.print(node.val + " ");
                    queue.offer(node.left);
                    queue.offer(node.right);
                } else {
                    System.out.print("null ");
                }
            }
            System.out.println();
        }
    }

    /**
     * 辅助方法：验证是否为完美二叉树
     */
    private boolean verifyPerfectBinaryTree(TreeNode root) {
        if (root == null) return true;
        
        // 计算树的深度
        int depth = getDepth(root);
        
        // 检查所有节点是否满足完美二叉树的要求
        return checkPerfect(root, 0, depth);
    }
    
    private int getDepth(TreeNode node) {
        if (node == null) return 0;
        return 1 + Math.max(getDepth(node.left), getDepth(node.right));
    }
    
    private boolean checkPerfect(TreeNode node, int level, int totalDepth) {
        if (node == null) return true;
        
        // 叶子节点必须在最后一层
        if (node.left == null && node.right == null) {
            return level == totalDepth - 1;
        }
        
        // 内部节点必须有两个子节点
        if (node.left == null || node.right == null) {
            return false;
        }
        
        return checkPerfect(node.left, level + 1, totalDepth) && 
               checkPerfect(node.right, level + 1, totalDepth);
    }

    @Test
    void testBuildPerfectBinaryTree_SingleNode() {
        // 测试用例 1: 单节点
        System.out.println("测试用例 1: 单节点");
        int[] arr1 = {1};
        TreeNode root1 = solution.buildPerfectBinaryTree(arr1);
        
        assertNotNull(root1, "根节点不应为 null");
        assertEquals(1, root1.val, "节点值应为 1");
        assertNull(root1.left, "左子节点应为 null");
        assertNull(root1.right, "右子节点应为 null");
        assertTrue(verifyPerfectBinaryTree(root1), "应该是完美二叉树");
        
        printTree(root1);
    }

    @Test
    void testBuildPerfectBinaryTree_ThreeNodes() {
        // 测试用例 2: 3 个节点的完美二叉树
        System.out.println("\n测试用例 2: 3 个节点");
        int[] arr2 = {1, 2, 3};
        TreeNode root2 = solution.buildPerfectBinaryTree(arr2);
        
        assertNotNull(root2);
        assertEquals(1, root2.val);
        assertEquals(2, root2.left.val);
        assertEquals(3, root2.right.val);
        assertNull(root2.left.left);
        assertNull(root2.left.right);
        assertNull(root2.right.left);
        assertNull(root2.right.right);
        assertTrue(verifyPerfectBinaryTree(root2));
        
        printTree(root2);
    }

    @Test
    void testBuildPerfectBinaryTree_SevenNodes() {
        // 测试用例 3: 7 个节点的完美二叉树
        System.out.println("\n测试用例 3: 7 个节点");
        int[] arr3 = {1, 2, 3, 4, 5, 6, 7};
        TreeNode root3 = solution.buildPerfectBinaryTree(arr3);
        
        assertNotNull(root3);
        assertEquals(1, root3.val);
        
        // 第二层
        assertEquals(2, root3.left.val);
        assertEquals(3, root3.right.val);
        
        // 第三层（叶子节点）
        assertEquals(4, root3.left.left.val);
        assertEquals(5, root3.left.right.val);
        assertEquals(6, root3.right.left.val);
        assertEquals(7, root3.right.right.val);
        
        assertTrue(verifyPerfectBinaryTree(root3));
        
        printTree(root3);
    }

    @Test
    void testBuildPerfectBinaryTree_FifteenNodes() {
        // 测试用例 4: 15 个节点的完美二叉树
        System.out.println("\n测试用例 4: 15 个节点");
        int[] arr4 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        TreeNode root4 = solution.buildPerfectBinaryTree(arr4);
        
        assertNotNull(root4);
        assertEquals(1, root4.val);
        assertTrue(verifyPerfectBinaryTree(root4));
        
        printTree(root4);
    }

    @Test
    void testBuildPerfectBinaryTree_EmptyArray() {
        // 测试用例 5: 空数组
        System.out.println("\n测试用例 5: 空数组");
        int[] arr5 = {};
        TreeNode root5 = solution.buildPerfectBinaryTree(arr5);
        
        assertNull(root5, "空数组应返回 null");
    }

    @Test
    void testBuildPerfectBinaryTree_NullArray() {
        // 测试用例 6: null 数组
        System.out.println("\n测试用例 6: null 数组");
        TreeNode root6 = solution.buildPerfectBinaryTree(null);
        
        assertNull(root6, "null 数组应返回 null");
    }

    @Test
    void testBuildPerfectBinaryTree_WithNegativeValues() {
        // 测试用例 7: 包含负数
        System.out.println("\n测试用例 7: 包含负数");
        int[] arr7 = {-1, -2, -3, -4, -5, -6, -7};
        TreeNode root7 = solution.buildPerfectBinaryTree(arr7);
        
        assertNotNull(root7);
        assertEquals(-1, root7.val);
        assertEquals(-2, root7.left.val);
        assertEquals(-3, root7.right.val);
        assertTrue(verifyPerfectBinaryTree(root7));
        
        printTree(root7);
    }

    @Test
    void testBuildPerfectBinaryTree_MixedValues() {
        // 测试用例 8: 混合正负数
        System.out.println("\n测试用例 8: 混合正负数");
        int[] arr8 = {3, 9, 20, -15, 17, -10, 25};
        TreeNode root8 = solution.buildPerfectBinaryTree(arr8);
        
        assertNotNull(root8);
        assertEquals(3, root8.val);
        assertEquals(9, root8.left.val);
        assertEquals(20, root8.right.val);
        assertEquals(-15, root8.left.left.val);
        assertEquals(17, root8.left.right.val);
        assertEquals(-10, root8.right.left.val);
        assertEquals(25, root8.right.right.val);
        assertTrue(verifyPerfectBinaryTree(root8));
        
        printTree(root8);
    }

    @Test
    void testBuildPerfectBinaryTree_LargeTree() {
        // 测试用例 9: 较大的树（31 个节点，深度为 5）
        System.out.println("\n测试用例 9: 31 个节点的大树");
        int[] arr9 = new int[31];
        for (int i = 0; i < 31; i++) {
            arr9[i] = i + 1;
        }
        
        TreeNode root9 = solution.buildPerfectBinaryTree(arr9);
        
        assertNotNull(root9);
        assertEquals(1, root9.val);
        assertTrue(verifyPerfectBinaryTree(root9));
        
        System.out.println("大树构建成功，根节点值：" + root9.val);
    }

    @Test
    void testBuildPerfectBinaryTree_AllSameValues() {
        // 测试用例 10: 所有节点值相同
        System.out.println("\n测试用例 10: 所有节点值相同");
        int[] arr10 = {5, 5, 5, 5, 5, 5, 5};
        TreeNode root10 = solution.buildPerfectBinaryTree(arr10);
        
        assertNotNull(root10);
        assertEquals(5, root10.val);
        assertEquals(5, root10.left.val);
        assertEquals(5, root10.right.val);
        assertTrue(verifyPerfectBinaryTree(root10));
        
        printTree(root10);
    }
}
