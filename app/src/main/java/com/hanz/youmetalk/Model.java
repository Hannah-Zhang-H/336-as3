package com.hanz.youmetalk;

public class Model {
    private String message;
    private String from;
    private String image;  // 确保有这个字段

    public Model() {}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getImage() {
        return image;  // 确保 getImage 正常返回 URL
    }

    public void setImage(String image) {
        this.image = image;
    }
}
