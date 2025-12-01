package org.dataledge.gateway.config;


import org.dataledge.gateway.config.exceptions.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public @ResponseBody ErrorResponse handleException(UnauthorizedException ex) {
        return new ErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED.value());
    }

}