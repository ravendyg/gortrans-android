package info.nskgortrans.maps.Services;

import info.nskgortrans.maps.Data.TrassData;
import info.nskgortrans.maps.Data.RoutesInfoData;

public interface IHttpService {
    RoutesInfoData getRoutesInfo(String hash);
    TrassData getTrassData(String code, String hash);
}
