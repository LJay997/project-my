package com.qq.ijay997;


import java.util.*;

/**
 * 基于邻接表实现的无向图类
 */
public class GraphAdjList {

    // 邻接表，key：顶点，value：该顶点的所有邻接顶点
    Map<Vertex, List<Vertex>> adjList;

    /* 构造方法 */
    public GraphAdjList(Vertex[][] edges) {
        this.adjList = new HashMap<>();
        // 初始化所有顶点
        for (Vertex[] edge : edges) {
            for (Vertex vertex : edge) {
                if (!adjList.containsKey(vertex)) {
                    adjList.put(vertex, new ArrayList<>());
                }
            }
        }
        // 添加边
        for (Vertex[] edge : edges) {
            if (edge.length == 2) {
                addEdge(edge[0], edge[1]);
            }
        }
    }

    /* 获取顶点数量 */
    public int size() {
        return adjList.size();
    }

    /* 添加边 */
    public void addEdge(Vertex vet1, Vertex vet2) {
        if (!adjList.containsKey(vet1) || !adjList.containsKey(vet2) || vet1 == vet2)
            throw new IllegalArgumentException();
        // 添加边 vet1 - vet2
        adjList.get(vet1).add(vet2);
        adjList.get(vet2).add(vet1);
    }

    /* 删除边 */
    public void removeEdge(Vertex vet1, Vertex vet2) {
        if (!adjList.containsKey(vet1) || !adjList.containsKey(vet2) || vet1 == vet2)
            throw new IllegalArgumentException();
        // 删除边 vet1 - vet2
        adjList.get(vet1).remove(vet2);
        adjList.get(vet2).remove(vet1);
    }

    /* 添加顶点 */
    public void addVertex(Vertex vet) {
        if (adjList.containsKey(vet))
            throw new IllegalArgumentException();
        adjList.put(vet, new ArrayList<>());
    }

    /* 删除顶点 */
    public void removeVertex(Vertex vet) {
        if (!adjList.containsKey(vet))
            throw new IllegalArgumentException();
        // 在邻接表中删除顶点 vet 对应的链表
        adjList.remove(vet);
        // 遍历其他顶点的链表，删除所有包含 vet 的边
        for (List<Vertex> list : adjList.values()) {
            list.remove(vet);
        }
    }

    /* 打印邻接表 */
    public void print() {
        System.out.println("邻接表 =");
        for (Map.Entry<Vertex, List<Vertex>> pair : adjList.entrySet()) {
            List<Integer> tmp = new ArrayList<>();
            for (Vertex vertex : pair.getValue())
                tmp.add(vertex.val);
            System.out.println(pair.getKey().val + ": " + tmp + ",");
        }
    }

    /* 广度优先遍历 */
    // 使用邻接表来表示图，以便获取指定顶点的所有邻接顶点
    List<Vertex> graphBFS(GraphAdjList graph, Vertex startVet) {
        // 顶点遍历序列
        List<Vertex> res = new ArrayList<>();
        ArrayDeque<Vertex> deque = new ArrayDeque<>();
        HashSet<Vertex> visited = new HashSet<>();
        deque.add(startVet);
        visited.add(startVet);

        Vertex cur;
        int size;
        for (; !deque.isEmpty(); ) {
            size = deque.size();
            cur = deque.poll();
            res.add(cur);
            for (Vertex vertex : graph.adjList.get(cur)) {
                if (visited.contains(vertex)) continue;

                deque.add(vertex);
                visited.add(vertex);
            }
        }
        return res;
    }

    /* 深度优先遍历 - 递归实现 */
// 使用邻接表来表示图，以便获取指定顶点的所有邻接顶点
    List<Vertex> graphDFS(GraphAdjList graph, Vertex startVet) {
        // 顶点遍历序列
        List<Vertex> res = new ArrayList<>();
        HashSet<Vertex> visited = new HashSet<>();

        dfs(graph, visited, res, startVet);
        return res;
    }

    private void dfs(GraphAdjList graph, HashSet<Vertex> visited, List<Vertex> res, Vertex startVet) {
        if (visited.contains(startVet)) return;

        res.add(startVet);
        visited.add(startVet);
        for (Vertex vertex : graph.adjList.get(startVet)) {
            dfs(graph, visited, res, vertex);
        }
    }

    /* 深度优先遍历 - 迭代实现（使用栈） */

    /**
     * 使用 Stack 数据结构实现的 DFS 迭代版本
     * 核心思路：利用栈的后进先出 (LIFO) 特性来模拟递归调用栈
     *
     * @param graph    图对象
     * @param startVet 起始顶点
     * @return DFS 遍历结果
     */
    List<Vertex> graphDFSIterative(GraphAdjList graph, Vertex startVet) {
        // 顶点遍历序列
        List<Vertex> res = new ArrayList<>();
        // 哈希集合，用于记录已被访问过的顶点
        Set<Vertex> visited = new HashSet<>();
        // 栈用于实现 DFS（后进先出）
        Deque<Vertex> stack = new ArrayDeque<>();

        // 将起始顶点入栈
        stack.push(startVet);
        visited.add(startVet);

        while (!stack.isEmpty()) {
            // 栈顶元素出栈
            Vertex vet = stack.pop();
            res.add(vet);

            // 将该顶点的所有未访问邻接顶点入栈
            // 注意：为了保持与递归相同的访问顺序，这里需要逆序入栈
            List<Vertex> neighbors = new ArrayList<>(graph.adjList.get(vet));
            Collections.reverse(neighbors); // 逆序，保证先访问的顶点后入栈

            for (Vertex vertex : neighbors) {
                if (!visited.contains(vertex)) {
                    stack.push(vertex);
                    visited.add(vertex); // 提前标记为已访问，避免重复入栈
                }
            }
        }

        return res;
    }

    /* 深度优先遍历 - 迭代实现（使用栈，延迟标记访问） */

    /**
     * 使用 Stack 实现的另一种 DFS 迭代版本
     * 与上面的方法区别：在出栈时才标记为已访问（更符合传统实现）
     *
     * @param graph    图对象
     * @param startVet 起始顶点
     * @return DFS 遍历结果
     */
    List<Vertex> graphDFSIterativeV2(GraphAdjList graph, Vertex startVet) {
        // 顶点遍历序列
        List<Vertex> res = new ArrayList<>();
        // 哈希集合，用于记录已被访问过的顶点
        Set<Vertex> visited = new HashSet<>();
        // 栈用于实现 DFS（后进先出）
        Deque<Vertex> stack = new ArrayDeque<>();

        // 将起始顶点入栈
        stack.push(startVet);

        while (!stack.isEmpty()) {
            // 栈顶元素出栈
            Vertex vet = stack.pop();

            // 如果已经访问过，跳过
            if (visited.contains(vet)) {
                continue;
            }

            // 标记为已访问并加入结果
            visited.add(vet);
            res.add(vet);

            // 将该顶点的所有邻接顶点入栈
            // 逆序入栈，保证正序访问
            List<Vertex> neighbors = new ArrayList<>(graph.adjList.get(vet));
            Collections.reverse(neighbors);

            for (Vertex vertex : neighbors) {
                if (!visited.contains(vertex)) {
                    stack.push(vertex);
                }
            }
        }

        return res;
    }

}
