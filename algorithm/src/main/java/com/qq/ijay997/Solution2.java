package com.qq.ijay997;

import java.util.Arrays;

public class Solution2 {
//    public static void main(String[] args) {
//        int[] nums = {0, 1, 0, 3, 12};
//        new Solution2().moveZeroes(nums);
//        System.out.println(Arrays.toString(nums));
//    }

    public void moveZeroes(int[] nums) {
        int fast, slow = 0;
        for (fast = 0; fast < nums.length; fast++) {
            if (nums[fast] != 0 && nums[slow] == 0) {
                nums[slow] = nums[fast];
                nums[fast] = 0;
                slow++;
            }
        }
        System.out.println(1);
    }

    public ListNode reverseList(ListNode head) {
        if (head == null)
            return null;

        ListNode pre = null, cur = head, next;
        for (; cur != null; ) {
            next = cur.next;
            cur.next = pre;
            pre = cur;
            cur = next;
        }
        return pre;
    }

    public static void quickSort(int[] nums, int start, int end) {
        if (start >= end)
            return;
        if (nums == null || nums.length == 1) {
            return;
        }
        int pivot = nums[start];
        int left = start, right = end;
        while (left < right) {
            while (!(nums[right] < pivot) && left < right) right--;
            while (!(nums[left] > pivot) && left < right) left++;
            if (left < right) {
                swap(nums, left, right);
            }
        }
        swap(nums, start, left);
        quickSort(nums, start, left - 1);
        quickSort(nums, left + 1, end);
    }
    public static void swap(int[] nums, int i, int j) {
        int temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }

    public static void main(String[] args) {
        int[] nums = {6, 1, 2, 7, 9, 3, 4, 5, 10, 8};
        quickSort(nums, 0, nums.length - 1);

        for (int num : nums) {
            System.out.print(num + " ");
        }
    }
}
