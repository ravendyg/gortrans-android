package info.nskgortrans.maps;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.util.ArrayList;

import info.nskgortrans.maps.Adapters.WaysAdapter;
import info.nskgortrans.maps.DataClasses.WayGroup;
import info.nskgortrans.maps.DataClasses.WayGroupElement;
import info.nskgortrans.maps.DataClasses.Ways;

/**
 * Created by me on 21/01/17.
 */

public class SearchBusDialog
{
    private Dialog dialog;

    private Button clearSearchBtn;
    private EditText searchInput;

    private Ways wayGroups;
    private WaysAdapter adapter;

    public Dialog showDialog(final Context context, ArrayList<WayGroup> _wayGroups)
    {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bus_search_menu);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.rgb(255,255,255)));
        dialog.show();

        wayGroups = new Ways(_wayGroups);
        adapter = new WaysAdapter(context, wayGroups);
        ListView list = (ListView) dialog.findViewById(R.id.wayGroups);
        list.setAdapter(adapter);

        searchInput = (EditText) dialog.findViewById(R.id.search_bar_route);
        clearSearchBtn = (Button) dialog.findViewById(R.id.clear_search);


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
                        clearSearchBtn.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        clearSearchBtn.setVisibility(View.INVISIBLE);
                    }

                    wayGroups.filterData(input);
                    adapter.notifyDataSetChanged();
                }
            }
        );

        clearSearchBtn.setOnClickListener(
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
                    else
                    {
                        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        ((MainActivity) context).selectBus(elem.code, elem.name, elem.type);
                    }
                }
            }
        );

        return dialog;
    }
}
