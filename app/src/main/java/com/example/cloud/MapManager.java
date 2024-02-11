package com.example.cloud;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public class MapManager {
    // Google maps objects
    private GoogleMap googleMap;
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

    // config variables
    private static final float zoom = 19f; // map zoom
    private boolean normal_map; // bool to control whether satellite or normal map is used

    public MapManager(GoogleMap mMap, float initialPosLat, float initialPosLong, Drawable markerDrawable, int markerColor, int lineColor) {
        // initialise googleMap
        googleMap = mMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE); // set map to satellite mode
        normal_map = false;

        mMap.getUiSettings().setScrollGesturesEnabled(true); // enable scroll gesture so other parts of map can be viewed while recording

        // ensure previous and current positions have a known initial value
        currentLatPosition = initialPosLat;
        currentLongPosition = initialPosLong;
        previousLatPosition = currentLatPosition;
        previousLongPosition = currentLongPosition;

        // initialise position marker
        // create bitmap of icon from drawables
        Bitmap markerBitmap = Bitmap.createBitmap(2 * markerDrawable.getIntrinsicWidth(),
                2 * markerDrawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        // draw color onto the icon's bitmap
        markerDrawable.setBounds(0, 0, 2 * markerDrawable.getIntrinsicWidth(), 2 * markerDrawable.getIntrinsicHeight());
        markerDrawable.setTint(markerColor);
        markerDrawable.draw(new Canvas(markerBitmap));

        // set locationMarker to the initial position on the map
        locationMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(currentLatPosition,currentLongPosition))
                                                            .title("Position")
                                                            .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap)));
        // set marker anchor point to the middle
        locationMarker.setAnchor(0.5f,0.5f);

        // initialise pathLine to start at the initial position using pastel blue colour
        pathLine = mMap.addPolyline(new PolylineOptions().add(new LatLng(currentLatPosition,currentLongPosition)).color(lineColor));

        // zoom to initial position on the map
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLatPosition,currentLongPosition), zoom));
    }

    public void updateMarker(float latDist, float longDist, float markerRotation) {
        // store last marker position
        previousLatPosition = currentLatPosition;
        previousLongPosition = currentLongPosition;

        // calculate new marker position using travelled long and lat distances
        currentLatPosition = previousLatPosition + (latDist/metersPerLatDegree);
        currentLongPosition = previousLongPosition + (longDist/metersPerLatDegree) / (float) Math.cos(previousLatPosition * ((float) Math.PI / 180));

        // update marker location on map
        LatLng position = new LatLng(currentLatPosition, currentLongPosition);
        locationMarker.setPosition(position);
        locationMarker.setRotation(markerRotation);

        // add new coordinate to pathLine
        List<LatLng> pathCordList = pathLine.getPoints();
        pathCordList.add(position);
        pathLine.setPoints(pathCordList);
    }

    public void toggleMapMode(){
        if (normal_map) {
            googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            normal_map = false;
        }
        else {
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            normal_map = true;
        }
    }
}
