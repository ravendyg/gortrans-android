package info.nskgortrans.maps;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;


import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import info.nskgortrans.maps.Adapters.BusListAdapter;
import info.nskgortrans.maps.Data.BusListElementData;
import info.nskgortrans.maps.Data.TrassData;
import info.nskgortrans.maps.DataClasses.BusRoute;
import info.nskgortrans.maps.Data.RoutesInfoData;
import info.nskgortrans.maps.DataClasses.StopInfo;
import info.nskgortrans.maps.DataClasses.UpdateParcel;
import info.nskgortrans.maps.Data.WayData;
import info.nskgortrans.maps.Services.BusPositionService;
import info.nskgortrans.maps.Services.HttpService;
import info.nskgortrans.maps.Services.IHttpService;
import info.nskgortrans.maps.Services.IStorageService;
import info.nskgortrans.maps.Services.ISyncService;
import info.nskgortrans.maps.Services.StorageService;
import info.nskgortrans.maps.Services.SyncService;
import info.nskgortrans.maps.UIComponents.SearchBusDialog;

public class MainActivity extends AppCompatActivity {
    private String LOG_TAG = "main activity";

    private final int COARSE_LOCATION_PERMISSION_GRANTED = 10;
    private final int FINE_LOCATION_PERMISSION_GRANTED = 11;
    private final int WRITE_STORAGE_PERMISSION_GRANTED = 12;

    private final int MIN_POSITION_TRACKING_TIME = 1000 * 60;
    private final long MIN_POSITION_TRACKING_DISTANCE = 10;

    private static final Integer[] colors = {R.color.busColor1, R.color.busColor2, R.color.busColor3, R.color.busColor4, R.color.busColor5};

    private Context context;
    private IStorageService storageService;
    private ISyncService syncService;

    private SharedPreferences pref;
    private Utils utils;

    private static Handler syncHandler;

    RoutesInfoData routesInfoData;
//    private ArrayList<WayGroup> wayGroups;
//    private String routesDataStr = "";

    private BroadcastReceiver serviceReceiver = null;

    private Dialog searchBusDialog = null;

    private Map map;
    private Location location;
    private boolean trackingUser = false;
    private boolean userFound = false;

    private ArrayList<Integer> availableColors;
    private ArrayList<BusListElementData> displayedBuses;
    private BusListAdapter displayedBusesAdapter;

    private HashMap<String, String> routeColors;

    private HashMap<String, StopInfo> stops;
    private HashMap<String, HashSet<String>> busStops;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        syncHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case SyncService.ROUTES_SYNC_DATA_WHAT: {
                        routesInfoData = (RoutesInfoData) msg.obj;
                        // TODO: handle different loading source and live update
                        if (routesInfoData != null) {
                            findViewById(R.id.bus_search_btn).setVisibility(View.VISIBLE);
                            replayDisplayed();
                        }
                        break;
                    }

                    case SyncService.TRASS_SYNC_DATA_WHAT: {
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
                }
                return true;
            }
        });

        context = this;
        pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String apiKey = ensureApiKey();
        utils = new Utils();
        IHttpService httpService = new HttpService(apiKey);
        storageService = new StorageService(context);
        syncService = new SyncService(storageService, httpService, pref, utils, syncHandler);

        boolean storageGranted = handlePermissionVerifiation(savedInstanceState);
        if (storageGranted) {
            // storage is critical for the map and sync
            init();
        }
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
        syncService.syncRoutesInfo().start();

        availableColors = new ArrayList<>(Arrays.asList(colors));
        routeColors = new HashMap<>();

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

        startListenForService();
        startService(new Intent(this, BusPositionService.class));
    }

    private void startTrackingUser() {
        if (trackingUser) {
            return;
        }

        trackingUser = true;
        // Acquire a reference to the system Location Manager
        final LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location _location) {
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

    public void selectBus(final WayData wayData, final boolean zoom) {
        removeDialog();

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
        Thread trassSyncThread = syncService.syncTrassInfo(wayData.getCode());
        if (trassSyncThread != null) {
            trassSyncThread.start();
        }

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
//      String displayed = pref.getString("displayed", "");
//      String[] codes = displayed.split("\\$");
//      for (String code : codes)
//      {
//        String codeChunks[] = code.split("\\-");
//        if (codeChunks.length == 4)
//        {
//          selectBus(code, codeChunks[3], Integer.parseInt(codeChunks[0]), false);
//        }
//      }
        } catch (Exception err) {
            Log.e(LOG_TAG, "parse displayed", err);
        }
    }

    private void removeDialog() {
        if (searchBusDialog != null) {
            searchBusDialog.cancel();
            searchBusDialog = null;
        }
    }

    // TODO: use a WS client running in a thread, not in a service
    private void addBusListener(final String code) {
        Intent intent = new Intent("info.nskgortrans.maps.gortrans.bus-service");
        intent.putExtra("event", "add-bus-listener");
        intent.putExtra("code", code);
        sendBroadcast(intent);
    }

    private void removeBusListener(final String code) {
        Intent intent = new Intent("info.nskgortrans.maps.gortrans.bus-service");
        intent.putExtra("event", "remove-bus-listener");
        intent.putExtra("code", code);
        sendBroadcast(intent);
    }

    private void showSearchBtn(RoutesInfoData routesInfoData) {
//        map.loadStops(
//                (HashMap<String, StopInfo>) intent.getSerializableExtra("stops"),
//                (HashMap<String, HashSet<String>>) intent.getSerializableExtra("busStops")
//        );
        this.routesInfoData = routesInfoData;
        findViewById(R.id.bus_search_btn).setVisibility(View.VISIBLE);
        replayDisplayed();
    }


    private void updateState(final String newStateStr) {

    }


    private void startListenForService() {
        if (serviceReceiver == null) {
            serviceReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.e(LOG_TAG, "receive broadcast");
                    String eventType = intent.getStringExtra("event");
          /*
          if (eventType.equals(("data")))
          {
            routesDataStr = intent.getStringExtra("way-groups");
            map.loadStops(
              (HashMap<String, StopInfo>) intent.getSerializableExtra("stops"),
              (HashMap<String, HashSet<String>>) intent.getSerializableExtra("busStops")
            );
            loadWayGrous();
            findViewById(R.id.bus_search_btn).setVisibility(View.VISIBLE);
            replayDisplayed();
          }
          else
          */
                    if (eventType.equals("route")) {
                        map.updateBusRoute(intent.getStringExtra("code"),
                                (BusRoute) intent.getSerializableExtra("data"));
                    } else if (eventType.equals("state-update")) {
                        updateState(intent.getStringExtra("new-state"));
                    } else if (eventType.equals("bus-update")) {
                        HashMap<String, UpdateParcel> parcels =
                                (HashMap<String, UpdateParcel>) intent.getSerializableExtra("parcels");
                        map.updateBusMarkers(parcels);
                    }
                }
            };
            LocalBroadcastManager.getInstance(context).
                    registerReceiver(serviceReceiver, new IntentFilter("info.nskgortrans.maps.main.activity"));
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

        removeDialog();

        if (!isServiceRunning(BusPositionService.class)) {
            stopService(new Intent(this, BusPositionService.class));
        }

        Log.e(LOG_TAG, "pause");
        if (serviceReceiver != null) {
            LocalBroadcastManager.getInstance(context)
                    .unregisterReceiver(serviceReceiver);
            serviceReceiver = null;
        }
    }

    public void zoomToUser(View bntView) {
        if (userFound) {
            map.zoomToUser(location);
        }
    }


    public void requestStorage(View view) {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                WRITE_STORAGE_PERMISSION_GRANTED
        );
    }


    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    private void requestBusOnMap(String busCode) {
        Intent intent =
                new Intent("info.nskgortrans.maps.gortrans.socket-service")
                        .putExtra("busCode", busCode)
                        .putExtra("event", "request add bus");

        sendBroadcast(intent);
    }

    private void addBus(final WayData wayData, int color, int icon, boolean zoom) {
        BusListElementData busListElement = new BusListElementData(wayData, icon, color, zoom);
        String code = busListElement.getCode();
        routeColors.put(code, "" + color);
        displayedBuses.add(busListElement);
        displayedBusesAdapter.notifyDataSetChanged();

        addBusListener(code);
    }

    private void addBusToMap(String code) {
        int color = Integer.parseInt(routeColors.get(code));
    }

    private void updateBusOnMap(String code) {
    }

    private boolean freeBusResources(String code) {
        removeBusListener(code);

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

    private boolean handlePermissionVerifiation(Bundle savedInstanceState) {
        boolean storageGranted = false;
        boolean askedPermissions = pref.getBoolean("asked-permissions", false);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("asked-permissions", true);
        editor.commit();

        // make sure all permissions granted
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

        View warningView = findViewById(R.id.storage_warnig);
        View mapView = findViewById(R.id.map_frame);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
        ) {
            if (!askedPermissions) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_STORAGE_PERMISSION_GRANTED
                );
            } else {
                if (warningView != null) {
                    warningView.setVisibility(View.VISIBLE);
                }
                if (mapView != null) {
                    mapView.setVisibility(View.GONE);
                }
            }
        } else {
            storageGranted = true;
            if (warningView != null) {
                warningView.setVisibility(View.GONE);
            }
            if (mapView != null) {
                mapView.setVisibility(View.VISIBLE);
            }
        }

        return storageGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case COARSE_LOCATION_PERMISSION_GRANTED:
            case FINE_LOCATION_PERMISSION_GRANTED: {
                if (grantResults != null && grantResults.length > 0 && grantResults[0] == 0) {
                    startTrackingUser();
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }
                break;
            }
            case WRITE_STORAGE_PERMISSION_GRANTED: {
                if (grantResults != null && grantResults.length > 0) {
                    View warningView = findViewById(R.id.storage_warnig);
                    View mapView = findViewById(R.id.map_frame);
                    if (grantResults[0] == 0) {
                        Intent intent = getIntent();
                        finish();

                        if (warningView != null) {
                            warningView.setVisibility(View.GONE);
                        }
                        if (mapView != null) {
                            mapView.setVisibility(View.VISIBLE);
                        }
                        startActivity(intent);
                    } else {
                        if (warningView != null) {
                            warningView.setVisibility(View.VISIBLE);
                        }
                        if (mapView != null) {
                            mapView.setVisibility(View.GONE);
                        }
                    }
                }
                break;
            }
        }
        return;
    }
}
