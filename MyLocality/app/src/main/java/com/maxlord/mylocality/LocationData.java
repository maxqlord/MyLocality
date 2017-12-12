package com.maxlord.mylocality;

/**
 * Created by maxlord on 11/16/17.
 */

class LocationData {

    String id;
    String location;
    double latitude;
    double longitude;
    LocationData(String playlist, String city, double lat, double lon) {
        id = playlist;
        location = city;
        latitude = lat;
        longitude = lon;
    }
    String getID() {
        return id;
    }
    String getLocation() {
        return location;
    }
    public double getLatitude() {
        return latitude;
    }
    public double getLongitude() {
        return longitude;
    }
    public String toString() {
        return "City: " + location + "\tLatitude: " + latitude + "\tLongitude" + longitude + "\tID: " + id;
    }
}
