package info.nskgortrans.maps.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import info.nskgortrans.maps.DataClasses.Way;
import info.nskgortrans.maps.DataClasses.WayGroup;
import info.nskgortrans.maps.DataClasses.WayGroupElement;
import info.nskgortrans.maps.DataClasses.Ways;
import info.nskgortrans.maps.R;

import static android.R.attr.data;
import static android.R.attr.name;
import static android.R.attr.type;

/**
 * Created by me on 4/12/16.
 */

public class WaysAdapter extends BaseAdapter
{
  private static final int TYPE_MAX_COUNT = 2;
  private static final int TYPE_HEADER = 0;
  private static final int TYPE_ITEM = 1;

  private Context ctx;
  private LayoutInflater inflater;
  private ArrayList<WayGroupElement> data;

  public WaysAdapter(Context context, Ways _data)
  {
    ctx = context;
    data = _data.filteredData;

    inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  @Override
  public int getCount()
  {
    return data.size();
  }

  @Override
  public Object getItem(int position)
  {
    return data.get(position);
  }
  
  public WayGroupElement getElem(int position)
  {
    return (WayGroupElement) getItem(position);
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

    final WayGroupElement element = getElem(position);

    if (view == null)
    {
      if (element.children > 0)
      {
        view = inflater.inflate(R.layout.adapter_way_group_header, parent, false);
      }
      else
      {
        view = inflater.inflate(R.layout.adapter_way_group_item, parent, false);
      }
    }

    TextView name;
    if (element.children > 0)
    {
      name = ((TextView) view.findViewById(R.id.wayGroupHeader));
      ImageView arrow = ((ImageView) view.findViewById(R.id.wayGroupHeaderArrow));
      if (element.opened)
      {
        arrow.setImageResource(R.drawable.ic_keyboard_arrow_down_black_48dp);
      }
      else
      {
        arrow.setImageResource(R.drawable.ic_keyboard_arrow_right_black_48dp);
      }
    }
    else
    {
      name = ((TextView) view.findViewById(R.id.busName));
    }

    name.setText(element.name);

    return view;
  }

  @Override
  public int getViewTypeCount()
  {
    return TYPE_MAX_COUNT;
  }

  @Override
  public int getItemViewType(int position)
  {
    WayGroupElement element = getElem(position);

    int out;

    if (element.children > 0)
    {
      return TYPE_HEADER;
    }
    else
    {
      return TYPE_ITEM;
    }
  }
}
