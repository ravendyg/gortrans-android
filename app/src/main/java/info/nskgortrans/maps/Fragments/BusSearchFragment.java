package info.nskgortrans.maps.Fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;

import info.nskgortrans.maps.Adapters.WaysAdapter;
import info.nskgortrans.maps.DataClasses.WayGroup;
import info.nskgortrans.maps.DataClasses.WayGroupElement;
import info.nskgortrans.maps.DataClasses.Ways;
import info.nskgortrans.maps.MainActivity;
import info.nskgortrans.maps.R;

/**
 * Created by me on 4/12/16.
 */

public class BusSearchFragment extends Fragment
{
  private Ways wayGroups;

  private WaysAdapter adapter;
  private ListView list;

  private EditText searchInput;
  private Button _clearSearchInput;

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
    holder.setLayoutParams(
      new LinearLayout.LayoutParams( width, LinearLayout.LayoutParams.MATCH_PARENT )
    );


    wayGroups = new Ways((ArrayList<WayGroup>) getArguments().getSerializable("ways"));

    adapter = new WaysAdapter(getContext(), wayGroups);
    list = (ListView) view.findViewById(R.id.wayGroups);
    list.setAdapter(adapter);

    searchInput = (EditText) view.findViewById(R.id.search_bar_route);
    _clearSearchInput = (Button) view.findViewById(R.id.clear_search);

    searchInput.addTextChangedListener(
      new TextWatcher()
      {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s)
        {
          String input = s.toString();

          if (s.length() > 0)
          {
            _clearSearchInput.setVisibility(View.VISIBLE);
          }
          else
          {
            _clearSearchInput.setVisibility(View.INVISIBLE);
          }

          wayGroups.filterData(input);
          adapter.notifyDataSetChanged();
        }
      }
    );

    _clearSearchInput.setOnClickListener(
      new View.OnClickListener()
      {
        @Override
        public void onClick(View btn)
        {
          searchInput.setText("");
          btn.setVisibility(View.GONE);
        }
      }
    );



    list.setOnItemClickListener(
      new AdapterView.OnItemClickListener()
      {
        @Override
        public void onItemClick( AdapterView<?> adapterView, View view, int position, long id)
        {
          WayGroupElement elem = adapter.getElem(position);
          if (elem.children != 0)
          {
            wayGroups.toggle(elem.children);
            adapter.notifyDataSetChanged();
          }
        }
      }
    );

    return view;
  }
}
