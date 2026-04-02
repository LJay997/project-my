package com.qq.ijay997;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GraphAdjMatTest {

    private GraphAdjMat graph;

    @BeforeEach
    void setUp() {
        // 初始化一个空的图，具体测试用例会单独创建
    }

    /**
     * 辅助方法：打印邻接矩阵
     */
    private void printGraph(GraphAdjMat graph) {
        System.out.println("顶点列表：" + graph.vertices);
        System.out.println("邻接矩阵:");
        for (List<Integer> row : graph.adjMat) {
            System.out.println(row);
        }
    }

    @Test
    void testGraphConstruction_SingleVertex() {
        // 测试用例 1: 单顶点图
        System.out.println("\n=== 测试用例 1: 单顶点图 ===");
        int[] vertices = {1};
        int[][] edges = {{0}};
        
        GraphAdjMat singleVertexGraph = new GraphAdjMat(vertices, edges);
        
        assertNotNull(singleVertexGraph);
        assertEquals(1, singleVertexGraph.vertices.size(), "应该只有 1 个顶点");
        assertEquals(1, singleVertexGraph.vertices.get(0), "顶点值应为 1");
        assertEquals(1, singleVertexGraph.adjMat.size(), "邻接矩阵应该有 1 行");
        assertEquals(1, singleVertexGraph.adjMat.get(0).size(), "第一行应该有 1 列");
        assertEquals(0, singleVertexGraph.adjMat.get(0).get(0), "单顶点无边，应为 0");
        
        printGraph(singleVertexGraph);
    }

    @Test
    void testGraphConstruction_TwoVertices() {
        // 测试用例 2: 两个顶点的图
        System.out.println("\n=== 测试用例 2: 两个顶点的图 ===");
        int[] vertices = {1, 2};
        int[][] edges = {
            {0, 1},  // 顶点 1 与顶点 2 相连
            {1, 0}   // 顶点 2 与顶点 1 相连
        };
        
        GraphAdjMat twoVerticesGraph = new GraphAdjMat(vertices, edges);
        
        assertNotNull(twoVerticesGraph);
        assertEquals(2, twoVerticesGraph.vertices.size(), "应该有 2 个顶点");
        assertEquals(1, twoVerticesGraph.vertices.get(0), "第一个顶点值为 1");
        assertEquals(2, twoVerticesGraph.vertices.get(1), "第二个顶点值为 2");
        
        // 验证邻接矩阵
        assertEquals(0, twoVerticesGraph.adjMat.get(0).get(0), "顶点 1 到自身的距离应为 0");
        assertEquals(1, twoVerticesGraph.adjMat.get(0).get(1), "顶点 1 到顶点 2 应相连");
        assertEquals(1, twoVerticesGraph.adjMat.get(1).get(0), "顶点 2 到顶点 1 应相连");
        assertEquals(0, twoVerticesGraph.adjMat.get(1).get(1), "顶点 2 到自身的距离应为 0");
        
        printGraph(twoVerticesGraph);
    }

    @Test
    void testGraphConstruction_CompleteGraph() {
        // 测试用例 3: 完全图（每两个顶点都相连）
        System.out.println("\n=== 测试用例 3: 3 个顶点的完全图 ===");
        int[] vertices = {1, 2, 3};
        int[][] edges = {
            {0, 1, 1},  // 顶点 1 与顶点 2、3 都相连
            {1, 0, 1},  // 顶点 2 与顶点 1、3 都相连
            {1, 1, 0}   // 顶点 3 与顶点 1、2 都相连
        };
        
        GraphAdjMat completeGraph = new GraphAdjMat(vertices, edges);
        
        assertNotNull(completeGraph);
        assertEquals(3, completeGraph.vertices.size(), "应该有 3 个顶点");
        
        // 验证邻接矩阵的对称性（无向图的特点）
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                assertEquals(
                    completeGraph.adjMat.get(i).get(j),
                    completeGraph.adjMat.get(j).get(i),
                    "无向图的邻接矩阵应该是对称的"
                );
            }
        }
        completeGraph.addVertex(4);
        printGraph(completeGraph);
//        completeGraph.removeVertex(3);
        completeGraph.addEdge(1,3);
        printGraph(completeGraph);
    }

    @Test
    void testGraphConstruction_DisconnectedGraph() {
        // 测试用例 4: 非连通图
        System.out.println("\n=== 测试用例 4: 非连通图 ===");
        int[] vertices = {1, 2, 3, 4};
        int[][] edges = {
            {0, 1, 0, 0},  // 顶点 1 只与顶点 2 相连
            {1, 0, 0, 0},  // 顶点 2 只与顶点 1 相连
            {0, 0, 0, 1},  // 顶点 3 只与顶点 4 相连
            {0, 0, 1, 0}   // 顶点 4 只与顶点 3 相连
        };
        
        GraphAdjMat disconnectedGraph = new GraphAdjMat(vertices, edges);
        
        assertNotNull(disconnectedGraph);
        assertEquals(4, disconnectedGraph.vertices.size(), "应该有 4 个顶点");
        
        // 验证顶点 1 和顶点 3 不相连
        assertEquals(0, disconnectedGraph.adjMat.get(0).get(2), "顶点 1 和顶点 3 不应相连");
        assertEquals(0, disconnectedGraph.adjMat.get(1).get(3), "顶点 2 和顶点 4 不应相连");
        
        printGraph(disconnectedGraph);
    }

    @Test
    void testGraphConstruction_WeightedGraph() {
        // 测试用例 5: 带权图（使用不同的权重值）
        System.out.println("\n=== 测试用例 5: 带权图 ===");
        int[] vertices = {1, 2, 3};
        int[][] edges = {
            {0, 5, 3},  // 顶点 1 到顶点 2 权重为 5，到顶点 3 权重为 3
            {5, 0, 2},  // 顶点 2 到顶点 1 权重为 5，到顶点 3 权重为 2
            {3, 2, 0}   // 顶点 3 到顶点 1 权重为 3，到顶点 2 权重为 2
        };
        
        GraphAdjMat weightedGraph = new GraphAdjMat(vertices, edges);
        
        assertNotNull(weightedGraph);
        assertEquals(3, weightedGraph.vertices.size(), "应该有 3 个顶点");
        
        // 验证权重
        assertEquals(5, weightedGraph.adjMat.get(0).get(1), "顶点 1 到顶点 2 的权重应为 5");
        assertEquals(3, weightedGraph.adjMat.get(0).get(2), "顶点 1 到顶点 3 的权重应为 3");
        assertEquals(2, weightedGraph.adjMat.get(1).get(2), "顶点 2 到顶点 3 的权重应为 2");
        
        printGraph(weightedGraph);
    }

    @Test
    void testGraphConstruction_EmptyEdges() {
        // 测试用例 6: 没有任何边的图（孤立点）
        System.out.println("\n=== 测试用例 6: 孤立点图 ===");
        int[] vertices = {1, 2, 3};
        int[][] edges = {
            {0, 0, 0},
            {0, 0, 0},
            {0, 0, 0}
        };
        
        GraphAdjMat isolatedGraph = new GraphAdjMat(vertices, edges);
        
        assertNotNull(isolatedGraph);
        assertEquals(3, isolatedGraph.vertices.size(), "应该有 3 个顶点");
        
        // 验证所有边都是 0
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                assertEquals(0, isolatedGraph.adjMat.get(i).get(j), "所有顶点都应该是孤立的");
            }
        }
        
        printGraph(isolatedGraph);
    }

    @Test
    void testGraphConstruction_LargerGraph() {
        // 测试用例 7: 较大的图（5 个顶点）
        System.out.println("\n=== 测试用例 7: 5 个顶点的图 ===");
        int[] vertices = {1, 2, 3, 4, 5};
        int[][] edges = {
            {0, 1, 1, 0, 0},
            {1, 0, 0, 1, 0},
            {1, 0, 0, 1, 1},
            {0, 1, 1, 0, 1},
            {0, 0, 1, 1, 0}
        };
        
        GraphAdjMat largerGraph = new GraphAdjMat(vertices, edges);
        
        assertNotNull(largerGraph);
        assertEquals(5, largerGraph.vertices.size(), "应该有 5 个顶点");
        
        // 验证邻接矩阵大小
        assertEquals(5, largerGraph.adjMat.size(), "应该有 5 行");
        for (int i = 0; i < 5; i++) {
            assertEquals(5, largerGraph.adjMat.get(i).size(), "每行应该有 5 列");
        }
        
        printGraph(largerGraph);
    }

    @Test
    void testGraphConstruction_NegativeWeights() {
        // 测试用例 8: 包含负数权重（虽然不常见，但应该能存储）
        System.out.println("\n=== 测试用例 8: 包含负数权重 ===");
        int[] vertices = {-1, -2, -3};
        int[][] edges = {
            {0, -5, -3},
            {-5, 0, -2},
            {-3, -2, 0}
        };
        
        GraphAdjMat negativeGraph = new GraphAdjMat(vertices, edges);
        
        assertNotNull(negativeGraph);
        assertEquals(3, negativeGraph.vertices.size(), "应该有 3 个顶点");
        assertEquals(-1, negativeGraph.vertices.get(0), "第一个顶点值应为 -1");
        assertEquals(-5, negativeGraph.adjMat.get(0).get(1), "权重可以为负数");
        
        printGraph(negativeGraph);
    }
}
