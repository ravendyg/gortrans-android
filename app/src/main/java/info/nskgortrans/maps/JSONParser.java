package info.nskgortrans.maps;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import info.nskgortrans.maps.DataClasses.Route;

/**
 * Created by me on 6/11/16.
 */
public class JSONParser
{
  public static long getTimestamp(JSONObject input, String prop)
  {
    long timestamp = 0;
    try
    {
      timestamp = input.getJSONObject(prop).getLong("timestamp");
    }
    catch (JSONException err)
    {
    }

    return timestamp;
  }

  public static ArrayList <Route> getRoutes(JSONObject input)
    throws JSONException
  {
    ArrayList <Route> listMarsh = new ArrayList<>(Arrays.asList(new Route[0]));

    JSONArray routes = input.getJSONObject("routes").getJSONArray("routes");

    for (int i = 0; i < routes.length(); i++)
    {
      try
      {
        listMarsh.add(new Route(routes.getJSONObject(i)));
      }
      catch (JSONException err)
      {}
    }

    return listMarsh;
  }
}
