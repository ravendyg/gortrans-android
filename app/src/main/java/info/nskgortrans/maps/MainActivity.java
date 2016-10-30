package info.nskgortrans.maps;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.callback.HttpConnectCallback;

import org.json.JSONObject;

import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener
{
  private int notGrantedPermissions = 2;
  private final int LOCATION_PERMISSION_GRANTED = 10;
  private final int INTERNET_PERMISSION_GRANTED = 11;
  private final int STORAGE_PERMISSION_GRANTED  = 12;
  private final int NETWORK_PERMISSION_GRANTED  = 13;

  private SharedPreferences pref;
  private GoogleMap map;

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
    if ( ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) !=
            PackageManager.PERMISSION_GRANTED
            )
    {
      ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.INTERNET},
              INTERNET_PERMISSION_GRANTED);
    }
    else
    {
      performSync(0, 0, 0);
    }
//    if ( ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
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
    if ( ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
            )
    {
      ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_NETWORK_STATE},
              NETWORK_PERMISSION_GRANTED);
    }
    else
    {
      notGrantedPermissions--;
    }

    setContentView(R.layout.activity_main);

    MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_container);
    mapFragment.getMapAsync(this);

    pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
  }

  private void performSync(long routesTimestamp, long trassesTimestamp, long stopsTimestamp)
  {
    String syncUrl = "https://maps.nskgortrans.info/sync?" +
            "routestimestamp=" + routesTimestamp +
            "&trassestimestamp=" + trassesTimestamp +
            "&stopstimestamp=" + stopsTimestamp;

    System.out.println(syncUrl);

    AsyncHttpClient.getDefaultInstance().execute(syncUrl, new HttpConnectCallback()
    {
      @Override
      public void onConnectCompleted(Exception ex, AsyncHttpResponse response)
      {
        if (ex != null)
        {
          ex.printStackTrace();
          return;
        }

        AsyncHttpRequest request = response.getRequest();
        request.setTimeout(5000);
        AsyncHttpClient.getDefaultInstance().executeJSONObject(request, new AsyncHttpClient.JSONObjectCallback()
        {
          @Override
          public void onCompleted(Exception e, AsyncHttpResponse source, JSONObject result)
          {
            if (e != null)
            {
              e.printStackTrace();
              return;
            }

            System.out.println("I got a JSONObject: " + result);
          }
        });
      }
    });
  }

  @Override
  public void onCameraIdle()
  {
    CameraPosition target = map.getCameraPosition();
    float lat = (float) target.target.latitude;
    float lng = (float) target.target.longitude;
    int zoom = Math.round(target.zoom);

    SharedPreferences.Editor editor = pref.edit();
    editor.putFloat( getString(R.string.pref_lat), lat );
    editor.putFloat( getString(R.string.pref_lng), lng );
    editor.putInt( getString(R.string.pref_zoom), zoom );
    editor.commit();
  }

  @Override
  public void onMapReady(GoogleMap _map)
  {
    map = _map;

    map.setOnCameraIdleListener(this);

    float lat = pref.getFloat( getString(R.string.pref_lat), (float)54.908593335436926 );
    float lng = pref.getFloat( getString(R.string.pref_lng), (float)83.0291748046875 );
    int zoom =  pref.getInt( getString(R.string.pref_zoom), 10 );

    LatLng start = new LatLng(lat, lng);
    CameraPosition target = CameraPosition.builder().target(start).zoom(zoom).build();
    _map.moveCamera(CameraUpdateFactory.newCameraPosition(target) );
  }

  // permissions handling
  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String permissions[], int[] grantResults) {
    switch (requestCode) {
      case INTERNET_PERMISSION_GRANTED:
      {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
          performSync(0, 0, 0);
        }
        else
        {
          // do nothing and let it crash
        }
        return;
      }

      // other 'case' lines to check for other
      // permissions this app might request
    }
  }
}
