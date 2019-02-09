package info.nskgortrans.maps.Services;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;

import java.util.HashSet;
import java.util.Set;

import info.nskgortrans.maps.Data.RoutesInfoData;
import info.nskgortrans.maps.Data.TrassData;
import info.nskgortrans.maps.Utils;

public class SyncService implements ISyncService {
    public static final long SYNC_VALID_FOR = 60 * 60 * 24;
    public static final String ROUTES_INFO_TSP = "routes-info-tsp";
    public static final String TRASS_INFO_TSP = "routes-info-tsp";
    public static final int ROUTES_SYNC_DATA_WHAT = 1;
    public static final int TRASS_SYNC_DATA_WHAT = 2;

    private IStorageService storageService;
    private IHttpService httpService;
    private SharedPreferences pref;
    private Utils utils;
    private Handler handler;
    // don't need several sync threads for the same bus
    private Set<String> trassesInSync;

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
        this.trassesInSync = new HashSet<>();
    }

    @Override
    public Thread syncRoutesInfo() {
        return (new Thread() {
            @Override
            public void run() {
                String key = ROUTES_INFO_TSP;
                long timestamp = utils.getUnixTsp();
                long tsp = pref.getLong(key, 0L);
                RoutesInfoData routesInfoData = storageService.getRoutesInfo();
                if (routesInfoData != null) {
                    Message message = handler.obtainMessage(ROUTES_SYNC_DATA_WHAT, routesInfoData);
                    handler.sendMessage(message);
                }
                String hash = routesInfoData.getHash();
                if (routesInfoData == null || tsp + SYNC_VALID_FOR < timestamp) {
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putLong(key, timestamp);
                    editor.commit();
                    RoutesInfoData httpRoutesInfoData = httpService.getRoutesInfo(hash);
                    if (httpRoutesInfoData != null) {
                        routesInfoData = httpRoutesInfoData;
                        Message message = handler.obtainMessage(ROUTES_SYNC_DATA_WHAT, httpRoutesInfoData);
                        handler.sendMessage(message);
                        storageService.setRoutesInfo(httpRoutesInfoData);
                    }
                }
                if (routesInfoData == null) {
                    // TODO: display a message "Failed to load"
                }
            }
        });
    }

    @Override
    public Thread syncTrassInfo(final String code) {
        if (trassesInSync.contains(code)) {
            return null;
        }
        trassesInSync.add(code);
        return (new Thread() {
            @Override
            public void run() {
                String key = TRASS_INFO_TSP + code;
                long tsp = pref.getLong(key, 0L);
                long timestamp = utils.getUnixTsp();
                TrassData trassData = storageService.getTrassData(code);
                if (trassData != null) {
                    Message message = handler.obtainMessage(TRASS_SYNC_DATA_WHAT, trassData);
                    handler.sendMessage(message);
                }
                if (trassData == null) {
                    // TODO: display a message "Loading way data"
                }
                if (trassData == null || tsp + SYNC_VALID_FOR < timestamp) {
                    TrassData httpTrassData = httpService.getTrassData(code, tsp);
                    if (httpTrassData != null) {
                        Message message = handler.obtainMessage(TRASS_SYNC_DATA_WHAT, httpTrassData);
                        handler.sendMessage(message);

                        SharedPreferences.Editor editor = pref.edit();
                        editor.putLong(key, timestamp);
                        editor.commit();
                        storageService.setTrassData(httpTrassData);
                    }
                }
                trassesInSync.remove(code);
            }
        });
    }
}
