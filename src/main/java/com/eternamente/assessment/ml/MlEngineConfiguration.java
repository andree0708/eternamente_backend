package com.eternamente.assessment.ml;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MlEngineConfiguration {

  @Bean
  @Primary
  public MlAnalysisService mlAnalysisService(
      @Value("${ml.engine:cognitive}") String engine,
      CognitiveMlAnalysisService cognitive,
      RuleBasedMlAnalysisService rules
  ) {
    if ("rules".equalsIgnoreCase(engine)) {
      return rules;
    }
    return cognitive;
  }
}
