package com.example.crudapp.exception;

public class ApiKeyNotConfiguredException extends RuntimeException {
    public ApiKeyNotConfiguredException(String message) {
        super(message);
    }
}
