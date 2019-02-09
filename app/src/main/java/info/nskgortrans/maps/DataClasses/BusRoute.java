package info.nskgortrans.maps.DataClasses;

import org.osmdroid.util.GeoPoint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by me on 12/02/17.
 */

public class BusRoute implements Serializable {
    private ArrayList<GeoPoint> points = new ArrayList<>(Arrays.asList(new GeoPoint[0]));

    public BusRoute(final String routeStr) {
        // parse JSON
    }

    public ArrayList<GeoPoint> getPoints() {
        return points;
    }
}
