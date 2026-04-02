package com.qq.ijay997;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GraphAdjListTest {

    private GraphAdjList graph;

    @BeforeEach
    void setUp() {
        // 初始化一个空的图，具体测试用例会单独创建
    }

    /**
     * 辅助方法：打印邻接表
     */
    private void printGraph(GraphAdjList graph) {
        System.out.println("邻接表 =");
        for (java.util.Map.Entry<Vertex, List<Vertex>> pair : graph.adjList.entrySet()) {
            List<Integer> tmp = new java.util.ArrayList<>();
            for (Vertex vertex : pair.getValue())
                tmp.add(vertex.val);
            System.out.println(pair.getKey().val + ": " + tmp + ",");
        }
    }

    /**
     * 辅助方法：将顶点列表转换为值列表
     */
    private List<Integer> vetsToVals(List<Vertex> vets) {
        return Vertex.vetsToVals(vets);
    }

    @Test
    void testGraphBFS_SingleVertex() {
        // 测试用例 1: 单顶点图的 BFS
        System.out.println("\n=== 测试用例 1: 单顶点图的 BFS ===");
        int[] vals = {1};
        Vertex[] vertices = Vertex.valsToVets(vals);
        Vertex[][] edges = {};
        
        GraphAdjList singleVertexGraph = new GraphAdjList(edges);
        singleVertexGraph.addVertex(vertices[0]);
        
        List<Vertex> result = singleVertexGraph.graphBFS(singleVertexGraph, vertices[0]);
        
        assertNotNull(result);
        assertEquals(1, result.size(), "应该只有 1 个顶点");
        assertEquals(1, result.get(0).val, "顶点值应为 1");
        
        System.out.println("BFS 遍历结果：" + vetsToVals(result));
        printGraph(singleVertexGraph);
    }

    @Test
    void testGraphBFS_TwoVertices() {
        // 测试用例 2: 两个顶点的图的 BFS
        System.out.println("\n=== 测试用例 2: 两个顶点的图的 BFS ===");
        int[] vals = {1, 2};
        Vertex[] vertices = Vertex.valsToVets(vals);
        Vertex[][] edges = {{vertices[0], vertices[1]}};
        
        GraphAdjList twoVerticesGraph = new GraphAdjList(edges);
        
        List<Vertex> result = twoVerticesGraph.graphBFS(twoVerticesGraph, vertices[0]);
        
        assertNotNull(result);
        assertEquals(2, result.size(), "应该有 2 个顶点");
        assertEquals(1, result.get(0).val, "第一个访问的顶点应为 1");
        assertEquals(2, result.get(1).val, "第二个访问的顶点应为 2");
        
        System.out.println("BFS 遍历结果：" + vetsToVals(result));
        printGraph(twoVerticesGraph);
    }

    @Test
    void testGraphBFS_LineGraph() {
        // 测试用例 3: 线性图的 BFS (1-2-3-4)
        System.out.println("\n=== 测试用例 3: 线性图的 BFS ===");
        int[] vals = {1, 2, 3, 4};
        Vertex[] vertices = Vertex.valsToVets(vals);
        Vertex[][] edges = {
            {vertices[0], vertices[1]}, // 1-2
            {vertices[1], vertices[2]}, // 2-3
            {vertices[2], vertices[3]}  // 3-4
        };
        
        GraphAdjList lineGraph = new GraphAdjList(edges);
        
        List<Vertex> result = lineGraph.graphBFS(lineGraph, vertices[0]);
        
        assertNotNull(result);
        assertEquals(4, result.size(), "应该有 4 个顶点");
        // BFS 应该按层次遍历，从 1 开始应该是 1,2,3,4
        assertEquals(1, result.get(0).val);
        assertEquals(2, result.get(1).val);
        assertEquals(3, result.get(2).val);
        assertEquals(4, result.get(3).val);
        
        System.out.println("BFS 遍历结果：" + vetsToVals(result));
        printGraph(lineGraph);
    }

    @Test
    void testGraphBFS_StarGraph() {
        // 测试用例 4: 星型图的 BFS (中心节点 1，连接 2,3,4,5)
        System.out.println("\n=== 测试用例 4: 星型图的 BFS ===");
        int[] vals = {1, 2, 3, 4, 5};
        Vertex[] vertices = Vertex.valsToVets(vals);
        Vertex[][] edges = {
            {vertices[0], vertices[1]}, // 1-2
            {vertices[0], vertices[2]}, // 1-3
            {vertices[0], vertices[3]}, // 1-4
            {vertices[0], vertices[4]}  // 1-5
        };
        
        GraphAdjList starGraph = new GraphAdjList(edges);
        
        List<Vertex> result = starGraph.graphBFS(starGraph, vertices[0]);
        
        assertNotNull(result);
        assertEquals(5, result.size(), "应该有 5 个顶点");
        assertEquals(1, result.get(0).val, "中心节点应该首先被访问");
        
        // 验证其他节点都被访问了（顺序可能不同，但都应该在结果中）
        assertTrue(result.stream().anyMatch(v -> v.val == 2));
        assertTrue(result.stream().anyMatch(v -> v.val == 3));
        assertTrue(result.stream().anyMatch(v -> v.val == 4));
        assertTrue(result.stream().anyMatch(v -> v.val == 5));
        
        System.out.println("BFS 遍历结果：" + vetsToVals(result));
        printGraph(starGraph);
    }

    @Test
    void testGraphBFS_CycleGraph() {
        // 测试用例 5: 环形图的 BFS (1-2-3-4-1)
        System.out.println("\n=== 测试用例 5: 环形图的 BFS ===");
        int[] vals = {1, 2, 3, 4};
        Vertex[] vertices = Vertex.valsToVets(vals);
        Vertex[][] edges = {
            {vertices[0], vertices[1]}, // 1-2
            {vertices[1], vertices[2]}, // 2-3
            {vertices[2], vertices[3]}, // 3-4
            {vertices[3], vertices[0]}  // 4-1
        };
        
        GraphAdjList cycleGraph = new GraphAdjList(edges);
        
        List<Vertex> result = cycleGraph.graphBFS(cycleGraph, vertices[0]);
        
        assertNotNull(result);
        assertEquals(4, result.size(), "应该有 4 个顶点（环不会重复访问）");
        assertEquals(1, result.get(0).val, "起始节点应该首先被访问");
        
        // 验证所有节点都被访问了一次
        assertTrue(result.stream().distinct().count() == 4, "所有节点应该只被访问一次");
        
        System.out.println("BFS 遍历结果：" + vetsToVals(result));
        printGraph(cycleGraph);
    }

    @Test
    void testGraphBFS_CompleteGraph() {
        // 测试用例 6: 完全图的 BFS（每两个顶点都相连）
        System.out.println("\n=== 测试用例 6: 完全图的 BFS ===");
        int[] vals = {1, 2, 3};
        Vertex[] vertices = Vertex.valsToVets(vals);
        Vertex[][] edges = {
            {vertices[0], vertices[1]}, // 1-2
            {vertices[0], vertices[2]}, // 1-3
            {vertices[1], vertices[2]}  // 2-3
        };
        
        GraphAdjList completeGraph = new GraphAdjList(edges);
        
        List<Vertex> result = completeGraph.graphBFS(completeGraph, vertices[0]);
        
        assertNotNull(result);
        assertEquals(3, result.size(), "应该有 3 个顶点");
        assertEquals(1, result.get(0).val, "起始节点应该首先被访问");
        
        // 验证所有节点都被访问了
        assertTrue(result.stream().anyMatch(v -> v.val == 2));
        assertTrue(result.stream().anyMatch(v -> v.val == 3));
        
        System.out.println("BFS 遍历结果：" + vetsToVals(result));
        printGraph(completeGraph);
    }

    @Test
    void testGraphBFS_DisconnectedGraph() {
        // 测试用例 7: 非连通图的 BFS（只能访问到连通分量中的节点）
        System.out.println("\n=== 测试用例 7: 非连通图的 BFS ===");
        int[] vals = {1, 2, 3, 4};
        Vertex[] vertices = Vertex.valsToVets(vals);
        Vertex[][] edges = {
            {vertices[0], vertices[1]}, // 1-2（第一组）
            {vertices[2], vertices[3]}  // 3-4（第二组，与第一组不连通）
        };
        
        GraphAdjList disconnectedGraph = new GraphAdjList(edges);
        
        // 从顶点 1 开始 BFS，应该只能访问到 1 和 2
        List<Vertex> result = disconnectedGraph.graphBFS(disconnectedGraph, vertices[0]);
        
        assertNotNull(result);
        assertEquals(2, result.size(), "应该只能访问到 2 个顶点（连通分量）");
        assertEquals(1, result.get(0).val);
        assertEquals(2, result.get(1).val);
        
        // 验证不会访问到 3 和 4
        assertFalse(result.stream().anyMatch(v -> v.val == 3));
        assertFalse(result.stream().anyMatch(v -> v.val == 4));
        
        System.out.println("从顶点 1 开始的 BFS 结果：" + vetsToVals(result));
        
        // 从顶点 3 开始 BFS，应该只能访问到 3 和 4
        List<Vertex> result2 = disconnectedGraph.graphBFS(disconnectedGraph, vertices[2]);
        assertNotNull(result2);
        assertEquals(2, result2.size());
        assertEquals(3, result2.get(0).val);
        assertEquals(4, result2.get(1).val);
        
        System.out.println("从顶点 3 开始的 BFS 结果：" + vetsToVals(result2));
        printGraph(disconnectedGraph);
    }

    @Test
    void testGraphBFS_LargerGraph() {
        // 测试用例 8: 较大的图的 BFS
        System.out.println("\n=== 测试用例 8: 较大图的 BFS ===");
        int[] vals = {1, 2, 3, 4, 5, 6, 7};
        Vertex[] vertices = Vertex.valsToVets(vals);
        Vertex[][] edges = {
            {vertices[0], vertices[1]}, // 1-2
            {vertices[0], vertices[2]}, // 1-3
            {vertices[1], vertices[3]}, // 2-4
            {vertices[1], vertices[4]}, // 2-5
            {vertices[2], vertices[5]}, // 3-6
            {vertices[2], vertices[6]}  // 3-7
        };
        
        GraphAdjList largerGraph = new GraphAdjList(edges);
        
        List<Vertex> result = largerGraph.graphBFS(largerGraph, vertices[0]);
        
        assertNotNull(result);
        assertEquals(7, result.size(), "应该有 7 个顶点");
        assertEquals(1, result.get(0).val, "根节点应该首先被访问");
        
        // 验证所有节点都被访问了
        for (int i = 1; i <= 7; i++) {
            final int val = i;
            assertTrue(result.stream().anyMatch(v -> v.val == val), 
                "应该包含顶点 " + val);
        }
        
        System.out.println("BFS 遍历结果：" + vetsToVals(result));
        printGraph(largerGraph);
    }

    @Test
    void testGraphConstruction() {
        // 测试用例 9: 验证图的构造是否正确
        System.out.println("\n=== 测试用例 9: 图的构造测试 ===");
        int[] vals = {1, 2, 3};
        Vertex[] vertices = Vertex.valsToVets(vals);
        Vertex[][] edges = {
            {vertices[0], vertices[1]},
            {vertices[1], vertices[2]}
        };
        
        GraphAdjList graph = new GraphAdjList(edges);
        
        assertNotNull(graph);
        assertEquals(3, graph.size(), "应该有 3 个顶点");
        
        printGraph(graph);
    }

}
