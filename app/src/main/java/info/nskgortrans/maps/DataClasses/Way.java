package info.nskgortrans.maps.DataClasses;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import static android.R.id.input;

/**
 * Created by me on 6/11/16.
 */
public class Way
{
  public String marsh;
  public String name;
  public String stopb;
  public String stope;
  public String code;
  public int type;

  public Way(JSONObject input, int _type)
    throws JSONException
  {
    marsh = input.getString("marsh");
    name  = input.getString("name");
    stope = input.getString("stope");
    stopb = input.getString("stopb");

    type = _type;
    code = (type + 1 ) + "-" + marsh + "-W-" + name;
  }

  public Way(String[] props)
  {
    type = Integer.parseInt(props[0]);
    code = props[1];
    marsh = props[2];
    name = props[3];
    stopb = props[4];
    stope = props[5];
  }

  public Way(String _name)
  {
    marsh = "";
    name  = _name;
    stope = "";
    stopb = "";
  }

  public String serialize()
  {
    String out = "" + type + "|" + code + "|" + marsh + "|" + name + "|" + stopb + "|" + stope;
    return out;
  }

}
