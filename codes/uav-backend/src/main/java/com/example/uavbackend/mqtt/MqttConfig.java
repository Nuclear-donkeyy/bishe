package com.example.uavbackend.mqtt;

import com.example.uavbackend.fleet.FleetService;
import com.example.uavbackend.fleet.TelemetryService;
import com.example.uavbackend.fleet.UavTelemetry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@RequiredArgsConstructor
@Slf4j
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

  private static final Pattern UAV_TOPIC_PATTERN = Pattern.compile("uav/([^/]+)/telemetry");

  private final FleetService fleetService;
  private final TelemetryService telemetryService;

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
    DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
    converter.setPayloadAsBytes(false);
    adapter.setConverter(converter);
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
      String uavCode = extractUavCode(topic);
      if (!StringUtils.hasText(uavCode)) {
        return;
      }
      try {
        telemetryService.upsertTelemetry(uavCode, payload);
        log.info("MQTT telemetry received, topic={}, uavCode={}, cachedToRedis=true", topic, uavCode);
        // Telemetry is only cached to Redis for real-time push; do not touch DB here.
      } catch (Exception e) {
        log.error("MQTT telemetry handling failed, topic={}, uavCode={}, payload={}", topic, uavCode, payload, e);
      }
    };
  }

  private String extractUavCode(String topic) {
    if (!StringUtils.hasText(topic)) {
      return null;
    }
    Matcher matcher = UAV_TOPIC_PATTERN.matcher(topic);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    return null;
  }
}
