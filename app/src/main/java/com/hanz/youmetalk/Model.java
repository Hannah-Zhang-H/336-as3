package com.hanz.youmetalk;

public class Model {
    String message;
    String from;


    public Model(String from, String message) {
        this.from = from;
        this.message = message;
    }

    public Model() {
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
