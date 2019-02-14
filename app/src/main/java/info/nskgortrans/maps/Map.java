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
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import info.nskgortrans.maps.Data.BusListElementData;
import info.nskgortrans.maps.Data.TrassData;
import info.nskgortrans.maps.Data.WayPointData;
import info.nskgortrans.maps.DataClasses.BusInfo;
import info.nskgortrans.maps.DataClasses.BusRoute;
import info.nskgortrans.maps.DataClasses.StopInfo;
import info.nskgortrans.maps.MapClasses.StopOnMap;
import info.nskgortrans.maps.DataClasses.UpdateParcel;

public class Map {
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

    private HashMap<String, Integer> routeColors;

    // data
    private HashMap<String, StopOnMap> allStops = new HashMap<>();
    private HashMap<String, HashSet<StopOnMap>> busCodeToStops = new HashMap<>();
    private HashMap<String, BusRoute> busRoutes = new HashMap<>();
    // stop markers on the map with corresponding routes counter
    private HashMap<String, Polyline> busRoutesOnMap = new HashMap<>();
    private HashSet<String> routeDisplayed = new HashSet<>();

    private HashMap<String, HashMap<String, Marker>> busMarkers = new HashMap<>();

    private HashMap<Integer, Drawable> busMarkerIcons = new HashMap<>();

    public void init(Context context, View view, SharedPreferences _pref) {
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

        busMarkerIcons.put(R.color.busColor1, createMarkerImage(R.drawable.bus_marker_red));
        busMarkerIcons.put(R.color.busColor2, createMarkerImage(R.drawable.bus_marker_blue));
        busMarkerIcons.put(R.color.busColor3, createMarkerImage(R.drawable.bus_marker_orange));
        busMarkerIcons.put(R.color.busColor4, createMarkerImage(R.drawable.bus_marker_yellow));
        busMarkerIcons.put(R.color.busColor5, createMarkerImage(R.drawable.bus_marker_gray));
        routeColors = new HashMap<>();

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

        map.setMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent scrollEvent) {
                hideUserInfo();
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent zoomEvent) {
                hideUserInfo();
                return false;
            }
        });
    }
/*
    public void loadStops(
            HashMap<String, StopInfo> stopsData,
            HashMap<String, HashSet<String>> busStops
    ) {
        if (stopsData == null) {
            stopsData = new HashMap<>();
        }
        if (busStops == null) {
            busStops = new HashMap<>();
        }
        this.stopsData = stopsData;
        this.busStops = busStops;
    }
*/

    public void saveState() {
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

    public void moveUser(final Location location) {
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


    public void zoomToUser(Location location) {
        if (location != null) {
            Log.e(LOG_TAG + " zoom user", location.toString());
            GeoPoint userPoint = new GeoPoint(location);
            mapController.setCenter(userPoint);
        }
    }

    public void zoomToRoute(final String code) {
        nextToZoomOn = code;
        tryToZoom();
    }

    public void setColor(String code, Integer color) {
        routeColors.put(code, color);
    }

    public void upsertBusMain(final BusListElementData busListElementData, final TrassData trassData) {
        String code = busListElementData.getCode();

        routeDisplayed.add(code);
        addPolyline(code, trassData);
        addBusStops(code, trassData);
        ensureCorrectMarkerZindex();
        ensureBusesDisplayed(code);

        if (busListElementData.isZoom()) {
            nextToZoomOn = code;
            busListElementData.disableZoom();
        }

        tryToZoom();
        map.invalidate();
    }

    public void removeBus(final String busCode) {
        routeDisplayed.remove(busCode);
        removeBusMarker(busCode, null);
        removeBusStops(busCode);
        removePolyline(busCode);
        map.invalidate();
    }

    public void addPolyline(final String busCode, final TrassData trassData) {
        removePolyline(busCode);
        List<WayPointData> waypoints = trassData.getWaypoints();
        removePolyline(busCode);
        Polyline poly = new Polyline();
        List<GeoPoint> geoPoints = new ArrayList<>();
        for (WayPointData wayPointData : waypoints) {
            geoPoints.add(new GeoPoint(wayPointData.getLat(), wayPointData.getLng()));
        }

        poly.setPoints(geoPoints);
        int color = routeColors.get(busCode);
        poly.setColor(ContextCompat.getColor(ctx, color));
        map.getOverlays().add(poly);
        busRoutesOnMap.put(busCode, poly);
        map.invalidate();
    }

    private void removePolyline(String code) {
        Polyline poly = busRoutesOnMap.get(code);
        if (poly != null) {
            map.getOverlays().remove(busRoutesOnMap.get(code));
        }
    }

    public void updateBusMarkers(HashMap<String, UpdateParcel> parcels, boolean reset) {
        Iterator<String> busCodeIterator = parcels.keySet().iterator();
        while (busCodeIterator.hasNext()) {
            String busCode = busCodeIterator.next();
            boolean routeAlreadyDisplayed = routeDisplayed.contains(busCode);
            UpdateParcel parcel = parcels.get(busCode);
            boolean _reset = reset || parcel.reset != null;
            HashMap<String, Marker> buses;
            if (!busMarkers.containsKey(busCode) || _reset) {
                buses = busMarkers.get(busCode);
                if (buses != null) {
                    for (String graph : buses.keySet()) {
                        removeBusMarker(busCode, graph);
                    }
                }
                buses = new HashMap<>();
                busMarkers.put(busCode, buses);
                map.invalidate();
            } else {
                buses = busMarkers.get(busCode);
            }
            HashMap<String, BusInfo> add;
            if (parcel.reset != null) {
                // reset
                add = parcel.reset;
            } else {
                // add
                add = parcel.add;
            }
            for (String graph : add.keySet()) {
                Marker marker = busMarkerFactory(
                        busCode,
                        add.get(graph)
                );
                if (routeAlreadyDisplayed) {
                    map.getOverlays().add(marker);
                }
                buses.put(graph, marker);
            }
            if (_reset) {
                continue;
            }
            // remove
            for (String graph : parcel.remove) {
                removeBusMarker(busCode, graph);
            }
            // update
            Iterator<String> updateIterator = parcel.update.keySet().iterator();
            while (updateIterator.hasNext()) {
                String graph = updateIterator.next();
                updateBusMarker(busCode, parcel.update.get(graph));
            }
        }

        map.invalidate();
    }

    public void dropBus(final String busCode) {
        removeBusMarker(busCode, null);
    }

    public void dropBuses() {
        for (String code : routeDisplayed) {
            dropBus(code);
        }
    }

    private void ensureCorrectMarkerZindex() {
        List<Overlay> routes = new ArrayList<>();
        List<Overlay> stops = new ArrayList<>();
        List<Overlay> buses = new ArrayList<>();
        List<Overlay> overlays = map.getOverlays();
        while (overlays.size() > 0) {
            Overlay overlay = overlays.remove(0);
            // TODO: Can it be done simpler?
            String name = overlay.getClass().getName();
            if (name.equals("org.osmdroid.views.overlay.Polyline")) {
                routes.add(overlay);
            } else if (name.equals("org.osmdroid.views.overlay.Marker")) {
                if (((Marker) overlay).getImage() == stopImage ) {
                    stops.add(overlay);
                } else {
                    buses.add(overlay);
                }
            }
        }
        for (Overlay over : routes) {
            overlays.add(over);
        }
        for (Overlay over : stops) {
            overlays.add(over);
        }
        for (Overlay over : buses) {
            overlays.add(over);
        }
    }

    private void ensureBusesDisplayed(String busCode) {
        HashMap<String, Marker> markers = busMarkers.get(busCode);
        if (markers != null) {
            List<Overlay> overlays = map.getOverlays();
            for (Marker marker : markers.values()) {
                if (!overlays.contains(marker)) {
                    overlays.add(marker);
                }
            }
        }
    }

    private void addBusStops(final String code, TrassData trassData) {
        List<StopInfo> stopInfoList = trassData.getStops();
        HashSet<StopOnMap> busStops = new HashSet<>();
        busCodeToStops.put(code, busStops);
        for (StopInfo stopInfo : stopInfoList) {
            String id = stopInfo.getId();
            StopOnMap stopOnMap = allStops.get(id);
            if (stopOnMap == null) {
                Marker marker = stopMarkerFactory(stopInfo);
                stopOnMap = new StopOnMap(stopInfo, marker);
                allStops.put(id, stopOnMap);
            }
            stopOnMap.addBus(code);
            busStops.add(stopOnMap);
        }
    }

    private void removeBusStops(final String code) {
        boolean popupOpened = true;
        HashSet<StopOnMap> stopsOnMap = busCodeToStops.get(code);
        busCodeToStops.remove(code);
        if (stopsOnMap == null) {
            return;
        }
        for (StopOnMap stopOnMap : stopsOnMap) {
            int busesLeft = stopOnMap.removeBus(code);
            if (busesLeft > 0) {
                // keep the stop
                continue;
            }
            Marker marker = stopOnMap.getMarker();
            if (popupOpened) {
                // need a marker, probably there is a way to get this info from the map itself
                InfoWindow wn = marker.getInfoWindow();
                if (wn.isOpen()) {
                    wn.close();
                }
                popupOpened = false;
            }
            marker.remove(map);
            allStops.remove(stopOnMap.getId());
        }
    }

    private void tryToZoom() {
        if (nextToZoomOn != null) {
            Polyline poly = busRoutesOnMap.get(nextToZoomOn);
            if (poly == null) {
                return;
            }
            double north = -1000, south = 1000, east = -1000, west = 1000;
            Iterator points = poly.getPoints().iterator();
            while (points.hasNext()) {
                GeoPoint point = (GeoPoint) points.next();
                double lat = point.getLatitude();
                if (north < lat) {
                    north = lat;
                }
                if (south > lat) {
                    south = lat;
                }
                double lng = point.getLongitude();
                if (east < lng) {
                    east = lng;
                }
                if (west > lng) {
                    west = lng;
                }
            }
            BoundingBox box = new BoundingBox(north, east, south, west);
            map.zoomToBoundingBox(box, true);
            nextToZoomOn = null;
        }
    }

    private void hideUserInfo() {
        if (userInfoWindow != null && userInfoWindow.isOpen()) {
            userInfoWindow.close();
        }
    }

    private Marker stopMarkerFactory(StopInfo info) {
        Marker mr = new Marker(map);
        mr.setPosition(new GeoPoint(info.getLat(), info.getLng()));
        mr.setIcon(stopImage);
        mr.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        mr.setTitle(info.getName());
        map.getOverlays().add(mr);
        return mr;
    }

    private Marker busMarkerFactory(String busCode, BusInfo info) {
        Marker mr = new Marker(map);
        mr.setPosition(new GeoPoint(info.lat, info.lng));
        int color = routeColors.get(busCode);
        mr.setIcon(busMarkerIcons.get(color));
        mr.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        mr.setRotation(transformAzimuth(info.azimuth));
        mr.setTitle(info.title);
        return mr;
    }

    private void removeBusMarker(String busCode, String graph) {
        HashMap<String, Marker> mrs = busMarkers.get(busCode);
        if (mrs != null) {
            List<Overlay> over = map.getOverlays();

            if (graph != null) {
                Marker marker = mrs.get(graph);
                if (marker != null) {
                    over.remove(marker);
                }
            } else {
                for (Marker marker : mrs.values()) {
                    over.remove(marker);
                }
            }
        }
    }

    private void updateBusMarker(String busCode, BusInfo info) {
        if (busMarkers.containsKey(busCode)) {
            HashMap<String, Marker> mrs = busMarkers.get(busCode);
            if (mrs.containsKey("" + info.graph)) {
                Marker marker = mrs.get("" + info.graph);
                marker.setPosition(new GeoPoint(info.lat, info.lng));
                marker.setRotation(transformAzimuth(info.azimuth));
            }
        }
    }

    private float transformAzimuth(int azimuth) {
        return (90 - azimuth);
    }

    private Drawable createMarkerImage(int resId) {

        Drawable markerImage = ContextCompat.getDrawable(ctx, resId);
        Bitmap redMarkerBitmap = ((BitmapDrawable) markerImage).getBitmap();


        markerImage = new BitmapDrawable(ctx.getResources(),
                Bitmap.createScaledBitmap(redMarkerBitmap, 50, 60, true));

        return markerImage;
    }
}
