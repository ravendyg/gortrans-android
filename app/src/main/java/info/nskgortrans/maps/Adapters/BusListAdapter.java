package info.nskgortrans.maps.Adapters;

/**
 * Created by me on 22/01/17.
 */

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import info.nskgortrans.maps.DataClasses.BusListElement;
import info.nskgortrans.maps.DataClasses.WayGroupElement;
import info.nskgortrans.maps.DataClasses.Ways;
import info.nskgortrans.maps.R;

//package info.nskgortrans.maps.Adapters;

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

public class BusListAdapter extends BaseAdapter
{
  private Context ctx;
  private LayoutInflater inflater;
  private ArrayList<BusListElement> data;

  public BusListAdapter(Context context, ArrayList<BusListElement> _data)
  {
    ctx = context;
    data = _data;
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

  public BusListElement getElem(int position)
  {
    return (BusListElement) getItem(position);
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

    BusListElement element = getElem(position);

    view = inflater.inflate(R.layout.bus_list_item, parent, false);
    TextView name = (TextView) view.findViewById(R.id.busName);
    name.setText(element.name);
    name.setTextColor(ContextCompat.getColor(ctx, element.color));
    ImageView icon = (ImageView) view.findViewById(R.id.busIcon);
    icon.setImageResource(element.icon);

    return view;
  }
}

