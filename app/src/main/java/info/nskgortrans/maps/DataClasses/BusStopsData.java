package info.nskgortrans.maps.DataClasses;

import java.io.Serializable;
import java.util.ArrayList;

public class BusStopsData implements Serializable {
    private String code;
    private ArrayList<StopInfo> stops;

    BusStopsData(String code, ArrayList<StopInfo> stops) {
        this.code = code;
        this.stops = stops;
    }

    public String getCode() {
        return code;
    }

    public ArrayList<StopInfo> getStops() {
        return stops;
    }
}
