package info.nskgortrans.maps.Data;

import info.nskgortrans.maps.DataClasses.WayData;

/**
 * Created by me on 22/01/17.
 */

public class BusListElementData {
    private WayData wayData;
    private int color;
    private int icon;
    private String name;
    private String code;
    private int type;

    public BusListElementData(final WayData wayData, final int icon, final int color) {
        this.wayData = wayData;
        this.color = color;
        this.icon = icon;
        this.name = wayData.getName();
        this.code = wayData.getCode();
        this.type = wayData.getType();
    }

    public String getName() {
        return this.name;
    }

    public String getCode() {
        return this.code;
    }

    public int getType() {
        return this.type;
    }

    public int getIcon() {
        return this.icon;
    }

    public int getColor() {
        return this.color;
    }
}
