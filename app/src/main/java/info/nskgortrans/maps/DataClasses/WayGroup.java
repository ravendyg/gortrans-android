package info.nskgortrans.maps.DataClasses;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by me on 4/12/16.
 */

public class WayGroup
{
  ArrayList<Way> ways;
  int type;

  public WayGroup(int _type)
  {
    type = _type;
    ways = new ArrayList<>(Arrays.asList(new Way[0]));
  }

  public void addWay(Way way)
  {
    ways.add(way);
  }
}
