package info.nskgortrans.maps.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Objects;

import info.nskgortrans.maps.DataClasses.WayGroup;
import info.nskgortrans.maps.R;

/**
 * Created by me on 4/12/16.
 */

public class WayGroupsAdapter extends BaseAdapter
{
  private Context ctx;
  private LayoutInflater inflater;
  private ArrayList<WayGroup> data, filteredData;
  private String filter = "";

  public WayGroupsAdapter(Context context, ArrayList<WayGroup> _data)
  {
    ctx = context;
    filteredData = data = _data;
    inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  @Override
  public int getCount()
  {
    return filteredData.size();
  }

  @Override
  public Object getItem(int position)
  {
    return filteredData.get(position);
  }

  @Override
  public long getItemId (int position)
  {
    return position;
  }

  @Override
  public View getView(int position, View _view, ViewGroup parent)
  {
    View view = _view;
Log.e("adapter", "getView");
    WayGroup element = (WayGroup) getItem(position);

    if (view == null)
    {
      if (true)//element.opened)
      {
        view = inflater.inflate(R.layout.adapter_way_group, parent, false);
      }
    }

    if (true) // element.opened)
    {
      String headerText;
      switch (element.type)
      {
        case 0:
          headerText = "Автобусы";
          break;

        case 1:
          headerText = "Троллейбусы";
          break;

        case 2:
          headerText = "Трамваи";
          break;

        default:
          headerText = "Маршрутки";
      }
      ((TextView) view.findViewById(R.id.wayGroupHeader)).setText(headerText);
      ((TextView) view.findViewById(R.id.wayGroupHeaderArrow)).setText(element.opened ? "opened" : "closed");
    }

    return view;
  }
}
