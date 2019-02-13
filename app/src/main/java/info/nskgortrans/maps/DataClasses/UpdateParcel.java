package info.nskgortrans.maps.DataClasses;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.HashMap;

/**
 * Created by me on 10/03/17.
 */

public class UpdateParcel implements Serializable {
    public String id;
    public HashMap<String, BusInfo> add;
    public String[] remove;
    public HashMap<String, BusInfo> update;
    public HashMap<String, BusInfo> reset;

    public UpdateParcel() {

    }
}
