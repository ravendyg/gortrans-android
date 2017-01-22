package info.nskgortrans.maps.Services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Debug;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import android.os.Handler;

import java.net.URL;
import java.security.Timestamp;
import java.util.ArrayList;
import java.util.logging.LogRecord;

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
  private static final long DATA_WITHOUT_ACTIVITY = 1000 * 60 * 5;
  private static final long LIVE_WITHOUT_ACTIVITY = 1000 * 60 * 30;

  private static final long SYNC_VALID_FOR = 1000 * 60 * 60 * 24;

  private  String apiKey;

  private Handler countDownHandler;
  private Runnable countDownRunnableData, countDownRunnableLife;
  private boolean dataLoaded = false;

  private SharedPreferences pref;

  private Socket socketIO;

  private BroadcastReceiver mainReceiver;

  private ArrayList<WayGroup> wayGroups;
  private String routesDataStr = "";


  public void onCreate()
  {
    super.onCreate();

    countDownHandler = new Handler();

    pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

    apiKey = pref.getString(getString(R.string.pref_api_key), null);
    if (apiKey == null)
    {
      apiKey = String.valueOf(Math.random()).substring(2);
      SharedPreferences.Editor editor = pref.edit();
      editor.putString(getString(R.string.pref_api_key), apiKey);
      editor.commit();
    }

    loadData();


//    // hardcoded behaviour: register receiver that will listen for bus 36
//    if (socketReceiver == null)
//    {
//      socketReceiver = new BroadcastReceiver()
//      {
//        @Override
//        public void onReceive(Context context, Intent intent)
//        {
//          String eventType = intent.getStringExtra("event");
//          if ( eventType.equals("request add bus") )
//          {
//            String busCode = intent.getStringExtra("busCode");
//            addBusListener(busCode);
//          }
//        }
//      };
//    }
//    registerReceiver(socketReceiver, new IntentFilter("gortrans-socket-service"));

    if (mainReceiver == null)
    {
      mainReceiver = new BroadcastReceiver()
      {
        @Override
        public void onReceive(Context context, Intent intent)
        {
          String eventType = intent.getStringExtra("event");
          if (eventType.equals("activity-offline"))
          {
            startFinalCountdown();
          }
          else if (eventType.equals("activity-online"))
          {
            if (countDownRunnableData != null) {
              countDownHandler.removeCallbacks(countDownRunnableData);
              countDownRunnableData = null;
            }
            if (countDownRunnableLife != null) {
              countDownHandler.removeCallbacks(countDownRunnableLife);
              countDownRunnableLife = null;
            }
            // if activity connected to already running service, provide it with data
            if (dataLoaded)
            {
              sendWayGroupsToMain();
            }
          }
        }
      };
    }
    registerReceiver(mainReceiver, new IntentFilter("gortrans-bus-service"));

//    connectToSocket();
  }

  public int onStartCommand(Intent intent, int flags, int startId)
  {
    return super.onStartCommand(intent, flags, startId);
  }

  public IBinder onBind(Intent intent)
  {
    Log.e(LOG_TAG, "onBind");
    return new Binder();
  }

  public void onDestroy() {
    super.onDestroy();

  }


  private void startFinalCountdown()
  {
    countDownRunnableData = new Runnable()
    {
      @Override
      public void run()
      {
        // stop receiving data
      }
    };
    countDownHandler.postDelayed(countDownRunnableData, DATA_WITHOUT_ACTIVITY);

    countDownRunnableLife = new Runnable()
    {
      @Override
      public void run()
      {
        stopSelf();
      }
    };
    countDownHandler.postDelayed(countDownRunnableLife, LIVE_WITHOUT_ACTIVITY);
  }

  /**
   * try to load data from memoty
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
      dataLoaded = true;
      sendWayGroupsToMain();
    }
    catch (JSONException err)
    {
      Log.e(LOG_TAG, "after load", err);
    }
  }

  private void sendWayGroupsToMain()
  {
    Intent intent = new Intent("gortrans-main-activity");
    intent.putExtra("event", "wayGroups");
    intent.putExtra("way-groups", routesDataStr);
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
          socketIO = IO.socket("https://maps.nskgortrans.info");
        }
        catch (URISyntaxException e)
        {
          Log.e("socket", "", e);
        }

        socketIO.connect();

        socketIO.on("connect", broadcastConnectionEstablished);

        socketIO.on("bus listener created", busListenerCreated);
      }
    }).start();
  }

  private Emitter.Listener broadcastConnectionEstablished = new Emitter.Listener()
  {
    @Override
    public void call(final Object... args)
    {
      Intent intent = new Intent("gortrans-socket-activity");
      intent.putExtra("event", "connection");
      sendBroadcast(intent);
    }
  };

  private Emitter.Listener busListenerCreated = new Emitter.Listener()
  {
    @Override
    public void call(final Object... args)
    {
      JSONObject data = (JSONObject) args[0];

      Intent intent = new Intent("gortrans-socket-activity");
      intent.putExtra("event", "bus listener created");
      sendBroadcast(intent);
    }
  };

  public void addBusListener(String code)
  {
    socketIO.emit("add bus listener", code);
  }

}

