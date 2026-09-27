package com.example.supermarket.catalog.api;

import com.example.supermarket.catalog.application.CatalogConflictException;
import com.example.supermarket.generated.model.ApiProblem;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;

@Order(0)
@RestControllerAdvice(assignableTypes = CatalogManagementController.class)
public class CatalogManagementExceptionHandler {
  @ExceptionHandler(MissingRequestHeaderException.class)
  ResponseEntity<ApiProblem> missingPrecondition(
      MissingRequestHeaderException exception, HttpServletRequest request) {
    return response(
        428,
        "Catalog precondition required",
        "Supply the ETag from the last catalog read in If-Match.",
        ApiProblem.CodeEnum.INVALID_REQUEST,
        request);
  }

  @ExceptionHandler(CatalogEtag.PreconditionRequired.class)
  ResponseEntity<ApiProblem> preconditionRequired(HttpServletRequest request) {
    return response(
        428,
        "Catalog precondition required",
        "Supply the ETag from the last catalog read in If-Match.",
        ApiProblem.CodeEnum.INVALID_REQUEST,
        request);
  }

  @ExceptionHandler(CatalogEtag.InvalidPrecondition.class)
  ResponseEntity<ApiProblem> invalidPrecondition(HttpServletRequest request) {
    return response(
        400,
        "Invalid catalog precondition",
        "If-Match must contain one strong catalog ETag.",
        ApiProblem.CodeEnum.INVALID_REQUEST,
        request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiProblem> invalidCatalog(
      IllegalArgumentException exception, HttpServletRequest request) {
    return response(
        400,
        "Invalid catalog",
        "The catalog contains invalid products or offers.",
        ApiProblem.CodeEnum.INVALID_REQUEST,
        request);
  }

  @ExceptionHandler(CatalogConflictException.class)
  ResponseEntity<ApiProblem> conflict(
      CatalogConflictException exception, HttpServletRequest request) {
    return response(
        412,
        "Catalog precondition failed",
        "Read the catalog again before replacing it.",
        ApiProblem.CodeEnum.CATALOG_CONFLICT,
        request);
  }

  private ResponseEntity<ApiProblem> response(
      int status,
      String title,
      String detail,
      ApiProblem.CodeEnum code,
      HttpServletRequest request) {
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(new ApiProblem("about:blank", title, status, detail, request.getRequestURI(), code));
  }
}
