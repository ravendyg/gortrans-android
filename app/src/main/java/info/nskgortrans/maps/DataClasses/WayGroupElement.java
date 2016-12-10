package info.nskgortrans.maps.DataClasses;

import org.json.JSONException;
import org.json.JSONObject;

import static android.R.attr.type;
import static android.R.id.input;
import static android.R.id.toggle;

/**
 * Created by me on 9/12/16.
 */

public class WayGroupElement
{
  public int type;
  public String name;
  public String marsh;
  public int children;
  public boolean opened;

  public WayGroupElement(Way way, int _type)
  {
    name = way.name;
    type = _type;
    marsh = way.marsh;
    children = 0;
    opened = true;
  }

  public WayGroupElement(String _name, int _type, int _children)
  {
    name = _name;
    type = _type;
    marsh = "";
    children = _children;
    opened = true;
  }
}
