package com.example.supermarket.catalog.api;

import com.example.supermarket.generated.model.ApiProblem;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@Profile("catalog-management")
public class CatalogManagementAccess implements WebMvcConfigurer {
  private final byte[] token;
  private final ObjectMapper mapper;

  public CatalogManagementAccess(
      @Value("${checkout.catalog-management.token:}") String token,
      ObjectMapper mapper,
      Environment environment) {
    if (environment.matchesProfiles("config-catalog")) {
      throw new IllegalArgumentException("Catalog management requires the database catalog");
    }
    if (token.length() < 32 || token.chars().anyMatch(Character::isWhitespace)) {
      throw new IllegalArgumentException(
          "Catalog management requires a token of at least 32 non-whitespace characters");
    }
    this.token = token.getBytes(StandardCharsets.UTF_8);
    this.mapper = mapper;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(
        new HandlerInterceptor() {
          @Override
          public boolean preHandle(
              HttpServletRequest request, HttpServletResponse response, Object handler)
              throws Exception {
            // Bind access control to the actual handler, independent of configurable URL prefixes.
            if (!(handler instanceof HandlerMethod method)
                || !CatalogManagementController.class.isAssignableFrom(method.getBeanType())) {
              return true;
            }
            response.setHeader("Cache-Control", "no-store");
            String supplied = request.getHeader("X-Catalog-Token");
            if (supplied != null
                && MessageDigest.isEqual(token, supplied.getBytes(StandardCharsets.UTF_8))) {
              return true;
            }
            response.setStatus(401);
            response.setHeader("WWW-Authenticate", "CatalogToken realm=\"catalog\"");
            response.setContentType("application/problem+json");
            mapper.writeValue(
                response.getOutputStream(),
                new ApiProblem(
                    "about:blank",
                    "Unauthorized",
                    401,
                    "A valid catalog operator token is required.",
                    request.getRequestURI(),
                    ApiProblem.CodeEnum.UNAUTHORIZED));
            return false;
          }
        });
  }
}
