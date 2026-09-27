package com.example.supermarket.support.api;

import com.example.supermarket.checkout.domain.InvalidCartException;
import com.example.supermarket.checkout.domain.UnknownProductException;
import com.example.supermarket.generated.model.ApiProblem;
import com.example.supermarket.generated.model.ApiProblem.CodeEnum;
import com.example.supermarket.generated.model.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var errors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
            .sorted(
                Comparator.comparing(FieldError::getField).thenComparing(FieldError::getMessage))
            .distinct()
            .toList();

    var problem =
        problem(
            HttpStatus.BAD_REQUEST,
            "Invalid request",
            "The request body contains invalid data or JSON.",
            CodeEnum.INVALID_REQUEST,
            path(request));
    problem.setErrors(errors);
    return response(problem);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return invalidRequest(path(request));
  }

  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return response(
        problem(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Unsupported media type",
            "Use application/json.",
            CodeEnum.INVALID_REQUEST,
            path(request)));
  }

  @ExceptionHandler({InvalidCartException.class, ConstraintViolationException.class})
  ResponseEntity<Object> invalidCart(Exception exception, HttpServletRequest request) {
    return invalidRequest(request.getRequestURI());
  }

  @ExceptionHandler(UnknownProductException.class)
  ResponseEntity<Object> unknownProduct(
      UnknownProductException exception, HttpServletRequest request) {
    return response(
        problem(
            HttpStatus.BAD_REQUEST,
            "Unknown product",
            "The cart contains an unknown product.",
            CodeEnum.UNKNOWN_PRODUCT,
            request.getRequestURI()));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Object> unexpectedFailure(Exception exception, HttpServletRequest request) {
    log.error("Unexpected API failure", exception);

    return response(
        problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error",
            "The request could not be completed.",
            CodeEnum.INTERNAL_ERROR,
            request.getRequestURI()));
  }

  private ResponseEntity<Object> invalidRequest(String instance) {
    return response(
        problem(
            HttpStatus.BAD_REQUEST,
            "Invalid request",
            "The request body contains invalid data or JSON.",
            CodeEnum.INVALID_REQUEST,
            instance));
  }

  private ApiProblem problem(
      HttpStatus status, String title, String detail, CodeEnum code, String instance) {
    return new ApiProblem("about:blank", title, status.value(), detail, instance, code);
  }

  private ResponseEntity<Object> response(ApiProblem problem) {
    return ResponseEntity.status(problem.getStatus())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  private String path(WebRequest request) {
    return ((ServletWebRequest) request).getRequest().getRequestURI();
  }
}
