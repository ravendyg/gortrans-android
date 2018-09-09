package info.nskgortrans.maps.Services;

import info.nskgortrans.maps.Data.HistoryData;
import info.nskgortrans.maps.Data.RoutesInfoData;

public interface IStorageService {
    RoutesInfoData getRoutesInfo();
    void setRoutesInfo(RoutesInfoData routesInfoData);

    HistoryData getSearchHistory();
    void setSearchHistory(HistoryData history);
}
