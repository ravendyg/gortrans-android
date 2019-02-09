package info.nskgortrans.maps.DataClasses;

import org.osmdroid.views.overlay.Marker;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by me on 29/01/17.
 */

public class StopInfo implements Serializable {
    public String id;
    public double lat;
    public double lng;
    public String name;
    public HashSet<String> buses;


    public StopInfo(String _id, double _lat, double _lng) {
        id = _id;
        lat = _lat;
        lng = _lng;
        name = null;
        buses = new HashSet<String>();
    }

    public void setName(String _name) {
        name = _name;
    }

    public void setBus(String code) {
        buses.add(code);
    }

}
