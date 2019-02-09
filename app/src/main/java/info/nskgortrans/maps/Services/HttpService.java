package info.nskgortrans.maps.Services;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import info.nskgortrans.maps.Data.TrassData;
import info.nskgortrans.maps.Data.RoutesInfoData;

public class HttpService implements IHttpService {
    private static final String BASE_URL = "http://192.168.1.67:3023";
    private String apiKey = "";

    public HttpService(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public RoutesInfoData getRoutesInfo(final String hash) {
        RoutesInfoData routesInfoData = null;
        try {
            URL url = new URL(BASE_URL + "/v2/sync/routes"
                    + "?hash=" + hash
                    + "&api_key=" + apiKey
            );
            JSONObject routesInfoJson = loadData(url);
            if (routesInfoJson != null) {
                routesInfoData = new RoutesInfoData(routesInfoJson);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return routesInfoData;
    }

    @Override
    public TrassData getTrassData(final String code, final String hash) {
        TrassData trassData = null;
        try {
            URL url = new URL(BASE_URL + "/v2/sync/trass/"
                    + code
                    + "?hash=" + hash
                    + "&api_key=" + apiKey
            );
            JSONObject trassDataJson = loadData(url);
            if (trassDataJson != null) {
                trassData = new TrassData(code, trassDataJson);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return trassData;
    }

    private JSONObject loadData(URL url) {
        JSONObject result = null;
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.connect();

            InputStream input = connection.getInputStream();
            StringBuffer buffer = new StringBuffer();

            if (input != null) {
                reader = new BufferedReader(new InputStreamReader(input));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                String data = buffer.toString();
                result = new JSONObject(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception er) {
                    er.printStackTrace();
                }
            }
        }

        return result;
    }
}
