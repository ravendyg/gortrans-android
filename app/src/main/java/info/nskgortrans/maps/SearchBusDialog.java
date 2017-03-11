package info.nskgortrans.maps;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import info.nskgortrans.maps.Adapters.WaysAdapter;
import info.nskgortrans.maps.DataClasses.Way;
import info.nskgortrans.maps.DataClasses.WayGroup;
import info.nskgortrans.maps.DataClasses.WayGroupElement;
import info.nskgortrans.maps.DataClasses.Ways;

/**
 * Created by me on 21/01/17.
 */

public class SearchBusDialog
{
  final static int HISTORY_SIZE = 5;

  private Dialog dialog;
  private Context ctx;

  private Button clearSearchBtn;
  private EditText searchInput;

  private WaysAdapter adapter;

  private int selectedIcon;
  private ArrayList<LinearLayout> icons;
  private ArrayList<LinearLayout> bars;

  private SharedPreferences pref;

  private String query;

  private ArrayList<Way> result = new ArrayList<>(Arrays.asList(new Way[0]));
  ArrayList<WayGroup> wayGroups;

  private ArrayList<Way>[] history = new ArrayList[]{
    new ArrayList<>(Arrays.asList(new Way[0])),
    new ArrayList<>(Arrays.asList(new Way[0])),
    new ArrayList<>(Arrays.asList(new Way[0])),
    new ArrayList<>(Arrays.asList(new Way[0]))
  };


  public Dialog showDialog(final Context context, ArrayList<WayGroup> _wayGroups)
  {
    ctx = context;
    dialog = new Dialog(context);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setContentView(R.layout.bus_search_menu);
    dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.rgb(255,255,255)));
    dialog.show();

    pref = PreferenceManager.getDefaultSharedPreferences(context);
    selectedIcon = pref.getInt("selected-icon", 0);
    bars = new ArrayList<>(Arrays.asList(new LinearLayout[0]));
    bars.add((LinearLayout) dialog.findViewById(R.id.selected_bar0));
    bars.add((LinearLayout) dialog.findViewById(R.id.selected_bar1));
    bars.add((LinearLayout) dialog.findViewById(R.id.selected_bar2));
    bars.add((LinearLayout) dialog.findViewById(R.id.selected_bar3));

    icons = new ArrayList<>(Arrays.asList(new LinearLayout[0]));
    icons.add((LinearLayout) dialog.findViewById(R.id.icon_wrap0));
    icons.add((LinearLayout) dialog.findViewById(R.id.icon_wrap1));
    icons.add((LinearLayout) dialog.findViewById(R.id.icon_wrap2));
    icons.add((LinearLayout) dialog.findViewById(R.id.icon_wrap3));

    for (int i = 0; i < icons.size(); i++)
    {
      final LinearLayout icon = icons.get(i);
      icon.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          int id = v.getId();
          for (int j = 0; j < icons.size(); j++)
          {
            if (id == icons.get(j).getId())
            {
              selectedIcon = j;
              SharedPreferences.Editor editor = pref.edit();
              editor.putInt("selected-icon", selectedIcon);
              editor.commit();
              updateBars();
              filterSearchResults();
              break;
            }
          }
        }
      });
    }

    this.wayGroups = _wayGroups;

    adapter = new WaysAdapter(context, result);
    ListView list = (ListView) dialog.findViewById(R.id.wayGroups);
    list.setAdapter(adapter);

    searchInput = (EditText) dialog.findViewById(R.id.search_bar_route);
    clearSearchBtn = (Button) dialog.findViewById(R.id.clear_search);

    query = searchInput.getText().toString();

    getHistory();

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
          query = s.toString();
          filterSearchResults();
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
        filterSearchResults();
          }
      }
    );

    list.setOnItemClickListener(
      new AdapterView.OnItemClickListener()
      {
        @Override
        public void onItemClick( AdapterView<?> adapterView, View view, int position, long id)
        {
          final Way elem = adapter.getElem(position);

          InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
          imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

          saveToHistory(elem);

          // give time to hide keyboard
          new Handler().postDelayed(
            new Runnable()
            {
                @Override
                public void run()
                {
              ((MainActivity) context).selectBus(elem.code, elem.name, elem.type, true);
                }
            },
            100
          );
        }
      }
    );

    updateBars();

    return dialog;
  }

  private void updateBars()
  {
    for (int i = 0; i < bars.size(); i++)
    {
      LinearLayout bar = bars.get(i);
      if (i == selectedIcon)
      {
        bar.setBackgroundColor(ctx.getResources().getColor(R.color.selectedSearchIcon));
      }
      else
      {
        bar.setBackgroundColor(ctx.getResources().getColor(R.color.unselectedSearchIcon));
      }
    }
    filterSearchResults();
  }

  private void filterSearchResults()
  {
    result.clear();
    if (query.length() > 0)
    {
      if (clearSearchBtn != null)
      {
        clearSearchBtn.setVisibility(View.VISIBLE);
      }

      ArrayList<Way> input = wayGroups.get(selectedIcon).ways;
      result.clear();
      for (Way el: input)
      {
        if ( el.name.matches(".*" + query + ".*"))
        {
          result.add(el);
        }
      }
    }
    else
    {
      if (clearSearchBtn != null)
      {
        clearSearchBtn.setVisibility(View.INVISIBLE);
      }
      for (Way el: history[selectedIcon])
      {
        result.add(el);
      }
    }

    adapter.notifyDataSetChanged();
  }

  private void getHistory()
  {
    history = new ArrayList[]{
      new ArrayList<>(Arrays.asList(new Way[0])),
      new ArrayList<>(Arrays.asList(new Way[0])),
      new ArrayList<>(Arrays.asList(new Way[0])),
      new ArrayList<>(Arrays.asList(new Way[0]))
    };

    String historyStr = pref.getString("history", "");
//    String historyStr = "";
    String[] historyItems = historyStr.split("\\$");
    for (String item: historyItems)
    {
      String[] props = item.split("\\|");
      if (props.length > 0)
      {
        try
        {
          Way way = new Way(props);
          int position = getGroup(way);

          history[position].add(way);
        }
        catch (Exception err)
        {
          Log.e("search dialog", "parse history", err);
        }
      }
    }
  }

  private void saveHistory()
  {
    String store = "";
    for (ArrayList<Way> el: history)
    {
      for (Way way: el)
      {
        store += way.serialize() + "$";
      }
    }
    SharedPreferences.Editor editor = pref.edit();
    editor.putString("history", store);
    editor.commit();
  }

  private void saveToHistory(Way element)
  {
    int pos = getGroup(element);
    for (int i = 0; i < history[pos].size(); i++)
    {
      if (history[pos].get(i).code.equals(element.code))
      {
        history[pos].remove(i);
        history[pos].add(0, element);
        saveHistory();
        return;
      }
    }

    history[pos].add(0, element);
    while (history[pos].size() > HISTORY_SIZE)
    {
      history[pos].remove(HISTORY_SIZE);
    }
    saveHistory();
  }

  private int getGroup(Way way)
  {
    int position = 0;
    switch (way.type)
    {
      case 1:
        position = 1;
        break;
      case 2:
        position = 2;
        break;
      case 7:
        position = 3;
        break;
    }
    return position;
  }
}
