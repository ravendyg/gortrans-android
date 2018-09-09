package info.nskgortrans.maps;

import info.nskgortrans.maps.Data.WayData;

/**
 * Created by me on 22/01/17.
 */

public class Utils {
    public String getTypeString(final int type) {
        switch (type) {
            case 1: return "Автобус";
            case 2: return "Троллейбус";
            case 3: return "Трамвай";
            default: return "Маршрутка";
        }
    }

    public int getType(int index) {
        switch (index) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            default:
                return 8;
        }
    }

    public int mapTypeToPosition(WayData wayData) {
        switch (wayData.getType()) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 2;
            default:
                return 3;
        }
    }

    public long getUnixTsp() {
        return System.currentTimeMillis() / 1000;
    }
}
