package com.hanz.youmetalk;

import java.util.List;

public class Model {
    private String message;
    private String from;
    private String image;
    private List<String> deleted_for;
    private long timestamp;
    private String messageId;
    private boolean isRead;

    public Model() {}

    public String getMessage() {
        return message != null ? message : "";
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFrom() {
        return from != null ? from : "";
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getImage() {
        return image != null ? image : "";
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getDeleted_for() {
        return deleted_for;
    }

    public void setDeleted_for(List<String> deleted_for) {
        this.deleted_for = deleted_for;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId != null ? messageId : "";
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
