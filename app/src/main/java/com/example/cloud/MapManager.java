package com.example.cloud;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class used to manage live position and indoor building views of a {@link GoogleMap} object.
 *
 * Key features include tracking/updating current position and trail on map,
 * displaying satellite and normal map views, showing/hiding indoorViews, detecting if a location
 * is inside an indoorView, and showing/hiding building polygons on map.
 *
 * This class is generally stateless and should have it's state handled by another layer. One exception to this
 * is that it tracks all available {@link IndoorView}s and displays a currently selected {@link IndoorView}.
 * @see com.example.cloud.fragments.RecordingFragment for usage with states.
 *
 * @author Ryan Wiebe
 */
public class MapManager {
    // Google maps objects
    private GoogleMap googleMap;
    private Marker locationMarker; // user position marker
    private Polyline pathLine; // line representing the user's path
    private GroundOverlay indoorViewOverlay;
    private List<Polygon> buildingPolys;

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

    /**
     * MapManager constructor.
     *
     * This firsts initialised google map. Hardcoded maps in the form of {@link IndoorView} objects are then loaded ]
     * and initialised as a {@link GroundOverlay} object on the map. The position marker and trail are then initialised
     * and loaded onto the map in the form of {@link Marker} and {@link Polyline} objects.
     *
     *
     * @param mMap    {@link GoogleMap} object to manage.
     * @param initialPosLat initial latitude position to use for location.
     * @param initialPosLng initial longitude position to use for location.
     * @param markerDrawable drawable vector to use to mark the current position on the map.
     * @param markerColor RGB color code for the position marker
     * @param lineColor RGB color code for the line used to mark the path taken.
     * @param polyColor RGB color code for the building polygons on the map
     */
    public MapManager(GoogleMap mMap, float initialPosLat, float initialPosLng, Drawable markerDrawable, int markerColor, int lineColor, int polyColor) {
        // initialise googleMap
        googleMap = mMap;
        googleMap.getUiSettings().setScrollGesturesEnabled(true); // enable scroll gesture so other parts of map can be viewed while recording

        // set initial map to satellite mode
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        // indoor map setup
        // initialised indoor view lists and poly list
        buildingPolys = new ArrayList<>();
        viewableIndoorViews = new ArrayList<>(); // set to empty until next position update
        unviewableIndoorViews = loadIndoorViews(polyColor); // load hardcoded indoor maps into unviewable views

        // initialise indoor GroundViewOverlay using first image in unviewableIndoorMaps
        GroundOverlayOptions indoorViewOpts = new GroundOverlayOptions()
                .image(unviewableIndoorViews.get(0).getBitMap())
                .positionFromBounds(unviewableIndoorViews.get(0).getViewBounds());
        indoorViewOverlay = googleMap.addGroundOverlay(indoorViewOpts);
        hideIndoorView(); // initially hide indoor view

        // ensure current position is a known initial value
        currentLatPosition = initialPosLat;
        currentLongPosition = initialPosLng;

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

    /**
     * Generates and returns a list of hardcoded {@link indoorView}s while also adding corresponding building {@link Polygon}s to the map
     *
     * @param polyColor Color to use for the building polygons on the map
     * @return {@link List<IndoorView>} containing all hardcoded {@link indoorViews}
     */
    public List<IndoorView> loadIndoorViews(int polyColor){
        List<IndoorView> hardcodedIndoorViews = new ArrayList<IndoorView>();

        // add Nucleus building
        LatLng nucleusSouthEast = new LatLng(55.9227834,-3.1746385);
        LatLng nucleusNorthEast = new LatLng(55.9233976,-3.1738120);
        List<LatLng> nucleusPolygon = Arrays.asList(new LatLng(55.9227952,-3.1746006), // polygon to determine if map available
                new LatLng(55.9227952,-3.1741111),
                new LatLng(55.9228795,-3.1738120),
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


        // adding polygons of each building to map
        PolygonOptions nucleusPolyOpts = new PolygonOptions().addAll(nucleusPolygon)
                .visible(false)
                .fillColor(polyColor - 0xBF000000) // subtract BF from MSBs to get 0.25 opacity
                .strokeColor(polyColor - 0x7F000000); // subtract 7F from MSBs to get 0.5 opacity
        buildingPolys.add(googleMap.addPolygon(nucleusPolyOpts));

        PolygonOptions libraryPolyOpts = new PolygonOptions().addAll(libraryPolygon)
                .visible(false)
                .fillColor(polyColor - 0xBF000000) // subtract BF from MSBs to get 0.25 opacity
                .strokeColor(polyColor - 0x7F000000); // subtract 7F from MSBs to get 0.5 opacity
        buildingPolys.add(googleMap.addPolygon(libraryPolyOpts));

        return hardcodedIndoorViews;
    }

    /**
     * Updates the current position marker to a newly calculated position using a latitude and longitude distance offset.
     *
     * @param latDist new latitude distance offset in meters
     * @param latDist new longitude distance offset in meters
     */
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

    /**
     * Sets the current {@link GoogleMap} map type to satellite.
     */
    public void showSatelliteMap() {
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    }

    /**
     * Sets the current {@link GoogleMap} map type to normal.
     */
    public void showNormalMap() {
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    /**
     * Displays all building polygons on the {@link GoogleMap}.
     */
    public void showBuildingPolygons() {
        for (Polygon poly: buildingPolys) {
            poly.setVisible(true);
        }
    }

    /**
     * Hides all building polygons on the {@link GoogleMap}.
     */
    public void hideBuildingPolygons() {
        for (Polygon poly: buildingPolys) {
            poly.setVisible(false);
        }
    }

    /**
     * Using the current location, determines which {@link IndoorView}s are available for viewing.
     * If the currently viewed {@link IndoorView} is no longer visible after update, use the next available view.
     */
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

    /**
     * Show the next {@link IndoorView} on the {@link GoogleMap} if available.
     */
    public void showNextIndoorView() {
        int viewIdx = viewableIndoorViews.indexOf(currentIndoorView);
        if (viewIdx + 1 < viewableIndoorViews.size()) {
            currentIndoorView = viewableIndoorViews.get(viewIdx + 1);
        }
        updateCurrentIndoorView(currentIndoorView);
    }

    /**
     * Show the previous {@link IndoorView} on the {@link GoogleMap} if available.
     */
    public void showPrevIndoorView() {
        int viewIdx = viewableIndoorViews.indexOf(currentIndoorView);
        if (viewIdx - 1 >= 0) {
            currentIndoorView = viewableIndoorViews.get(viewIdx - 1);
        }
        updateCurrentIndoorView(currentIndoorView);
    }

    /**
     * Determines if there is a next {@link IndoorView} available.
     *
     * @return True if an {@link IndoorView} is available next, false otherwise.
     */
    public boolean isNextIndoorView() {
        int viewIdx = viewableIndoorViews.indexOf(currentIndoorView);
        if (viewIdx + 1 < viewableIndoorViews.size()) {
            return true;
        }
        return false;
    }

    /**
     * Determines if there is a previous {@link IndoorView} available.
     *
     * @return True if an {@link IndoorView} is available previously, false otherwise.
     */
    public boolean isPrevIndoorView() {
        int viewIdx = viewableIndoorViews.indexOf(currentIndoorView);
        if (viewIdx - 1 >= 0) {
            return true;
        }
        return false;
    }

    /**
     * Determines if there are any available {@link IndoorView}s to view.
     *
     * @return True if an {@link IndoorView} is available for viewing, false otherwise.
     */
    public boolean isIndoorViewViewable() {
        return viewableIndoorViews.size() > 0;
    }

    /**
     * Gets the ID associated with the currently viewed {@link IndoorView}
     *
     * @return string containing the {@link IndoorView} ID.
     */
    public String getIndoorViewID() { return currentIndoorView.getID(); }

    /**
     * Gets the currently estimated latitude and longitude position.
     *
     * @return Current latitude and longitude position estimate.
     */
    public float[] getEstimatedLatLng() { return new float[] {currentLatPosition, currentLongPosition}; }

    /**
     * Hides the currently selected {@link IndoorView} using a {@link GroundOverlay}.
     */
    public void hideIndoorView() { indoorViewOverlay.setVisible(false); }

    /**
     * Shows the currently selected {@link IndoorView} using a {@link GroundOverlay}.
     */
    public void showIndoorView() { indoorViewOverlay.setVisible(true); }

    /**
     * Updates the currently displayed {@link IndoorView} on the {@link GoogleMap}.
     *
     * @param view  The view to now display as a {@link GroundOverlay}.
     */
    private void updateCurrentIndoorView(IndoorView view) {
        indoorViewOverlay.setPositionFromBounds(view.getViewBounds());
        indoorViewOverlay.setImage(view.getBitMap());
    }

}
