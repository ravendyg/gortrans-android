package info.nskgortrans.maps.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import info.nskgortrans.maps.MainActivity;
import info.nskgortrans.maps.R;

/**
 * Created by me on 4/12/16.
 */

public class EmptyFragment extends Fragment
{
  private boolean loaded = false;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.empty_fragment, container, false);

    ((MainActivity) getActivity()).showBtns(true);

    return view;
  }

  @Override
  public void onDestroy()
  {
    super.onDestroy();
    ((MainActivity) getActivity()).hideBtns();
  }
}
