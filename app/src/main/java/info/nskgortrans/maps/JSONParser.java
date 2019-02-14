package info.nskgortrans.maps;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import info.nskgortrans.maps.DataClasses.BusInfo;
import info.nskgortrans.maps.DataClasses.UpdateParcel;
import info.nskgortrans.maps.DataClasses.Way;
import info.nskgortrans.maps.DataClasses.WayGroup;

/**
 * Created by me on 6/11/16.
 */
public class JSONParser {
    private static final String LOG_TAG = "JSON parser";

    public static long getTimestamp(JSONObject input) {
        long timestamp = 0;
        try {
            timestamp = input.getLong("timestamp");
        } catch (JSONException err) {
        }

        return timestamp;
    }

    public static ArrayList<WayGroup> getWayGroups(JSONObject input)
            throws JSONException {
        ArrayList<WayGroup> wayGroups = new ArrayList<>(Arrays.asList(new WayGroup[0]));

        JSONArray routes = input.getJSONArray("routes");

        for (int i = 0; i < routes.length(); i++) {
            try {
                JSONObject rawRoute = routes.getJSONObject(i);
                int type = rawRoute.getInt("type");

                WayGroup group = new WayGroup(type);

                JSONArray ways = rawRoute.getJSONArray("ways");
                for (int j = 0; j < ways.length(); j++) {
                    JSONObject way = ways.getJSONObject(j);
                    group.addWay(new Way(way, type));
                }

                wayGroups.add(group);
            } catch (JSONException err) {
                Log.e(LOG_TAG, "error", err);
            }
        }

        return wayGroups;
    }

    public static HashMap<String, HashSet<String>> extractBusStops(JSONObject input) {
        HashMap<String, HashSet<String>> out = new HashMap<String, HashSet<String>>();
        Iterator<String> vehicles = input.keys();
        while (vehicles.hasNext()) {
            try {
                String _id = vehicles.next();
                JSONObject stopsHolder = input.getJSONObject(_id);
                HashSet<String> stop = new HashSet<String>();
                Iterator<String> stops = stopsHolder.keys();
                while (stops.hasNext()) {
                    String stopsCode = stops.next();
                    stop.add(stopsCode);
                }
                out.put(_id, stop);
            } catch (JSONException err) {
                Log.e(LOG_TAG, "error", err);
            }
        }

        return out;
    }

    public static ArrayList<GeoPoint> parseRoutePoints(JSONArray points)
            throws JSONException {
        ArrayList<GeoPoint> out = new ArrayList<>(Arrays.asList(new GeoPoint[0]));
        for (int i = 0; i < points.length(); i++) {
            JSONObject point = points.getJSONObject(i);
            out.add(new GeoPoint(Double.parseDouble(point.getString("lat")), Double.parseDouble(point.getString("lng"))));
        }
        return out;
    }

    public static HashMap<String, UpdateParcel> parseCreatedBus(JSONObject ob) {
        HashMap<String, UpdateParcel> out = new HashMap<>();
        try {
            Iterator<String> keys = ob.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject busInfo = ob.getJSONObject(key);

                UpdateParcel parcel = new UpdateParcel();
                parcel.add = parseBusDict(busInfo);

                out.put(key, parcel);
            }
        } catch (JSONException err) {
            Log.e(LOG_TAG, "parseCreatedBus", err);
        }

        return out;
    }

    public static HashMap<String, UpdateParcel> parseUpdatedBus(JSONObject data) {
        HashMap<String, UpdateParcel> out = new HashMap<>();
        try {
            Iterator<String> keys = data.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject busInfo = data.getJSONObject(key);

                UpdateParcel parcel = new UpdateParcel();
                if (busInfo.has("add")) {
                    parcel.add = parseBusDict(busInfo.getJSONObject("add"));
                }
                if (busInfo.has("remove")) {
                    parcel.remove = parseStringList(busInfo.getJSONArray("remove"));
                }
                if (busInfo.has("update")) {
                    parcel.update = parseBusDict(busInfo.getJSONObject("update"));
                }
                if (busInfo.has("reset")) {
                    parcel.reset = parseBusDict(busInfo.getJSONObject("reset"));
                }

                out.put(key, parcel);
            }
        } catch (JSONException err) {
            Log.e(LOG_TAG, "parseCreatedBus", err);
        }

        return out;
    }

    private static String[] parseStringList(JSONArray arr) {
        String[] out = {};
        try {
            int len = arr.length();
            out = new String[len];
            for (int i = 0; i < len; i++) {
                out[i] = arr.getString(i);
            }
        } catch (JSONException err) {
            Log.e(LOG_TAG, "parseStringList", err);
        }

        return out;
    }

    private static HashMap<String, BusInfo> parseBusDict(JSONObject ob) {
        HashMap<String, BusInfo> out = new HashMap<>();
        try {
            Iterator<String> keys = ob.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject bus = ob.getJSONObject(key);
                BusInfo busInfo = jsonToBusInfo(bus);
                if (busInfo != null) {
                    out.put(key, busInfo);
                }
            }
        } catch (JSONException err) {
            Log.e(LOG_TAG, "parseBusDict", err);
        }

        return out;
    }

    private static BusInfo jsonToBusInfo(JSONObject ob) {
        BusInfo busInfo = null;
        try {
            int azimuth = ob.getInt("azimuth");
            String direction = ob.getString("direction");
            int graph = ob.getInt("graph");
            int id_typetr = ob.getInt("id_typetr");
            double lat = ob.getDouble("lat");
            double lng = ob.getDouble("lng");
            String marsh = ob.getString("marsh");
            int speed = ob.getInt("speed");
            String time_nav = ob.getString("time_nav");
            String title = ob.getString("title");

            busInfo = new BusInfo(azimuth, direction, graph, id_typetr,
                    lat, lng, marsh, speed, time_nav, title);
        } catch (JSONException err) {
            Log.e(LOG_TAG, "jsonToBusInfo", err);
        }

        return busInfo;
    }
}
