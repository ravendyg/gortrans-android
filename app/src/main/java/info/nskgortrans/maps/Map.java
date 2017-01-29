package info.nskgortrans.maps;

/**
 * Created by me on 22/01/17.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import info.nskgortrans.maps.DataClasses.StopInfo;

public class Map
{
  private final int FINE_LOCATION_PERMISSION_GRANTED = 11;

  private SharedPreferences pref;
  private MapView map;
  private IMapController mapController;
  private Context ctx;
  private Location location;

  private Marker userMarker;
  private InfoWindow userInfoWindow;

  private HashMap<String, StopInfo> stops;
  private HashMap<String, HashSet<String>> busStops;
  private HashMap<String, HashSet<String>> stopBuses = new HashMap<>();
  private HashMap<String, Marker> stopMarkersOnMap = new HashMap<>();

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

    mapController = map.getController();
    mapController.setZoom(zoom);
    GeoPoint startPoint = new GeoPoint(lat, lng);
    mapController.setCenter(startPoint);

    map.setMapListener(new MapListener()
    {
      @Override
      public boolean onScroll(ScrollEvent scrollEvent)
      {
        hideUserInfo();
        return false;
      }

      @Override
      public boolean onZoom(ZoomEvent zoomEvent)
      {
        hideUserInfo();
        return false;
      }
    });
  }

  public void loadStops(final HashMap<String, StopInfo> _stops, final HashMap<String, HashSet<String>> _busStops)
  {
    stops = _stops;
    busStops = _busStops;
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

  public void moveUser(final Location location)
  {
    if (userMarker == null)
    {
      userMarker = new Marker(map);
      userMarker.setPosition(new GeoPoint(location));
      Drawable image = ContextCompat.getDrawable(ctx, R.drawable.pin);
      Bitmap bitmap = ((BitmapDrawable) image).getBitmap();
      image = new BitmapDrawable(ctx.getResources(), Bitmap.createScaledBitmap(bitmap, 50, 80, true));
      userMarker.setIcon(image);
      userMarker.setAnchor(Marker.ANCHOR_CENTER, 1.0f);
      userInfoWindow = new UserInfoWindow(R.layout.bubble, map);
      userMarker.setInfoWindow(userInfoWindow);
      map.getOverlays().add(userMarker);
      userMarker.setOnMarkerClickListener(new Marker.OnMarkerClickListener()
      {
        @Override
        public boolean onMarkerClick(Marker marker, MapView mapView)
        {
          if (userInfoWindow.isOpen())
          {
            userInfoWindow.close();
          }
          else
          {
            userInfoWindow.open(userMarker, new GeoPoint(location), 0, -70);
          }
          return true;
        }
      });
    }
    else
    {
      userMarker.setPosition(new GeoPoint(location));
    }
    map.invalidate();
    Log.e("save map position", location.toString());
  }


  public void zoomToUser(Location location)
  {
    Log.e("zoom to user", location.toString());
    GeoPoint userPoint = new GeoPoint(location);
    mapController.setCenter(userPoint);
  }

  public void addBusStops(final String code)
  {
    Iterator<String> stopIds = busStops.get(code).iterator();
    while (stopIds.hasNext())
    {
      String id = stopIds.next();
      HashSet<String> thisStopRoutes = stopBuses.get(id);
      if (thisStopRoutes == null)
      { // create record and a marker
        thisStopRoutes = new HashSet<String>();
        stopMarkersOnMap.put(id, null);
      }
      thisStopRoutes.add(code);
      stopBuses.put(id, thisStopRoutes);
    }
  }

  public void removeBusStops(final String code)
  {
    Iterator<String> stopIds = busStops.get(code).iterator();
    while (stopIds.hasNext())
    {
      String id = stopIds.next();
      HashSet<String> thisStopRoutes = stopBuses.get(id);
      if (thisStopRoutes == null)
      {
        thisStopRoutes = new HashSet<String>();
      }
      thisStopRoutes.remove(code);
      if (thisStopRoutes.size() == 0)
      { // remove marker and HashSet from stopBuses
        Marker stopMarkerToRemove = stopMarkersOnMap.get(id);
        if (stopMarkerToRemove != null)
        {
          stopMarkerToRemove.remove(map);
          stopMarkersOnMap.remove(id);
        }
        if (stopBuses.containsKey(id))
        {
          stopBuses.remove(id);
        }
      }
      else
      { // replace
        stopBuses.put(id, thisStopRoutes);
      }
    }
  }

//    Iterator<String> stopMarkersOnMapIds = stopMarkersOnMap.keySet().iterator();
//    // remove those missing in the new HashMap
//    while (stopMarkersOnMapIds.hasNext())
//    {
//      String stopId = stopMarkersOnMapIds.next();
//      if (!busStops.containsKey(stopId))
//      {
//        Marker markerToRemove =  stopMarkersOnMap.get(stopId);
//        if (markerToRemove != null)
//        {
//          markerToRemove.remove(map);
//        }
//        stopMarkersOnMap.remove(stopId);
//      }
//    }
//    // add new stops
//    stopMarkersOnMapIds = stopMarkersOnMap.keySet().iterator();
//    while (stopMarkersOnMapIds.hasNext())
//    {
//      String stopId = stopMarkersOnMapIds.next();
//      if (!busStops.containsKey(stopId))
//      {
//        Marker markerToRemove =  stopMarkersOnMap.get(stopId);
//        if (markerToRemove != null)
//        {
//          markerToRemove.remove(map);
//        }
//        stopMarkersOnMap.remove(stopId);
//      }
//      // add new stops
//
//    }
//  }

  private void hideUserInfo()
  {
    if (userInfoWindow != null && userInfoWindow.isOpen())
    {
      userInfoWindow.close();
    }
  }

  private class UserInfoWindow extends InfoWindow
  {
    public UserInfoWindow(int layoutResId, MapView mapView)
    {
      super(layoutResId, mapView);
    }

    public void onClose()
    {
    }

    public void onOpen(Object arg0)
    {
//      LinearLayout layout = (LinearLayout) mView.findViewById(R.id.bubble);

//      layout.setOnClickListener(new OnClickListener() {
//        public void onClick(View v) {
//          // Override Marker's onClick behaviour here
//        }
//      });
    }
  }
}
