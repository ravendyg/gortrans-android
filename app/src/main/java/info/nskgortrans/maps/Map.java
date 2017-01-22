package info.nskgortrans.maps;

/**
 * Created by me on 22/01/17.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;

public class Map
{
  private SharedPreferences pref;
  private MapView map;
  private Context ctx;

  public void init(Context context, View view, SharedPreferences _pref)
  {
    pref = _pref;
    OpenStreetMapTileProviderConstants.setUserAgentValue(BuildConfig.APPLICATION_ID);

    /** init map */
    map = (MapView) view;
    ctx = context;
    map.setTileSource(TileSourceFactory.MAPNIK);

    map.setBuiltInZoomControls(true);
    map.setMultiTouchControls(true);

    float lat = pref.getFloat(ctx.getString(R.string.pref_lat), (float) 54.908593335436926);
    float lng = pref.getFloat(ctx.getString(R.string.pref_lng), (float) 83.0291748046875);
    int zoom = pref.getInt(ctx.getString(R.string.pref_zoom), 10);

    IMapController mapController = map.getController();
    mapController.setZoom(zoom);
    GeoPoint startPoint = new GeoPoint(lat, lng);
    mapController.setCenter(startPoint);
  }

  public void saveState()
  {
    try { // save current map position and zoom
      Projection proj = map.getProjection();
      GeoPoint center = proj.getBoundingBox().getCenter();

      float lat = (float) center.getLatitude();
      float lng = (float) center.getLongitude();
      int zoom = proj.getZoomLevel();

      SharedPreferences.Editor editor = pref.edit();
      editor.putFloat(ctx.getString(R.string.pref_lat), lat);
      editor.putFloat(ctx.getString(R.string.pref_lng), lng);
      editor.putInt(ctx.getString(R.string.pref_zoom), zoom);
      editor.commit();
    } catch (Exception err) {
      Log.e("save map position", "", err);
    }
  }
}
