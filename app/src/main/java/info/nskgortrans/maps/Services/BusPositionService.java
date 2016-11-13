package info.nskgortrans.maps.Services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.ShareCompat;
import android.util.Log;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URISyntaxException;

import info.nskgortrans.maps.R;
import info.nskgortrans.maps.Singletons.SocketIO;

/**
 * Created by me on 13/11/16.
 */
public class BusPositionService extends Service
{
  private static final String LOG_TAG = "Bus position service";

  private SocketIO socket;

  public void onCreate()
  {
    super.onCreate();
    Log.e(LOG_TAG, "onCreate");

    connectToSocket();
  }

  public int onStartCommand(Intent intent, int flags, int startId)
  {
    Log.e(LOG_TAG, "onStart");

    return super.onStartCommand(intent, flags, startId);
  }

  public IBinder onBind(Intent intent)
  {
    Log.e(LOG_TAG, "onBind");
    return null;
  }

  public void onDestroy()
  {
    super.onDestroy();
    Log.e(LOG_TAG, "onDestroy");
  }

  private void connectToSocket()
  {
    Log.e(LOG_TAG, "connect");
    new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        Log.e(LOG_TAG, "get instance");
        socket = SocketIO.getInstance();
      }
    }).start();
  }
}

