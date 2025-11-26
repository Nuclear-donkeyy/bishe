package com.example.uavbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.example.uavbackend")
@EnableScheduling
public class UavBackendApplication {
  public static void main(String[] args) {
    SpringApplication.run(UavBackendApplication.class, args);
  }
}
