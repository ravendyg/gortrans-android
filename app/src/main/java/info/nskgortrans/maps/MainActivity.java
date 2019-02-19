package info.nskgortrans.maps;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import info.nskgortrans.maps.Adapters.BusListAdapter;
import info.nskgortrans.maps.DataClasses.BusListElementData;
import info.nskgortrans.maps.DataClasses.TrassData;
import info.nskgortrans.maps.DataClasses.RoutesInfoData;
import info.nskgortrans.maps.DataClasses.UpdateParcel;
import info.nskgortrans.maps.DataClasses.WayData;
import info.nskgortrans.maps.Services.BusService;
import info.nskgortrans.maps.Services.HttpService;
import info.nskgortrans.maps.Services.IHttpService;
import info.nskgortrans.maps.Services.IStorageService;
import info.nskgortrans.maps.Services.ISyncService;
import info.nskgortrans.maps.Services.StorageService;
import info.nskgortrans.maps.Services.SyncService;
import info.nskgortrans.maps.UIComponents.SearchBusDialog;
import info.nskgortrans.maps.UIComponents.SettingsDialog;

public class MainActivity extends AppCompatActivity {
    private String LOG_TAG = "main activity";

    private final int COARSE_LOCATION_PERMISSION_GRANTED = 10;
    private final int FINE_LOCATION_PERMISSION_GRANTED = 11;
    private final int MIN_POSITION_TRACKING_TIME = 1000 * 60;
    private final long MIN_POSITION_TRACKING_DISTANCE = 10;

    private static final Integer[] colors = {R.color.busColor1, R.color.busColor2, R.color.busColor3, R.color.busColor4, R.color.busColor5};

    private Context context;
    private IStorageService storageService;
    private ISyncService syncService;
    private BusService busService;

    private SharedPreferences pref;
    private Utils utils;

    private static Handler syncHandler;
    private static Handler updateHandler;

    RoutesInfoData routesInfoData;

    private Dialog searchBusDialog = null;
    private Dialog settingsDialog = null;

    private Map map;
    private Location location;
    private boolean trackingUser = false;
    private boolean userFound = false;

    private ArrayList<Integer> availableColors;
    private ArrayList<BusListElementData> displayedBuses;
    private BusListAdapter displayedBusesAdapter;

    private Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        File tilesDir = new File(getApplicationContext().getFilesDir() + "/tiles");
        IConfigurationProvider configuration = Configuration.getInstance();
        configuration.setOsmdroidTileCache(tilesDir);
        // Now I own the server, there is no reason to follow OSM tile usage policy
        // https://operations.osmfoundation.org/policies/tiles/
        configuration.setTileDownloadThreads((short) 12);

        setContentView(R.layout.activity_main);

        syncHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case SyncService.ROUTES_SYNC_DATA_WHAT: {
                        routesInfoData = (RoutesInfoData) msg.obj;
                        // TODO: handle different loading source and live update
                        if (routesInfoData != null) {
                            replayDisplayed();
                        }
                        break;
                    }

                    case SyncService.TRASS_SYNC_DATA_WHAT: {
                        if (toast != null) {
                            toast.cancel();
                            toast = null;
                        }
                        TrassData trassData = (TrassData) msg.obj;
                        for (BusListElementData displayedBus : displayedBuses) {
                            String code = trassData.getCode();
                            if (displayedBus.getCode().equals(code)) {
                                map.upsertBusMain(displayedBus, trassData);
                                break;
                            }
                        }
                        break;
                    }

                    case SyncService.SYNCING_DATA_WHAT: {
                        System.out.println("syncing");
                        if (toast != null) {
                            toast.cancel();

                        }
                        toast = Toast.makeText(
                                context,
                                "Загружаю данные маршрута",
                                Toast.LENGTH_LONG
                        );
                        toast.show();
                        break;
                    }
                }
                return true;
            }
        });

        updateHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case BusService.BUS_STATE_WHAT: {
                        HashMap<String, UpdateParcel> parcels =
                                (HashMap<String, UpdateParcel>) msg.obj;
                        map.updateBusMarkers(parcels, false);
                        break;
                    }
                }
                return true;
            }
        });

        pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String apiKey = ensureApiKey();
        utils = new Utils();
        IHttpService httpService = new HttpService(apiKey);
        storageService = new StorageService(context);
        syncService = new SyncService(storageService, httpService, pref, utils, syncHandler);
        busService = new BusService(updateHandler, apiKey);

        init();
    }

    private String ensureApiKey() {
        String apiKey = pref.getString(getString(R.string.pref_api_key), null);
        if (apiKey == null) {
            apiKey = String.valueOf(Math.random()).substring(2);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(getString(R.string.pref_api_key), apiKey);
            editor.commit();
        }
        return apiKey;
    }

    private void init() {
        syncService.syncRoutesInfo();
        busService.start();

        availableColors = new ArrayList<>(Arrays.asList(colors));

        // TODO refactor into a separate UI component
        displayedBuses = new ArrayList<>(Arrays.asList(new BusListElementData[0]));
        displayedBusesAdapter = new BusListAdapter(context, displayedBuses);
        ListView displayedBusesList = (ListView) findViewById(R.id.bus_list);
        displayedBusesList.setAdapter(displayedBusesAdapter);
        displayedBusesList.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                        BusListElementData elem = displayedBusesAdapter.getElem(position);
                        BusActionDialog.showDialog(context, elem, utils);
                    }
                }
        );

        map = new Map();
        map.init(context, findViewById(R.id.map), pref);

        // make sure all permissions granted
        boolean askedPermissions = pref.getBoolean("asked-permissions", false);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("asked-permissions", true);
        editor.commit();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
        ) {
            if (!askedPermissions) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        FINE_LOCATION_PERMISSION_GRANTED
                );
            }
        } else {
            startTrackingUser();
        }
    }

    private void moveUser (Location _location) {
        location = _location;
        View gotoUserBtn = findViewById(R.id.user_location);
        if (location != null) {
            if (map != null) {
                map.moveUser(_location);
            }
            if (!userFound) {
                userFound = true;
                if (gotoUserBtn != null) {
                    gotoUserBtn.setVisibility(View.VISIBLE);
                }
            }
        } else {
            userFound = false;
            if (gotoUserBtn != null) {
                gotoUserBtn.setVisibility(View.GONE);
            }
        }
    }

    private void startTrackingUser() {
        if (trackingUser) {
            return;
        }

        trackingUser = true;
        final LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // display current position
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
        ) {
            Location _location = locationManager.getLastKnownLocation("network");
            if (_location == null) {
                _location = locationManager.getLastKnownLocation("gps");
            }
            moveUser(_location);
        }
        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location _location) {
                moveUser(_location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        // Register the listener with the Location Manager to receive location updates
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    FINE_LOCATION_PERMISSION_GRANTED);
        } else {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, MIN_POSITION_TRACKING_TIME,
                    MIN_POSITION_TRACKING_DISTANCE, locationListener);
        }
    }

    public void showSearchBusDialog(View bntView) {
        searchBusDialog = new SearchBusDialog(context, routesInfoData, storageService, utils);
    }

    public void showSettingsDialog(View bntView) {
        settingsDialog = new SettingsDialog(context);
    }

    public void selectBus(final WayData wayData, final boolean zoom) {
        removeDialogs();

        String code = wayData.getCode();

        // check for dupes
        for (int i = 0; i < displayedBuses.size(); i++) {
            if (displayedBuses.get(i).getCode().equals(code)) {
                return;
            }
        }

        int newColor, icon;
        int size = availableColors.size();
        if (size > 0) {
            newColor = availableColors.get(size - 1);
            availableColors.remove(size - 1);
        } else { // remove the oldest one
            removeBus(displayedBuses.get(0).getCode());
            // and use it's color
            newColor = availableColors.get(0);
        }

        // select icon
        switch (wayData.getType()) {
            case 1:
                icon = R.drawable.bus;
                break;

            case 2:
                icon = R.drawable.trolley;
                break;

            case 3:
                icon = R.drawable.tram;
                break;

            default:
                icon = R.drawable.minibus;
        }

        addBus(wayData, newColor, icon, zoom);
        syncService.syncTrassInfo(wayData.getCode());

        if (zoom) {
            storeDisplayed();
        }
    }

    public void removeBus(final String code) {
        freeBusResources(code);
        map.removeBus(code);
        displayedBusesAdapter.notifyDataSetChanged();
        storeDisplayed();
    }

    public void zoomToRoute(final String code) {
        map.zoomToRoute(code);
    }

    private void storeDisplayed() {
        SharedPreferences.Editor editor = pref.edit();
        String displayed = "";
        for (BusListElementData el : displayedBuses) {
            displayed += el.getCode() + "$";
        }
        editor.putString("displayed", displayed);
        editor.commit();
    }

    private void replayDisplayed() {
        try {
            String displayed = pref.getString("displayed", "");
            String[] codes = displayed.split("\\$");
            for (String code : codes) {
                try {
                    String codeChunks[] = code.split("\\-");
                    int type = Integer.parseInt(codeChunks[0]);
                    String marsh = codeChunks[1];
                    for (WayData wayData : routesInfoData.getWaysByType(type)) {
                        if (wayData.getMarsh().equals(marsh)) {
                            selectBus(wayData, false);
                            continue;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception err) {
            Log.e(LOG_TAG, "parse displayed", err);
        }
    }

    private void removeDialogs() {
        if (searchBusDialog != null) {
            searchBusDialog.cancel();
            searchBusDialog = null;
        }
        if (settingsDialog != null) {
            settingsDialog.cancel();
            settingsDialog = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (map != null) {
            map.saveState();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        removeDialogs();
    }

    public void zoomToUser(View bntView) {
        if (userFound) {
            map.zoomToUser(location);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (busService != null) {
            busService.resume();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (busService != null) {
            busService.pause();
        }
        if (map != null) {
            map.dropBuses();
        }
    }

    private void addBus(final WayData wayData, int color, int icon, boolean zoom) {
        BusListElementData busListElement = new BusListElementData(wayData, icon, color, zoom);
        String code = busListElement.getCode();
        displayedBuses.add(busListElement);
        displayedBusesAdapter.notifyDataSetChanged();
        map.setColor(code, color);

        busService.subscribe(code);
    }

    private boolean freeBusResources(String code) {
        busService.unsubscrive(code);

        boolean removed = false;
        for (int i = 0; i < displayedBuses.size(); i++) {
            BusListElementData temp = displayedBuses.get(i);
            if (temp.getCode().equals(code)) {
                availableColors.add(temp.getColor());
                displayedBuses.remove(i);
                removed = true;
                break;
            }
        }
        return removed;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case COARSE_LOCATION_PERMISSION_GRANTED:
            case FINE_LOCATION_PERMISSION_GRANTED: {
                if (grantResults != null && grantResults.length > 0 && grantResults[0] == 0) {
                    startTrackingUser();
                }
                break;
            }
        }
        return;
    }
}
