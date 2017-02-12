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
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import info.nskgortrans.maps.DataClasses.BusRoute;
import info.nskgortrans.maps.DataClasses.StopInfo;
import info.nskgortrans.maps.DataClasses.StopMarker;

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

  private Drawable stopImage;
  private Drawable userImage;

  private String nextToZoomOn;

  // data
  private HashMap<String, StopInfo> stops;
  private HashMap<String, HashSet<String>> busStops;
  private HashMap<String, BusRoute> busRoutes;
  // stop markers on the map with corresponding routes counter
  private HashMap<String, StopMarker> stopsOnMap = new HashMap<>();
  // route polylines
  private HashMap<String, Polyline> busRoutesOnMap = new HashMap<>();
  private HashSet<String> routeDisplayed = new HashSet<>();


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

    stopImage = ContextCompat.getDrawable(ctx, R.drawable.bus_stop2);
    Bitmap stopBitmap = ((BitmapDrawable) stopImage).getBitmap();
    stopImage = new BitmapDrawable(ctx.getResources(), Bitmap.createScaledBitmap(stopBitmap, 50, 50, true));
    userImage = ContextCompat.getDrawable(ctx, R.drawable.pin);
    Bitmap userBitmap = ((BitmapDrawable) userImage).getBitmap();
    userImage = new BitmapDrawable(ctx.getResources(), Bitmap.createScaledBitmap(userBitmap, 50, 80, true));

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

  public void loadStops(final HashMap<String, StopInfo> stops, final HashMap<String, HashSet<String>> busStops)
  {
    this.stops = stops;
    this.busStops = busStops;
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
      userMarker.setTitle(ctx.getString(R.string.user_marker));
      userMarker.setPosition(new GeoPoint(location));
      userMarker.setIcon(userImage);
      userMarker.setAnchor(Marker.ANCHOR_CENTER, 1.0f);
      map.getOverlays().add(userMarker);
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

  public void zoomToRoute(final String code)
  {

  }


  public void addBus(final String code, int color, boolean zoom)
  {
    if (zoom)
    {
      nextToZoomOn = code;
    }

    if (hasPolyline(code))
    {
      Polyline poly = busRoutesOnMap.get(code);
      poly.setColor(color);
    }

    addBusStops(code);

    tryToZoom();

    map.invalidate();
  }

  public void updateBusRoute(final String code, final BusRoute route)
  {
    busRoutes.put(code, route);
    if (busRoutesOnMap.containsKey(code))
    {
      // replace the old polyline with a new one
    }
  }

  public void removeBus(final String busCode)
  {
    // remove route
    removeBusStops(busCode);
    if (routeDisplayed.contains(busCode))
    {
      map.getOverlays().remove(busRoutesOnMap.get(busCode));
    }
    map.invalidate();
  }

  public boolean hasPolyline(final String busCode)
  {
    return busRoutesOnMap.containsKey(busCode);
  }

  public void addPolyline(String busCode, ArrayList<GeoPoint> points, String color, boolean update)
  {
    Polyline poly = new Polyline();

    if (hasPolyline(busCode) && update)
    {
      if (routeDisplayed.contains(busCode))
      {
        map.getOverlays().remove(busRoutesOnMap.get(busCode));
      }
      busRoutesOnMap.remove(busCode);
    }
    else if (hasPolyline(busCode))
    {
      return;
    }

    poly.setPoints(points);
    busRoutesOnMap.put(busCode, poly);

    if (!routeDisplayed.contains(busCode) && color != null)
    {
      routeDisplayed.add(busCode);
      poly.setColor(ContextCompat.getColor(ctx, Integer.parseInt(color)));
      map.getOverlays().add(poly);
      resetStopAndBusMarkers();
      map.invalidate();
    }
  }

  private void addBusStops(final String code)
  {
    Iterator<String> stopIds = busStops.get(code).iterator();
    while (stopIds.hasNext())
    {
      String id = stopIds.next();
      if (stopsOnMap.containsKey(id))
      {
        stopsOnMap.get(id).addBus(code);
      }
      else
      {
        stopsOnMap.put(id,
                new StopMarker(
                        stopMarkerFactory(stops.get(id)), code
                )
        );
      }
    }
  }

  private void resetStopAndBusMarkers()
  {
    Iterator stopIterator = stopsOnMap.keySet().iterator();
    while (stopIterator.hasNext())
    {
      String key = (String) stopIterator.next();
      Marker mr = stopsOnMap.get(key).getMarker();
      map.getOverlays().remove(mr);
      map.getOverlays().add(mr);
    }
  }

  private void tryToZoom()
  {
    if (nextToZoomOn != null)
    {
      // zoom
      nextToZoomOn = null;
    }
  }

  private void removeBusStops(final String code)
  {
    boolean closed = false;
    Iterator<String> stopIds = busStops.get(code).iterator();
    while (stopIds.hasNext())
    {
      String id = stopIds.next();
      if (stopsOnMap.containsKey(id))
      {
        StopMarker temp = stopsOnMap.get(id);
        if (!closed)
        {
          // since right now it's a bit of problem to detect where popup opened always close
          InfoWindow wn = temp.getMarker().getInfoWindow();
          if (wn.isOpen())
          {
            wn.close();
          }
          closed = true;
        }
        int left = temp.removeBus(code);
        if (left == 0)
        {
          Marker mr = temp.getMarker();
          mr.remove(map);
          stopsOnMap.remove(id);
        }
      }
    }
  }


  private void hideUserInfo()
  {
    if (userInfoWindow != null && userInfoWindow.isOpen())
    {
      userInfoWindow.close();
    }
  }


  private Marker stopMarkerFactory(StopInfo info)
  {
    Marker mr = new Marker(map);
    mr.setPosition(new GeoPoint(info.lat, info.lng));
    mr.setIcon(stopImage);
    mr.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
    mr.setTitle(info.name);
    map.getOverlays().add(mr);
    return mr;
  }
}
