package info.nskgortrans.maps;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import info.nskgortrans.maps.DataClasses.Route;
import info.nskgortrans.maps.Services.BusPositionService;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback
{
  private int notGrantedPermissions = 2;
  private final int LOCATION_PERMISSION_GRANTED = 10;
  private final int STORAGE_PERMISSION_GRANTED  = 11;

  private SharedPreferences pref;
  private GoogleMap map;

  private ArrayList<Route> listMarsh;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    // make sure all permissions granted
//    if ( ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
//          PackageManager.PERMISSION_GRANTED
//    )
//    {
//      ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
//              LOCATION_PERMISSION_GRANTED);
//    }
//    else
//    {
//      notGrantedPermissions--;
//    }
//    if ( ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
//            PackageManager.PERMISSION_GRANTED
//            )
//    {
//      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//              STORAGE_PERMISSION_GRANTED);
//    }
//    else
//    {
//      notGrantedPermissions--;
//    }

    pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

    long routesTimestamp = pref.getLong(getString(R.string.routes_timestamp), 0);
    long trassesTimestamp = pref.getLong(getString(R.string.routes_timestamp), 0);
    long stopsTimestamp = pref.getLong(getString(R.string.routes_timestamp), 0);
//    performSync(routesTimestamp, trassesTimestamp, stopsTimestamp);
    performSync(0, 0, 0);

    setContentView(R.layout.activity_main);

    MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_container);
    mapFragment.getMapAsync(this);

    // start bus position service
    startService( new Intent(this, BusPositionService.class) );
  }

  @Override
  protected void onPause()
  {
    super.onPause();

    try
    {
      CameraPosition target = map.getCameraPosition();
      float lat = (float) target.target.latitude;
      float lng = (float) target.target.longitude;
      int zoom = Math.round(target.zoom);

      SharedPreferences.Editor editor = pref.edit();
      editor.putFloat(getString(R.string.pref_lat), lat);
      editor.putFloat(getString(R.string.pref_lng), lng);
      editor.putInt(getString(R.string.pref_zoom), zoom);
      editor.commit();
    }
    catch (Exception err)
    {
      Log.e("save map position", "", err);
    }
  }

  @Override
  protected void onDestroy()
  {
    super.onDestroy();
    // stop bus position service
    stopService( new Intent(this, BusPositionService.class) );
  }

  private void performSync(final long routesTimestamp, final long trassesTimestamp, final long stopsTimestamp)
  {
    final String TAG = "sync request";
    new Thread(new Runnable()
    {
      HttpURLConnection connection = null;
      BufferedReader reader = null;

      @Override
      public void run()
      {
        JSONObject result;

        try
        {
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

          if (input == null)
          {
            return;
          }

          reader = new BufferedReader(new InputStreamReader(input));

          String line;
          while ((line = reader.readLine()) != null)
          {
            buffer.append(line + "\n");
          }

          if (buffer.length() == 0)
          {
            return;
          }

          try
          {
            result = new JSONObject( buffer.toString() );

            System.out.println("I got a JSONObject: " + result);

            SharedPreferences.Editor editor = pref.edit();

            long newRoutesTimestamp = JSONParser.getTimestamp(result, "routes");
            if (newRoutesTimestamp > routesTimestamp)
            {
              editor.putLong( getString(R.string.routes_timestamp), newRoutesTimestamp );
            }
            long newTrassesTimestamp = JSONParser.getTimestamp(result, "trasses");
            if (newTrassesTimestamp > trassesTimestamp)
            {
              editor.putLong( getString(R.string.trasses_timestamp), newTrassesTimestamp );
            }
            long newStopsTimestamp = JSONParser.getTimestamp(result, "stopsData");
            if (newStopsTimestamp > stopsTimestamp)
            {
              editor.putLong( getString(R.string.stops_timestamp), newStopsTimestamp );
            }
            editor.commit();

            listMarsh = JSONParser.getRoutes(result);

            System.out.println(newRoutesTimestamp + " " + newTrassesTimestamp + " " + newStopsTimestamp);
          }
          catch (JSONException err)
          {
            listMarsh = new ArrayList<Route>(Arrays.asList(new Route[0]));
          }
        }
        catch (java.net.SocketTimeoutException e)
        {
         Log.e(TAG, "error", e);
        }
        catch (IOException e)
        {
         Log.e(TAG, "error", e);
        }
        finally
        {
         if (connection != null)
         {
           connection.disconnect();
         }
         if (reader != null)
         {
           try
           {
             reader.close();
           }
           catch (final IOException e)
           {
             Log.e(TAG, "closing stream", e);
           }
         }
        }
      }
    }).start();

    String routesFileString = "";
    if ( FileAPI.isFileExists(getBaseContext(), getString(R.string.routes_file)) )
    {
      routesFileString = FileAPI.readFile(getBaseContext(), getString(R.string.routes_file));
    }
  }


  @Override
  public void onMapReady(GoogleMap _map)
  {
    map = _map;

    float lat = pref.getFloat( getString(R.string.pref_lat), (float)54.908593335436926 );
    float lng = pref.getFloat( getString(R.string.pref_lng), (float)83.0291748046875 );
    int zoom =  pref.getInt( getString(R.string.pref_zoom), 10 );

    LatLng start = new LatLng(lat, lng);
    CameraPosition target = CameraPosition.builder().target(start).zoom(zoom).build();
    _map.moveCamera(CameraUpdateFactory.newCameraPosition(target) );
  }

//  // permissions handling
//  @Override
//  public void onRequestPermissionsResult(int requestCode,
//                                         String permissions[], int[] grantResults) {
//    switch (requestCode) {
//      case INTERNET_PERMISSION_GRANTED:
//      {
//        // If request is cancelled, the result arrays are empty.
//        if (grantResults.length > 0
//                && grantResults[0] == PackageManager.PERMISSION_GRANTED)
//        {
//          performSync(0, 0, 0);
//        }
//        else
//        {
//          // do nothing and let it crash
//        }
//        return;
//      }
//
//      // other 'case' lines to check for other
//      // permissions this app might request
//    }
//  }
}
