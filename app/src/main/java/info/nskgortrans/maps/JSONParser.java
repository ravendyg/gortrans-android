package info.nskgortrans.maps;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import info.nskgortrans.maps.DataClasses.Way;
import info.nskgortrans.maps.DataClasses.WayGroup;

/**
 * Created by me on 6/11/16.
 */
public class JSONParser
{
  public static long getTimestamp(JSONObject input)
  {
    long timestamp = 0;
    try
    {
      timestamp = input.getLong("timestamp");
    }
    catch (JSONException err)
    {
    }

    return timestamp;
  }

  public static ArrayList <WayGroup> getWayGroups(JSONObject input)
    throws JSONException
  {
    ArrayList <WayGroup> wayGroups = new ArrayList<>(Arrays.asList(new WayGroup[0]));

    JSONArray routes = input.getJSONArray("routes");

    for (int i = 0; i < routes.length(); i++)
    {
      try
      {
        JSONObject rawRoute = routes.getJSONObject(i);
        int type = rawRoute.getInt("type");

        WayGroup group = new WayGroup(type);

        JSONArray ways = rawRoute.getJSONArray("ways");
        for (int j = 0; j < ways.length(); j++)
        {
          JSONObject way = ways.getJSONObject(j);
          group.addWay( new Way(way) );
        }

        wayGroups.add(group);
      }
      catch (JSONException err)
      {
        Log.e("JSON parser", "error", err);
      }
    }

    return wayGroups;
  }
}
