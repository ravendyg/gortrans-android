package info.nskgortrans.maps.MapClasses;

import android.content.Context;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

// hack to bypass NPE inside nMapViewRepository
public class StopMarker extends Marker {
    public StopMarker(MapView mapView) {
        super(mapView);
    }

    public StopMarker(MapView mapView, Context resourceProxy) {
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
