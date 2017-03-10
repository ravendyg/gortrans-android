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
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;


import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import info.nskgortrans.maps.Adapters.BusListAdapter;
import info.nskgortrans.maps.DataClasses.BusListElement;
import info.nskgortrans.maps.DataClasses.BusRoute;
import info.nskgortrans.maps.DataClasses.StopInfo;
import info.nskgortrans.maps.DataClasses.UpdateParcel;
import info.nskgortrans.maps.DataClasses.WayGroup;
import info.nskgortrans.maps.Services.BusPositionService;

public class MainActivity extends AppCompatActivity
{
  private String LOG_TAG = "main activity";

  Context context;

  private final int COARSE_LOCATION_PERMISSION_GRANTED = 10;
  private final int FINE_LOCATION_PERMISSION_GRANTED = 11;
  private final int WRITE_STORAGE_PERMISSION_GRANTED = 12;

  private final int MIN_POSITION_TRACKING_TIME = 0;//1000 * 60;
  private final long MIN_POSITION_TRACKING_DISTANCE = 0;//10;

  private boolean locationGranted = false;
  private boolean storageGranted = false;

  private SharedPreferences pref;

  private ArrayList<WayGroup> wayGroups;
  private String routesDataStr = "";

  private BroadcastReceiver serviceReceiver = null;

  private SearchBusDialog searchDialog;

  private boolean waysLoaded;

  private Dialog searchBusDialog = null;

  private Map map;
  private Location location;
  private boolean trackingUser = false;
  private boolean userFound = false;

  private ArrayList<Integer> availableColors;
  private ArrayList<BusListElement> displayedBuses;
  private BusListAdapter displayedBusesAdapter;

  private HashMap<String, String> routeColors;

  private HashMap<String, StopInfo> stops;
  private HashMap<String, HashSet<String>> busStops;


//  private SocketIO socket;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    context = this;

    // create colors array
    Integer[] colors = {R.color.busColor1, R.color.busColor2, R.color.busColor3, R.color.busColor4, R.color.busColor5};
    availableColors = new ArrayList<>(Arrays.asList(colors));
    routeColors = new HashMap<>();
    // handle list of selected buses
    displayedBuses = new ArrayList<>(Arrays.asList(new BusListElement[0]));
    displayedBusesAdapter = new BusListAdapter(context, displayedBuses);
    ListView displayedBusesList = (ListView) findViewById(R.id.bus_list);
    displayedBusesList.setAdapter(displayedBusesAdapter);

    displayedBusesList.setOnItemClickListener(
      new AdapterView.OnItemClickListener()
      {
        @Override
        public void onItemClick( AdapterView<?> adapterView, View view, int position, long id)
        {
          BusListElement elem = displayedBusesAdapter.getElem(position);
          BusActionDialog.showDialog(context, elem.code, elem.type, elem.name);
        }
      }
    );


    // make sure all permissions granted
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
            ) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
              FINE_LOCATION_PERMISSION_GRANTED);
    }
    else
    {
      locationGranted = true;
      startTrackingUser();
    }
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
            ) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
              WRITE_STORAGE_PERMISSION_GRANTED);
    }
    else
    {
      storageGranted = true;
    }

    if (savedInstanceState != null)
    {
      routesDataStr = savedInstanceState.getString("savedWays");
      loadWayGrous();
    }



    if (storageGranted)
    { // storage is critical for the map
      init();
    }
    else
    {
      // do smth about permissions
    }
  }

  private void loadWayGrous()
  {
    if (routesDataStr != null)
    {
      try
      {
        wayGroups = JSONParser.getWayGroups( new JSONObject(routesDataStr));
      }
      catch (JSONException err)
      {
      }
    }
  }

  private void init() {
//    waysLoaded = false;

    pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

    map = new Map();
    map.init(context, findViewById(R.id.map), pref);

    startListenForService();

    if (!isServiceRunning(BusPositionService.class))
    {
      startService(new Intent(this, BusPositionService.class));
    }
    else
    {
      // notify bus position service
      Intent intent = new Intent("info.nskgortrans.maps.gortrans.bus-service");
      intent.putExtra("event", "activity-online");
      sendBroadcast(intent);
    }

    searchDialog = new SearchBusDialog();
  }

  private void startTrackingUser()
  {
    if (trackingUser)
    {
      return;
    }

    trackingUser = true;
    // Acquire a reference to the system Location Manager
    LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

    // Define a listener that responds to location updates
    LocationListener locationListener = new LocationListener() {
      public void onLocationChanged(Location _location)
      {
        location = _location;
        if (map != null)
        {
          map.moveUser(_location);
        }
        if (!userFound)
        {
          userFound = true;
          ((FloatingActionButton) findViewById(R.id.user_location)).setVisibility(View.VISIBLE);
        }
      }

      public void onStatusChanged(String provider, int status, Bundle extras) {}

      public void onProviderEnabled(String provider) {}

      public void onProviderDisabled(String provider) {}
    };

    // Register the listener with the Location Manager to receive location updates
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
            ) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
              FINE_LOCATION_PERMISSION_GRANTED);
    }
    locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER, MIN_POSITION_TRACKING_TIME,
            MIN_POSITION_TRACKING_DISTANCE, locationListener);
  }

  public void showSearchBusDialog(View bntView)
  {
    searchBusDialog = searchDialog.showDialog(context, wayGroups);
  }

  public void selectBus(final String code, final String name, final int type, boolean zoom)
  {
    removeDialog();
    Log.e("bus code: ", code);

    // check for dupes
    for (int i = 0; i < displayedBuses.size(); i++)
    {
      if (displayedBuses.get(i).code.equals(code))
      {
        return;
      }
    }

    int newColor, icon;
    int size = availableColors.size();
    if (size > 0)
    {
      newColor = availableColors.get(size - 1);
      availableColors.remove(size - 1);
    }
    else
    { // remove the oldest one
      removeBus(displayedBuses.get(0).code);
      // and use it's color
      newColor = availableColors.get(0);
    }

    // select icon
    switch (type)
    {
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

    addBus(code, name, newColor, icon, type, zoom);
  }

  public void removeBus(final String code)
  {
    freeBusResources(code);
    map.removeBus(code);
    displayedBusesAdapter.notifyDataSetChanged();
  }

  public void zoomToRoute(final String code)
  {
    map.zoomToRoute(code);
  }

  private void removeDialog()
  {
    if (searchBusDialog != null)
    {
      searchBusDialog.cancel();
      searchBusDialog = null;
    }
  }


  private void addBusListener(final String code)
  {
    Intent intent = new Intent("info.nskgortrans.maps.gortrans.bus-service");
    intent.putExtra("event", "add-bus-listener");
    intent.putExtra("code", code);
    intent.putExtra("line-required", !map.hasPolyline(code));
    sendBroadcast(intent);
  }

  private void removeBusListener(final String code)
  {
    Intent intent = new Intent("info.nskgortrans.maps.gortrans.bus-service");
    intent.putExtra("event", "remove-bus-listener");
    intent.putExtra("code", code);
    sendBroadcast(intent);
  }


  private void updateState(final String newStateStr)
  {

  }


  private void startListenForService()
  {
    if (serviceReceiver == null)
    {
      serviceReceiver = new BroadcastReceiver()
      {
        @Override
        public void onReceive(Context context, Intent intent)
        {
          Log.e(LOG_TAG, "receive broadcast");
          String eventType = intent.getStringExtra("event");
          if (eventType.equals(("data")))
          {
            routesDataStr = intent.getStringExtra("way-groups");
            map.loadStops(
              (HashMap<String, StopInfo>) intent.getSerializableExtra("stops"),
              (HashMap<String, HashSet<String>>) intent.getSerializableExtra("busStops")
            );
            loadWayGrous();
            findViewById(R.id.bus_search_btn).setVisibility(View.VISIBLE);
          }
          else if (eventType.equals("route"))
          {
            map.updateBusRoute(intent.getStringExtra("code"),
                    (BusRoute) intent.getSerializableExtra("data"));
          }
          else if (eventType.equals("state-update"))
          {
            updateState(intent.getStringExtra("new-state"));
          }
          else if (eventType.equals("points"))
          {
            String busCode = intent.getStringExtra("busCode");
            boolean update = intent.getBooleanExtra("update", false);
            ArrayList<GeoPoint> points = (ArrayList<GeoPoint>) intent.getSerializableExtra("points");
            String color = routeColors.get(busCode);
            map.addPolyline(busCode, points, color, update);
          }
          else if (eventType.equals("bus-update"))
          {
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

    map.saveState();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString("savedWays", routesDataStr);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    removeDialog();

    // notify bus position service
    Intent intent = new Intent("info.nskgortrans.maps.gortrans.bus-service");
    intent.putExtra("event", "activity-offline");
    sendBroadcast(intent);

    Log.e(LOG_TAG, "pause");
    if (serviceReceiver != null)
    {
      LocalBroadcastManager.getInstance(context)
        .unregisterReceiver(serviceReceiver);
      serviceReceiver = null;
    }
  }

  public void zoomToUser(View bntView)
  {
    if (userFound)
    {
      map.zoomToUser(location);
    }
  }


  private boolean isServiceRunning(Class<?> serviceClass)
  {
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

  private void addBus(String code, String name, int color, int icon, int type, boolean zoom)
  {
    routeColors.put(code, "" + color);
    displayedBuses.add(new BusListElement(name, code, color, icon, type));
    displayedBusesAdapter.notifyDataSetChanged();

    addBusListener(code);

    map.addBus(code, color, zoom);
  }

  private void addBusToMap(String code)
  {
    int color = Integer.parseInt(routeColors.get(code));
  }

  private void updateBusOnMap(String code)
  {
  }

  private boolean freeBusResources(String code)
  {
    removeBusListener(code);

    boolean removed = false;
    for (int i = 0; i < displayedBuses.size(); i++)
    {
      BusListElement temp = displayedBuses.get(i);
      if (temp.code.equals(code))
      {
        availableColors.add(temp.color);
        displayedBuses.remove(i);
        removed = true;
        break;
      }
    }
    return removed;
  }


  // permissions handling
  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
  {
    switch (requestCode) {
      case COARSE_LOCATION_PERMISSION_GRANTED:
      case FINE_LOCATION_PERMISSION_GRANTED:
      {
        locationGranted = true;
        startTrackingUser();
        break;
      }
      case WRITE_STORAGE_PERMISSION_GRANTED:
      {
        storageGranted = true;
        break;
      }

      // other 'case' lines to check for other
      // permissions this app might request
    }

    if (locationGranted)
    {
      init();
    }

    return;
  }
}
