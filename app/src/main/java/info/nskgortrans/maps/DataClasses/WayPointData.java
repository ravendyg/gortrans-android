package info.nskgortrans.maps.DataClasses;

import java.io.Serializable;

public class WayPointData implements Serializable {
    private double lat;
    private double lng;

    public WayPointData(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }
}
