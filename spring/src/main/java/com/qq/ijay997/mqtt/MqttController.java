package com.qq.ijay997.mqtt;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//@RestController
//@RequestMapping("/mqtt")
public class MqttController {

    private final MqttService mqttService;

    public MqttController(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    /**
     * 发布消息到默认主题
     * GET /mqtt/publish?message=hello
     */
    @GetMapping("/publish")
    public String publish(@RequestParam String message) {
        mqttService.publish(message);
        return "消息已发布到默认主题: " + message;
    }

    /**
     * 发布消息到指定主题
     * POST /mqtt/publish/{topic}
     */
    @PostMapping("/publish/{topic}")
    public String publishToTopic(@PathVariable String topic, @RequestBody String message) {
        mqttService.publish(topic, message);
        return "消息已发布到主题 [" + topic + "]: " + message;
    }

    /**
     * 发布消息到指定主题，指定 QoS
     * POST /mqtt/publish/{topic}/qos/{qos}
     */
    @PostMapping("/publish/{topic}/qos/{qos}")
    public String publishWithQos(@PathVariable String topic, 
                                  @PathVariable int qos, 
                                  @RequestBody String message) {
        if (qos < 0 || qos > 2) {
            return "QoS 必须为 0、1 或 2";
        }
        mqttService.publish(topic, message, qos);
        return String.format("消息已发布到主题 [%s] (QoS=%d): %s", topic, qos, message);
    }

    /**
     * 发布保留消息
     * POST /mqtt/publish/{topic}/retain
     */
    @PostMapping("/publish/{topic}/retain")
    public String publishRetained(@PathVariable String topic, 
                                   @RequestBody String message) {
        mqttService.publish(topic, message, 1, true);
        return "保留消息已发布到主题 [" + topic + "]: " + message;
    }

    /**
     * 发布传感器模拟数据
     * GET /mqtt/sensor/{sensorId}
     */
    @GetMapping("/sensor/{sensorId}")
    public String publishSensorData(@PathVariable String sensorId) {
        String topic = "sensor/" + sensorId + "/data";
        String payload = String.format(
            "{\"sensorId\":\"%s\",\"temperature\":%.1f,\"humidity\":%.1f,\"timestamp\":\"%s\"}",
            sensorId,
            20 + Math.random() * 10,
            40 + Math.random() * 30,
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        mqttService.publish(topic, payload, 1);
        return "传感器数据已发布到主题 [" + topic + "]: " + payload;
    }
}