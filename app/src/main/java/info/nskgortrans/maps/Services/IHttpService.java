package info.nskgortrans.maps.Services;

import info.nskgortrans.maps.DataClasses.RoutesInfoData;

public interface IHttpService {
    RoutesInfoData getRoutesInfo(long tsp);
}
