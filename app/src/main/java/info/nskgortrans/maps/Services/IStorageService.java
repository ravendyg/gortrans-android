package info.nskgortrans.maps.Services;

import info.nskgortrans.maps.DataClasses.HistoryData;
import info.nskgortrans.maps.DataClasses.RoutesInfoData;
import info.nskgortrans.maps.DataClasses.TrassData;

public interface IStorageService {
    RoutesInfoData getRoutesInfo();
    void setRoutesInfo(RoutesInfoData routesInfoData);

    HistoryData getSearchHistory();
    void setSearchHistory(HistoryData history);

    TrassData getTrassData(String code);
    void setTrassData(TrassData trassData);
}
