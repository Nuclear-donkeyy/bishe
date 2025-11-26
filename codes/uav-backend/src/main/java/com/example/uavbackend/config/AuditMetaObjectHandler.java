package com.example.uavbackend.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.Instant;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {
  @Override
  public void insertFill(MetaObject metaObject) {
    Instant now = Instant.now();
    strictInsertFill(metaObject, "createdAt", Instant.class, now);
    strictInsertFill(metaObject, "updatedAt", Instant.class, now);
  }

  @Override
  public void updateFill(MetaObject metaObject) {
    strictUpdateFill(metaObject, "updatedAt", Instant.class, Instant.now());
  }
}
