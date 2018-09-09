package info.nskgortrans.maps.Services;

public interface ISyncService {
    Thread syncRoutesInfo();
    Thread syncBusStops(String code);
}
