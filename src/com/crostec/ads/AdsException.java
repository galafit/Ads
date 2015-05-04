package com.crostec.ads;

public class AdsException extends RuntimeException{
    public AdsException(String message, Throwable cause) {
        super(message, cause);
    }
    public AdsException(String message) {
        super(message);
    }
}
