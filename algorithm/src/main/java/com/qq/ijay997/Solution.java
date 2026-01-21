package com.qq.ijay997;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * 1. 两数之和
 */
// https://leetcode.cn/problems/two-sum/?spm=5176.28103460.0.0.3f906308rp5Ufn
public class Solution {
    public int[] twoSum(int[] nums, int target) {
        for (int firstNumberIndex = 0; firstNumberIndex < nums.length - 1; firstNumberIndex++) {
            int shengyu = target - nums[firstNumberIndex];
            for (int lastNumberIndex = nums.length - 1; lastNumberIndex > 0 && lastNumberIndex > firstNumberIndex; lastNumberIndex--) {
                if (shengyu == nums[lastNumberIndex]) {
                    return new int[]{firstNumberIndex, lastNumberIndex};
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        Solution solution = new Solution();
        int i;
//        int[] ints = solution.twoSum2(new int[]{3, 2, 4}, 6);

//        int[] nums = {1, 1, 2};
//        int i = solution.removeDuplicates(nums);
//        solution.removeElement(new int[]{3, 2, 2, 3}, 3);
//        int i = solution.searchInsert(new int[]{1, 3, 5, 6}, 5);
        i = solution.maxProfit(new int[]{7, 1, 5, 3, 6, 4});
        System.out.println(i);
    }


    //    查找表的两个常见实现， 哈希表、平衡二叉搜索数（可维护元素的顺序性）
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
        int slow = 0;
        for (int fast = 1; fast < nums.length; fast++) {
            if (nums[fast] != nums[slow]) {
                nums[++slow] = nums[fast];
            }
        }
        return slow + 1;
    }

    /**
     * https://leetcode.cn/problems/remove-element/?spm=5176.28103460.0.0.50c06308jxdXrh
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
     * https://leetcode.cn/problems/search-insert-position/?spm=5176.28103460.0.0.50c06308jxdXrh
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
     * https://leetcode.cn/problems/best-time-to-buy-and-sell-stock/?spm=5176.28103460.0.0.50c06308jxdXrh
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
     * https://leetcode.cn/problems/move-zeroes/description/?spm=5176.28103460.0.0.50c06308jxdXrh
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
     * https://leetcode.cn/problems/longest-substring-without-repeating-characters/?spm=5176.28103460.0.0.50c06308jxdXrh
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
     * https://leetcode.cn/problems/maximum-subarray/?spm=5176.28103460.0.0.50c06308jxdXrh
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
}