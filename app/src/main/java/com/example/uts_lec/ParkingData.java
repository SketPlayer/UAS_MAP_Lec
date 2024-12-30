package com.example.uts_lec;

// ParkingData.java
public class ParkingData {
    private String imageUrl;
    private double latitude;
    private double longitude;
    private long timestamp;
    private String userUID;

    public ParkingData() {
    }

    public ParkingData(String imageUrl, double latitude, double longitude, long timestamp, String userUID) {
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.userUID = userUID;
    }

    // Getters and Setters
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserUID() {
        return userUID;
    }

    public void setUserUID(String userUID) {
        this.userUID = userUID;
    }
}

