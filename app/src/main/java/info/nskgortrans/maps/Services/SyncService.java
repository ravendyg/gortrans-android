package info.nskgortrans.maps.Services;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;

import info.nskgortrans.maps.Constants;
import info.nskgortrans.maps.DataClasses.RoutesInfoData;
import info.nskgortrans.maps.Utils;

public class SyncService implements ISyncService {
    public static final long SYNC_VALID_FOR = 60 * 60 * 24;
    public static final String ROUTES_INFO_TSP = "routes-info-tsp";
    public static final int ROUTES_SYNC_DATA_WHAT = 1;

    private IStorageService storageService;
    private IHttpService httpService;
    private SharedPreferences pref;
    private Utils utils;
    private Handler handler;

    public SyncService(
            IStorageService storageService,
            IHttpService httpService,
            SharedPreferences pref,
            Utils utils,
            Handler handler
    ) {
        this.storageService = storageService;
        this.httpService = httpService;
        this.pref = pref;
        this.utils = utils;
        this.handler = handler;
    }

    @Override
    public Thread syncRoutesInfo() {
        return (new Thread() {
            @Override
            public void run() {
                long tsp = pref.getLong(ROUTES_INFO_TSP, 0L);
                long timestamp = utils.getUnixTsp();
                RoutesInfoData routesInfoData = storageService.getRoutesInfo();
                if (routesInfoData == null || tsp + SYNC_VALID_FOR < timestamp) {
                    RoutesInfoData httpRoutesInfoData = httpService.getRoutesInfo(tsp);
                    if (httpRoutesInfoData != null) {
                        routesInfoData = httpRoutesInfoData;
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putLong(ROUTES_INFO_TSP, timestamp);
                        editor.commit();
                        storageService.setRoutesInfo(httpRoutesInfoData);
                    }
                }
                if (routesInfoData != null) {
                    Message message = handler.obtainMessage(ROUTES_SYNC_DATA_WHAT, routesInfoData);
                    handler.sendMessage(message);
                }
            }
        });
    }
}
