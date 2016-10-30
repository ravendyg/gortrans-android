package info.nskgortrans.maps;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
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

import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener
{
  private int notGrantedPermissions = 4;
  private int LOCATION_PERMISSION_GRANTED = 10;
  private int INTERNET_PERMISSION_GRANTED = 11;
  private int STORAGE_PERMISSION_GRANTED  = 12;
  private int NETWORK_PERMISSION_GRANTED  = 13;

  private SharedPreferences pref;
  private GoogleMap map;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    // make sure all permissions granted
    if ( ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
          PackageManager.PERMISSION_GRANTED
    )
    {
      ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
              LOCATION_PERMISSION_GRANTED);
    }
    else
    {
      notGrantedPermissions--;
    }
    if ( ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) !=
            PackageManager.PERMISSION_GRANTED
            )
    {
      ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.INTERNET},
              INTERNET_PERMISSION_GRANTED);
    }
    else
    {
      notGrantedPermissions--;
    }
    if ( ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
            )
    {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
              STORAGE_PERMISSION_GRANTED);
    }
    else
    {
      notGrantedPermissions--;
    }
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
}
