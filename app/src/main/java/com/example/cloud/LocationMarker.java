package com.example.cloud;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public class LocationMarker {
    // Google maps objects
    private Marker locationMarker; // user position marker
    private Polyline pathLine; // line representing the user's path

    // current marker and previous marker positions
    private float currentLongPosition;
    private float currentLatPosition;
    private float previousLongPosition;
    private float previousLatPosition;

    // constants used for calculating change in latitude and longitude
    private static final float earthRadius = 6371 * (float) Math.pow(10, 3); // earth radius per meter

    private static final float metersPerLatDegree =  ((float) Math.PI * earthRadius) / 180; // number of meters per degree of latitude

    public LocationMarker(GoogleMap mMap, float initialPosLat, float initialPosLong, int lineColor) {
        // ensure previous and current positions have a known initial value
        currentLatPosition = initialPosLat;
        currentLongPosition = initialPosLong;
        previousLatPosition = currentLatPosition;
        previousLongPosition = currentLongPosition;

        // set locationMarker to the initial position on the map
        locationMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(currentLatPosition,currentLongPosition)).title("Position"));

        // initialise pathLine to start at the initial position using pastel blue colour
        pathLine = mMap.addPolyline(new PolylineOptions().add(new LatLng(currentLatPosition,currentLongPosition)).color(lineColor));
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

        // add new coordinate to pathLine
        List<LatLng> pathCordList = pathLine.getPoints();
        pathCordList.add(position);
        pathLine.setPoints(pathCordList);
    }
}
