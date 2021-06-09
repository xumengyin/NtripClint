package com.xu.ntripclint.pojo;

public class CustomNetError {

    public Throwable throwable;
    public String error;

    public CustomNetError(Throwable throwable, String error) {
        this.throwable = throwable;
        this.error = error;
    }
}
