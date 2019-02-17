package info.nskgortrans.maps.Services;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import android.os.Handler;
import android.os.Message;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;

import info.nskgortrans.maps.Constants;
import info.nskgortrans.maps.DataClasses.EWsRequestType;
import info.nskgortrans.maps.DataClasses.UpdateParcel;
import info.nskgortrans.maps.JSONParser;

public class BusService implements IBusService {
    public static final int BUS_STATE_WHAT = 3;

    private WebSocketClient client = null;
    private final String BASE_URL = Constants.WS_PREFIX + Constants.BASE_URL;
    private final String apiKey;
    private final Handler handler;
    private HashSet<String> subscriptions = new HashSet<>();

    public BusService(Handler handler, String apiKey) {
        this.handler = handler;
        this.apiKey = apiKey;
    }

    @Override
    public void start() {
        (new Thread() {
            private void createWS() {
                try {
                    URI uri = new URI(BASE_URL + "/ws?api_key=" + apiKey + "&asd=dsf");
                    System.out.println(BASE_URL + "/ws");

                    client = new WebSocketClient(uri) {
                        @Override
                        public void onOpen(ServerHandshake serverHandshake) {
                            for (String code : subscriptions) {
                                subscribe(code);
                            }
                        }

                        @Override
                        public void onMessage(String s) {
                            try {
                                JSONObject socketMessage = new JSONObject(s);
                                int type = socketMessage.getInt("type");
                                int what;
                                if (type == EWsRequestType.STATE) {
                                    what = BUS_STATE_WHAT;
                                } else {
                                    return;
                                }
                                JSONObject payload = socketMessage.getJSONObject("payload");
                                String id = payload.getString("id");
                                confirm(id);
                                JSONObject data = payload.getJSONObject("data");
                                HashMap<String, UpdateParcel> parcels = JSONParser.parseUpdatedBus(data);
                                if (parcels == null) {
                                    return;
                                }
                                Message message = handler.obtainMessage(what, parcels);
                                handler.sendMessage(message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onClose(int i, String s, boolean b) {
                            try {
                                Thread.sleep(5000);
                                client = null;
                                BusService.this.start();
                            } catch (Exception e) {
                                // don't expect to be interrupted
                            }
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

            @Override
            public void run() {
                createWS();
            }
        }).start();
    }

    @Override
    public void subscribe(String code) {
        try {
            subscriptions.add(code);
            JSONObject json = new JSONObject();
            json.put("type", EWsRequestType.SUBSCRIBE);
            json.put("code", code);
            String message = json.toString();
            if (client != null) {
                this.client.send(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unsubscrive(String code) {
        try {
            subscriptions.remove(code);
            JSONObject json = new JSONObject();
            json.put("type", EWsRequestType.UNSUBSCRIBE);
            json.put("code", code);
            this.client.send(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pause() {
        try {
            for (String code : subscriptions) {
                JSONObject json = new JSONObject();
                json.put("type", EWsRequestType.UNSUBSCRIBE);
                json.put("code", code);
                this.client.send(json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resume() {
        try {
            for (String code : subscriptions) {
                JSONObject json = new JSONObject();
                json.put("type", EWsRequestType.SUBSCRIBE);
                json.put("code", code);
                String message = json.toString();
                if (client != null) {
                    this.client.send(message);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void confirm(String id) {
        try {
            JSONObject json = new JSONObject();
            json.put("type", EWsRequestType.CONFIRM);
            json.put("confirmedId", id);
            String message = json.toString();
            if (client != null) {
                this.client.send(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
