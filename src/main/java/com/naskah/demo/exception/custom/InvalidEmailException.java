package com.naskah.demo.exception.custom;

public class InvalidEmailException extends RuntimeException {
    public InvalidEmailException() {
        super("The provided email address is invalid.");
    }
}
