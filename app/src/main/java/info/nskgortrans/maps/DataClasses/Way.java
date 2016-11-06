package info.nskgortrans.maps.DataClasses;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by me on 6/11/16.
 */
public class Way
{
  public String marsh;
  public String name;
  public String stopb;
  public String stopa;

  public Way(JSONObject input)
    throws JSONException
  {
    marsh = input.getString("marsh");
    name  = input.getString("name");
    stopa = input.getString("stopa");
    stopb = input.getString("stopb");
  }
}
