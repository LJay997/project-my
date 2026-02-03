package com.qq.ijay997;


import java.util.*;
import java.util.stream.Collectors;


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
}