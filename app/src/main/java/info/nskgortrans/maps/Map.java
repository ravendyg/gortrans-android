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
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
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

import info.nskgortrans.maps.DataClasses.BusListElementData;
import info.nskgortrans.maps.DataClasses.TrassData;
import info.nskgortrans.maps.DataClasses.WayPointData;
import info.nskgortrans.maps.DataClasses.BusInfo;
import info.nskgortrans.maps.DataClasses.StopInfo;
import info.nskgortrans.maps.MapClasses.BusMarker;
import info.nskgortrans.maps.MapClasses.StopMarker;
import info.nskgortrans.maps.MapClasses.StopOnMap;
import info.nskgortrans.maps.DataClasses.UpdateParcel;
import info.nskgortrans.maps.UIComponents.SettingsDialog;

public class Map {
    private static final String LOG_TAG = "Map service";
    private final String TILE_BASE_URL = "http://tile.nskgortrans.info/";

    private SharedPreferences pref;
    private MapView map;
    private IMapController mapController;
    private Context ctx;

    private Marker userMarker;

    private Drawable stopImage;
    private Drawable userImage;

    private String nextToZoomOn;

    private HashMap<String, Integer> routeColors;

    // data
    private HashMap<String, StopOnMap> allStops = new HashMap<>();
    private HashMap<String, HashSet<StopOnMap>> busCodeToStops = new HashMap<>();
    // stop markers on the map with corresponding routes counter
    private HashMap<String, Polyline> busRoutesOnMap = new HashMap<>();
    private HashSet<String> routeDisplayed = new HashSet<>();

    private HashMap<String, HashMap<String, Marker>> busMarkers = new HashMap<>();

    private HashMap<Integer, Drawable> busMarkerIcons = new HashMap<>();
    private HashMap<Integer, Drawable> colorToMarker = new HashMap<>();
    private int markerType;

    public void init(Context context, View view, SharedPreferences _pref) {
        pref = _pref;

        /** init map */
        map = (MapView) view;
        final ITileSource tileSource = new XYTileSource( "nskgortrans", 1, 20, 256, ".png",
                new String[] {
                        TILE_BASE_URL + "a/",
                        TILE_BASE_URL + "b/",
                        TILE_BASE_URL + "c/",
                        TILE_BASE_URL + "d/",});
        map.setTileSource(tileSource);
        ctx = context;
        map.setMultiTouchControls(true);

        pref = PreferenceManager.getDefaultSharedPreferences(context);
        markerType = pref.getInt(SettingsDialog.MARKER_TYPE, 1);

        stopImage = ContextCompat.getDrawable(ctx, R.drawable.bus_stop2);
        Bitmap stopBitmap = ((BitmapDrawable) stopImage).getBitmap();
        stopImage = new BitmapDrawable(ctx.getResources(), Bitmap.createScaledBitmap(stopBitmap, 50, 50, true));

        busMarkerIcons.put(R.color.busColor1, createMarkerImage(R.drawable.bus_marker_red, false));
        busMarkerIcons.put(R.color.busColor2, createMarkerImage(R.drawable.bus_marker_blue, false));
        busMarkerIcons.put(R.color.busColor3, createMarkerImage(R.drawable.bus_marker_orange, false));
        busMarkerIcons.put(R.color.busColor4, createMarkerImage(R.drawable.bus_marker_yellow, false));
        busMarkerIcons.put(R.color.busColor5, createMarkerImage(R.drawable.bus_marker_gray, false));
        routeColors = new HashMap<>();

        userImage = ContextCompat.getDrawable(ctx, R.drawable.pin);
        Bitmap userBitmap = ((BitmapDrawable) userImage).getBitmap();
        userImage = new BitmapDrawable(ctx.getResources(), Bitmap.createScaledBitmap(userBitmap, 50, 80, true));

        float lat = pref.getFloat(ctx.getString(R.string.pref_lat), (float) 54.984408);
        float lng = pref.getFloat(ctx.getString(R.string.pref_lng), (float) 82.959072);
        float zoom = 12F;
        try {
            zoom = pref.getFloat(ctx.getString(R.string.pref_zoom), zoom);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mapController = map.getController();
        mapController.setZoom(zoom);
        GeoPoint startPoint = new GeoPoint(lat, lng);
        mapController.setCenter(startPoint);
    }

    public void saveState() {
        try { // save current map position and zoom
            Projection proj = map.getProjection();
            GeoPoint center = proj.getBoundingBox().getCenter();

            float lat = (float) center.getLatitude();
            float lng = (float) center.getLongitude();
            float zoom = (float) proj.getZoomLevel();

            SharedPreferences.Editor editor = pref.edit();
            editor.putFloat(ctx.getString(R.string.pref_lat), lat);
            editor.putFloat(ctx.getString(R.string.pref_lng), lng);
            editor.putFloat(ctx.getString(R.string.pref_zoom), zoom);
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

    public void setColor(String code, Integer color, int type) {
        routeColors.put(code, color);
        Drawable icon;
        switch (type) {
            case 1:
                icon = createMarkerImage(R.drawable.bus_90, true);
                break;

            case 2:
                icon = createMarkerImage(R.drawable.trolley_90, true);
                break;

            case 3:
                icon = createMarkerImage(R.drawable.tram_90, true);
                break;

            default:
                icon = createMarkerImage(R.drawable.minibus_90, true);
                break;
        }
        colorToMarker.put(color, icon);
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
            for (BusInfo busInfo : add.values()) {
                Marker marker = busMarkerFactory(
                        busCode,
                        add.get(busInfo.graph)
                );
                if (marker != null) {
                    if (routeAlreadyDisplayed) {
                        map.getOverlays().add(marker);
                    }
                    buses.put(busInfo.graph, marker);
                }
            }
            if (_reset) {
                continue;
            }
            // remove
            for (String graph : parcel.remove) {
                removeBusMarker(busCode, graph);
            }
            // update
            for (BusInfo busInfo : parcel.update.values()) {
                updateBusMarker(busCode, busInfo);
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

    public void updateIcons(int markerType) {
        this.markerType = markerType;
        for(String busCode : busMarkers.keySet()) {
            Drawable icon = getIcon(routeColors.get(busCode));
            for(Marker marker : busMarkers.get(busCode).values()) {
                marker.setIcon(icon);
            }
        }
        map.invalidate();
    }

    public void changeMarkerType(int newType) {
        markerType = newType;
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
            } else if (name.equals("info.nskgortrans.maps.MapClasses.BusMarker")) {
                buses.add(overlay);
            } else if (name.equals("info.nskgortrans.maps.MapClasses.StopMarker")) {
                stops.add(overlay);
            } else {
                // user marker
                stops.add(overlay);
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
            double width = east - west;
            west -= width *0.05;
            east += width *0.05;
            double height = north - south;
            south -= height *0.05;
            north += height *0.05;
            BoundingBox box = new BoundingBox(north, east, south, west);
            map.zoomToBoundingBox(box, true);
            nextToZoomOn = null;
        }
    }

    private Marker stopMarkerFactory(StopInfo info) {
        Marker mr = new StopMarker(map);
        mr.setPosition(new GeoPoint(info.getLat(), info.getLng()));
        mr.setIcon(stopImage);
        mr.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        mr.setTitle(info.getName());
        map.getOverlays().add(mr);
        return mr;
    }

    private Marker busMarkerFactory(String busCode, BusInfo info) {
        try {
            Marker mr = new BusMarker(map);
            mr.setPosition(new GeoPoint(info.lat, info.lng));
            int color = routeColors.get(busCode);
            mr.setIcon(getIcon(color));
            mr.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            mr.setRotation(transformAzimuth(info.azimuth));
            mr.setTitle(info.title);
            return mr;
        } catch (Exception err) {
            err.printStackTrace();
            return null;
        }
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

    private Drawable getIcon(int color) {
        return markerType == 1 ? busMarkerIcons.get(color) : colorToMarker.get(color);
    }

    private float transformAzimuth(int azimuth) {
        return (azimuth + 270) % 360;
    }

    private Drawable createMarkerImage(int resId, boolean isOld) {

        Drawable markerImage = ContextCompat.getDrawable(ctx, resId);
        Bitmap redMarkerBitmap = ((BitmapDrawable) markerImage).getBitmap();


        markerImage = new BitmapDrawable(ctx.getResources(),
                Bitmap.createScaledBitmap(redMarkerBitmap, isOld ? 75 : 50, isOld ? 90 : 60, true));

        return markerImage;
    }
}
