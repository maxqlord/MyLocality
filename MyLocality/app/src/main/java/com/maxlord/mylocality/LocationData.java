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
    public String getID() {
        return id;
    }
    public String getLocation() {
        return location;
    }
    public double getLatitude() {
        return latitude;
    }
    public double getLongitude() {
        return longitude;
    }
}
