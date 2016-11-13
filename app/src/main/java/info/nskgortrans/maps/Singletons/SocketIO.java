package info.nskgortrans.maps.Singletons;

import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Objects;

import info.nskgortrans.maps.R;

/**
 * Created by me on 13/11/16.
 */
public class SocketIO
{
  private static SocketIO ourInstance = new SocketIO();

  public static SocketIO getInstance()
  {
    return ourInstance;
  }

  private Socket socketIO;

  private SocketIO()
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

    socketIO.on("bus listener created", busListenerCreated);

    socketIO.emit("add bus listener", "1-036-W-36");
  }

  private Emitter.Listener busListenerCreated = new Emitter.Listener()
  {
    @Override
    public void call(final Object... args)
    {
      JSONObject data = (JSONObject) args[0];
    }
  };

}
