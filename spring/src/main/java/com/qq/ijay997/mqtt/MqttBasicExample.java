package com.qq.ijay997.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Scanner;

/**
 * MQTT 基础示例 - 纯 Java 实现
 * 展示 MQTT 客户端的基本用法：连接、发布、订阅
 */
public class MqttBasicExample {

    // EMQX 公共测试服务器
    private static final String BROKER_URL = "tcp://broker.emqx.io:1883";
    private static final String CLIENT_ID_PUBLISHER = "mqtt-java-publisher";
    private static final String CLIENT_ID_SUBSCRIBER = "mqtt-java-subscriber";
    private static final String TOPIC = "test/java/mqtt";
    private static final int QOS = 1;
    private static final String USERNAME = "emqx";
    private static final String PASSWORD = "public";

    public static void main(String[] args) throws MqttException {
        System.out.println("=== MQTT 基础示例 ===");
        System.out.println("1. 启动订阅者");
        System.out.println("2. 启动发布者");
        System.out.println("3. 退出");
        
        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();
        
        switch (choice) {
            case 1:
                startSubscriber();
                break;
            case 2:
                startPublisher();
                break;
            case 3:
                System.exit(0);
                break;
            default:
                System.out.println("无效选择");
        }
        
        scanner.close();
    }

    /**
     * 启动 MQTT 订阅者
     */
    private static void startSubscriber() throws MqttException {
        MqttClient client = new MqttClient(BROKER_URL, CLIENT_ID_SUBSCRIBER, new MemoryPersistence());
        
        // 设置连接选项
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(USERNAME);
        options.setPassword(PASSWORD.toCharArray());
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);
        options.setCleanSession(false);
        options.setAutomaticReconnect(true);
        
        // 设置回调
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("\n连接断开: " + cause.getMessage());
            }
            
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload());
                System.out.println("\n【消息接收】");
                System.out.println("主题: " + topic);
                System.out.println("QoS: " + message.getQos());
                System.out.println("保留: " + message.isRetained());
                System.out.println("内容: " + payload);
                System.out.println("-------------------");
            }
            
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // 订阅者不需要处理这个回调
            }
        });
        
        // 连接并订阅
        client.connect(options);
        System.out.println("订阅者已连接到: " + BROKER_URL);
        client.subscribe(TOPIC, QOS);
        System.out.println("已订阅主题: " + TOPIC);
        System.out.println("等待消息... (按 Ctrl+C 退出)");
        
        // 保持程序运行
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        client.disconnect();
        client.close();
    }

    /**
     * 启动 MQTT 发布者
     */
    private static void startPublisher() throws MqttException {
        MqttClient client = new MqttClient(BROKER_URL, CLIENT_ID_PUBLISHER, new MemoryPersistence());
        
        // 设置连接选项
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(USERNAME);
        options.setPassword(PASSWORD.toCharArray());
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);
        options.setCleanSession(false);
        
        // 连接
        client.connect(options);
        System.out.println("发布者已连接到: " + BROKER_URL);
        
        Scanner scanner = new Scanner(System.in);
        String input;
        
        while (true) {
            System.out.print("\n请输入要发布的消息 (输入 'exit' 退出): ");
            input = scanner.nextLine();
            
            if ("exit".equalsIgnoreCase(input)) {
                break;
            }
            
            // 创建消息
            MqttMessage message = new MqttMessage(input.getBytes());
            message.setQos(QOS);
            message.setRetained(false);
            
            // 发布消息
            client.publish(TOPIC, message);
            System.out.println("消息已发布到主题 [" + TOPIC + "]: " + input);
        }
        
        scanner.close();
        client.disconnect();
        client.close();
        System.out.println("已断开连接");
    }
}