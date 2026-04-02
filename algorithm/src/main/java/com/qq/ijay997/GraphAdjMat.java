package com.qq.ijay997;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于邻接矩阵实现的无向图类
 */
public class GraphAdjMat {
    List<Integer> vertices; // 顶点列表，元素代表“顶点值”，索引代表“顶点索引”
    List<List<Integer>> adjMat; // 邻接矩阵，行列索引对应“顶点索引”

    GraphAdjMat(int[] vertices, int[][] edges) {
        this.vertices = new ArrayList<>();

        this.adjMat = new ArrayList<List<Integer>>();
        for (int i = 0; i < vertices.length; i++) {
            int[] rows = edges[i];
            this.vertices.add(vertices[i]);
            ArrayList<Integer> integers = new ArrayList<>();
            this.adjMat.add(integers);
            for (int j = 0; j < rows.length; j++) {
                integers.add(rows[j]);
            }
        }
    }

    /* 获取顶点数量 */
    public int size() {
        return vertices.size();
    }

    /* 添加顶点 */
    public void addVertex(int val) {
        vertices.add(val);
        for (int i = 0; i < adjMat.size(); i++) {
            adjMat.get(i).add(0);
        }
        ArrayList<Integer> e = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            e.add(0);
        }
        adjMat.add(e);
    }

    /* 删除顶点 */
    public void removeVertex(int index) {
        if (index >= vertices.size()) return;
        vertices.remove(index);
        adjMat.remove(index);
        for (int i = 0; i < adjMat.size(); i++) {
            adjMat.get(i).remove(index);
        }
    }

    /* 添加边 */
    // 参数 i, j 对应 vertices 元素索引
    public void addEdge(int i, int j){
        if (vertices == null ) return;
        if (i >= vertices.size() || j >= vertices.size()) return;

        adjMat.get(j).set(i, 1);
        adjMat.get(i).set(j, 1);
    }

    /* 删除边 */
    // 参数 i, j 对应 vertices 元素索引
    public void removeEdge(int i, int j){
        if (vertices == null ) return;
        if (i >= vertices.size() || j >= vertices.size()) return;

        adjMat.get(j).set(i, 0);
        adjMat.get(i).set(j, 0);
    }
}
