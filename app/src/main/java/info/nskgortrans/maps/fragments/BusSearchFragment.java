package info.nskgortrans.maps.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import info.nskgortrans.maps.Adapters.WayGroupsAdapter;
import info.nskgortrans.maps.DataClasses.WayGroup;
import info.nskgortrans.maps.MainActivity;
import info.nskgortrans.maps.R;

import static android.R.attr.theme;
import static android.R.attr.width;
import static android.R.id.list;

/**
 * Created by me on 4/12/16.
 */

public class BusSearchFragment extends Fragment
{
  private ArrayList<WayGroup> _wayGroups,
          wayGroups;

  private WayGroupsAdapter adapter;
  private ListView list;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.bus_search_menu, container, false);

    LinearLayout holder = (LinearLayout) view.findViewById(R.id.bus_search_menu);
    DisplayMetrics metric = getResources().getDisplayMetrics();
    float dpWidth = metric.widthPixels / metric.density;

    int width = dpWidth > 400 ? (int) (400 * metric.density) : metric.widthPixels;

    Log.e("size", ""+dpWidth);
    holder.setLayoutParams(new LinearLayout.LayoutParams(
            width, LinearLayout.LayoutParams.MATCH_PARENT
    ));

    ((MainActivity) getActivity()).hideBtns();

    wayGroups = (ArrayList<WayGroup>) getArguments().getSerializable("ways");
    _wayGroups = (ArrayList<WayGroup>) getArguments().getSerializable("ways");

    adapter = new WayGroupsAdapter(getContext(), wayGroups);
    list = (ListView) view.findViewById(R.id.wayGroups);
    list.setAdapter(adapter);

    return view;
  }

  private void copyWayGroups(String filter)
  {
    for (int i=0; i < _wayGroups.size(); i++)
    {
      wayGroups.get(i).setWays( _wayGroups.get(i).getMatching(filter) );
    }

    adapter.notifyDataSetChanged();
  }
}
