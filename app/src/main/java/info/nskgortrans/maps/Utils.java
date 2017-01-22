package info.nskgortrans.maps;

/**
 * Created by me on 22/01/17.
 */

public class Utils
{
  public static String getTypeString(final int type)
  {
    switch (type)
    {
      case 1:
        return "Автобус";
      case 2:
        return "Троллейбус";
      case 3:
        return "Трамвай";
      default:
        return "Маршрутка";
    }
  }
}
