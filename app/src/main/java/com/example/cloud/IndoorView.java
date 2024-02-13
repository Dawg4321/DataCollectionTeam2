package com.example.cloud;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.PolyUtil;

import java.util.List;

/**
 * A class used to represent an indoor view which can be used within a {@link GoogleMap}.
 *
 * Contains boundary information for displaying the image, polygon information of indoor view location,
 * and bitmap for displaying the image as an {@link com.google.android.gms.maps.model.GroundOverlay}.
 *
 * @author Ryan Wiebe
 */
public class IndoorView {
    private String id; // indoor view id to display
    private LatLngBounds indoorPosition; // bounds used by GroundOverlay when rendering the image on the map
    private List<LatLng> indoorPolygon; // polygon of coordinates used to set view detection boundary
    private BitmapDescriptor indoorBitmap; // bitmap used by GroundOverlay to render the image

    /**
     * IndoorView constructor.
     *
     * @param id ID to display with the IndoorView.
     * @param anchorSouthWest Southwest anchor coordinate used for when display the IndoorView with a {@link GroundOverlay} object.
     * @param anchorNorthEast Northeast anchor coordinate used for when display the IndoorView with a {@link GroundOverlay} object.
     * @param indoorPolygon List of coordinates used to specify the IndoorView's detection area.
     * @param indoorBitmap Bitmap to display.
     *
     */
    IndoorView(String id, LatLng anchorSouthWest, LatLng anchorNorthEast, List<LatLng> indoorPolygon, BitmapDescriptor indoorBitmap) {
        this.id = id;
        this.indoorPosition = new LatLngBounds(anchorSouthWest, anchorNorthEast);
        this.indoorPolygon = indoorPolygon;
        this.indoorBitmap = indoorBitmap;
    }

    /**
     * Determines if a coordinate is located within the IndoorView's polygon.
     *
     * @param location the location to test.
     *
     * @return True if point is inside the polygon, otherwise false.
     */
    public boolean isLocationInView(LatLng location) {
        return PolyUtil.containsLocation(location, indoorPolygon, true);
    }

    /**
     * Determines if a coordinate is located within the IndoorView's polygon.
     *
     * @return The boundary coordinates used to display the image with a {@link GroundOverlay} object.
     */
    public LatLngBounds getViewBounds() {
        return indoorPosition;
    }

    /**
     * Gets the IndoorView's bitmap.
     *
     * @return The bitmap used to display the IndoorView with a {@link GroundOverlay} object.
     */
    public BitmapDescriptor getBitMap() { return indoorBitmap; }

    /**
     * Gets the ID associated with the IndoorView
     *
     * @return string of the IndoorView's ID
     */
    public String getID() { return id; }
}
