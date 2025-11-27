package com.example.uavbackend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MqttCommandPublisher {
  private final MqttPahoClientFactory mqttClientFactory;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${mqtt.broker-url}")
  private String brokerUrl;

  @Value("${mqtt.command-topic-prefix:uav/}")
  private String commandTopicPrefix;

  public void publish(String uavCode, Map<String, Object> payload) throws Exception {
    String topic = commandTopicPrefix.endsWith("/")
        ? commandTopicPrefix + uavCode + "/command"
        : commandTopicPrefix + "/" + uavCode + "/command";
    IMqttClient client = mqttClientFactory.getClientInstance(brokerUrl, "uav-command-" + uavCode);
    if (!client.isConnected()) {
      client.connect(mqttClientFactory.getConnectionOptions());
    }
    byte[] body = objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
    MqttMessage msg = new MqttMessage(body);
    msg.setQos(1);
    client.publish(topic, msg);
  }
}
