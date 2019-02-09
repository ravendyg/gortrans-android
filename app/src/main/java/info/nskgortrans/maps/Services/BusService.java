package info.nskgortrans.maps.Services;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import android.os.Handler;
import android.os.Message;

import java.net.URI;
import java.util.HashMap;

import info.nskgortrans.maps.Constants;
import info.nskgortrans.maps.DataClasses.EWsRequestType;
import info.nskgortrans.maps.DataClasses.UpdateParcel;
import info.nskgortrans.maps.JSONParser;

public class BusService implements IBusService {
    public static final int BUS_UPDATE_WHAT = 3;

    private WebSocketClient client;
    private final String BASE_URL = Constants.WS_PREFIX + Constants.BASE_URL;
    private final String apiKey;
    private final Handler handler;

    public BusService(Handler handler, String apiKey) {
        this.handler = handler;
        this.apiKey = apiKey;
    }

    @Override
    public void start() {
        (new Thread() {
            @Override
            public void run() {
                try {
                    URI uri = new URI(BASE_URL + "/ws?api_key=" + apiKey + "&asd=dsf");
                    System.out.println(BASE_URL + "/ws");
                    client = new WebSocketClient(uri) {
                        @Override
                        public void onOpen(ServerHandshake serverHandshake) {

                        }

                        @Override
                        public void onMessage(String s) {
                            try {
                                JSONObject socketMessage = new JSONObject(s);
                                int type = socketMessage.getInt("type");
                                if (type == EWsRequestType.STATE || type == EWsRequestType.UPDATE) {
                                    JSONObject payload = socketMessage.getJSONObject("payload");
                                    HashMap<String, UpdateParcel> parcels = JSONParser.parseUpdatedBus(payload);
                                    if (parcels == null) {
                                        return;
                                    }
                                    Message message = handler.obtainMessage(BUS_UPDATE_WHAT, parcels);
                                    handler.sendMessage(message);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onClose(int i, String s, boolean b) {
//                    client.connect();
                        }

                        @Override
                        public void onError(Exception e) {
                            e.printStackTrace();
                        }
                    };
                    client.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void subscribe(String code) {
        try {
            JSONObject json = new JSONObject();
            json.put("type", EWsRequestType.SUBSCRIBE);
            json.put("code", code);
            this.client.send(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unsubscrive(String code) {
        try {
            JSONObject json = new JSONObject();
            json.put("type", EWsRequestType.UNSUBSCRIBE);
            json.put("code", code);
            this.client.send(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
