package info.nskgortrans.maps.DataClasses;

/**
 * Created by me on 22/01/17.
 */

public class BusListElement
{
  public String name;
  public String code;
  public int color;
  public int icon;

  public BusListElement(final String _name, final String _code, final int _color, final int _icon)
  {
    name = _name;
    code = _code;
    color = _color;
    icon = _icon;
  }
}
