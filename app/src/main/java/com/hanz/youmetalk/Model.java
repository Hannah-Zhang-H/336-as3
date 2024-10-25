package com.hanz.youmetalk;

import java.util.List;

/**
 * The `Model` class represents a chat message model in the YouMeTalk application, holding details about
 * each message and its associated metadata.
 * <p>
 * Fields:
 * - `message` (String): The text content of the message.
 * - `from` (String): The ID of the user who sent the message.
 * - `image` (String): Optional image URL for an image message.
 * - `deleted_for` (List<String>): List of user IDs for whom the message is marked as deleted.
 * - `timestamp` (long): The time the message was sent, stored as a Unix timestamp.
 * - `messageId` (String): Unique identifier for each message.
 * - `isRead` (boolean): Flag indicating whether the message has been read by the recipient.
 * <p>
 * Constructor:
 * - Default constructor for initializing a `Model` instance.
 * <p>
 * Getter/Setter Methods:
 * - Provides getter and setter methods for each field to retrieve and modify message details.
 * - Includes null checks for fields like `message`, `from`, and `image` to ensure non-null values.
 */

public class Model {
    private String message;
    private String from;
    private String image;
    private List<String> deleted_for;
    private long timestamp;
    private String messageId;
    private boolean isRead;

    public Model() {
    }

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
