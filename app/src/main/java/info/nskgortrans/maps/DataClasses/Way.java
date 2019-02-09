package info.nskgortrans.maps.DataClasses;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import static android.R.id.input;

/**
 * Created by me on 6/11/16.
 */
public class Way {
    public String marsh;
    public String name;
    public String stopb;
    public String stope;
    public int type;

    public boolean even = true;

    public Way(JSONObject input, int type) throws JSONException {
        marsh = input.getString("marsh");
        name = input.getString("name");
        stope = input.getString("stope");
        stopb = input.getString("stopb");
        this.type = type + 1;
    }

    public Way(String[] props) {
        type = Integer.parseInt(props[0]);
        marsh = props[2];
        name = props[3];
        stopb = props[4];
        stope = props[5];
    }

    public Way(String name) {
        marsh = "";
        this.name = name;
        stope = "";
        stopb = "";
    }

    public String getCode() {
        return type + "-" + marsh + "-W-" + name;
    }

    public String serialize() {
        String out = "" + type + "|" + getCode() + "|" + marsh + "|" + name + "|" + stopb + "|" + stope;
        return out;
    }
}
