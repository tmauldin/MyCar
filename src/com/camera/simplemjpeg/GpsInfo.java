package com.camera.simplemjpeg;

/**
 * Author: tmauldin
 * Date: 1/4/14
 * Time: 5:57 PM
 */
public class GpsInfo {

    //http://192.168.8.116:8000/nmea.json

//    {
//        speed:0 km/h,
//        longitude:-97.769782128,
//        latitude:30.246355456,
//        range:200,
//        tilt:30,
//        heading:0,
//        altitude:169.666
//    }

    private int speed;
    private double longitude;
    private double latitude;
    private int range;
    private int tilt;
    private int heading;
    private double altitude;

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public int getTilt() {
        return tilt;
    }

    public void setTilt(int tilt) {
        this.tilt = tilt;
    }

    public int getHeading() {
        return heading;
    }

    public void setHeading(int heading) {
        this.heading = heading;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }
}
