package com.qq.ijay997.mqtt;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

//@Service
public class MqttService {

    private final MessageChannel mqttOutboundChannel;

    public MqttService(MessageChannel mqttOutboundChannel) {
        this.mqttOutboundChannel = mqttOutboundChannel;
    }

    /**
     * 发布消息到默认主题
     */
    public void publish(String payload) {
        mqttOutboundChannel.send(MessageBuilder.withPayload(payload).build());
    }

    /**
     * 发布消息到指定主题
     */
    public void publish(String topic, String payload) {
        mqttOutboundChannel.send(MessageBuilder.withPayload(payload)
                .setHeader("mqtt_topic", topic)
                .build());
    }

    /**
     * 发布消息到指定主题，指定 QoS
     */
    public void publish(String topic, String payload, int qos) {
        mqttOutboundChannel.send(MessageBuilder.withPayload(payload)
                .setHeader("mqtt_topic", topic)
                .setHeader("mqtt_qos", qos)
                .build());
    }

    /**
     * 发布消息到指定主题，指定 QoS 和是否保留
     */
    public void publish(String topic, String payload, int qos, boolean retained) {
        mqttOutboundChannel.send(MessageBuilder.withPayload(payload)
                .setHeader("mqtt_topic", topic)
                .setHeader("mqtt_qos", qos)
                .setHeader("mqtt_retained", retained)
                .build());
    }
}