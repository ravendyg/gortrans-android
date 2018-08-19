package info.nskgortrans.maps.Services;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import info.nskgortrans.maps.DataClasses.RoutesInfoData;
import info.nskgortrans.maps.R;

public class HttpService implements IHttpService {
    private static final String BASE_URL = "http://192.168.1.67:3023";
    private String apiKey = "";

    public HttpService(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public RoutesInfoData getRoutesInfo(long tsp) {
        RoutesInfoData routesInfoData = null;
        try {
            URL url = new URL(BASE_URL + "/v2/sync/routes"
                    + "?tsp=" + tsp
                    + "&api_key=" + apiKey
            );
            JSONObject routesInfoJson = loadData(url);
            if (routesInfoJson != null) {
                routesInfoData = new RoutesInfoData(routesInfoJson.getJSONArray("data"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return routesInfoData;
    }

    private JSONObject loadData(URL url) {
        JSONObject result = null;
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
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
