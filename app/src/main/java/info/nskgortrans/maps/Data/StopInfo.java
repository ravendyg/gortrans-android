package info.nskgortrans.maps.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

public class StopInfo implements Serializable {
    private StopData stopData;
    private HashSet<String> buses;

    public StopInfo(StopData stopData) {
        this.stopData = stopData;
        this.buses = new HashSet<>();
    }

    public int getId() {
        return stopData.getId();
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
