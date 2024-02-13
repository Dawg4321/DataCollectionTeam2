package com.example.cloud;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.PolyUtil;

import java.util.List;

public class IndoorView {
    private String id; // indoor view id to display
    private LatLngBounds indoorPosition; // bounds used by GroundOverlay when rendering the image on the map
    private List<LatLng> indoorPolygon; // polygon used to set view detection boundary
    private BitmapDescriptor indoorBitmap; // bitmap used by GroundOverlay to render the image

    IndoorView(String id, LatLng anchorSouthWest, LatLng anchorNorthEast, List<LatLng> indoorPolygon, BitmapDescriptor indoorBitmap) {
        this.id = id;
        this.indoorPosition = new LatLngBounds(anchorSouthWest, anchorNorthEast);
        this.indoorPolygon = indoorPolygon;
        this.indoorBitmap = indoorBitmap;
    }

    public boolean isLocationInView(LatLng location) {
        return PolyUtil.containsLocation(location, indoorPolygon, true);
    }

    public LatLngBounds getViewBounds() {
        return indoorPosition;
    }

    public BitmapDescriptor getBitMap() { return indoorBitmap; }

    public String getID() { return id; }
}
