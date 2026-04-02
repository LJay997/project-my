package com.qq.ijay997;


import com.sun.jmx.remote.internal.ArrayQueue;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.swap;


/**
 * leetcode
 */
public class Solution {

    /**
     * <a href="https://leetcode.cn/problems/two-sum/">1. 两数之和</a>
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
     *
     * <a href="https://leetcode.cn/problems/remove-duplicates-from-sorted-array/solutions/728105/shan-chu-pai-xu-shu-zu-zhong-de-zhong-fu-tudo">删除排序数组中的重复项</a>
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
     * <a href="https://leetcode.cn/problems/remove-element">27. 移除元素</a>
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
     * <a href="https://leetcode.cn/problems/search-insert-position">35. 搜索插入位置</a>
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
     * <a href="https://leetcode.cn/problems/best-time-to-buy-and-sell-stock">121. 买卖股票的最佳时机</a>
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
     * <a href="https://leetcode.cn/problems/move-zeroes/description">283. 移动零</a>
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
     * <a href="https://leetcode.cn/problems/longest-substring-without-repeating-characters">3. 无重复字符的最长子串</a>
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
     * <a href="https://leetcode.cn/problems/maximum-subarray">53. 最大子数组和</a>
     * TODO 再次学习
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
     * <a href="https://leetcode.cn/problems/reverse-linked-list">206. 反转链表</a>
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
     * <a href="https://leetcode.cn/problems/merge-two-sorted-lists">21. 合并两个有序链表</a>
     * TODO 继续学习
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
     * <a href="https://leetcode.cn/problems/linked-list-cycle">141. 环形链表</a>
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
     * <a href="https://leetcode.cn/problems/remove-nth-node-from-end-of-list">19. 删除链表的倒数第 N 个结点</a>
     * TODO 优化优化
     *
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

    /**
     * <a href="https://leetcode.cn/problems/merge-k-sorted-lists">23. 合并K个升序链表i</a>
     *
     * @param lists
     * @return
     */
    public ListNode mergeKLists(ListNode[] lists) {
        if (lists == null || lists.length == 0) return null;

        PriorityQueue<ListNode> pq = new PriorityQueue<>(Comparator.comparingInt((ListNode o) -> o.val));
        // 将每个链表的头节点添加到优先队列中
        for (ListNode list : lists)
            for (; list != null; list = list.next) pq.add(list);

        ListNode dummy = new ListNode(-1);
        ListNode current = dummy;
        for (; !pq.isEmpty(); current = current.next) {
            ListNode node = pq.poll();
            current.next = node;
        }
        return dummy.next;
    }

    public ListNode mergeKLists1(ListNode[] lists) {
        if (lists == null || lists.length == 0) return null;

        ListNode dummy = new ListNode(-1);
        ListNode current = dummy;
        while (true) {
            ListNode minNode = null;
            int minIndex = -1;

            for (int i = 0; i < lists.length; i++) {
                if (lists[i] != null) {
                    if (minNode == null || lists[i].val < minNode.val) {
                        minNode = lists[i];
                        minIndex = i;
                    }
                }
            }
            if (minNode == null) break;
            current.next = minNode;
            current = current.next;
            lists[minIndex] = lists[minIndex].next;
        }
        return dummy.next;
    }

    /**
     * <a href="https://leetcode.cn/problems/binary-tree-level-order-traversal">102. 二叉树的层序遍历</a>
     *
     * @param root
     * @return
     */
    public List<List<Integer>> levelOrder(TreeNode root) {
        ArrayList<List<Integer>> result = new ArrayList<>();
        if (root == null) return result;

        TreeNode curNode = root;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(curNode);
        while (!queue.isEmpty()) {
            int size = queue.size();
            ArrayList<Integer> curLevel = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                curNode = queue.poll();
                curLevel.add(curNode.val);
                if (curNode.left != null)
                    queue.add(curNode.left);
                if (curNode.right != null)
                    queue.add(curNode.right);
            }
            result.add(curLevel);
        }
        return result;
    }

    /**
     * 104. 二叉树的最大深度
     * https://leetcode.cn/problems/maximum-depth-of-binary-tree
     *
     * @param root
     * @return
     */
    public int maxDepth(TreeNode root) {
        if (root == null) return 0;
        int leftDepth = maxDepth(root.left);
        int rightDepth = maxDepth(root.right);
        return Math.max(leftDepth, rightDepth) + 1;
    }

    /**
     *
     * <a href="https://leetcode.cn/problems/symmetric-tree">101. 对称二叉树</a>
     *
     * @param root
     * @return
     */
    public boolean isSymmetric(TreeNode root) {
        if (root == null) return true;

        return isMirror(root.left, root.right);
    }

    private boolean isMirror(TreeNode left, TreeNode right) {
        if (left == null && right == null) return true;

        if (left == null || right == null) return false;

        if (left.val != right.val) return false;

        return isMirror(left.left, right.right) && isMirror(left.right, right.left);
    }

    public boolean isSymmetric1(TreeNode root) {
        if (root == null) return true;

        LinkedList<TreeNode> queue = new LinkedList<>();
        queue.add(root.left);
        queue.add(root.right);
        while (!queue.isEmpty()) {
            TreeNode left = queue.poll();
            TreeNode right = queue.poll();
            if (left == null && right == null) continue;

            if (left == null || right == null) return false;

            if (left.val != right.val) return false;

            queue.add(left.left);
            queue.add(right.right);
            queue.add(left.right);
            queue.add(right.left);
        }
        return true;
    }

    /**
     * <a href="https://leetcode.cn/problems/invert-binary-tree">226. 翻转二叉树</a>
     *
     * @param root
     * @return
     */
    public TreeNode invertTree(TreeNode root) {
        if (root == null) return null;
        dfs(root);
        return root;
    }

    private void dfs(TreeNode node) {
        if (node == null) return;

        TreeNode temp = node.left;
        node.left = node.right;
        node.right = temp;
        dfs(node.left);
        dfs(node.right);
    }

    public TreeNode invertTree1(TreeNode root) {
        if (root == null) return null;

        Stack<TreeNode> stack = new Stack<>();
        stack.push(root);
        TreeNode node;
        while (!stack.isEmpty()) {
            node = stack.pop();
            if (node.left != null) stack.push(node.left);
            if (node.right != null) stack.push(node.right);
            TreeNode temp = node.left;
            node.left = node.right;
            node.right = temp;
        }

        return root;
    }

    /**
     * <a href="https://leetcode.cn/problems/kth-largest-element-in-an-array">215. 数组中的第K个最大元素</a>
     *
     * @param nums
     * @param k
     * @return
     */
    public int findKthLargest(int[] nums, int k) {
        if (nums == null || nums.length == 0 || k < 1) return -1;


        PriorityQueue<Integer> pq = new PriorityQueue<>(nums.length, Integer::compareTo);
        //region 有待优化
        /*
                Comparator<Integer> integerComparator = (o1, o2) -> -o1.compareTo(o2);
        for (int num : nums) pq.add(num);
        int result = 0;
        for (; k > 0 && !pq.isEmpty(); k--)
            result = pq.poll();
        return result; */
        //endregion

        for (int i = 0; i < nums.length; i++) {
            if (pq.size() < k) pq.add(nums[i]);
            else if (!pq.isEmpty() && pq.peek() < nums[i]) {
                pq.poll();
                pq.add(nums[i]);
            }
        }
        return pq.peek();
    }

    public int findKthLargest1(int[] nums, int k) {
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();

        for (int num : nums) {
            if (minHeap.size() < k) {
                minHeap.offer(num);
            } else if (!minHeap.isEmpty() && num > minHeap.peek()) {
                minHeap.poll();
                minHeap.offer(num);
            }
        }

        return minHeap.peek();
    }

    /**
     * 效率低
     * <a href="https://leetcode.cn/problems/top-k-frequent-elements">347. 前 K 个高频元素</a>
     *
     * @param nums
     * @param k
     * @return
     */
    public int[] topKFrequent(int[] nums, int k) {
        if (nums == null || nums.length == 0 || k < 1) return new int[0];

        // K: 元素, V: 元素出现的次数
        Map<Integer, Integer> map = new HashMap<>();
        for (int num : nums) {
            map.put(num, map.getOrDefault(num, 0) + 1);
        }

        return map.entrySet()
                .stream()
                // 按照频率排倒序
                .sorted(Map.Entry
                        .comparingByValue(Comparator.reverseOrder())
                )
                // 取前 K 个 map.K
                .limit(k)
                .mapToInt(Map.Entry::getKey)
                .toArray();
    }

    public int[] topKFrequent1(int[] nums, int k) {
        if (nums == null || nums.length == 0 || k < 1) return new int[0];

        int[] result = new int[k];
        // K: 元素, V: 元素出现的次数
        Map<Integer, Integer> map = new HashMap<>();
        for (int num : nums) {
            map.put(num, map.getOrDefault(num, 0) + 1);
        }

        // 逆频率队列, PriorityQueue 存放的元素排序算法需要依赖于 Map.V 频率 来进行排序
        PriorityQueue<Integer> queue = new PriorityQueue<>((o1, o2) -> -(map.get(o1) - map.get(o2)));
        // TODO 继续优化
        map.forEach((key, value) -> queue.add(key));
        for (int i = 0; i < k && !queue.isEmpty(); i++) {
            result[i] = queue.poll();
        }
        return result;
    }

    public int[] topKFrequent2(int[] nums, int k) {
        if (nums == null || nums.length == 0 || k < 1) return new int[0];

        int[] result = new int[k];
        // K: 元素, V: 元素出现的次数
        Map<Integer, Integer> map = new HashMap<>();
        for (int num : nums) {
            map.put(num, map.getOrDefault(num, 0) + 1);
        }
        // 维护一个最小堆
        PriorityQueue<Integer> queue = new PriorityQueue<>(k, Comparator.comparingInt(map::get));
        Iterator<Map.Entry<Integer, Integer>> mapIterator = map.entrySet().iterator();
        for (int i = 0; i < nums.length && mapIterator.hasNext(); i++) {
            Integer key = mapIterator.next().getKey();
            if (queue.size() < k) queue.add(key);
            else if (map.get(queue.peek()) < map.get(key)) {
                // 当前 元素的频率 与最小堆顶部元素的频率比较  频率高者入最小堆
                queue.poll();
                queue.add(key);
            }
        }
        for (int i = 0; i < k && !queue.isEmpty(); i++) {
            result[i] = queue.poll();
        }
        return result;
    }

    /**
     * <a href="https://leetcode.cn/problems/intersection-of-two-linked-lists">160. 相交链表</a>
     *
     * @param headA
     * @param headB
     * @return
     */
    public ListNode getIntersectionNode(ListNode headA, ListNode headB) {
        if (headA == null || headB == null) return null;

        HashSet<ListNode> set = new HashSet<>();
        while (headA != null) {
            set.add(headA);
            headA = headA.next;
        }

        for (; headB != null; headB = headB.next) {
            if (set.contains(headB)) return headB;
        }
        return null;
    }

    /**
     * 160. 相交链表-双指针
     *
     * @param headA
     * @param headB
     * @return
     */
    public ListNode getIntersectionNode1(ListNode headA, ListNode headB) {
        if (headA == null || headB == null) return null;

        ListNode pA = headA, pB = headB;
        while (pA != pB) {
            if (pA != null) pA = pA.next;
            else pA = headB;
            if (pB != null) pB = pB.next;
            else pB = headA;
        }
        return pA;
    }

    /**
     * <a href="https://leetcode.cn/problems/lowest-common-ancestor-of-a-binary-tree">236. 二叉树的最近公共祖先</a>
     *
     * @param root
     * @param p
     * @param q
     * @return
     */
    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        if (root == null || root == p || root == q) return root;

        TreeNode left = lowestCommonAncestor(root.left, p, q);
        TreeNode right = lowestCommonAncestor(root.right, p, q);
        if (left != null && right != null) return root;
        return left != null ? left : right;
    }

    /**
     * <a href="https://leetcode.cn/problems/palindrome-linked-list">234. 回文链表</a>
     *
     * @param head
     * @return
     */
    public boolean isPalindrome(ListNode head) {
        if (head == null) return false;

        LinkedList<Integer> list = new LinkedList<>();
        for (; head != null; head = head.next) {
            list.add(head.val);
        }
        while (list.size() > 1) {
            if (!Objects.equals(list.poll(), list.pollLast())) return false;
        }
        return true;
    }

    private ListNode frontPointer;

    public boolean isPalindrome1(ListNode head) {
        frontPointer = head;
        return recursivelyCheck(head);
    }

    /**
     * 递归检查 TODO 02-05 继续学习
     *
     * @param currentNode
     * @return
     */
    private boolean recursivelyCheck(ListNode currentNode) {
        if (currentNode == null) return true;

        // 递归检查后续节点
        if (!recursivelyCheck(currentNode.next)) return false;

        if (!Objects.equals(currentNode.val, frontPointer.val)) return false;

        // 移动 frontPointer 指针
        frontPointer = frontPointer.next;
        return true;
    }

    /**
     * <a href="https://leetcode.cn/problems/group-anagrams">49. 字母异位词分组</a>
     * 排序法
     *
     * @param strs
     * @return
     */
    public List<List<String>> groupAnagrams(String[] strs) {
        if (strs == null || strs.length == 0) return new ArrayList<>();

        Map<String, List<String>> map = new HashMap<>();
        char[] charArray;
        for (String str : strs) {
            charArray = str.toCharArray();
            Arrays.sort(charArray);
            String key = new String(charArray);
            map.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(str);
        }
        return new ArrayList<>(map.values());
    }

    /**
     * <a href="https://leetcode.cn/problems/group-anagrams">49. 字母异位词分组</a>
     * 计数法
     *
     * @param strs
     * @return
     */
    public List<List<String>> groupAnagrams1(String[] strs) {
        if (strs == null || strs.length == 0) return new ArrayList<>();

        HashMap<String, List<String>> map = new HashMap<>();
        for (String str : strs) {
            // 统计每个字符出现的次数
            int[] count = new int[26];
            for (char c : str.toCharArray()) {
                count[c - 'a']++;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 26; i++) {
                sb.append(count[i]);
            }
            String key = sb.toString();
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(str);
        }
        return new ArrayList<>(map.values());
    }

    /**
     * <a href="https://leetcode.cn/problems/longest-consecutive-sequence">128. 最长连续序列</a>
     *
     * @param nums
     * @return
     */
    public int longestConsecutive(int[] nums) {
        if (nums == null || nums.length == 0) return 0;

        int maxLen = 1, curLength = 1;
        Arrays.sort(nums);
        for (int i = 1; i < nums.length; i++) {
            if (nums[i] == nums[i - 1] + 1) {
                curLength++;
                maxLen = Math.max(maxLen, curLength);
            } else {
                curLength = 1;
            }
        }
        return maxLen;
    }

    public void moveZeroes1(int[] nums) {
        if (nums == null || nums.length == 0) return;

        int slow = 0, fast = 1;
        for (; fast < nums.length; fast++) {
            if (nums[slow] != 0) slow++;

            if (nums[slow] == 0 && nums[fast] != 0) {
                nums[slow] = nums[fast];
                nums[fast] = 0;
                slow++;
            }
        }
    }

    /**
     * <a href="https://leetcode.cn/problems/container-with-most-water">11. 盛最多水的容器</a>
     * 双指针
     *
     * @param height
     * @return
     */
    public int maxArea(int[] height) {
        if (height == null || height.length == 0) return 0;

        int left = 0, right = height.length - 1, maxArea = 0, curArea = 0;
        while (left < right) {
            curArea = Math.min(height[left], height[right]) * (right - left);
            maxArea = Math.max(maxArea, curArea);
            if (height[left] < height[right]) left++;
            else right--;
        }
        return maxArea;
    }

    /**
     * <a href="https://leetcode.cn/problems/3sum">15. 三数之和</a>
     *
     * @param nums
     * @return
     */
    public List<List<Integer>> threeSum(int[] nums) {
        if (nums == null || nums.length < 3) return new ArrayList<>();

        List<List<Integer>> res = new ArrayList<>();
        HashMap<Integer, Integer> map = new HashMap<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            map.put(nums[i], 0);
        }
        // TODO
        return res;
    }

    public int longestPalindrome(String s) {
        if (s == null || s.length() == 0) return 0;
        HashMap<Character /*字符*/, Integer /*出现的次数*/> map = new HashMap<>();
        for (char c : s.toCharArray()) {
            map.put(c, map.getOrDefault(c, 0) + 1);
        }
        int length = 0;
        for (Map.Entry<Character, Integer> entry : map.entrySet()) {
            if (entry.getValue() % 2 == 0) length += entry.getValue();
            else length += entry.getValue() - 1;
        }
        return length == s.length() ? length : length + 1;
    }

    public boolean isSubsequence(String s, String t) {
        if (s == null || s.length() == 0) return false;

        char[] charArray = t.toCharArray();
        int sIndex = 0;
        for (int tIndex = 0; tIndex < t.length() && sIndex < s.length(); tIndex++) {
            if (Objects.equals(charArray[tIndex], s.charAt(sIndex))) {
                sIndex++;
            }
        }
        return sIndex == s.length();
    }

    public ListNode middleNode(ListNode head) {
        if (head == null) return null;
        ArrayList<ListNode> nodes = new ArrayList<>();
        ListNode curNode = head;
        for (; curNode != null; curNode = curNode.next) {
            nodes.add(curNode);
        }
        // 取中间节点
        int midIndex = nodes.size() / 2;
//        if (nodes.size() % 2 == 0) midIndex += 1;
        return nodes.get(midIndex);
    }

    public static void main(String[] args) {
        Solution solution = new Solution();
        ListNode linkedList = createLinkedList(new int[]{1, 2, 3, 4, 5});
        ListNode listNode = solution.middleNode(linkedList);
        System.out.println(listNode.val);
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

    public boolean rotateString(String s, String goal) {
        if (s == null || goal == null || s.length() != goal.length()) return false;

        return s.concat(s).contains(goal);
    }

    /**
     * 判断字符串 s 是否可以通过旋转操作变成 goal
     * 旋转操作：将字符串的某个字符移动到开头
     * 例如：s = "abcde", goal = "cdeab" -> true (将 'c' 移到开头)
     *
     * @param s    原始字符串
     * @param goal 目标字符串
     * @return 如果 s 可以旋转得到 goal 则返回 true，否则返回 false
     */
    public boolean rotateString1(String s, String goal) {
        // 边界条件检查：null 值或长度不相等直接返回 false
        if (s == null || goal == null || s.length() != goal.length()) return false;

        int n = s.length();
        for (int i = 0; i < n; i++) {
            boolean flag = true;
            for (int j = 0; j < n; j++) {
                if (goal.charAt(j) != s.charAt((j + i) % n)) {
                    flag = false;
                    break;
                }
            }
            if (flag) return true;
        }
        return false;
    }

    public boolean rotateString3(String A, String B) {
        if (A.equals(B)) {
            return true;
        }
        int n = A.length();
        for (int i = 0; i < n; i++) {
            A = rotate(A);
            if (A.equals(B)) {
                return true;
            }
        }
        return false;
    }

    private String rotate(String A) {
        int n = A.length();
        char[] arr = A.toCharArray();
        char[] arr2 = new char[n];
        int index = 0;
        for (int i = 1; i < n; i++) {
            arr2[index++] = arr[i];
        }
        arr2[index] = arr[0];
        return new String(arr2);
    }

    /**
     * 验证栈序列
     * 给定 pushed 和 popped 两个序列，判断它们是否可以是同一个栈的压入和弹出序列
     * <p>
     * 解题思路：使用辅助栈模拟压栈和弹栈过程
     * 1. 遍历 pushed 数组，将元素依次压入栈
     * 2. 每次压入后，检查栈顶元素是否等于 popped[j]
     * 3. 如果相等就弹出，并移动 popped 的指针
     * 4. 最后检查栈是否为空
     *
     * @param pushed 压入序列
     * @param popped 弹出序列
     * @return 如果是有效的栈序列返回 true，否则返回 false
     */
    public boolean validateStackSequences(int[] pushed, int[] popped) {
        if (pushed == null || popped == null) return false;

        int length = pushed.length;
        if (length != popped.length) return false;

        Stack<Integer> stack = new Stack<>();
        for (int i = 0, j = 0; i < length; i++) {
            stack.push(pushed[i]);
            while (!stack.isEmpty() && j < length
                    && stack.peek() == popped[j]) {
                j++;
                stack.pop();
            }
        }
        return stack.isEmpty();
    }

    /**
     * 青蛙跳台阶问题
     * <p>
     * 题目描述：
     * 一只青蛙一次可以跳上 1 级台阶，也可以跳上 2 级台阶。
     * 求该青蛙跳上一个 n 级的台阶总共有多少种跳法？
     * <p>
     * 示例：
     * 输入：n = 2
     * 输出：2
     * 解释：有两种方法可以跳到第 2 级台阶
     * 方法 1：每次跳 1 级，共跳 2 次 (1+1)
     * 方法 2：一次跳 2 级，共跳 1 次 (2)
     * <p>
     * 输入：n = 3
     * 输出：3
     * 解释：有三种方法可以跳到第 3 级台阶
     * 方法 1: 1 + 1 + 1
     * 方法 2: 1 + 2
     * 方法 3: 2 + 1
     * <p>
     * 提示：
     * - 0 <= n <= 100
     * - 答案需要取模 1e9+7（1000000007），如计算初始结果为：1000000008，请返回 1
     * <p>
     * 解题思路：
     * 这是一个典型的动态规划问题，类似于斐波那契数列
     * - 当 n=0 时，有 1 种跳法（不跳）
     * - 当 n=1 时，有 1 种跳法（跳 1 级）
     * - 当 n=2 时，有 2 种跳法（1+1 或 2）
     * - 当 n>2 时，f(n) = f(n-1) + f(n-2)
     * 因为最后一步可以是跳 1 级或跳 2 级
     *
     * @param n 台阶数
     * @return 跳法总数
     */
    public int climbStairs(int n) {
        // TODO: 请在这里实现你的代码
        if (n == 0) return 1;
        if (n == 1) return 1;
        return climbStairs(n - 1) + climbStairs(n - 2);
    }


    /**
     * 回文排列
     * <p>
     * 题目描述：
     * 给定一个字符串，编写一个函数判定其是否为某个回文串的排列之一。
     * 回文串是指正反两个方向都一样的字符串，排列是指字母可以重新排列。
     * <p>
     * 示例：
     * 输入：s = "code"
     * 输出：false
     * 解释：无法排列成回文串
     * <p>
     * 输入：s = "carerac"
     * 输出：true
     * 解释：可以排列成回文串 "racecar" 或 "carrac" 等
     * <p>
     * 输入：s = "aabbccdd"
     * 输出：true
     * 解释：可以排列成回文串 "abcdcba" 或 "abddcba" 等
     * <p>
     * 输入：s = "aabbcccddd"
     * 输出：false
     * 解释：无法排列成回文串
     * <p>
     * 提示：
     * - 字符串只包含英文字母（大小写敏感）
     * - 字符串长度范围：0 <= s.length() <= 1000
     * <p>
     * 解题思路提示：
     * 回文串的特点是：最多只能有一个字符出现奇数次
     * - 如果字符串长度是偶数，所有字符都必须出现偶数次
     * - 如果字符串长度是奇数，有且仅有一个字符可以出现奇数次
     *
     * @param s 输入字符串
     * @return 如果可以排列成回文串返回 true，否则返回 false
     */
    public boolean canPermutePalindrome(String s) {
        if (s == null || s.length() == 0) return false;

        int length = s.length();
        int[] ints = new int[26];
        for (int i = 0; i < length; i++) {
            ints[s.charAt(i) - 'a']++;
        }

        if (length % 2 == 0) {
            for (int anInt : ints) {
                if (anInt % 2 != 0) return false;
            }
        }
        int count = 0;
        for (int anInt : ints) {
            if (count > 1) return false;
            if (anInt % 2 == 1) count++;
        }
        return true;
    }

    /**
     * <a href="https://leetcode-cn.com/problems/zigzag-conversion/">6. Z 字形变换</a>
     *
     * @param s
     * @param numRows
     * @return
     */
    public String convert(String s, int numRows) {
        if (numRows == 1 || s == null) return s;
        return "";
    }

    /**
     * <a href="https://leetcode-cn.com/problems/search-in-rotated-sorted-array/">33. 搜索旋转排序数组</a>
     *
     * @param nums
     * @param target
     * @return
     */
    public int search(int[] nums, int target) {
        if (nums == null || nums.length == 0) return -1;

        int left = 0, right = nums.length - 1, mid;
        for (; left <= right; ) {
            mid = (left + right) / 2;
            if (nums[mid] == target) return mid;
            if (nums[mid] > target) {
                right = mid - 1;
            } else if (nums[mid] < target) {
                left = mid + 1;
            }
        }
        return -1;
    }

    public int firstBadVersion(int n) {
        if (n <= 0) return -1;
        int left = 1, right = n, mid;
        for (; left <= right; ) {
            mid = (right - left) / 2 + left;
            if (!this.isBadVersion(mid) && this.isBadVersion(mid + 1)) {
                return mid + 1;
            }
            if (!this.isBadVersion(mid)) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return 1;
    }

    public boolean isBadVersion(int version) {
        return true;
    }

    public void selectSort(int[] arr) {
        if (arr == null || arr.length <= 1) return;

        for (int i = 0; i < arr.length - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < arr.length; j++) {
                if (arr[j] < arr[minIndex]) {
                    minIndex = j;
                }
            }
            swap(arr, i, minIndex);
        }
    }

    private void swap(int[] arr, int i, int j) {
        if (i == j) return;
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    public void sort2(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        for (int i = 0; i < arr.length - 1; i++) {
            boolean swapped = false;  // 标记本轮是否发生交换
            for (int j = 0; j < arr.length - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    swap(arr, j, j + 1);
                    swapped = true;
                }
            }
            // 如果本轮没有交换，说明已经有序，提前结束
            if (!swapped) {
                break;
            }
        }
    }

    public void quickSort(int[] arr) {
        if (arr == null || arr.length <= 1) return;

        extracted(arr, 0, arr.length - 1);
    }

    private void extracted(int[] arr, int left, int right) {
        if (left >= right) return;

        int tmpLeft = left, tmpRight = right, pivot = arr[tmpLeft];
        for (; tmpLeft <= tmpRight; ) {
            for (; tmpLeft <= tmpRight; ) {
                if (arr[tmpRight] >= pivot) {
                    tmpRight--;
                } else {
                    swap(arr, tmpLeft, tmpRight);
                    break;
                }
            }
            for (; tmpLeft <= tmpRight; ) {
                if (arr[tmpLeft] <= pivot) {
                    tmpLeft++;
                } else {
                    swap(arr, tmpLeft, tmpRight);
                    break;
                }
            }
        }

        // 结束的时候 tmpLeft = tmpRight
        extracted(arr, left, tmpLeft - 1);

        extracted(arr, tmpLeft + 1, right);
    }

    public boolean exist(char[][] board, String word) {
        if (board == null || board.length == 0) return false;

        int rows = board.length;
        int cols = board[0].length;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] == word.charAt(0)) {
                    if (dfs(board, word, i, j, 0)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean dfs(char[][] board, String word, int row, int col, int index) {
        if (index == word.length()) return true;

        if (row < 0 || row >= board.length
                || col < 0 || col >= board[0].length
                || board[row][col] != word.charAt(index)) {
            return false;
        }

        // 标记当前位置已访问（用特殊字符标记）
        char temp = board[row][col];
        board[row][col] = '#';

        // 向四个方向继续搜索
        boolean found = dfs(board, word, row - 1, col, index + 1) ||  // 上
                dfs(board, word, row + 1, col, index + 1) ||  // 下
                dfs(board, word, row, col - 1, index + 1) ||  // 左
                dfs(board, word, row, col + 1, index + 1);    // 右

        // 回溯：恢复当前位置
        board[row][col] = temp;

        return found;
    }

    /* 前序遍历 */
    void preOrder(TreeNode root) {
        if (root == null) return;
        preOrder(root.left);
        System.out.println(root.val);
        preOrder(root.right);
    }

    /* 插入节点 */
    void insert(int num, TreeNode root) {
        if (root == null) {
            root = new TreeNode(num);
            return;
        }
        TreeNode cur = root, pre = null;
        for (; cur != null; ) {
            pre = cur;
            if (num < cur.val) {
                cur = cur.left;
            } else if (num > cur.val) {
                cur = cur.right;
            } else {
                // 数据相同 无需插入
                return;
            }
        }
        cur = new TreeNode(num);
        if (num < pre.val) {
            pre.left = cur;
        } else {
            pre.right = cur;
        }
    }

    /* 二分查找最左一个 target */
    int binarySearchLeftEdge(int[] nums, int target) {
        if (nums == null || nums.length == 0) return -1;
        int left = 0, right = nums.length - 1, mid;

        // 标准二分查找，找到最左边的 target
        for (; left <= right; ) {
            mid = (right - left) / 2 + left;
            if (nums[mid] < target) {
                left = mid + 1;      // target 在右半部分
            } else if (nums[mid] > target) {
                right = mid - 1;     // target 在左半部分
            } else {
                // nums[mid] == target，继续在左半部分查找
                right = mid - 1;
            }
        }

        // 循环结束后，left 指向第一个 >= target 的位置
        // 需要验证 left 是否越界以及是否等于 target
        if (left >= nums.length || nums[left] != target) {
            return -1;
        }

        return left;
    }

    int[] twoSumBruteForce(int[] nums, int target) {
        if (nums == null || nums.length == 0) return nums;

        int length = nums.length;
        for (int i = 0; i < length; i++) {
            for (int j = i + 1; j < length; j++) {
                if (nums[i] + nums[j] == target) return new int[]{i, j};
            }
        }
        return null;
    }

    /* 方法二：辅助哈希表 */
    int[] twoSumHashTable(int[] nums, int target) {
        if (nums == null || nums.length == 0) return nums;
        int[] result = new int[2];
        HashMap<Integer /* nums 元素的值 */ , Integer /* nums 元素的下标 */> map = new HashMap<>();
        map.put(nums[0], 0);
        for (int i = 1; i < nums.length; i++) {
            if (map.containsKey(target - nums[i])) {
                result[0] = map.get(target - nums[i]);
                result[1] = i;
            }
        }

        return result;
    }

    void selectionSort(int[] nums) {
        if (nums == null || nums.length <= 1) return;
        int min;
        for (int i = 0; i < nums.length; i++) {
            min = i;
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[j] < nums[min]) min = j;
            }
            swap(nums, i, min);
        }
    }

    void bubbleSort(int[] nums) {
        if (nums == null || nums.length <= 1) return;

        boolean flag;
        for (int i = 0; i < nums.length - 1; i++) {
            flag = true;
            for (int j = 0; j < nums.length - 1 - i; j++) {
                if (nums[j] > nums[j + 1]) {
                    swap(nums, j, j + 1);
                    flag = false;
                }
            }
            if (flag) break;
        }
    }

    void quickSort2(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        quickSortHelp(arr, 0, arr.length - 1);
    }

    private void quickSortHelp(int[] arr, int left, int right) {
        if (left >= right) return;
        int pivot = part(arr, left, right);
        quickSortHelp(arr, left, pivot - 1);
        quickSortHelp(arr, pivot + 1, right);
    }

    private int part(int[] arr, int left, int right) {
        int i = left, j = right;
        double v = Math.random() * (right - left + 1) + left;
        swap(arr, left, (int) v);
        // 选择 arr[left] 作为基准点
        for (; i < j; ) {
            while (i < j && arr[j] >= arr[left])
                j--;
            while (i < j && arr[i] <= arr[left])
                i++;
            swap(arr, i, j);
        }
        swap(arr, left, i);
        return i;
    }

    /* 构建二叉树 */
    TreeNode buildTree(int[] preorder, int[] inorder) {
        if (preorder == null || preorder.length == 0) return null;

        HashMap<Integer /*元素*/, Integer/*索引*/> inorderMap = new HashMap<>();
        int inorderEndIndex = inorder.length - 1;
        for (int i = 0; i <= inorderEndIndex; i++) {
            inorderMap.put(inorder[i], i);
        }
        return buildTreeHelp(preorder, inorderMap, 0, 0, inorderEndIndex);
    }

    private TreeNode buildTreeHelp(int[] preorder, HashMap<Integer, Integer> inorderMap, int preorderStartIndex, int inorderStartIndex, int inorderEndIndex) {
        if (inorderStartIndex > inorderEndIndex) return null;

        TreeNode node = new TreeNode(preorder[preorderStartIndex]);
        Integer nodeInOrderIndex = inorderMap.get(preorder[preorderStartIndex]);
        node.left = buildTreeHelp(preorder, inorderMap, preorderStartIndex + 1, inorderStartIndex, nodeInOrderIndex - 1);
        // nodeInOrderIndex - inorderStartIndex 是 左子树的 个数
        node.right = buildTreeHelp(preorder, inorderMap, preorderStartIndex + 1 + nodeInOrderIndex - inorderStartIndex, nodeInOrderIndex + 1, inorderEndIndex);
        return node;
    }

    /* 求解汉诺塔问题 */
    void solveHanota(List<Integer> A, List<Integer> B, List<Integer> C) {
        int n = A.size();
        // 将 A 顶部 n 个圆盘借助 B 移到 C
        dfs(n, A, B, C);
    }

    /* 求解汉诺塔问题 f(i) */
    private void dfs(int i, List<Integer> src, List<Integer> buf, List<Integer> tar) {
        // 若 src 只剩下一个圆盘，则直接将其移到 tar
        if (i == 1) {
            move(src, tar);
            return;
        }
        // 子问题 , 将 n-1 个圆盘移动到 buf
        dfs(i - 1, src, tar, buf);
        // 移动最后一个盘
        move(src, tar);
        // 将 n-1 个圆盘移动到 tar
        dfs(i - 1, buf, src, tar);
    }

    /* 移动一个圆盘 */
    void move(List<Integer> src, List<Integer> tar) {
        // 从 src 顶部拿出一个圆盘
        Integer pan = src.remove(src.size() - 1);
        // 将圆盘放入 tar 顶部
        tar.add(pan);
    }

    public TreeNode lowestCommonAncestor1(TreeNode root, TreeNode p, TreeNode q) {
        if (root == null || root == p || root == q) return root;
        TreeNode left = lowestCommonAncestor1(root.left, p, q);
        TreeNode right = lowestCommonAncestor1(root.right, p, q);
        if (left != null && right != null) return root;
        return left != null ? left : right;
    }

    public TreeNode buildTree1(int[] preorder, int[] inorder) {
        if (preorder == null || preorder.length == 0) return null;
        int inorderLength = inorder.length;
        HashMap<Integer /*元素*/, Integer /*索引*/> inorderMap = new HashMap<>(inorderLength);
        for (int i = 0; i < inorderLength; i++) {
            inorderMap.put(inorder[i], i);
        }

        return buildTreeHelp1(preorder, inorderMap, 0, 0, inorderLength - 1);
    }

    private TreeNode buildTreeHelp1(int[] preorder, HashMap<Integer, Integer> inorderMap, int preorderStartIndex, int inorderStartIndex, int inorderEndIndex) {
        if (inorderStartIndex > inorderEndIndex) return null;
        TreeNode node = new TreeNode(preorder[preorderStartIndex]);
        Integer midIndex = inorderMap.get(preorder[preorderStartIndex]);
        node.left = buildTreeHelp1(preorder, inorderMap, preorderStartIndex + 1, inorderStartIndex, midIndex - 1);
        node.right = buildTreeHelp1(preorder, inorderMap, preorderStartIndex + 1 + midIndex - inorderStartIndex, midIndex + 1, inorderEndIndex);
        return node;
    }

    /**
     * 在二叉树中搜索所有值为 target 的节点，返回根节点到这些节点的路径
     *
     * @param root   二叉树的根节点
     * @param target 目标值
     * @return 所有从根节点到值为 target 的节点的路径列表
     */
    public List<List<Integer>> findPathToTarget(TreeNode root, int target) {
        if (root == null) return null;

        List<List<Integer>> result = new ArrayList<>();

        dfs(root, target, new ArrayList<>(), result);
        return result;
    }

    private void dfs(TreeNode node, int target, List<Integer> currentPath, List<List<Integer>> result) {
        if (node == null) return;
        currentPath.add(node.val);
        if (node.val == target) {
            result.add(new ArrayList<>(currentPath));
        }

        //     遍历所有选择
        dfs(node.left, target, currentPath, result);
        dfs(node.right, target, currentPath, result);
        // 状态回退
        currentPath.remove(currentPath.size() - 1);
    }

    // 全排列
    public List<List<Integer>> permute(int[] nums) {
        if (nums == null || nums.length == 0) return null;

        List<List<Integer>> result = new ArrayList<>();
        permuteHelp(nums, result, new ArrayDeque<Integer>(), new boolean[nums.length]);
        return result;
    }

    private void permuteHelp(int[] nums, List<List<Integer>> result, ArrayDeque<Integer> path, boolean[] booleans) {
        if (path.size() == nums.length) {
            result.add(new ArrayList<>(path));
            return;
        }

        for (int i = 0; i < nums.length; i++) {
            if (booleans[i]) continue;

            booleans[i] = true;
            path.add(nums[i]);

            permuteHelp(nums, result, path, booleans);

            booleans[i] = false;
            path.removeLast();
        }
    }

    /**
     * 路径总和 II
     * @param root
     * @param targetSum
     * @return
     */
    public List<List<Integer>> pathSum(TreeNode root, int targetSum) {
        if (root == null) return null;
        List<List<Integer>> result = new ArrayList<>();

        pathSumHelp(root,targetSum, result,new ArrayDeque<Integer>(),0);
        return result;
    }

    private void pathSumHelp(TreeNode node, int targetSum, List<List<Integer>> result, ArrayDeque<Integer> path, int curSum) {
        if (node == null) return;
    }
}