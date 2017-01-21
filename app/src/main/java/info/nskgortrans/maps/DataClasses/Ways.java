package info.nskgortrans.maps.DataClasses;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * Created by me on 10/12/16.
 */

public class Ways
{
  // data is a source, filteredData -representation
  public ArrayList<WayGroupElement> filteredData;
  private ArrayList<WayGroup> data;
  private String filter = "";

  private boolean[] opened = {true, true, true, true};

  public Ways(ArrayList<WayGroup> _data)
  {
    data = _data;
    filteredData = new ArrayList<WayGroupElement>(Arrays.asList(new WayGroupElement[0]));

    filterData("");
  }

  public void filterData(String input)
  {
    // remember for toggling or restore
    if (input != null)
    {
      filter = input;
    }
    else
    {
      input = filter;
    }

    filteredData.clear();
    if ( !input.isEmpty() )
    {
      // flat headers and data into single array list
      for (int i = 0; i < data.size(); i++)
      {
        ArrayList<Way> temp = data.get(i).getMatching(input);

        if (temp.size() > 0)
        { // there are vehicles matching input in the group
          int type;
          WayGroupElement header;
          // headers
          switch (i)
          {
            case 0:
              header = new WayGroupElement("Автобусы", 0, 1);
              type = 1;
            break;

            case 1:
              header = new WayGroupElement("Троллейбусы", 0, 2);
              type = 2;
            break;

            case 2:
              header = new WayGroupElement("Трамваи", 0, 3);
              type = 3;
            break;

            default:
              header = new WayGroupElement("Маршрутки", 0, 8);
              type = 8;
          }
          filteredData.add(header);

          if (opened[i])
          {
            header.opened = true;
            populate(temp, type);
          }
          else
          {
            header.opened = false;
          }
        }
      }
    }
  }

  public void toggle(int index)
  {
    int k;
    switch (index)
    {
      case 1: k = 0; break;
      case 2: k = 1; break;
      case 3: k = 2; break;
      default: k = 3; break;
    }
    opened[k] = !opened[k];
    filterData(null);
  }

  private void populate(ArrayList<Way> temp, int type)
  {
    // content
    for (int j = 0; j < temp.size(); j++)
    {
      filteredData.add(new WayGroupElement(temp.get(j), type));
    }
  }
}
