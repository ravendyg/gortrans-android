package info.nskgortrans.maps.Data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TrassData implements Serializable {
    private String code;
    private List<WayPointData> waypoints;
    private List<StopData> stops;

    public TrassData(String code, JSONArray input) {
        this.code = code;
        waypoints = new ArrayList<>();
        stops = new ArrayList<>();
        for (int i = 0; i < input.length(); i++) {
            try {
                JSONObject wayPoint = input.getJSONObject(i);
                double lat = wayPoint.getDouble("t");
                double lng = wayPoint.getDouble("g");
                String name = wayPoint.getString("n");
                int id = wayPoint.getInt("i");
                WayPointData wayPointData = new WayPointData(lat, lng);
                waypoints.add(wayPointData);
                if (name != null) {
                    StopData stopData = new StopData(id, name, wayPointData);
                    stops.add(stopData);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getCode() {
        return code;
    }

    public List<StopData> getStops() {
        return stops;
    }

    public List<WayPointData> getWaypoints() {
        return waypoints;
    }
}
