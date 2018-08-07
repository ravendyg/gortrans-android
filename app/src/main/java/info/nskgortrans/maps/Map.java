package info.nskgortrans.maps;

/**
 * Created by me on 22/01/17.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.IntentCompat;
import android.util.Log;
import android.view.View;

import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
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
import java.util.List;
import java.util.Set;

import info.nskgortrans.maps.DataClasses.BusInfo;
import info.nskgortrans.maps.DataClasses.BusRoute;
import info.nskgortrans.maps.DataClasses.StopInfo;
import info.nskgortrans.maps.DataClasses.StopMarker;
import info.nskgortrans.maps.DataClasses.UpdateParcel;

public class Map
{
  private static final String LOG_TAG = "Map service";
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
  private HashMap<String, ArrayList<GeoPoint>> busRoutePoints = new HashMap<>();
  private HashMap<String, Polyline> busRoutesOnMap = new HashMap<>();
  private HashSet<String> routeDisplayed = new HashSet<>();

  private HashMap<String, HashMap<String, Marker>> busMarkers = new HashMap<>();

  private HashMap<String, Drawable> busMarkerIcons = new HashMap<>();
  private HashMap<String, String> colorsMap = new HashMap<>();


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

    busMarkerIcons.put(""+R.color.busColor1, createMarkerImage(R.drawable.bus_marker_red));
    busMarkerIcons.put(""+R.color.busColor2, createMarkerImage(R.drawable.bus_marker_blue));
    busMarkerIcons.put(""+R.color.busColor3, createMarkerImage(R.drawable.bus_marker_orange));
    busMarkerIcons.put(""+R.color.busColor4, createMarkerImage(R.drawable.bus_marker_yellow));
    busMarkerIcons.put(""+R.color.busColor5, createMarkerImage(R.drawable.bus_marker_gray));

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
      Log.e(LOG_TAG, "save map position", err);
    }
  }

  public void moveUser(final Location location)
  {
    if (location != null) {
      if (userMarker == null) {
        userMarker = new Marker(map);
        userMarker.setTitle(ctx.getString(R.string.user_marker));
        userMarker.setPosition(new GeoPoint(location));
        userMarker.setIcon(userImage);
        userMarker.setAnchor(Marker.ANCHOR_CENTER, 1.0f);
        map.getOverlays().add(userMarker);
      } else {
        userMarker.setPosition(new GeoPoint(location));
      }
      map.invalidate();
      Log.e(LOG_TAG + " save posit", location.toString());
    }
  }


  public void zoomToUser(Location location)
  {
      if (location != null) {
          Log.e(LOG_TAG + " zoom user", location.toString());
          GeoPoint userPoint = new GeoPoint(location);
          mapController.setCenter(userPoint);
      }
  }

  public void zoomToRoute(final String code)
  {
    nextToZoomOn = code;
    tryToZoom();
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
      colorsMap.put(code, "" + color);
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
    colorsMap.remove(busCode);
    if (routeDisplayed.contains(busCode))
    {
      map.getOverlays().remove(busRoutesOnMap.get(busCode));
      routeDisplayed.remove(busCode);

      removeBusMarker(busCode, null);
    }
    map.invalidate();
  }

  public boolean hasPolyline(final String busCode)
  {
    return busRoutesOnMap.containsKey(busCode);
  }

  public void addPolyline(String busCode, ArrayList<GeoPoint> points, String color)
  {
    Polyline poly = new Polyline();


    if (routeDisplayed.contains(busCode))
    {
      map.getOverlays().remove(busRoutesOnMap.get(busCode));
    }
    busRoutesOnMap.remove(busCode);

    poly.setPoints(points);
    busRoutesOnMap.put(busCode, poly);
    busRoutePoints.put(busCode, points);

    if (!routeDisplayed.contains(busCode) && color != null)
    {
      routeDisplayed.add(busCode);
    }
    poly.setColor(ContextCompat.getColor(ctx, Integer.parseInt(color)));
    colorsMap.put(busCode, color);
    map.getOverlays().add(poly);
    resetStopAndBusMarkers();
    map.invalidate();

    tryToZoom();
  }

  public void updateBusMarkers(HashMap<String, UpdateParcel> parcels)
  {
    Iterator<String> busCodeIterator = parcels.keySet().iterator();
    while (busCodeIterator.hasNext())
    {
      String busCode = busCodeIterator.next();
      UpdateParcel parcel = parcels.get(busCode);
      HashMap<String, Marker> buses;
      if (!busMarkers.containsKey(busCode))
      {
        buses = new HashMap<>();
        busMarkers.put(busCode, buses);
      }
      else
      {
        buses = busMarkers.get(busCode);
      }
      // add
      Iterator<String> addIterator = parcel.add.keySet().iterator();
      while (addIterator.hasNext())
      {
        String graph = addIterator.next();
        buses.put(graph, busMarkerFactory(busCode, parcel.add.get(graph)));
      }
      // remove
      for (String graph: parcel.remove)
      {
        removeBusMarker(busCode, graph);
      }
      // update
      Iterator<String> updateIterator = parcel.update.keySet().iterator();
      while (updateIterator.hasNext())
      {
        String graph = updateIterator.next();
        updateBusMarker(busCode, parcel.update.get(graph));
      }
    }

    map.invalidate();
  }

  private void addBusStops(final String code)
  {
    Iterator<String> stopIds = busStops.get(code).iterator();
    while (stopIds.hasNext()) {
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
    List<Overlay> over = map.getOverlays();
    Iterator<StopMarker> stopIterator = stopsOnMap.values().iterator();
    while (stopIterator.hasNext())
    {
      Marker mr = stopIterator.next().getMarker();
      over.remove(mr);
      over.add(mr);
    }
    Iterator<HashMap<String, Marker>> busGroupIterator = busMarkers.values().iterator();
    while (busGroupIterator.hasNext())
    {
      Iterator<Marker> busIterator = busGroupIterator.next().values().iterator();
      while (busIterator.hasNext())
      {
        Marker mr = busIterator.next();
        over.remove(mr);
        over.add(mr);
      }
    }
  }

  private void tryToZoom()
  {
    if (nextToZoomOn != null)
    {
      Polyline poly = busRoutesOnMap.get(nextToZoomOn);
      if (poly == null)
      {
        return;
      }
      double north = -1000, south = 1000, east = -1000, west = 1000;
      Iterator points = poly.getPoints().iterator();
      while (points.hasNext())
      {
        GeoPoint point = (GeoPoint) points.next();
        double lat = point.getLatitude();
        if (north < lat)
        {
          north = lat;
        }
        if (south > lat)
        {
          south = lat;
        }
        double lng = point.getLongitude();
        if (east < lng)
        {
          east = lng;
        }
        if (west > lng)
        {
          west = lng;
        }
      }
      BoundingBox box = new BoundingBox(north, east, south, west);
      map.zoomToBoundingBox(box, true);
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

    removeBusesByRoute(code);
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

  private Marker busMarkerFactory(String busCode, BusInfo info)
  {
    Marker mr = new Marker(map);
    mr.setPosition(new GeoPoint(info.lat, info.lng));
    String color = colorsMap.get(busCode);
    mr.setIcon(busMarkerIcons.get(color));
    mr.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
    mr.setRotation(transformAzimuth(info.azimuth));
    mr.setTitle(info.title);
    map.getOverlays().add(mr);
    return mr;
  }

  private void removeBusMarker(String busCode, String graph)
  {
    if (busMarkers.containsKey(busCode))
    {
      List<Overlay> over = map.getOverlays();
      HashMap<String, Marker> mrs = busMarkers.get(busCode);
      if (graph != null)
      {
        if (mrs.containsKey(graph))
        {
          Marker marker = mrs.get(graph);
          over.remove(marker);
        }
      }
      else
      {
        Iterator<Marker> mrIterator = mrs.values().iterator();
        while (mrIterator.hasNext())
        {
          over.remove(mrIterator.next());
        }
      }
      busMarkers.remove(busCode);
    }
  }

  private void removeBusesByRoute(String busCode)
  {
    if (busMarkers.containsKey(busCode))
    {
      HashMap<String, Marker> mrs = busMarkers.get(busCode);
      Iterator<Marker> mrsIterator = mrs.values().iterator();
      while (mrsIterator.hasNext())
      {
        Marker marker = mrsIterator.next();
        map.getOverlays().remove(marker);
      }
    }
  }

  private void updateBusMarker(String busCode, BusInfo info)
  {
    if (busMarkers.containsKey(busCode))
    {
      HashMap<String, Marker> mrs = busMarkers.get(busCode);
      if (mrs.containsKey(""+info.graph))
      {
        Marker marker = mrs.get(""+info.graph);
        marker.setPosition(new GeoPoint(info.lat, info.lng));
        marker.setRotation(transformAzimuth(info.azimuth));
      }
    }
  }

  private float transformAzimuth(int azimuth)
  {
    return (90 - azimuth);
  }

  private Drawable createMarkerImage(int resId)
  {

    Drawable markerImage = ContextCompat.getDrawable(ctx, resId);
    Bitmap redMarkerBitmap = ((BitmapDrawable) markerImage).getBitmap();


    markerImage = new BitmapDrawable(ctx.getResources(),
            Bitmap.createScaledBitmap(redMarkerBitmap, 50, 60, true));

    return markerImage;
  }
}
