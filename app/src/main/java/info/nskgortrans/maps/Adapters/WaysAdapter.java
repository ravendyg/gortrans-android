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

  private Context ctx;
  private LayoutInflater inflater;
  private ArrayList<Way> data;

  public WaysAdapter(Context context, ArrayList<Way> _data)
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
  
  public Way getElem(int position)
  {
    return (Way) getItem(position);
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

    final Way element = getElem(position);

    if (view == null)
    {
      view = inflater.inflate(R.layout.adapter_way_group_item, parent, false);
    }

    TextView name = ((TextView) view.findViewById(R.id.busName));

    name.setText(element.name);

    return view;
  }

  @Override
  public int getViewTypeCount()
  {
    return 1;
  }

  @Override
  public int getItemViewType(int position)
  {
    return 0;
  }
}
