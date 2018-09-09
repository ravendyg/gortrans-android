package info.nskgortrans.maps.Services;

import info.nskgortrans.maps.Data.TrassData;
import info.nskgortrans.maps.Data.RoutesInfoData;

public interface IHttpService {
    RoutesInfoData getRoutesInfo(long tsp);
    TrassData getTrassData(String code, long tsp);
}
