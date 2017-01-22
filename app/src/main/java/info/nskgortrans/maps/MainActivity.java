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
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;


import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;

import info.nskgortrans.maps.DataClasses.WayGroup;
import info.nskgortrans.maps.Services.BusPositionService;
import info.nskgortrans.maps.Fragments.BusSearchFragment;
import info.nskgortrans.maps.Fragments.EmptyFragment;

public class MainActivity extends AppCompatActivity {
  private String LOG_TAG = "main activity";

  final Context context = this;

  private int notGrantedPermissions = 2;
  private final int COARSE_LOCATION_PERMISSION_GRANTED = 10;
  private final int FINE_LOCATION_PERMISSION_GRANTED = 11;
  private final int WRITE_STORAGE_PERMISSION_GRANTED = 12;

  private SharedPreferences pref;
  private MapView map;

  private ArrayList<WayGroup> wayGroups;
  private String routesDataStr = "";

  private BroadcastReceiver serviceReceiver = null;

  private FrameLayout menuHolder;

  private SearchBusDialog searchDialog;

  private boolean waysLoaded;

  private Dialog searchBusDialog = null;


//  private SocketIO socket;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // make sure all permissions granted
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
            ) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
              FINE_LOCATION_PERMISSION_GRANTED);
    } else {
      notGrantedPermissions--;
    }
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
            ) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
              WRITE_STORAGE_PERMISSION_GRANTED);
    } else {
      notGrantedPermissions--;
    }


//    menuHolder = (FrameLayout) findViewById(R.id.fragment_view);
//
//    if (savedInstanceState == null)
//    { // start with empty fragment
//      if (menuHolder != null)
//      {
//        EmptyFragment empty = new EmptyFragment();
//        getSupportFragmentManager().beginTransaction().add(R.id.fragment_view, empty).commit();
//      }
//    }
//    else
//    {
    if (savedInstanceState != null)
    {
      routesDataStr = savedInstanceState.getString("savedWays");
      loadWayGrous();
    }


//    }
//
    // if ok initialize map and start service
    if (notGrantedPermissions == 0) {
      init();
//      if (wayGroups == null)
//      {
//        new SyncData(false).execute();
//      }
//      else
//      {
//        waysLoaded = true;
//        showBtns(false);
//      }
    } else {
      // do smth about permissions
    }
//
//    // add click listeners to the buttons
//    busSearchBtn = (AppCompatImageButton) findViewById(R.id.bus_search_btn);
//    busSearchBtn.setOnClickListener(
//      new View.OnClickListener()
//      {
//        public void onClick(View v)
//        {
//          showBusSearch();
//        }
//      }
//    );
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


    initMap();


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

  public void showSearchBusDialog(View bntView)
  {
    searchBusDialog = searchDialog.showDialog(context, wayGroups);
  }

  public void selectBus(final String code)
  {
    searchBusDialog.cancel();
    Log.e("bus code: ", code);
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
//            if (searchBusDialog != null)
//            {
//              searchBusDialog.cancel();
//              searchBusDialog = null;
//            }
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

  private void initMap() {
    // set user agent for the map
    OpenStreetMapTileProviderConstants.setUserAgentValue(BuildConfig.APPLICATION_ID);

    /** init map */
    map = (MapView) findViewById(R.id.map);
    map.setTileSource(TileSourceFactory.MAPNIK);

    map.setBuiltInZoomControls(true);
    map.setMultiTouchControls(true);

    float lat = pref.getFloat(getString(R.string.pref_lat), (float) 54.908593335436926);
    float lng = pref.getFloat(getString(R.string.pref_lng), (float) 83.0291748046875);
    int zoom = pref.getInt(getString(R.string.pref_zoom), 10);

    IMapController mapController = map.getController();
    mapController.setZoom(zoom);
    GeoPoint startPoint = new GeoPoint(lat, lng);
    mapController.setCenter(startPoint);
  }




  @Override
  public void onResume() {
    super.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();

    try { // save current map position and zoom
      Projection proj = map.getProjection();
      GeoPoint center = proj.getBoundingBox().getCenter();

      float lat = (float) center.getLatitude();
      float lng = (float) center.getLongitude();
      int zoom = proj.getZoomLevel();

      SharedPreferences.Editor editor = pref.edit();
      editor.putFloat(getString(R.string.pref_lat), lat);
      editor.putFloat(getString(R.string.pref_lng), lng);
      editor.putInt(getString(R.string.pref_zoom), zoom);
      editor.commit();
    } catch (Exception err) {
      Log.e("save map position", "", err);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString("savedWays", routesDataStr);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

//    if (socketReceiver != null) {
//      unregisterReceiver(socketReceiver);
//    }

    // notify bus position service
    Intent intent = new Intent("gortrans-bus-service");
    intent.putExtra("event", "activity-offline");
    sendBroadcast(intent);
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
/*
  private class SyncData extends AsyncTask<URL, Void, String> {
    final String TAG = "sync request";
    private boolean loaded;

    public SyncData(boolean _loaded) {
      loaded = _loaded;
    }

    protected String doInBackground(URL... urls) {
      HttpURLConnection connection = null;
      BufferedReader reader = null;

      long routesTimestamp = 0, trassesTimestamp = 0, stopsTimestamp = 0;

      JSONObject result = new JSONObject(), newResult = new JSONObject();

      String syncFileString = "", syncWebStr = "";
      if (FileAPI.isFileExists(getBaseContext(), getString(R.string.routes_file))) {
        try {
          syncFileString = FileAPI.readFile(getBaseContext(), getString(R.string.routes_file));
          result = new JSONObject(syncFileString);
          routesTimestamp = JSONParser.getTimestamp(result, "routes");
          trassesTimestamp = JSONParser.getTimestamp(result, "trasses");
          stopsTimestamp = JSONParser.getTimestamp(result, "stopsData");
        } catch (JSONException err) {
          Log.e(TAG, "read file json error", err);
        } catch (Exception err) {
          Log.e(TAG, "read file general error", err);
        }
      }

      try {
        if (!loaded) {
          URL syncUrl = new URL(
                  getString(R.string.base_url) +
                          "/sync?" +
                          "routestimestamp=" + routesTimestamp +
                          "&trassestimestamp=" + trassesTimestamp +
                          "&stopstimestamp=" + stopsTimestamp
          );

          connection = (HttpURLConnection) syncUrl.openConnection();
          connection.setRequestMethod("GET");
          connection.setConnectTimeout(5000);
          connection.setReadTimeout(5000);
          connection.connect();

          InputStream input = connection.getInputStream();
          StringBuffer buffer = new StringBuffer();

          if (input != null) {
            reader = new BufferedReader(new InputStreamReader(input));

            String line;
            while ((line = reader.readLine()) != null) {
              buffer.append(line + "\n");
            }

            syncWebStr = buffer.toString();
          }
        }
      } catch (SocketTimeoutException e) {
        Log.e(TAG, "error", e);
      } catch (IOException e) {
        Log.e(TAG, "error", e);
      } finally {
        if (connection != null) {
          connection.disconnect();
        }
        if (reader != null) {
          try {
            reader.close();
          } catch (final IOException e) {
            Log.e(TAG, "closing stream", e);
          }
        }
      }

      try {
        if (syncWebStr.length() > 0) {
          if (syncFileString.length() == 0) {
            syncFileString = syncWebStr;
          }
          newResult = new JSONObject(syncFileString);

          System.out.println("I got a JSONObject: " + newResult);

          long newRoutesTimestamp = JSONParser.getTimestamp(newResult, "routes");
          long newTrassesTimestamp = JSONParser.getTimestamp(newResult, "trasses");
          long newStopsTimestamp = JSONParser.getTimestamp(newResult, "stopsData");
          if (newStopsTimestamp > stopsTimestamp ||
                  newTrassesTimestamp > trassesTimestamp ||
                  newRoutesTimestamp > routesTimestamp
                  ) { // overwrite if any timestamp changed
            FileAPI.writeFile(getBaseContext(), getString(R.string.routes_file), syncFileString);
            result = newResult;
            syncFileString = syncWebStr;
          }
        }

        wayGroupsStr = syncFileString;
        wayGroups = JSONParser.getWayGroups(result);

        System.out.println(wayGroups);
      } catch (JSONException err) {

      }


      return "";
    }

    protected void onPostExecute(String time) {
      waysLoaded = true;
      showBtns(false);
    }
  }
*/

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

  private void addBusToMap(String code) {
    Log.e(LOG_TAG, code);
  }

  private void updateBusOnMap(String code) {

  }

  private void removeBusFromMap(String code) {

  }


//  public void showBusSearch() {
//    if (menuHolder != null) {
//      BusSearchFragment menu = new BusSearchFragment();
//      Bundle bundle = new Bundle();
//      bundle.putSerializable("ways", wayGroups);
//      menu.setArguments(bundle);
//
//      getSupportFragmentManager()
//              .beginTransaction()
//              .replace(R.id.fragment_view, menu)
//              .addToBackStack(null)
//              .commit();
//    }
//  }

  public void hideBtns() {

  }

  public void showBtns(boolean fromEmpty) {

  }

  // permissions handling
  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    switch (requestCode) {
      case COARSE_LOCATION_PERMISSION_GRANTED:
      case FINE_LOCATION_PERMISSION_GRANTED:
      case WRITE_STORAGE_PERMISSION_GRANTED: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          notGrantedPermissions--;
        }
      }

      // other 'case' lines to check for other
      // permissions this app might request
    }

    if (notGrantedPermissions == 0) {
//      if (wayGroups == null)
//      {
//        new SyncData(false).execute();
//        init();
//      }
//      else
//      {
//        waysLoaded = true;
//        showBtns(false);
//      }
      recreate();
    }

    return;
  }
}
