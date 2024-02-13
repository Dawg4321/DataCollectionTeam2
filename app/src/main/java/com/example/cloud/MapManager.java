package com.example.cloud;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapManager {
    // Google maps objects
    private GoogleMap googleMap;
    private Marker locationMarker; // user position marker
    private Polyline pathLine; // line representing the user's path
    private GroundOverlay indoorViewOverlay;

    // IndoorView
    private List<IndoorView> unviewableIndoorViews; // list of currently unviewable indoor views
    private List<IndoorView> viewableIndoorViews; // list of currently available indoor views
    IndoorView currentIndoorView; // current indoor view

    // current marker coordinates
    private float currentLongPosition;
    private float currentLatPosition;

    // constants used for calculating change in latitude and longitude
    private static final float earthRadius = 6371 * (float) Math.pow(10, 3); // earth radius per meter

    private static final float metersPerLatDegree =  ((float) Math.PI * earthRadius) / 180; // number of meters per degree of latitude

    // config variables
    private static final float zoom = 19f; // map zoom
    private boolean normalMap; // bool to control whether satellite or normal map is used

    public MapManager(GoogleMap mMap, float initialPosLat, float initialPosLong, Drawable markerDrawable, int markerColor, int lineColor) {
        // initialise googleMap
        googleMap = mMap;
        googleMap.getUiSettings().setScrollGesturesEnabled(true); // enable scroll gesture so other parts of map can be viewed while recording

        // set initial map to satellite mode
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        normalMap = false;

        // indoor map setup
        // initialised indoor view lists
        unviewableIndoorViews = loadIndoorViews(); // load hardcoded indoor maps into unviewable views
        viewableIndoorViews = new ArrayList<IndoorView>(); // set to empty until next position update

        // initialise indoor GroundViewOverlay using first image in unviewableIndoorMaps
        GroundOverlayOptions indoorViewOpts = new GroundOverlayOptions()
                .image(unviewableIndoorViews.get(0).getBitMap())
                .positionFromBounds(unviewableIndoorViews.get(0).getViewBounds());
        indoorViewOverlay = googleMap.addGroundOverlay(indoorViewOpts);
        hideIndoorView();

        // ensure previous and current positions have a known initial value
        currentLatPosition = initialPosLat;
        currentLongPosition = initialPosLong;

        // initialising position marker
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

    public List<IndoorView> loadIndoorViews(){
        List<IndoorView> hardcodedIndoorViews = new ArrayList<IndoorView>();

        // add Nucleus building
        LatLng nucleusSouthEast = new LatLng(55.9227834,-3.1746385);
        LatLng nucleusNorthEast = new LatLng(55.9233976,-3.1738120);
        List<LatLng> nucleusPolygon = Arrays.asList(new LatLng(55.9227952,-3.1746006), // polygon to determine if map available
                new LatLng(55.9227952,-3.1738120),
                new LatLng(55.9233137,-3.1738120),
                new LatLng(55.9233137,-3.1746006),
                new LatLng(55.922795,-3.1746006));

        hardcodedIndoorViews.add(new IndoorView("nucleus_lg", nucleusSouthEast, nucleusNorthEast, nucleusPolygon,
                BitmapDescriptorFactory.fromResource(R.drawable.nucleus_lg)));
        hardcodedIndoorViews.add(new IndoorView("nucleus_g", nucleusSouthEast, nucleusNorthEast, nucleusPolygon,
                BitmapDescriptorFactory.fromResource(R.drawable.nucleus_g)));
        hardcodedIndoorViews.add(new IndoorView("nucleus_1f", nucleusSouthEast, nucleusNorthEast, nucleusPolygon,
                BitmapDescriptorFactory.fromResource(R.drawable.nucleus_f1)));
        hardcodedIndoorViews.add(new IndoorView("nucleus_2f", nucleusSouthEast, nucleusNorthEast, nucleusPolygon,
                BitmapDescriptorFactory.fromResource(R.drawable.nucleus_f2)));
        hardcodedIndoorViews.add(new IndoorView("nucleus_3f", nucleusSouthEast, nucleusNorthEast, nucleusPolygon,
                BitmapDescriptorFactory.fromResource(R.drawable.nucleus_f3)));

        // add Noreen and Kenneth Murray library
        LatLng librarySouthEast = new LatLng(55.9227289, -3.1751799);
        LatLng libraryNorthEast = new LatLng(55.9230537, -3.1747692);
        List<LatLng> libraryPolygon = Arrays.asList(new LatLng(55.9227931, -3.1751766), // polygon to determine if map available
                new LatLng(55.9227931,-3.1747910),
                new LatLng(55.9230569, -3.1747910),
                new LatLng(55.9230569,-3.1751766),
                new LatLng(55.9227931, -3.1751766));

        hardcodedIndoorViews.add(new IndoorView("library_g", librarySouthEast, libraryNorthEast, libraryPolygon,
                BitmapDescriptorFactory.fromResource(R.drawable.library_g)));
        hardcodedIndoorViews.add(new IndoorView("library_1f", librarySouthEast, libraryNorthEast, libraryPolygon,
                BitmapDescriptorFactory.fromResource(R.drawable.library_1f)));
        hardcodedIndoorViews.add(new IndoorView("library_2f", librarySouthEast, libraryNorthEast, libraryPolygon,
                BitmapDescriptorFactory.fromResource(R.drawable.library_2f)));
        hardcodedIndoorViews.add(new IndoorView("library_3f", librarySouthEast, libraryNorthEast, libraryPolygon,
                BitmapDescriptorFactory.fromResource(R.drawable.library_3f)));

        return hardcodedIndoorViews;
    }

    public void updateMarker(float latDist, float longDist, float markerRotation) {
        // store last marker position
        float previousLatPosition = currentLatPosition;
        float previousLongPosition = currentLongPosition;

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

    public void toggleMapMode() {
        if (normalMap) { // change to satellite from normal map view
            googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            normalMap = false;
        } else { // change to normal from satellite map view
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            normalMap = true;
        }
    }

    public void updateViewableIndoorViews() {

        // updating currently viewable indoor views
        List<IndoorView> newViewableViews = new ArrayList<IndoorView>();;
        List<IndoorView> newUnviewableViews = new ArrayList<IndoorView>();;
        // gathering newly viewable views if location is now inside a view's polygon
        for (IndoorView view: unviewableIndoorViews) {
            if (view.isLocationInView(new LatLng(currentLatPosition, currentLongPosition))) {
                newViewableViews.add(view);
            }
        }
        // gathering now unviewable views if location is outside of a view's polygon
        for (IndoorView view: viewableIndoorViews) {
            if (!view.isLocationInView(new LatLng(currentLatPosition, currentLongPosition))) {
                newUnviewableViews.add(view);
            }
        }
        // swapping locations of newly viewable and unviewable views
        unviewableIndoorViews.removeAll(newViewableViews);
        viewableIndoorViews.addAll(newViewableViews);
        viewableIndoorViews.removeAll(newUnviewableViews);
        unviewableIndoorViews.addAll(newUnviewableViews);

        // update currently shown view current view is gone and another view is available
        if (!viewableIndoorViews.contains(currentIndoorView) && viewableIndoorViews.size() > 0) {
            currentIndoorView = viewableIndoorViews.get(0);
            updateCurrentIndoorView(currentIndoorView);
        }
    }

    public void showNextIndoorView() {
        int viewIdx = viewableIndoorViews.indexOf(currentIndoorView);
        if (viewIdx + 1 < viewableIndoorViews.size()) {
            currentIndoorView = viewableIndoorViews.get(viewIdx + 1);
        }
        updateCurrentIndoorView(currentIndoorView);
    }

    public void showPrevIndoorView() {
        int viewIdx = viewableIndoorViews.indexOf(currentIndoorView);
        if (viewIdx - 1 >= 0) {
            currentIndoorView = viewableIndoorViews.get(viewIdx - 1);
        }
        updateCurrentIndoorView(currentIndoorView);
    }

    public boolean isNextIndoorView() {
        int viewIdx = viewableIndoorViews.indexOf(currentIndoorView);
        if (viewIdx + 1 < viewableIndoorViews.size()) {
            return true;
        }
        return false;
    }

    public boolean isPrevIndoorView() {
        int viewIdx = viewableIndoorViews.indexOf(currentIndoorView);
        if (viewIdx - 1 >= 0) {
            return true;
        }
        return false;
    }

    public boolean isIndoorViewViewable() {
        return viewableIndoorViews.size() > 0;
    }

    public String getIndoorViewID() { return currentIndoorView.getID(); }

    public float[] getEstimatedGNSS() { return new float[] {currentLatPosition, currentLongPosition}; }

    private void updateCurrentIndoorView(IndoorView view) {
        indoorViewOverlay.setPositionFromBounds(view.getViewBounds());
        indoorViewOverlay.setImage(view.getBitMap());
    }
    
    public void hideIndoorView() { indoorViewOverlay.setVisible(false); }
    public void showIndoorView() { indoorViewOverlay.setVisible(true); }
}
