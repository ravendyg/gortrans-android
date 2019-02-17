package info.nskgortrans.maps.DataClasses;

import java.io.Serializable;

/**
 * Created by me on 10/03/17.
 */

public class BusInfo implements Serializable {
    public int azimuth;
    public String direction;
    public String graph;
    public int id_typetr;
    public double lat;
    public double lng;
    public String marsh;
    public int speed;
    public String time_nav;
    public String title;

    public BusInfo(
            int _azimuth,
            String _direction,
            String _graph,
            int _id_typetr,
            double _lat,
            double _lng,
            String _marsh,
            int _speed,
            String _time_nav,
            String _title
    ) {
        azimuth = _azimuth;
        direction = _direction;
        graph = _graph;
        id_typetr = _id_typetr;
        lat = _lat;
        lng = _lng;
        marsh = _marsh;
        speed = _speed;
        time_nav = _time_nav;
        title = _title;
    }
}
