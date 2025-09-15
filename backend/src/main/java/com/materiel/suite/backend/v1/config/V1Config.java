package com.materiel.suite.backend.v1.config;

import com.materiel.suite.backend.v1.service.ChangeFeedService;
import com.materiel.suite.backend.v1.service.IdempotencyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class V1Config {
  @Bean
  public ChangeFeedService changeFeedService(){ return new ChangeFeedService(); }

  @Bean
  public IdempotencyService idempotencyService(){ return new IdempotencyService(); }
}
