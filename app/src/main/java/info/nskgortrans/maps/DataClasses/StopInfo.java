package info.nskgortrans.maps.DataClasses;

import org.osmdroid.views.overlay.Marker;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by me on 29/01/17.
 */

public class StopInfo implements Serializable {
    private String id;
    private double lat;
    private double lng;
    private String name;

    public StopInfo(String id, String name, double lat, double lng) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }
}
