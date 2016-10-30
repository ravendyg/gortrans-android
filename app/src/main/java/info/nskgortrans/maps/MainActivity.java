package info.nskgortrans.maps;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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

  private SharedPreferences pref;
  private GoogleMap map;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
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
