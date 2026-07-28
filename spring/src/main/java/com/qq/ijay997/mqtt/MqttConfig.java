package com.qq.ijay997.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

//@Configuration
public class MqttConfig {

    @Value("${mqtt.broker-url:tcp://broker.emqx.io:1883}")
    private String brokerUrl;

    @Value("${mqtt.client-id-prefix:spring-mqtt-}")
    private String clientIdPrefix;

    @Value("${mqtt.username:emqx}")
    private String username;

    @Value("${mqtt.password:public}")
    private String password;

    @Value("${mqtt.default-topic:test/topic}")
    private String defaultTopic;

    @Value("${mqtt.qos:1}")
    private int qos;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        if (username != null && !username.isEmpty()) {
            options.setUserName(username);
        }
        if (password != null && !password.isEmpty()) {
            options.setPassword(password.toCharArray());
        }
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);
        options.setCleanSession(false);
        options.setAutomaticReconnect(true);
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        String clientId = clientIdPrefix + "publisher";
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(clientId, mqttClientFactory());
        handler.setDefaultQos(qos);
        handler.setDefaultTopic(defaultTopic);
        handler.setAsync(true);
        return handler;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer inbound() {
        String clientId = clientIdPrefix + "subscriber";
        String[] topics = {defaultTopic, "test/topic2", "sensor/#"};
        int[] qosLevels = {qos, qos, qos};
        MqttPahoMessageDrivenChannelAdapter adapter = 
            new MqttPahoMessageDrivenChannelAdapter(clientId, mqttClientFactory(), topics);
        adapter.setQos(qosLevels);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler mqttInputHandler() {
        return message -> {
            String topic = message.getHeaders().get("mqtt_topic", String.class);
            String payload = message.getPayload().toString();
            System.out.println("\n【MQTT 消息接收】");
            System.out.println("主题: " + topic);
            System.out.println("消息: " + payload);
            System.out.println("===================");
        };
    }
}