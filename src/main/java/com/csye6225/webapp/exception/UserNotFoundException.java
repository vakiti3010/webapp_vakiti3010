package com.csye6225.webapp.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }

    // You can also add constructors that accept other details like cause, etc., if needed
}

