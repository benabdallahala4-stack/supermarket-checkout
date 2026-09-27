package com.example.supermarket.support.api;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JsonConfiguration {
  static final long MAX_REQUEST_DOCUMENT_LENGTH = 1024 * 1024;

  @Bean
  Jackson2ObjectMapperBuilderCustomizer strictRequestJson() {
    return builder -> {
      builder.featuresToEnable(
          DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
          DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
      builder.featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);

      builder.postConfigurer(
          mapper -> {
            mapper
                .getFactory()
                .setStreamReadConstraints(
                    StreamReadConstraints.builder()
                        .maxDocumentLength(MAX_REQUEST_DOCUMENT_LENGTH)
                        .build());

            mapper
                .coercionConfigFor(LogicalType.Integer)
                .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);

            mapper
                .coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
          });
    };
  }
}
