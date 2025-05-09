package com.naskah.demo.exception;

import com.naskah.demo.exception.custom.NotFoundException;
import com.naskah.demo.model.response.DefaultResponse;
import com.naskah.demo.model.response.ResponseMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ApiExceptionHandler {
    private static final String SUCCESS = "Success";

    @ExceptionHandler(value = NotFoundException.class)
    public ResponseEntity<DefaultResponse> handleNotfoundException(NotFoundException e) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        DefaultResponse response = new DefaultResponse(SUCCESS, ResponseMessage.DATA_NOT_FOUND, status.value());
        return new ResponseEntity<>(response, new HttpHeaders(), status);
    }


}
