package info.nskgortrans.maps.DataClasses;

import org.osmdroid.views.overlay.Marker;

import java.util.HashSet;

/**
 * Created by me on 12/02/17.
 */

public class StopMarker {
    private Marker marker;
    private HashSet<String> buses;

    public StopMarker(Marker marker, String busCode) {
        this.marker = marker;
        this.buses = new HashSet<>();
        this.buses.add(busCode);
    }

    public Marker getMarker() {
        return marker;
    }

    public void addBus(String busCode) {
        this.buses.add(busCode);
    }

    public int removeBus(String busCode) {
        this.buses.remove(busCode);
        return this.buses.size();
    }
}
