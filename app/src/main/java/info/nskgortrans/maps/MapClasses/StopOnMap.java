package info.nskgortrans.maps.MapClasses;

import org.osmdroid.views.overlay.Marker;

import java.util.HashSet;

import info.nskgortrans.maps.DataClasses.StopInfo;

public class StopOnMap {
    private final String id;
    private final StopInfo stopInfo;
    private final HashSet<String> buses;
    private final Marker marker;

    public StopOnMap(final StopInfo stopInfo, final Marker marker) {
        this.id = stopInfo.getId();
        this.stopInfo = stopInfo;
        this.marker = marker;
        buses = new HashSet<>();
    }

    public String getId() {
        return id;
    }

    public Marker getMarker() {
        return marker;
    }

    public int addBus(String code) {
        buses.add(code);
        return buses.size();
    }

    public int removeBus(String code) {
        buses.remove(code);
        return buses.size();
    }
}
