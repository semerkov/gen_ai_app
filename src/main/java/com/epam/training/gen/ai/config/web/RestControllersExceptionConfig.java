package com.epam.training.gen.ai.config.web;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ControllerAdvice
public class RestControllersExceptionConfig {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static Map<String, String> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {

        return ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        fieldError -> ObjectUtils.defaultIfNull(fieldError.getDefaultMessage(), StringUtils.EMPTY),
                        (firstValue, secondValue) -> firstValue + "; " + secondValue));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {

        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Failed to read request");
    }
}
