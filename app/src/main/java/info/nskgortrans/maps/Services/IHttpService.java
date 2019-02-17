package info.nskgortrans.maps.Services;

import info.nskgortrans.maps.DataClasses.TrassData;
import info.nskgortrans.maps.DataClasses.RoutesInfoData;

public interface IHttpService {
    RoutesInfoData getRoutesInfo(String hash);
    TrassData getTrassData(String code, String hash);
}
