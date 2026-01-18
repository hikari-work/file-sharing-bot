package com.yann.forcesub.exceptions;

public class SendErrorException extends RuntimeException{
    public SendErrorException(String message) {
        super(message);
    }
}
