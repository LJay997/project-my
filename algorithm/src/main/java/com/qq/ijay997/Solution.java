package com.qq.ijay997;


import com.sun.jmx.remote.internal.ArrayQueue;

import java.util.*;

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

        Arrays.sort(nums);
        // 要保证 i、left、right 不重复 所以循环  nums.length - 2 次
        for (int i = 0; i < nums.length - 2; i++) {
            //  i = 0 的时候 没有前一个元素
            if (i != 0 && nums[i] == nums[i - 1]) continue;

            // 如果当前数字大于0，则三数之和不可能为0（因为数组已排序）
            if (nums[i] > 0) break;

            int left = i + 1, right = nums.length - 1;
            while (left < right) {
                int sum = nums[i] + nums[left] + nums[right];
                if (sum == 0) {
                    res.add(Arrays.asList(nums[i], nums[left], nums[right]));

                    // 跳过重复元素
                    while (left < right && nums[left] == nums[left + 1]) left++;
                    while (left < right && nums[right] == nums[right - 1]) right--;
                    left++;
                    right--;
                } else if (sum < 0) {
                    // 如果和小于0，则左指针向右移动
                    left++;
                } else {
                    // 如果和大于0，则右指针向左移动
                    right--;
                }
            }
        }
        return res;
    }

    /**
     * <a href="https://leetcode.cn/problems/trapping-rain-water">42. 接雨水</a>
     *
     * @param height
     * @return
     */
    public int trap(int[] height) {
        if (height == null || height.length < 3) return 0;

        int result = 0, left = 0, right = height.length - 1, leftMax = 0, rightMax = 0;
        while (left < right) {
            // 维护左右两侧的最大高度
            leftMax = Math.max(leftMax, height[left]);
            rightMax = Math.max(rightMax, height[right]);

            // 贪心策略：总是处理较小的一侧
            if (height[left] < height[right]) {
                result += leftMax - height[left];  // 左侧水量确定
                left++;
            } else {
                result += rightMax - height[right]; // 右侧水量确定
                right--;
            }
        }
        return result;
    }

    /**
     * <a href="https://leetcode.cn/problems/longest-substring-without-repeating-characters">3. 无重复字符的最长子串</a>
     *
     * @param s
     * @return
     */
    public int lengthOfLongestSubstring1(String s) {
        if (s == null || s.isEmpty()) return 0;

        int result = 0, slow = 0;
        // 存放指针与字符的索引
        HashMap<Character, Integer> map = new HashMap<>();
        for (int fast = 0; fast < s.length(); fast++) {
            char curChar = s.charAt(fast);
            if (map.containsKey(curChar)) {
                slow = Math.max(slow, map.get(curChar) + 1);
            }
            map.put(curChar, fast);
            result = Math.max(result, fast - slow + 1);
        }
        return result;
    }

    /**
     * <a href="https://leetcode.cn/problems/find-all-anagrams-in-a-string">438. 找到字符串中所有字母异位词</a>
     *
     * @param s
     * @param p
     * @return
     */
    public List<Integer> findAnagrams(String s, String p) {
        if (s == null || p == null || s.length() < p.length()) return new ArrayList<>();

        List<Integer> res = new ArrayList<>();
        int pLen = p.length();
        int sLen = s.length();

        // 使用数组统计字符频次（假设只有小写字母）
        int[] pCount = new int[26];
        int[] windowCount = new int[26];

        // 统计模式字符串p的字符频次
        for (char c : p.toCharArray()) {
            pCount[c - 'a']++;
        }

        // 当长度超过的时候移动左边指针
        for (int i = 0; i < sLen; i++) {
            windowCount[s.charAt(i) - 'a']++;
            if (i >= pLen) {
                windowCount[s.charAt(i - pLen) - 'a']--;
            }
            // 当长度符合要求时，判断频次是否相等
            if (i >= pLen - 1) {
                if (Arrays.equals(pCount, windowCount)) {
                    res.add(i - pLen + 1);
                }
            }
        }

        return res;
    }

    public List<Integer> findAnagrams1(String s, String p) {
        List<Integer> res = new ArrayList<>();
        if (s == null || p == null || s.length() < p.length()) return res;

        int pLength = p.length();
        ArrayList<Character> list = new ArrayList<>(pLength);
        for (char c : p.toCharArray()) {
            list.add(c);
        }

        ArrayDeque<Character> deque = new ArrayDeque<>(pLength);
        for (int i = 0; i < s.length(); i++) {
            deque.add(s.charAt(i));
            if (deque.size() == pLength) {
                if (list.containsAll(deque)) {
                    res.add(i - pLength + 1);
                }
                deque.removeFirst();
            }
        }
        return res;
    }

    public int[] twoSum1(int[] nums, int target) {
        if (nums == null || nums.length < 2) return new int[0];

        int[] result = new int[2];
        // 值, 下标
        HashMap<Integer, Integer> map = new HashMap<>();

        int complement;
        for (int i = 0; i < nums.length; i++) {

            complement = target - nums[i];
            if (map.containsValue(complement) && map.get(complement) != i) {
                result[0] = map.get(complement);
                result[1] = i;
                break;
            }
            map.put(i, nums[i]);
        }
        return result;
    }

    public boolean hasCycle3(ListNode head) {
        if (head == null || head.next == null) return false;

        ListNode slow = head;
        ListNode fast = head.next;
        while (fast != null && fast.next != null) {
            if (slow == fast) return true;
            fast = fast.next.next;
            slow = slow.next;
        }
        return false;
    }

    public ListNode removeNthFromEnd1(ListNode head, int n) {
        if (head == null || head.next == null) return null;

        ListNode dummy = new ListNode(0, head);
        ListNode slow = dummy, fast = dummy;
        for (int i = 0; i < n + 1; i++) {
            fast = fast.next;
        }
        for (; fast != null; ) {
            fast = fast.next;
            slow = slow.next;
        }
        slow.next = slow.next.next;
        return dummy.next;
    }

    public ListNode getIntersectionNode2(ListNode headA, ListNode headB) {
        if (headA == null || headB == null) return null;

        ListNode index1 = headA, index2 = headB;
        while (index1 != index2) {
            index1 = index1 == null ? headB : index1.next;
            index2 = index2 == null ? headA : index2.next;
        }
        return index1;
    }

    public int maxDepth1(TreeNode root) {
        if (root == null) return 0;
        int leftDepth = maxDepth1(root.left);
        int rightDepth = maxDepth1(root.right);
        return Math.max(leftDepth, rightDepth) + 1;
    }

    public int maxDepth2(TreeNode root) {
        if (root == null) return 0;

        ArrayDeque<TreeNode> deque = new ArrayDeque<>();
        deque.add(root);
        int depth = 0;
        while (!deque.isEmpty()) {
            int size = deque.size();
            for (int i = 0; i < size; i++) {
                TreeNode node = deque.poll();
                if (node.left != null) {
                    deque.add(node.left);
                }
                if (node.right != null) {
                    deque.add(node.right);
                }
            }
            depth++;
        }
        return depth;
    }

    public int climbStairs(int n) {
        if (n <= 2) return n;
        int[] dp = new int[n + 1];
        dp[1] = 1;
        dp[2] = 2;
        for (int i = 3; i <= n; i++) {
            dp[i] = dp[i - 1] + dp[i - 2];
        }
        return dp[n];
    }

    public void deleteNode(ListNode node) {
        if (node == null) return;
        node.val = node.next.val;
        node.next = node.next.next;
    }

    /**
     * <a href="https://leetcode.cn/problems/copy-list-with-random-pointer">138. 复制带随机指针的链表</a>
     *
     * @param head
     * @return
     */
    public Node copyRandomList(Node head) {
        if (head == null) return null;

        Node cur = head;
        HashMap<Node /*旧链表*/, Node /*新链表*/> map = new HashMap<>();

        // 创建节点
        for (; cur != null; cur = cur.next) {
            map.put(cur, new Node(cur.val));
        }
        // 处理新链表的属性 next、 random
        for (cur = head; cur != null; cur = cur.next) {
            map.get(cur).next = map.get(cur.next);
            map.get(cur).random = map.get(cur.random);
        }
        return map.get(head);
    }

    public boolean isValid(String s) {
        if (s == null || s.isEmpty() || s.length() % 2 != 0) return false;

        char curChar;
        ArrayDeque<Character> stack = new ArrayDeque<>();
        for (int i = 0; i < s.length(); i++) {
            curChar = s.charAt(i);
            if (curChar == '('
                    || curChar == '['
                    || curChar == '{') {
                stack.add(curChar);
                continue;
            }

            if (stack.isEmpty()) return false;
            // 栈顶元素
            Character lastQu = stack.peekLast();
            if ((curChar == ')' && lastQu == '(')
                    || (curChar == ']' && lastQu == '[')
                    || (curChar == '}' && lastQu == '{')) {
                stack.pollLast();
                continue;
            }
            return false;
        }
        return stack.isEmpty();
    }

    public ListNode partition(ListNode head, int x) {
        ListNode smallNode = new ListNode(0), largeNode = new ListNode(0);
        ListNode smallIndex = smallNode, largeIndex = largeNode, curIndex = head;

        for (; curIndex != null; curIndex = curIndex.next) {
            if (curIndex.val < x) {
                smallIndex.next = curIndex;
                smallIndex = smallIndex.next;
            } else {
                largeIndex.next = curIndex;
                largeIndex = largeIndex.next;
            }
        }

        smallIndex.next = largeNode.next;
        largeIndex.next = null;

        return smallNode.next;
    }

    public static void main(String[] args) {
        System.out.println(']' - '0');
        //a 49, [ 43, ] 45
    }

    public String decodeString(String s) {
        if (s == null || s.isEmpty()) return "";

        Stack<Integer> numStack = new Stack<>();
        Stack<StringBuilder> strStack = new Stack<>();

        StringBuilder curStr = new StringBuilder();
        int num = 0;
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                num = num * 10 + c - '0';
            } else if (c == '[') {
                numStack.add(num);
                strStack.add(curStr);
                num = 0;
                curStr = new StringBuilder();
            } else if (c == ']') {
                StringBuilder tmp = strStack.pop();
                int repeat = numStack.pop();
                for (int i = 0; i < repeat; i++) {
                    tmp.append(curStr);
                }
                curStr = tmp;
            } else {
                curStr.append(c);
            }
        }
        return curStr.toString();
    }

    public boolean isAnagram(String s, String t) {
        if (s == null || t == null || s.length() != t.length()) return false;

        HashMap<Character, Integer> map = new HashMap<>();
        for (int i = 0; i < s.length(); i++) {
            char key = s.charAt(i);
            char key1 = t.charAt(i);
            if (Objects.equals(key, key1)) continue;
            map.put(key, map.getOrDefault(key, 0) + 1);
            map.put(key1, map.getOrDefault(key1, 0) - 1);
        }
        for (Map.Entry<Character, Integer> entry : map.entrySet()) {
            if (entry.getValue() != 0) return false;
        }
        return true;
    }

    public int firstUniqChar(String s) {
        if (s == null || s.isEmpty()) return -1;
        int[] chars = new int[26];
        char[] charArray = s.toCharArray();
        for (char c : charArray) {
            chars[c - 'a']++;
        }

        for (int i = 0; i < s.length(); i++) {
            if (chars[s.charAt(i) - 'a'] == 1) return i;
        }
        return -1;
    }

    public boolean isIsomorphic(String s, String t) {
        if (s == null || t == null || s.length() != t.length()) return false;

        HashMap<Character, Character> map = new HashMap<>();
        int length = s.length();
        char sC, tC;
        for (int i = 0; i < length; i++) {
            sC = s.charAt(i);
            tC = t.charAt(i);
            if (map.containsKey(sC)) {
                if (!map.get(sC).equals(tC)) return false;
            } else if (map.containsValue(tC)) return false;
            else map.put(sC, tC);
        }
        return true;
    }
}