package info.nskgortrans.maps.Data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import info.nskgortrans.maps.DataClasses.StopInfo;

public class TrassData implements Serializable {
    private String code;
    private List<WayPointData> waypoints;
    private List<StopInfo> stops;
    private String hash;

    public TrassData(String code, JSONObject input) {
        this.code = code;
        try {
            JSONArray data = input.getJSONArray("data");
            this.hash = input.getString("hash");
            waypoints = new ArrayList<>();
            stops = new ArrayList<>();
            for (int i = 0; i < data.length(); i++) {
                JSONObject wayPoint = data.getJSONObject(i);
                double lat = wayPoint.getDouble("t");
                double lng = wayPoint.getDouble("g");
                WayPointData wayPointData = new WayPointData(lat, lng);
                waypoints.add(wayPointData);
                if (wayPoint.has("n") && wayPoint.has("i")) {
                    String name = wayPoint.getString("n");
                    String id = wayPoint.getString("i");
                    StopInfo stopInfo = new StopInfo(id, name, lat, lng);
                    stops.add(stopInfo);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getCode() {
        return code;
    }

    public String getHash() {
        return hash;
    }

    public List<StopInfo> getStops() {
        return stops;
    }

    public List<WayPointData> getWaypoints() {
        return waypoints;
    }
}
