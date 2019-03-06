package info.nskgortrans.maps.MapClasses;

import android.content.Context;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class BusMarker extends Marker {
    public BusMarker(MapView mapView) {
        super(mapView);
    }

    public BusMarker(MapView mapView, Context resourceProxy) {
        super(mapView, resourceProxy);
    }

    @Override
    public void setDefaultIcon() {
        try {
            super.setDefaultIcon();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
