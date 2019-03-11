package info.nskgortrans.maps.UIComponents;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.List;

import info.nskgortrans.maps.Adapters.WaysAdapter;
import info.nskgortrans.maps.DataClasses.HistoryData;
import info.nskgortrans.maps.DataClasses.RoutesInfoData;
import info.nskgortrans.maps.DataClasses.WayData;
import info.nskgortrans.maps.MainActivity;
import info.nskgortrans.maps.R;
import info.nskgortrans.maps.Services.IStorageService;
import info.nskgortrans.maps.Utils;

/**
 * Created by me on 21/01/17.
 */

public class SearchBusDialog extends Dialog {
    final static int HISTORY_SIZE = 5;

    private Context context;
    private IStorageService storageService;
    private Utils utils;

    private Button clearSearchBtn;
    private EditText searchInput;

    private WaysAdapter adapter;

    private int selectedIcon;
    private ArrayList<LinearLayout> icons;
    private ArrayList<LinearLayout> bars;

    private SharedPreferences pref;

    private String query;

    private ArrayList<WayData> result = new ArrayList<>();
    private RoutesInfoData routesInfoData;

    private HistoryData history;

    public SearchBusDialog(
            final Context context,
            final RoutesInfoData routesInfoData,
            final IStorageService storageService,
            final Utils utils
    ) {
        super(context);

        this.context = context;
        this.routesInfoData = routesInfoData;
        this.storageService = storageService;
        this.utils = utils;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.bus_search_menu);
        getWindow().setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
        );
        getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.rgb(255, 255, 255))
        );
        show();

        adapter = new WaysAdapter(context, result);
        ListView list = (ListView) findViewById(R.id.wayGroups);
        list.setAdapter(adapter);

        searchInput = (EditText) findViewById(R.id.search_bar_route);
        clearSearchBtn = (Button) findViewById(R.id.clear_search);

        query = searchInput.getText().toString();

        getHistory();

        drawMenu();

        searchInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        query = s.toString();
                        filterSearchResults();
                    }
                });

        clearSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View btn) {
                searchInput.setText("");
                btn.setVisibility(View.GONE);
                filterSearchResults();
            }
        });

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                final WayData elem = adapter.getElem(position);

                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                saveToHistory(elem);

                // give time to hide keyboard
                new Handler().postDelayed(new Runnable() {
                                              @Override
                                              public void run() {
                                                  ((MainActivity) context).selectBus(elem, true);
                                              }
                                          },
                        100
                );
            }
        });

        updateBars();
        filterSearchResults();
    }

    private void drawMenu() {
        pref = PreferenceManager.getDefaultSharedPreferences(context);
        selectedIcon = pref.getInt("selected-icon", 0);
        bars = new ArrayList<>(Arrays.asList(new LinearLayout[0]));
        bars.add((LinearLayout) findViewById(R.id.selected_bar0));
        bars.add((LinearLayout) findViewById(R.id.selected_bar1));
        bars.add((LinearLayout) findViewById(R.id.selected_bar2));
        bars.add((LinearLayout) findViewById(R.id.selected_bar3));

        icons = new ArrayList<>(Arrays.asList(new LinearLayout[0]));
        icons.add((LinearLayout) findViewById(R.id.icon_wrap0));
        icons.add((LinearLayout) findViewById(R.id.icon_wrap1));
        icons.add((LinearLayout) findViewById(R.id.icon_wrap2));
        icons.add((LinearLayout) findViewById(R.id.icon_wrap3));

        for (int i = 0; i < icons.size(); i++) {
            final LinearLayout icon = icons.get(i);
            icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int id = v.getId();
                    for (int j = 0; j < icons.size(); j++) {
                        if (id == icons.get(j).getId()) {
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
    }

    private void updateBars() {
        for (int i = 0; i < bars.size(); i++) {
            LinearLayout bar = bars.get(i);
            if (i == selectedIcon) {
                bar.setBackgroundColor(context.getResources().getColor(R.color.selectedSearchIcon));
            } else {
                bar.setBackgroundColor(context.getResources().getColor(R.color.unselectedSearchIcon));
            }
        }
    }

    private void filterSearchResults() {
        result.clear();
        if (query.length() > 0) {
            if (clearSearchBtn != null) {
                clearSearchBtn.setVisibility(View.VISIBLE);
            }

            List<WayData> input = routesInfoData.getWaysByType(utils.getType(selectedIcon));
            result.clear();
            for (WayData el : input) {
                if (el.getName().matches(".*" + query + ".*")) {
                    result.add(el);
                }
            }
        } else {
            if (clearSearchBtn != null) {
                clearSearchBtn.setVisibility(View.INVISIBLE);
            }
            for (WayData el : history.getType(selectedIcon)) {
                result.add(el);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void getHistory() {
        history = storageService.getSearchHistory();
    }

    private void saveToHistory(WayData element) {
        int pos = utils.mapTypeToPosition(element);
        history.save(element, pos);
        storageService.setSearchHistory(history);
    }
}
