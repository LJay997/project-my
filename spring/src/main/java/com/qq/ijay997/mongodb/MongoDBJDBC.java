package com.qq.ijay997.mongodb;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;

public class MongoDBJDBC {
    public static void main(String args[]) {
        try {
            // 连接到 mongodb 服务
            MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");

            // 连接到数据库
            MongoDatabase mongoDatabase = mongoClient.getDatabase("mycol");
            System.out.println("Connect to database successfully");

            MongoCollection<Document> collection = mongoDatabase.getCollection("test");
            System.out.println("集合 test 选择成功");

//            //插入文档
            insertDocument(collection);

            //检索所有文档
//            findDocument(collection);

            //更新文档   将文档中likes=100的文档修改为likes=200   
//            updateMany(collection);
            //删除符合条件的第一个文档
//            deleteDocument(collection);

            // 关闭客户端
            mongoClient.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private static void deleteDocument(MongoCollection<Document> collection) {
        DeleteResult likes = collection.deleteOne(Filters.eq("likes", 200));
        //删除所有符合条件的文档
        DeleteResult likes1 = collection.deleteMany(Filters.eq("likes", 200));
        //检索查看结果
        FindIterable<Document> findIterable = collection.find();
        MongoCursor<Document> mongoCursor = findIterable.iterator();
        while(mongoCursor.hasNext()){
            System.out.println(mongoCursor.next());
        }
    }

    private static void updateMany(MongoCollection<Document> collection) {
        collection.updateMany(Filters.eq("likes", 100), new Document("$set",new Document("likes",200)));
        //检索查看结果  
        FindIterable<Document> findIterable = collection.find();
        MongoCursor<Document> mongoCursor = findIterable.iterator();
        while(mongoCursor.hasNext()){
            System.out.println(mongoCursor.next());
        }
    }

    /**
     * 1. 获取迭代器FindIterable<Document>
     * 2. 获取游标MongoCursor<Document>
     * 3. 通过游标遍历检索出的文档集合
     *
     */
    private static void findDocument(MongoCollection<Document> collection) {
        FindIterable<Document> findIterable = collection.find();
        MongoCursor<Document> mongoCursor = findIterable.iterator();
        while (mongoCursor.hasNext()) {
            System.out.println(mongoCursor.next());
        }
    }

    /**
     * 1. 创建文档 org.bson.Document 参数为key-value的格式
     * 2. 创建文档集合List<Document>
     * 3. 将文档集合插入数据库集合中 mongoCollection.insertMany(List<Document>) 插入单个文档可以用 mongoCollection.insertOne(Document)
     *
     */
    private static void insertDocument(MongoCollection<Document> collection) {
        Document document = new Document("title", "MongoDB").
                append("description", "database").
                append("likes", 100).
                append("by", "Fly");
        List<Document> documents = new ArrayList<Document>();
        documents.add(document);
        collection.insertMany(documents);
        System.out.println("文档插入成功");
    }
}
