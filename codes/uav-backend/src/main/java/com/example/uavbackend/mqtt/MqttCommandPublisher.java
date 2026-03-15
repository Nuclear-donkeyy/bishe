package com.example.uavbackend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttCommandPublisher {
  private final MqttPahoClientFactory mqttClientFactory;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Object clientMonitor = new Object();

  @Value("${mqtt.broker-url}")
  private String brokerUrl;

  @Value("${mqtt.command-topic-prefix:uav/}")
  private String commandTopicPrefix;

  private final String publisherClientId = "uav-command-publisher-" + UUID.randomUUID();
  private IMqttClient publisherClient;

  public void publish(String uavCode, Map<String, Object> payload) throws Exception {
    String topic = commandTopicPrefix.endsWith("/")
        ? commandTopicPrefix + uavCode + "/command"
        : commandTopicPrefix + "/" + uavCode + "/command";
    byte[] body = objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
    MqttMessage msg = new MqttMessage(body);
    msg.setQos(1);

    synchronized (clientMonitor) {
      log.info("topic{}", topic);
      IMqttClient client = ensureConnectedClient();
      try {
        client.publish(topic, msg);
      } catch (MqttException firstFailure) {
        log.warn("MQTT publish failed on topic {}, retrying with a fresh client", topic, firstFailure);
        resetClientQuietly();
        client = ensureConnectedClient();
        client.publish(topic, msg);
      }
    }
  }

  private IMqttClient ensureConnectedClient() throws MqttException {
    if (publisherClient == null) {
      publisherClient = mqttClientFactory.getClientInstance(brokerUrl, publisherClientId);
    }
    if (!publisherClient.isConnected()) {
      publisherClient.connect(mqttClientFactory.getConnectionOptions());
    }
    return publisherClient;
  }

  private void resetClientQuietly() {
    if (publisherClient == null) {
      return;
    }
    try {
      if (publisherClient.isConnected()) {
        publisherClient.disconnectForcibly(0, 0);
      }
    } catch (Exception e) {
      log.debug("Ignore MQTT publisher disconnect failure", e);
    }
    try {
      publisherClient.close();
    } catch (Exception e) {
      log.debug("Ignore MQTT publisher close failure", e);
    }
    publisherClient = null;
  }

  @PreDestroy
  public void shutdown() {
    synchronized (clientMonitor) {
      resetClientQuietly();
    }
  }
}
