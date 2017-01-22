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
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;


import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;

import java.util.ArrayList;

import info.nskgortrans.maps.DataClasses.WayGroup;
import info.nskgortrans.maps.Services.BusPositionService;

public class MainActivity extends AppCompatActivity {
  private String LOG_TAG = "main activity";

  final Context context = this;

  private int notGrantedPermissions = 2;
  private final int COARSE_LOCATION_PERMISSION_GRANTED = 10;
  private final int FINE_LOCATION_PERMISSION_GRANTED = 11;
  private final int WRITE_STORAGE_PERMISSION_GRANTED = 12;

  private SharedPreferences pref;

  private ArrayList<WayGroup> wayGroups;
  private String routesDataStr = "";

  private BroadcastReceiver serviceReceiver = null;

  private FrameLayout menuHolder;

  private SearchBusDialog searchDialog;

  private boolean waysLoaded;

  private Dialog searchBusDialog = null;

  private Map map;


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

  public void showSearchBusDialog(View bntView)
  {
    searchBusDialog = searchDialog.showDialog(context, wayGroups);
  }

  public void selectBus(final String code)
  {
    removeDialog();
    Log.e("bus code: ", code);
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

  private void addBusToMap(String code) {
    Log.e(LOG_TAG, code);
  }

  private void updateBusOnMap(String code) {
  }

  private void removeBusFromMap(String code) {
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
