package com.foodbridge.exception;

public class InsufficientFoodException extends RuntimeException {
    public InsufficientFoodException(String message) {
        super(message);
    }
}
