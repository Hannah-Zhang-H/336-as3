package com.hanz.youmetalk;

import android.util.Log;

public class User {
    private String id;
    private String userName;
    private String image;
    private String youMeId;

    public User() {
        // empty constructor for firebase
    }

    public User(String id, String userName, String profileImage) {
        this.id = id;
        this.userName = userName;
        this.image = profileImage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getImage() {
        Log.d("UserImage", "Image URL: " + image);
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getYouMeId() {
        return youMeId;
    }

    public void setYouMeId(String youMeId) {
        this.youMeId = youMeId;
    }
}
