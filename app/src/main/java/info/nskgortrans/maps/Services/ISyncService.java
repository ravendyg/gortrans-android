package info.nskgortrans.maps.Services;

public interface ISyncService {
    Thread syncRoutesInfo();
    Thread syncTrassInfo(String code);
}
