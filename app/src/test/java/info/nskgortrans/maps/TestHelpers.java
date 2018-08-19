package info.nskgortrans.maps;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Random;

public class TestHelpers {
    private static final Random rand = new Random();

    public static JSONObject generateWay() {
        JSONObject res = new JSONObject();
        try {
            String source = "" + rand.nextInt();
            String marsh = source.substring(0, 1);
            String name = source.substring(1, 2);
            String stopb = source.substring(2, 3);
            String stope = source.substring(3, 4);
            res.put("m", marsh);
            res.put("n", name);
            res.put("s", stopb);
            res.put("e", stope);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    }

    public static JSONObject generateType(int type, int capacity) {
        JSONObject res = new JSONObject();
        try {
            res.put("t", type);
            JSONArray ways = new JSONArray();
            for (int i = 0; i < capacity; i++) {
                JSONObject way = generateWay();
                ways.put(way);
            }
            res.put("w", ways);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    }

    public static JSONArray generateRoutesInfo() {
        JSONArray res = new JSONArray();
        int[] types = {1, 2, 3, 8};
        try {
            for (int i : types) {
                JSONObject type = generateType(i, 5);
                res.put(type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    }
}
