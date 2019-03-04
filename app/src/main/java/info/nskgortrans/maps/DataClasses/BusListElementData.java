package info.nskgortrans.maps.DataClasses;

import info.nskgortrans.maps.R;

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
    private boolean zoom;

    public BusListElementData(
            final WayData wayData,
            final int color,
            final boolean zoom,
            final int markerType
    ) {
        this.wayData = wayData;
        this.color = color;
        setIcon(markerType);
        this.name = wayData.getName();
        this.code = wayData.getCode();
        this.type = wayData.getType();
        this.zoom = zoom;
    }

    public void toggleType(int markerType) {
        setIcon(markerType);
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

    public boolean isZoom() {
        return zoom;
    }

    public void disableZoom() {
        this.zoom = false;
    }

    private void setIcon(int markerType) {
        switch (wayData.getType()) {
            case 1:
                icon = markerType == 1 ? R.drawable.bus : R.drawable.bus_90;
                break;

            case 2:
                icon = markerType == 1 ? R.drawable.trolley : R.drawable.trolley_90;
                break;

            case 3:
                icon = markerType == 1 ? R.drawable.tram : R.drawable.tram_90;
                break;

            default:
                icon = markerType == 1 ? R.drawable.minibus : R.drawable.minibus_90;
        }
    }
}
