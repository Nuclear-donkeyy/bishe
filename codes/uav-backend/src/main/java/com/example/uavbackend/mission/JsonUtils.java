package com.example.uavbackend.mission;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.springframework.util.StringUtils;

public final class JsonUtils {
  private static final ObjectMapper mapper = new ObjectMapper();

  private JsonUtils() {}

  public static String toJson(List<String> values) {
    if (values == null) {
      return null;
    }
    try {
      return mapper.writeValueAsString(values);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("无法序列化 JSON", e);
    }
  }

  @SuppressWarnings("unchecked")
  public static List<String> fromJsonArray(String json) {
    if (!StringUtils.hasText(json)) {
      return Collections.emptyList();
    }
    try {
      return mapper.readValue(json, List.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("无法解析 JSON", e);
    }
  }
}
