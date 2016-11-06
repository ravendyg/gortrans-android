package info.nskgortrans.maps.DataClasses;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by me on 6/11/16.
 */
public class Route
{
  int type;
  ArrayList<Way> ways;

  public Route(JSONObject input)
    throws JSONException
  {
    type = input.getInt("type") + 1;
    ways = new ArrayList<>(Arrays.asList(new Way[0]));

    JSONArray _ways = input.getJSONArray("ways");

    for (int i = 0; i < _ways.length(); i++)
    {
      try
      {
        ways.add( new Way(_ways.getJSONObject(i)) );
      }
      catch (JSONException err)
      {
      }
    }
  }
}
