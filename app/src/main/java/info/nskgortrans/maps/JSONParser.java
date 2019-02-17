package info.nskgortrans.maps;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Iterator;

import info.nskgortrans.maps.DataClasses.BusInfo;
import info.nskgortrans.maps.DataClasses.UpdateParcel;

/**
 * Created by me on 6/11/16.
 */
public class JSONParser {
    private static final String LOG_TAG = "JSON parser";

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
            String graph = ob.getString("graph");
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
