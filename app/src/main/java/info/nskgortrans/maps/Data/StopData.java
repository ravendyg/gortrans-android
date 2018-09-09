package info.nskgortrans.maps.Data;

public class StopData extends WayPointData {
    private String id;
    private String name;

    public StopData(String id, String name, double lat, double lng) {
        super(lat, lng);
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
