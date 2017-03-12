package info.nskgortrans.maps.Services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import android.os.Handler;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import info.nskgortrans.maps.DataClasses.BusInfo;
import info.nskgortrans.maps.DataClasses.StopInfo;
import info.nskgortrans.maps.DataClasses.UpdateParcel;
import info.nskgortrans.maps.DataClasses.WayGroup;
import info.nskgortrans.maps.FileAPI;
import info.nskgortrans.maps.JSONParser;
import info.nskgortrans.maps.R;

/**
 * Created by me on 13/11/16.
 */
public class BusPositionService extends Service
{
  private static final String LOG_TAG = "Bus position service";

  private static final long SYNC_VALID_FOR = 1000 * 60 * 60 * 24;


  private String apiKey;

  private SharedPreferences pref;

  private Socket socketIO;

  private BroadcastReceiver mainReceiver;

  private ArrayList<WayGroup> wayGroups;
  private String routesDataStr = "";

  private HashSet<String> selectedBuses = new HashSet<>();

  private HashMap<String, StopInfo> stops;
  private HashMap<String, HashSet<String>> busStops;
  private HashMap<String, String> routeLastRefresh = new HashMap<>();
  private HashMap<String, ArrayList<GeoPoint>> lines = new HashMap<>();


  public void onCreate()
  {
    super.onCreate();
  }

  public int onStartCommand(Intent intent, int flags, int startId)
  {
    if (intent == null)
    {
      return super.onStartCommand(intent, flags, startId);
    }

    pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

    apiKey = pref.getString(getString(R.string.pref_api_key), null);
    if (apiKey == null)
    {
      apiKey = String.valueOf(Math.random()).substring(2);
      SharedPreferences.Editor editor = pref.edit();
      editor.putString(getString(R.string.pref_api_key), apiKey);
      editor.commit();
    }

    if (mainReceiver == null)
    {
      mainReceiver = new BroadcastReceiver()
      {
        @Override
        public void onReceive(Context context, Intent intent)
        {
          String eventType = intent.getStringExtra("event");
          if (eventType.equals("add-bus-listener"))
          {
            String busCode = intent.getStringExtra("code");
            selectedBuses.add(busCode);

            //send request to the server
            String tsp = routeLastRefresh.get(busCode);
            if (tsp != null)
            { // loaded from disk
              addBusListener(busCode, Long.parseLong(tsp));
            }
            else
            {
              String routeFileName = "route_" + busCode;
              if (FileAPI.isFileExists(getBaseContext(), routeFileName))
              { // get route from disk
                String routeStr = FileAPI.readFile(getBaseContext(), routeFileName);
                try
                {
                  JSONObject routeData = new JSONObject(routeStr);
                  ArrayList<GeoPoint> points = JSONParser.parseRoutePoints(routeData.getJSONArray("points"));
                  lines.put(busCode, points);
                  tsp = routeData.getString("tsp");
                  addBusListener(busCode, Long.parseLong(tsp));
                  routeLastRefresh.put(busCode, tsp);
                }
                catch (JSONException err)
                {
                  Log.e(LOG_TAG, "parse route", err);
                }
              }
              else
              { // not yet loaded even to the disk
                addBusListener(busCode, 0);
              }
            }
            sendRouteToMain(busCode);
          }
          else if(eventType.equals("remove-bus-listener"))
          {
            String busCode = intent.getStringExtra("code");
            removeBusListener(busCode);
          }
        }
      };
    }
    registerReceiver(mainReceiver, new IntentFilter("info.nskgortrans.maps.gortrans.bus-service"));

    loadData();

    connectToSocket();

    return super.onStartCommand(intent, flags, startId);
  }

  public IBinder onBind(Intent intent)
  {
    Log.e(LOG_TAG, "onBind");
    return new Binder();
  }

  public void onDestroy()
  {
    super.onDestroy();

    Iterator<String> selectedIterator = selectedBuses.iterator();
    while (selectedIterator.hasNext())
    {
      removeBusListener(selectedIterator.next());
    }
    socketIO.disconnect();
    socketIO.close();
  }

  /**
   * try to load data from memory
   */
  private void loadData()
  {
    long lastSync = pref.getLong(getString(R.string.pref_last_sync), 0);
    long now = System.currentTimeMillis();

    long routesTimestamp = 0, stopsTimestamp = 0;

    JSONObject routesData = new JSONObject(), stopsData = new JSONObject();
    if (FileAPI.isFileExists(getBaseContext(), getString(R.string.routes_file)) &&
            FileAPI.isFileExists(getBaseContext(), getString(R.string.stops_file)))
    {
      try
      {
        String routesFileString = FileAPI.readFile(getBaseContext(), getString(R.string.routes_file));
        String stopsFileString = FileAPI.readFile(getBaseContext(), getString(R.string.stops_file));

        routesData = new JSONObject(routesFileString);
        stopsData = new JSONObject(stopsFileString);

        routesTimestamp = JSONParser.getTimestamp(routesData);
        stopsTimestamp = JSONParser.getTimestamp(stopsData);
      }
      catch (JSONException err)
      {
        Log.e(LOG_TAG, "read file json error", err);
      }
      catch (Exception err)
      {
        Log.e(LOG_TAG, "read file general error", err);
      }
    }

    lastSync = 0; // for debugging
    if (now - lastSync > SYNC_VALID_FOR || routesTimestamp == 0 || stopsTimestamp == 0)
    { // outdated or missing data
      sync(now, routesTimestamp, stopsTimestamp, routesData, stopsData);
    }
    else
    {
      afterLoad(routesData, stopsData);
    }
  }

  private void sync(final long now, final long routesTimestamp, final long stopsTimestamp, final JSONObject _routesData, final JSONObject _stopsData)
  {
    new Thread(new Runnable() {
      @Override
      public void run() {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        JSONObject routesData = _routesData, stopsData = _stopsData;

        String syncWebStr = "";
        try
        {
          URL syncUrl = new URL(
            getString(R.string.base_url) +
              "/sync?" +
              "routestimestamp=" + routesTimestamp +
              "&stopstimestamp=" + stopsTimestamp +
              "&api_key=" + apiKey
          );

          connection = (HttpURLConnection) syncUrl.openConnection();
          connection.setRequestMethod("GET");
          connection.setConnectTimeout(5000);
          connection.setReadTimeout(5000);
          connection.connect();

          InputStream input = connection.getInputStream();
          StringBuffer buffer = new StringBuffer();

          if (input != null) {
            reader = new BufferedReader(new InputStreamReader(input));

            String line;
            while ((line = reader.readLine()) != null) {
              buffer.append(line + "\n");
            }

            syncWebStr = buffer.toString();
          }
        }
        catch (SocketTimeoutException e)
        {
          Log.e(LOG_TAG, "error", e);
        }
        catch (IOException e)
        {
          Log.e(LOG_TAG, "error", e);
        }
        finally
        {
          if (connection != null) {
            connection.disconnect();
          }
          if (reader != null) {
            try {
              reader.close();
            } catch (final IOException e) {
              Log.e(LOG_TAG, "closing stream", e);
            }
          }
        }

        try
        {
          if (syncWebStr.length() > 0)
          {
            JSONObject response = new JSONObject(syncWebStr);

            System.out.println("I got a JSONObject: " + response);

            JSONObject syncRoutesData = response.getJSONObject("routes");
            JSONObject syncStopsData = response.getJSONObject("stopsData");

            long newRoutesTimestamp = JSONParser.getTimestamp(syncRoutesData);
            long newStopsTimestamp = JSONParser.getTimestamp(syncStopsData);

            if (newRoutesTimestamp > routesTimestamp)
            { // overwrite if any timestamp changed
              FileAPI.writeFile(getBaseContext(), getString(R.string.routes_file), syncRoutesData.toString());
              routesData = syncRoutesData;
            }

            if (newStopsTimestamp > stopsTimestamp)
            { // overwrite if any timestamp changed
              FileAPI.writeFile(getBaseContext(), getString(R.string.stops_file), syncStopsData.toString());
              stopsData = syncStopsData;
            }

            SharedPreferences.Editor editor = pref.edit();
            editor.putLong(getString(R.string.pref_last_sync), now);
            editor.commit();
          }
        }
        catch (JSONException err)
        {
          Log.e(LOG_TAG, "parsing sync response", err);
        }

        afterLoad(routesData, stopsData);
      }
    }).start();
  }


  private void afterLoad(JSONObject routesData, JSONObject stopsData)
  {
    try
    {
      wayGroups = JSONParser.getWayGroups(routesData);
      routesDataStr = routesData.toString();

      stops = JSONParser.extractStops(stopsData.getJSONObject("stops"));
      busStops = JSONParser.extractBusStops(stopsData.getJSONObject("busStops"));

      sendDataToMain();
    }
    catch (JSONException err)
    {
      stops = new HashMap<>();
      Log.e(LOG_TAG, "after load", err);
    }
  }

  private void sendDataToMain()
  {
    Intent intent = new Intent("info.nskgortrans.maps.main.activity");
    intent.putExtra("event", "data");
    intent.putExtra("way-groups", routesDataStr);
    intent.putExtra("stops", stops);
    intent.putExtra("busStops", busStops);
    LocalBroadcastManager.getInstance(this).
      sendBroadcast(intent);
  }

  private void sendRouteToMain(String busCode)
  {
    if (lines.containsKey(busCode))
    {
      Intent intent = new Intent("info.nskgortrans.maps.main.activity");
      intent.putExtra("event", "points");
      intent.putExtra("busCode", busCode);
      intent.putExtra("points", lines.get(busCode));
      LocalBroadcastManager.getInstance(this).
              sendBroadcast(intent);
    }
  }

  private void sendBusUpdateToMain(HashMap<String, UpdateParcel> parcels)
  {
    Intent intent = new Intent("info.nskgortrans.maps.main.activity");
    intent.putExtra("event", "bus-update");
    intent.putExtra("parcels", parcels);
    LocalBroadcastManager.getInstance(this).
            sendBroadcast(intent);
  }

  private void connectToSocket()
  {
    Log.e(LOG_TAG, "connect");
    new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          socketIO = IO.socket(getString(R.string.base_url) + "?api_key=" + apiKey);
        }
        catch (URISyntaxException e)
        {
          Log.e("socket", "", e);
        }

        socketIO.connect();

        socketIO.on("connect", broadcastConnectionEstablished);

        socketIO.on("bus listener created", busListenerCreated);

        socketIO.on("bus update", busUpdate);
      }
    }).start();
  }

  private Emitter.Listener broadcastConnectionEstablished = new Emitter.Listener()
  {
    @Override
    public void call(final Object... args)
    {
      Intent intent = new Intent("info.nskgortrans.maps.gortrans.socket.activity");
      intent.putExtra("event", "connection");
      sendBroadcast(intent);
    }
  };

  private Emitter.Listener busListenerCreated = new Emitter.Listener()
  {
    @Override
    public void call(final Object... args)
    {
      try
      {
        String busCode = (String) args[0];
        JSONObject buses = (JSONObject) args[1];
        JSONArray jsonPoints = (JSONArray) args[2];

        HashMap<String, UpdateParcel> parcels = JSONParser.parseCreatedBus(buses);
        sendBusUpdateToMain(parcels);

        if (jsonPoints != null && jsonPoints.length() > 0)
        {
          try
          {
            JSONObject routeData = new JSONObject();
            routeData.put("points", jsonPoints);
            String tsp = "" + System.currentTimeMillis();
            routeData.put("tsp", tsp);
            ArrayList<GeoPoint> points = JSONParser.parseRoutePoints(jsonPoints);
            tsp = routeData.getString("tsp");

            routeLastRefresh.put(busCode, tsp);
            lines.put(busCode, points);
            sendRouteToMain(busCode);

            FileAPI.writeFile(getBaseContext(), "route_" + busCode, routeData.toString());
          }
          catch (JSONException err)
          {
            Log.e(LOG_TAG, "parse route", err);
          }
        }
      }
      catch (Exception err)
      {
        Log.e(LOG_TAG, "bus listener created", err);
      }
    }
  };

  private Emitter.Listener busUpdate = new Emitter.Listener()
  {
    @Override
    public void call(final Object... args)
    {
      try
      {
        JSONObject buses = (JSONObject) args[0];
        HashMap<String, UpdateParcel> parcels = JSONParser.parseUpdatedBus(buses);
        sendBusUpdateToMain(parcels);
      }
      catch (Exception err)
      {
        Log.e(LOG_TAG, "bus update", err);
      }
    }
  };

  public void addBusListener(String code, long tsp)
  {
    socketIO.emit("add bus listener", code, tsp);
  }

  public void removeBusListener(String busCode)
  {
    socketIO.emit("remove bus listener", busCode);
    selectedBuses.remove(busCode);
  }
}

