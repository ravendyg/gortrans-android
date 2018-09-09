package info.nskgortrans.maps.Data;

import java.io.Serializable;

public class StopData implements Serializable {
    private int id;
    private String name;
    private WayPointData wayPointData;

    public StopData(int id, String name, WayPointData wayPointData) {
        this.id = id;
        this.name = name;
        this.wayPointData = wayPointData;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getLat() {
        return wayPointData.getLat();
    }

    public double getLng() {
        return wayPointData.getLng();
    }
}
