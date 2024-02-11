package com.example.cloud;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.PolyUtil;

import java.util.List;

public class IndoorView {
    private LatLngBounds indoorPosition;
    private List<LatLng> indoorPolygon;
    private BitmapDescriptor indoorBitmap;

    IndoorView(LatLng anchorSouthWest, LatLng anchorNorthEast, List<LatLng> indoorPolygon, BitmapDescriptor indoorBitmap) {
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
}
