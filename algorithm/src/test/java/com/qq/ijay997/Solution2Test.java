package com.qq.ijay997;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Solution2Test {

    private Solution2 solution2;

    @BeforeEach
    void setUp() {
        solution2 = new Solution2();
    }

    @Test
    void moveZeroes() {
        int[] nums = {0, 1, 0, 3, 12};
        solution2.moveZeroes(nums);
        assertArrayEquals(new int[]{1, 3, 12, 0, 0}, nums);
    }

    @Test
    void moveZeroes_AllZeros() {
        int[] nums = {0, 0, 0};
        solution2.moveZeroes(nums);
        assertArrayEquals(new int[]{0, 0, 0}, nums);
    }

    @Test
    void moveZeroes_NoZeros() {
        int[] nums = {1, 2, 3};
        solution2.moveZeroes(nums);
        assertArrayEquals(new int[]{1, 2, 3}, nums);
    }

    @Test
    void moveZeroes_SingleZero() {
        int[] nums = {0};
        solution2.moveZeroes(nums);
        assertArrayEquals(new int[]{0}, nums);
    }

    @Test
    void moveZeroes_LeadingZeros() {
        int[] nums = {0, 0, 1, 2, 3};
        solution2.moveZeroes(nums);
        assertArrayEquals(new int[]{1, 2, 3, 0, 0}, nums);
    }

    @Test
    void moveZeroes_TrailingZeros() {
        int[] nums = {1, 2, 3, 0, 0};
        solution2.moveZeroes(nums);
        assertArrayEquals(new int[]{1, 2, 3, 0, 0}, nums);
    }

    @Test
    void reverseList_NullHead() {
        ListNode result = solution2.reverseList(null);
        assertNull(result);
    }

    @Test
    void reverseList_SingleNode() {
        ListNode head = new ListNode(1);
        ListNode result = solution2.reverseList(head);
        assertNotNull(result);
        assertEquals(1, result.val);
        assertNull(result.next);
    }

    @Test
    void reverseList_TwoNodes() {
        ListNode head = createLinkedList(new int[]{1, 2});
        ListNode result = solution2.reverseList(head);
        assertNotNull(result);
        assertLinkedListEquals(new int[]{2, 1}, result);
    }

    @Test
    void reverseList_MultipleNodes() {
        ListNode head = createLinkedList(new int[]{1, 2, 3, 4, 5});
        ListNode result = solution2.reverseList(head);
        assertNotNull(result);
        assertLinkedListEquals(new int[]{5, 4, 3, 2, 1}, result);
    }

    @Test
    void reverseList_AllSameValues() {
        ListNode head = createLinkedList(new int[]{5, 5, 5, 5});
        ListNode result = solution2.reverseList(head);
        assertNotNull(result);
        assertLinkedListEquals(new int[]{5, 5, 5, 5}, result);
    }

    @Test
    void reverseList_NegativeNumbers() {
        ListNode head = createLinkedList(new int[]{-3, -2, -1, 0, 1, 2, 3});
        ListNode result = solution2.reverseList(head);
        assertNotNull(result);
        assertLinkedListEquals(new int[]{3, 2, 1, 0, -1, -2, -3}, result);
    }

    @Test
    void reverseList_LargeList() {
        int[] values = new int[100];
        for (int i = 0; i < 100; i++) {
            values[i] = i + 1;
        }
        ListNode head = createLinkedList(values);
        ListNode result = solution2.reverseList(head);

        ListNode current = result;
        for (int i = 100; i >= 1; i--) {
            assertNotNull(current);
            assertEquals(i, current.val);
            current = current.next;
        }
        assertNull(current);
    }

    @Test
    void reverseList_DuplicateValues() {
        ListNode head = createLinkedList(new int[]{1, 2, 2, 3, 3, 3});
        ListNode result = solution2.reverseList(head);
        assertNotNull(result);
        assertLinkedListEquals(new int[]{3, 3, 3, 2, 2, 1}, result);
    }

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

    private static List<Integer> linkedListToList(ListNode head) {
        List<Integer> list = new ArrayList<>();
        ListNode current = head;
        while (current != null) {
            list.add(current.val);
            current = current.next;
        }
        return list;
    }

    private static void assertLinkedListEquals(int[] expected, ListNode actual) {
        List<Integer> actualList = linkedListToList(actual);
        List<Integer> expectedList = new ArrayList<>();
        for (int val : expected) {
            expectedList.add(val);
        }
        assertEquals(expectedList, actualList);
    }
}