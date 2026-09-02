package com.decoupledx.reservation.shared.adapter.web;

import java.net.URI;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.decoupledx.reservation.shared.domain.BusinessException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusinessException(BusinessException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(exception.errorCode().httpStatus());
        problem.setType(URI.create("urn:reservation:error:" + exception.errorCode().name().toLowerCase()));
        problem.setTitle(exception.errorCode().name());
        problem.setDetail(exception.getMessage());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidationException(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(422);
        problem.setTitle("VALIDATION_FAILED");
        problem.setDetail(exception.getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .reduce((left, right) -> left + "; " + right)
                .orElse("Request validation failed"));
        return problem;
    }
}
