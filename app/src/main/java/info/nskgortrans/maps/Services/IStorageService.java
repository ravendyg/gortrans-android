package info.nskgortrans.maps.Services;

import java.util.ArrayList;

import info.nskgortrans.maps.DataClasses.HistoryData;
import info.nskgortrans.maps.DataClasses.RoutesInfoData;
import info.nskgortrans.maps.DataClasses.WayData;

public interface IStorageService {
    RoutesInfoData getRoutesInfo();
    void setRoutesInfo(RoutesInfoData routesInfoData);

    HistoryData getSearchHistory();
    void setSearchHistory(HistoryData history);
}
