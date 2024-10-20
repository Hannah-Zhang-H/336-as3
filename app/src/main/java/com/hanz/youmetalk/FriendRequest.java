package com.hanz.youmetalk;

public class FriendRequest {
    private String from;          // The UID of the user who sent the friend request
    private String requestId;     // The unique ID of the request
    private String status;        // The current status of the request (e.g., waiting, accepted, declined)

    // Default constructor required for calls to DataSnapshot.getValue(FriendRequest.class)
    public FriendRequest() {
    }

    // Constructor to initialize a friend request
    public FriendRequest(String from, String requestId, String status) {
        this.from = from;
        this.requestId = requestId;
        this.status = status;
    }

    // Getters and setters for each field
    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
