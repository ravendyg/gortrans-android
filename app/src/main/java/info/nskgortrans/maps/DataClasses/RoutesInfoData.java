package info.nskgortrans.maps.DataClasses;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info.nskgortrans.maps.Constants;

public class RoutesInfoData implements Serializable {
    private Map<Integer, List<WayData>> routes;
    private String hash;

    public RoutesInfoData(JSONObject input) {
        routes = new HashMap<>();

        try {
            JSONArray data = input.getJSONArray("data");
            this.hash = input.getString("hash");
            for (int i = 0; i < Constants.typeCount; i++) {
                JSONObject routeGroup = data.getJSONObject(i);
                int type = routeGroup.getInt("t");
                List<WayData> waysData = new ArrayList<>();
                JSONArray ways = routeGroup.getJSONArray("w");
                for (int j = 0; j < ways.length(); j++) {
                    try {
                        JSONObject way = ways.getJSONObject(j);
                        WayData wayData = new WayData(way, type);
                        waysData.add(wayData);
                    } catch (Exception er) {
                        er.printStackTrace();
                    }
                }
                routes.put(type, waysData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<WayData> getWaysByType(int type) {
        if (routes.containsKey(type)) {
            return routes.get(type);
        } else {
            return new ArrayList<>();
        }
    }

    public String getHash() {
        return hash;
    }
}
