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

import info.nskgortrans.maps.MainActivity;
import info.nskgortrans.maps.R;

import static android.R.attr.width;

/**
 * Created by me on 4/12/16.
 */

public class BusSearchFragment extends Fragment
{
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


    return view;
  }
}
