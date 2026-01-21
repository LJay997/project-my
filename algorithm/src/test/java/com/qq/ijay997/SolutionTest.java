package com.qq.ijay997;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SolutionTest {

    @org.junit.jupiter.api.Test
    void moveZeroes() {
        int[] nums = {1,0,1};
        new Solution().moveZeroes(nums);
        assertArrayEquals(new int[]{1, 1, 0}, nums);
    }

    @Test
    void lengthOfLongestSubstring() {
        assertEquals(2, new Solution().lengthOfLongestSubstring("abcabcbb"));
    }

    @Test
    void maxSubArray() {
        assertEquals(6, new Solution().maxSubArray(new int[]{-2,1,-3,4,-1,2,1,-5,4}));
    }
}