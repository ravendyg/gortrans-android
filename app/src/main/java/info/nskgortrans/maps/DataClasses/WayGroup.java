package info.nskgortrans.maps.DataClasses;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by me on 4/12/16.
 */

public class WayGroup {
    public ArrayList<Way> ways;
    public int type;

    public WayGroup(int _type) {
        type = _type;
        ways = new ArrayList<>(Arrays.asList(new Way[0]));
    }

    public void addWay(Way way) {
        ways.add(way);
    }

    public void setWays(ArrayList<Way> _ways) {
        ways = _ways;
    }

    public ArrayList<Way> getMatching(String filter) {
        ArrayList<Way> out = new ArrayList<>(Arrays.asList(new Way[0]));
        for (int i = 0; i < ways.size(); i++) {
            Way temp = ways.get(i);
            if (filter.equals("") || temp.name.matches(".*" + filter + ".*")) {
                out.add(temp);
            }
        }
        return out;
    }
}
