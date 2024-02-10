package com.example.cloud;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class LocationMarker {
    // marker used on google maps
    private Marker locationMarker;

    // current marker and previous marker positions
    private float currentLongPosition;
    private float currentLatPosition;
    private float previousLongPosition;
    private float previousLatPosition;

    // constants used for calculating change in latitude and longitude
    private final float earthRadius = 6371 * (float) Math.pow(10, 3); // earth radius per meter

    private final float metersPerLatDegree =  ((float) Math.PI * earthRadius) / 180; // number of meters per degree of latitude

    public LocationMarker(GoogleMap mMap, float initialPosLat, float initialPosLong) {
        // ensure previous and current positions have a known initial value
        currentLatPosition = initialPosLat;
        currentLongPosition = initialPosLong;
        previousLatPosition = currentLatPosition;
        previousLongPosition = currentLongPosition;

        // add location marker to initial position on the map
        locationMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(currentLatPosition,currentLongPosition)).title("Position"));
    }

    public void updateMarkerPos(float latDist, float longDist) {
        // store last marker position
        previousLatPosition = currentLatPosition;
        previousLongPosition = currentLongPosition;

        // calculate new marker position using travelled long and lat distances
        currentLatPosition = previousLatPosition + (latDist/metersPerLatDegree);
        currentLongPosition = previousLongPosition + (longDist/metersPerLatDegree) / (float) Math.cos(previousLatPosition * ((float) Math.PI / 180));

        // update marker location on map
        LatLng position = new LatLng(currentLatPosition, currentLongPosition);
        locationMarker.setPosition(position);
    }
}
