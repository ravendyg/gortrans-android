package info.nskgortrans.maps.DataClasses;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class WayData implements Serializable {
    private int type;
    private String marsh;
    private String name;
    public String stopb;
    public String stope;

    public WayData(JSONObject input, int type) throws JSONException {
        marsh = input.getString("m");
        name  = input.getString("n");
        stopb = input.getString("s");
        stope = input.getString("e");
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return type + "-" + marsh + "-W-" + name;
    }
}
