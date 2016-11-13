package info.nskgortrans.maps.Services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Created by me on 13/11/16.
 */
public class BusPositionService extends Service
{
  private static final String LOG_TAG = "Bus position service";

  private Socket socketIO;

  private BroadcastReceiver socketReceiver;

  public static Handler handler;


  public void onCreate()
  {
    super.onCreate();
    Log.e(LOG_TAG, "onCreate");

    // hardcoded behaviour: register receiver that will listen for bus 36
    if (socketReceiver == null)
    {
      socketReceiver = new BroadcastReceiver()
      {
        @Override
        public void onReceive(Context context, Intent intent)
        {
          String eventType = intent.getStringExtra("event");
          if ( eventType.equals("request add bus") )
          {
            String busCode = intent.getStringExtra("busCode");
            addBusListener(busCode);
          }
        }
      };
    }
    registerReceiver(socketReceiver, new IntentFilter("gortrans-socket-service"));

    connectToSocket();
  }

  public int onStartCommand(Intent intent, int flags, int startId)
  {
    Log.e(LOG_TAG, "onStart");

    SocketThread socketThread = new SocketThread();
    socketThread.start();

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

  class SocketThread extends Thread
  {
    public void run()
    {
      Looper.prepare();

      handler = new Handler()
      {
        public void handleMessage(Intent intent)
        {
          Log.e("handler", "");
        }

        @Override
        public void publish(LogRecord logRecord)
        {

        }

        @Override
        public void flush()
        {

        }

        @Override
        public void close() throws SecurityException
        {

        }
      };
      Looper.loop();
    }
  }
}

