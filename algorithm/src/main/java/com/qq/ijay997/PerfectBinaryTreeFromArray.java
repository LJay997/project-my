package com.qq.ijay997;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 完美二叉树的数组表示与还原
 * <p>
 * 题目描述：
 * 给定一个按层序遍历存储的整数数组，请将其还原成一棵完美二叉树。
 * <p>
 * 完美二叉树定义：
 * - 所有内部节点都有两个子节点
 * - 所有叶子节点都在同一层
 * - 如果深度为 k，则节点总数为 2^k - 1
 * <p>
 * 数组表示规则（下标从 0 开始）：
 * - 对于下标为 i 的节点：
 * - 左子节点下标：2*i + 1
 * - 右子节点下标：2*i + 2
 * - 父节点下标：(i-1)/2
 * <p>
 * 示例 1：
 * 输入：[1, 2, 3, 4, 5, 6, 7]
 * 输出：构建如下完美二叉树
 * 1
 * / \
 * 2   3
 * / \ / \
 * 4  5 6  7
 * <p>
 * 示例 2：
 * 输入：[3, 9, 20, 15, 17]
 * 输出：构建如下完美二叉树
 * 3
 * / \
 * 9  20
 * / \
 * 15 17
 * <p>
 * 示例 3：
 * 输入：[1]
 * 输出：单节点树
 * 1
 * <p>
 * 提示：
 * - 数组长度一定是 2^k - 1 (k >= 1)，即：1, 3, 7, 15, 31...
 * - 数组元素范围：-100 <= arr[i] <= 100
 * <p>
 * 解题思路：
 * 方法 1：递归法
 * - 根节点是 arr[0]
 * - 递归构建左子树和右子树
 * - 利用数组下标关系确定子节点位置
 * <p>
 * 方法 2：迭代法（层序遍历）
 * - 使用队列辅助构建
 * - 按层依次连接父子节点
 */
public class PerfectBinaryTreeFromArray {

    /**
     * 将有序数组转换为完美二叉树
     *
     * @param arr 层序遍历存储的数组
     * @return 构建好的完美二叉树的根节点
     */
    public TreeNode buildPerfectBinaryTree(int[] arr) {
        if (arr == null || arr.length == 0) {
            return null;
        }

        // TODO: 请在这里实现你的代码
        TreeNode[] nodes = new TreeNode[arr.length];
        for (int i = 0; i < arr.length; i++) {
            nodes[i] = new TreeNode(arr[i]);
        }
        int floor = arr.length / 2;
        for (int i = 0; i < floor; i++) {
            // 左子节点
            nodes[i].left = nodes[2 * i + 1];
            // 右子节点
            nodes[i].right = nodes[2 * i + 2];
        }
        return nodes[0];
    }

    /**
     * 递归构建完美二叉树
     *
     * @param arr   数组
     * @param index 当前节点在数组中的下标
     * @return 构建的子树根节点
     */
    private TreeNode buildTree(int[] arr, int index) {
        if (index >= arr.length) return null;

        TreeNode left = buildTree(arr, index * 2 + 1);
        TreeNode rigt = buildTree(arr, index * 2 + 2);

        return new TreeNode(arr[index], left, rigt);
    }

    /**
     * 验证构建的二叉树是否为完美二叉树
     *
     * @param root 树的根节点
     * @return 如果是完美二叉树返回 true，否则返回 false
     */
    public boolean isPerfectBinaryTree(TreeNode root) {
        if (root == null) {
            return true;
        }

        // 计算树的深度
        int depth = getDepth(root);

        // 检查是否为完美二叉树
        return checkPerfect(root, 0, depth);
    }

    /**
     * 计算树的深度
     */
    private int getDepth(TreeNode node) {
        if (node == null) {
            return 0;
        }
        return 1 + Math.max(getDepth(node.left), getDepth(node.right));
    }

    /**
     * 检查是否为完美二叉树
     *
     * @param node       当前节点
     * @param level      当前层级
     * @param totalDepth 树的总深度
     * @return 是否为完美二叉树
     */
    private boolean checkPerfect(TreeNode node, int level, int totalDepth) {
        if (node == null) {
            return true;
        }

        // 叶子节点必须在最后一层
        if (node.left == null && node.right == null) {
            return level == totalDepth - 1;
        }

        // 内部节点必须有两个子节点
        if (node.left == null || node.right == null) {
            return false;
        }

        // 递归检查左右子树
        return checkPerfect(node.left, level + 1, totalDepth) &&
                checkPerfect(node.right, level + 1, totalDepth);
    }


}
