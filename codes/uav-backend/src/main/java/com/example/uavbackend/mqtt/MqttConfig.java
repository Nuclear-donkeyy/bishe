package com.example.uavbackend.mqtt;

import com.example.uavbackend.fleet.FleetService;
import com.example.uavbackend.fleet.UavTelemetry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
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
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
@RequiredArgsConstructor
public class MqttConfig {
  @Value("${mqtt.broker-url}")
  private String brokerUrl;

  @Value("${mqtt.client-id}")
  private String clientId;

  @Value("${mqtt.username:}")
  private String username;

  @Value("${mqtt.password:}")
  private String password;

  @Value("${mqtt.telemetry-topic}")
  private String telemetryTopic;

  private final FleetService fleetService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Bean
  public MqttConnectOptions mqttConnectOptions() {
    MqttConnectOptions options = new MqttConnectOptions();
    options.setServerURIs(new String[] {brokerUrl});
    if (!username.isEmpty()) {
      options.setUserName(username);
      options.setPassword(password.toCharArray());
    }
    options.setAutomaticReconnect(true);
    return options;
  }

  @Bean
  public MqttPahoClientFactory mqttClientFactory() {
    DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
    factory.setConnectionOptions(mqttConnectOptions());
    return factory;
  }

  @Bean
  public MessageChannel mqttInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageProducer inbound() {
    MqttPahoMessageDrivenChannelAdapter adapter =
        new MqttPahoMessageDrivenChannelAdapter(clientId, mqttClientFactory(), telemetryTopic);
    adapter.setCompletionTimeout(5000);
    adapter.setConverter(new DefaultPahoMessageConverter());
    adapter.setQos(1);
    adapter.setOutputChannel(mqttInputChannel());
    return adapter;
  }

  @Bean
  @ServiceActivator(inputChannel = "mqttInputChannel")
  public MessageHandler handler() {
    return message -> {
      String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
      String payload = (String) message.getPayload();
      try {
        JsonNode node = objectMapper.readTree(payload.getBytes(StandardCharsets.UTF_8));
        String type = node.path("type").asText();
        if ("uav.telemetry".equals(type)) {
          String uavId = node.path("uavId").asText();
          UavTelemetry telemetry = new UavTelemetry();
          telemetry.setBatteryPercent(node.path("battery").isMissingNode() ? null : node.path("battery").asInt());
          telemetry.setRangeKm(node.path("rangeKm").isMissingNode() ? null : BigDecimal.valueOf(node.path("rangeKm").asDouble()));
          if (node.has("location")) {
            telemetry.setLocationLat(BigDecimal.valueOf(node.path("location").path("lat").asDouble()));
            telemetry.setLocationLng(BigDecimal.valueOf(node.path("location").path("lng").asDouble()));
            telemetry.setLocationAlt(BigDecimal.valueOf(node.path("location").path("alt").asDouble()));
          }
          if (node.has("rtt")) {
            telemetry.setRttMs(node.path("rtt").asInt());
          }
          fleetService.applyTelemetry(uavId, telemetry);
        }
      } catch (Exception e) {
        // log error in real implementation
      }
    };
  }
}
