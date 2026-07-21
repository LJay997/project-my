package com.qq.ijay997;

import java.util.Arrays;

public class Solution2 {
    public static void main(String[] args) {
        int[] nums = {0, 1, 0, 3, 12};
        new Solution2().moveZeroes(nums);
        System.out.println(Arrays.toString(nums));
    }
    public void moveZeroes(int[] nums) {
        int fast, slow = 0;
        for (fast = 0; fast < nums.length; fast++) {
            if (nums[fast] != 0 && nums[slow] == 0){
                nums[slow] = nums[fast];
                nums[fast] = 0;
                slow++;
            }
        }
        System.out.println(1);
    }

}
