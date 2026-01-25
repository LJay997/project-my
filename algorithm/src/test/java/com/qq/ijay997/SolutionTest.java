package com.qq.ijay997;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SolutionTest {

    @org.junit.jupiter.api.Test
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
}