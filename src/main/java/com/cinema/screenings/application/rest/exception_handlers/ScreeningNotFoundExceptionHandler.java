package com.cinema.screenings.application.rest.exception_handlers;

import com.cinema.screenings.domain.exceptions.ScreeningNotFoundException;
import com.cinema.shared.exceptions.ExceptionMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ScreeningNotFoundExceptionHandler {

    @ExceptionHandler(ScreeningNotFoundException.class)
    ResponseEntity<ExceptionMessage> handle(ScreeningNotFoundException exception) {
        var exceptionMessage = new ExceptionMessage(exception.getMessage());
        return new ResponseEntity<>(exceptionMessage, HttpStatus.NOT_FOUND);
    }
}
