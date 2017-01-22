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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;


import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import info.nskgortrans.maps.Adapters.BusListAdapter;
import info.nskgortrans.maps.DataClasses.BusListElement;
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


//  private SocketIO socket;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    context = this;

    // create colors array
    Integer[] colors = {R.color.busColor1, R.color.busColor2, R.color.busColor3, R.color.busColor4, R.color.busColor5};
    availableColors = new ArrayList<>(Arrays.asList(colors));
    // handle list of selected buses
    displayedBuses = new ArrayList<>(Arrays.asList(new BusListElement[0]));
    displayedBusesAdapter = new BusListAdapter(context, displayedBuses);
    ListView displayedBusesList = (ListView) findViewById(R.id.bus_list);
    displayedBusesList.setAdapter(displayedBusesAdapter);


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
      Log.e(LOG_TAG, "service not running");
      startService(new Intent(this, BusPositionService.class));
    }
    else
    {
      Log.e(LOG_TAG, "service running");
      // notify bus position service
      Intent intent = new Intent("gortrans-bus-service");
      intent.putExtra("event", "activity-online");
      sendBroadcast(intent);
    }

    searchDialog = new SearchBusDialog();


    /** /init map */

//    // hardcoded behaviour: register receiver that will listen for bus 36
//    if (socketReceiver == null)
//    {
//      socketReceiver = new BroadcastReceiver()
//      {
//        @Override
//        public void onReceive(Context context, Intent intent)
//        {
//          String eventType = intent.getStringExtra("event");
//          if ( eventType.equals("connection") )
//          {
//            requestBusOnMap("1-036-W-36");
//          }
//          else if (eventType.equals("bus listener created") )
//          {
//            addBusToMap("1-036-W-36");
//          }
//        }
//      };
//    }
//    registerReceiver(socketReceiver, new IntentFilter("gortrans-socket-activity"));


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

  public void selectBus(final String code, final String name, final int type)
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

    addBusToMap(code, name, newColor, icon);
    // implement add to map and subscribe
    displayedBusesAdapter.notifyDataSetChanged();
  }

  private void removeBus(final String code)
  {
    removeBusFromMap(code);
    // implement removal from the map and unsubscribing
    displayedBusesAdapter.notifyDataSetChanged();
  }

  private void removeDialog()
  {
    if (searchBusDialog != null)
    {
      searchBusDialog.cancel();
      searchBusDialog = null;
    }
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
          String eventType = intent.getStringExtra("event");
          if (eventType.equals(("wayGroups")))
          {
            routesDataStr = intent.getStringExtra("way-groups");
            loadWayGrous();
          }
//          else if (eventType.equals("busSelected"))
//          { // from search bus dialog
//            removeDialog();
//            String busCode = intent.getStringExtra("busCode");
//            Log.e("bus code: ", busCode);
//          }
//          if (eventType.equals("connection"))
//          {
//            requestBusOnMap("1-036-W-36");
//          }
//          else if (eventType.equals("bus listener created") )
//          {
//            addBusToMap("1-036-W-36");
//          }
        }
      };
    }
    registerReceiver(serviceReceiver, new IntentFilter("gortrans-main-activity"));
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

    // notify bus position service
    Intent intent = new Intent("gortrans-bus-service");
    intent.putExtra("event", "activity-offline");
    sendBroadcast(intent);

    if (serviceReceiver != null)
    {
      unregisterReceiver(serviceReceiver);
      serviceReceiver = null;
    }

    removeDialog();
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

  private void connectToSocket() {
    Log.e(LOG_TAG, "connect");
    new Thread(new Runnable() {
      @Override
      public void run() {
        Log.e(LOG_TAG, "get instance");
//        socket = SocketIO.getInstance();
      }
    }).start();
  }

  private void requestBusOnMap(String busCode) {
    Intent intent =
            new Intent("gortrans-socket-service")
                    .putExtra("busCode", busCode)
                    .putExtra("event", "request add bus");

    sendBroadcast(intent);
  }

  private void addBusToMap(String code, String name, int color, int icon) {
    Log.e(LOG_TAG, code);
    displayedBuses.add(new BusListElement(name, code, color, icon));
  }

  private void updateBusOnMap(String code)
  {
  }

  private boolean removeBusFromMap(String code)
  {
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
