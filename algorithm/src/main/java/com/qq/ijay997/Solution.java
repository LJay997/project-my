package com.qq.ijay997;

import java.util.*;


/**
 * leetcode
 */
public class Solution {

    /**
     * 1. 两数之和
     * https://leetcode.cn/problems/two-sum/?spm=5176.28103460.0.0.3f906308rp5Ufn
     */
    public int[] twoSum(int[] nums, int target) {
        for (int left = 0; left < nums.length - 1; left++) {
            int shengyu = target - nums[left];
            for (int right = nums.length - 1; right > 0 && right > left; right--) {
                if (shengyu == nums[right]) {
                    return new int[]{left, right};
                }
            }
        }
        return null;
    }

    /**
     * 1. 两数之和
     * 基于查找表实现 查找表的两个常见实现， 哈希表、平衡二叉搜索数（可维护元素的顺序性）
     *
     * @param nums
     * @param target
     * @return
     */
    public int[] twoSum2(int[] nums, int target) {
        // K:num 的元素, V: num 的元素的所在下标,
        HashMap<Integer, Integer> hashMap = new HashMap<>(nums.length);
        hashMap.put(nums[0], 0);
        for (int i = 1; i < nums.length; i++) {
            int shengyu = target - nums[i];
            if (hashMap.containsKey(shengyu)) {
                return new int[]{hashMap.get(shengyu), i};
            } else {
                hashMap.put(nums[i], i);
            }
        }
        return null;
    }

    /**
     * 删除排序数组中的重复项
     * https://leetcode.cn/problems/remove-duplicates-from-sorted-array/solutions/728105/shan-chu-pai-xu-shu-zu-zhong-de-zhong-fu-tudo/?spm=5176.28103460.0.0.3f906308rp5Ufn
     *
     * @param nums
     * @return 数组的长度
     */
    public int removeDuplicates(int[] nums) {
        // 快慢指针
        int slow = 0;
        for (int fast = 1; fast < nums.length; fast++) {
            if (nums[fast] != nums[slow]) {
                nums[++slow] = nums[fast];
            }
        }
        return slow + 1;
    }

    /**
     * https://leetcode.cn/problems/remove-element
     * 27. 移除元素
     *
     * @param nums
     * @param val
     * @return
     */
    public int removeElement(int[] nums, int val) {
        int fast = 0, slow = 0;
        for (; fast < nums.length; fast++) {
            if (nums[fast] != val) {
                nums[slow++] = nums[fast];
            }
        }
        return slow;
    }

    /**
     * https://leetcode.cn/problems/search-insert-position
     * 35. 搜索插入位置
     *
     * @param nums
     * @param target
     * @return
     */
    public int searchInsert(int[] nums, int target) {
        int start = 0, end = nums.length - 1, mid;
        for (; start <= end; ) {
            mid = start + (end - start) / 2;
            if (nums[mid] == target) return mid;
            if (nums[mid] > target) {
                end = mid - 1;
            } else {
                start = mid + 1;
            }
        }
        return start;
    }

    /**
     * https://leetcode.cn/problems/best-time-to-buy-and-sell-stock
     * 121. 买卖股票的最佳时机
     *
     * @param prices
     * @return
     */
    public int maxProfit(int[] prices) {
        // 记录到目前为止的最低价格, 记录最大利润
        int minPrice = prices[0], maxProfit = 0;

        for (int i = 0; i < prices.length; i++) {
            if (prices[i] < minPrice) {
                // 如果当前价格更低，更新最低价格
                minPrice = prices[i];
            } else {
                // 否则，计算利润
                maxProfit = Math.max(maxProfit, prices[i] - minPrice);
            }
        }
        return maxProfit;
    }

    /**
     * https://leetcode.cn/problems/move-zeroes/description
     * 283. 移动零
     *
     * @param nums
     */
    public void moveZeroes(int[] nums) {
        int fast, slow = 0;
        for (fast = 0; fast < nums.length; fast++) {
            if (nums[fast] != 0 && nums[slow] == 0) {
                nums[slow] = nums[fast];
                nums[fast] = 0;
                slow++;
            }
        }
    }

    /**
     * https://leetcode.cn/problems/longest-substring-without-repeating-characters
     * 3. 无重复字符的最长子串
     *
     * @param s
     * @return
     */
    public int lengthOfLongestSubstring(String s) {
        if (s == null || s.isEmpty()) return 0;

        int maxLength = 0;
        LinkedHashMap<Character, Integer> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put(s.charAt(0), 0);
        HashMap<Character, Integer> charMap = new HashMap<>();
        for (int slow = 0, fast = 0; fast < s.length(); fast++) {
            char curChar = s.charAt(fast);

            // 如果当前字符已经出现过，则更新slow 直接提到重复的字符的下一个位置,防止 slow 越来越小，导致重复判断
            if (charMap.containsKey(curChar) && charMap.get(curChar) >= slow) {
                slow = charMap.get(curChar) + 1;
            }
            // 添加当前字符
            charMap.put(curChar, fast);
            // 更新最大长度
            maxLength = Math.max(maxLength, fast - slow + 1);
        }
        return maxLength;
    }

    /**
     * https://leetcode.cn/problems/maximum-subarray
     * TODO 再次学习
     * 53. 最大子数组和
     *
     * @param nums
     * @return
     */
    public int maxSubArray(int[] nums) {
        int maxSum = nums[0];
        int curSum = nums[0];
        for (int i = 1; i < nums.length; i++) {
            // 如果越加越小 则不加 用原来的数
            curSum = Math.max(curSum + nums[i], nums[i]);
            // 更新最大值
            maxSum = Math.max(maxSum, curSum);
        }
        return maxSum;
    }

    public ListNode reverseList2(ListNode head) {
        if (head == null) return null;

        ArrayDeque<ListNode> nodeQue = new ArrayDeque<>();
        while (head != null) {
            nodeQue.add(head);
            head = head.next;
        }

        ListNode last = nodeQue.peekLast();
        ListNode node;
        while (!nodeQue.isEmpty()) {
            node = nodeQue.pollLast();
            if (!nodeQue.isEmpty())
                node.next = nodeQue.peekLast();
            else node.next = null;
        }
        return last;
    }

    /**
     * https://leetcode.cn/problems/reverse-linked-list
     * 206. 反转链表
     *
     * @param head
     * @return
     */
    public ListNode reverseList(ListNode head) {
        ListNode curNode = head;
        ListNode preNode = null;
        ListNode tmp;
        for (; curNode != null; ) {
            tmp = curNode.next;

            curNode.next = preNode;
            // 移动指针
            preNode = curNode;
            curNode = tmp;
        }
        return preNode;
    }

    /**
     * https://leetcode.cn/problems/merge-two-sorted-lists
     * 21. 合并两个有序链表 TODO 继续学习
     *
     * @param list1
     * @param list2
     * @return
     */
    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {
        // 创建虚拟头节点
        ListNode dummy = new ListNode(-1);
        ListNode cur = dummy;

        while (list1 != null && list2 != null) {

            // 比较两个链表的值，将较小的节点链接到结果链表中
            if (list1.val < list2.val) {
                cur.next = list1;
                list1 = list1.next;
            } else {
                cur.next = list2;
                list2 = list2.next;
            }
            cur = cur.next;
        }

        // 链接剩余的节点
        cur.next = list1 == null ?
                list2 : list1;
        return dummy.next;
    }

    /**
     * https://leetcode.cn/problems/linked-list-cycle
     * 141. 环形链表
     *
     * @param head
     * @return
     */
    public boolean hasCycle(ListNode head) {
        HashMap<ListNode, Integer> map = new HashMap<>();

        for (int i = 0; head != null; head = head.next, i++) {
            if (map.containsKey(head)) {
                return true;
            } else map.put(head, i);
        }
        return false;
    }

    public boolean hasCycle1(ListNode head) {
        if (head == null || head.next == null) return false;

        ListNode slow = head;
        ListNode fast = head.next;
        for (; slow != null && fast != null; ) {
            if (slow == fast) return true;

            if (fast.next == null) return false;
            fast = fast.next.next;
            slow = slow.next;
        }
        return false;
    }

    /**
     * https://leetcode.cn/problems/remove-nth-node-from-end-of-list
     * 19. 删除链表的倒数第 N 个结点
     * TODO 优化优化
     * @param head
     * @param n
     * @return
     */
    public ListNode removeNthFromEnd(ListNode head, int n) {
        if (head == null || n <= 0) return head;

        ListNode cur = head;
        List<ListNode> list = new ArrayList<>();

        for (; cur != null; cur = cur.next)
            list.add(cur);

        int index = list.size() - n;
        if (index < 0) return head;

        // 删除头结点
        if (index == 0) return head.next;

        // 切断链表
        if (n == 1) {
            list.get(index - 1).next = null;
            return head;
        }

        ListNode pr = list.get(index - 1);
        pr.next = list.get(index + 1);
        return head;
    }
}