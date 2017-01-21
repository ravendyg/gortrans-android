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
import android.os.Handler;
import java.util.logging.LogRecord;

/**
 * Created by me on 13/11/16.
 */
public class BusPositionService extends Service
{
  private static final String LOG_TAG = "Bus position service";
  private static final long DATA_WITHOUT_ACTIVITY = 1000 * 60 * 5;
  private static final long LIVE_WITHOUT_ACTIVITY = 1000 * 60 * 30;

  private Handler countDownHandler;
  private Runnable countDownRunnableData, countDownRunnableLife;

  private Socket socketIO;

  private BroadcastReceiver mainReceiver;


  public void onCreate()
  {
    super.onCreate();
    Log.e(LOG_TAG, "onCreate");

    countDownHandler = new Handler();

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

    // hardcoded behaviour: register receiver that will listen for bus 36
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

