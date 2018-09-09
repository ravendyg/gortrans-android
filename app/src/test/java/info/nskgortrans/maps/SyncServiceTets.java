package info.nskgortrans.maps;

import android.content.SharedPreferences;
import android.os.Handler;

import org.json.JSONArray;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import info.nskgortrans.maps.Data.RoutesInfoData;
import info.nskgortrans.maps.Services.HttpService;
import info.nskgortrans.maps.Services.IHttpService;
import info.nskgortrans.maps.Services.IStorageService;
import info.nskgortrans.maps.Services.ISyncService;
import info.nskgortrans.maps.Services.StorageService;
import info.nskgortrans.maps.Services.SyncService;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SyncServiceTets {
    private IStorageService storageService;
    private IHttpService httpService;
    private SharedPreferences pref;
    private ISyncService syncService;
    private Utils utils;
    private Handler handler;

    public SyncServiceTets() {
        storageService = mock(StorageService.class);
        httpService = mock(HttpService.class);
        pref = mock(SharedPreferences.class);
        utils = mock(Utils.class);
        handler = mock(Handler.class);
        syncService = new SyncService(storageService, httpService, pref, utils, handler);
    }

    @Test
    public void should_load_nothing_from_server_if_nothing_here_and_server_sent_nothing() {
        syncService.syncRoutesInfo().run();
    }

    @Test
    public void should_load_from_server_if_nothing_here_and_remember_timestamp() {
        final long prefTsp = 11111111;
        final long nowTsp = prefTsp + 100;
        JSONArray routesInfoJSON = TestHelpers.generateRoutesInfo();
        final RoutesInfoData routesInfoData = new RoutesInfoData(routesInfoJSON);
        final SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        ArgumentCaptor<Integer> messageTypeCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<RoutesInfoData> dataCaptor = ArgumentCaptor.forClass(RoutesInfoData.class);

        when(pref.getLong(SyncService.ROUTES_INFO_TSP, 0L)).thenReturn(prefTsp);
        when(utils.getUnixTsp()).thenReturn(nowTsp);
        when(httpService.getRoutesInfo(prefTsp)).thenReturn(routesInfoData);
        when(pref.edit()).thenReturn(editor);

        syncService.syncRoutesInfo().run();

        verify(pref, times(1)).edit();
        verify(editor, times(1)).putLong(SyncService.ROUTES_INFO_TSP, nowTsp);
        verify(editor, times(1)).commit();
        verify(handler).obtainMessage(messageTypeCaptor.capture(), dataCaptor.capture());
        int messageType = messageTypeCaptor.getValue();
        RoutesInfoData data = dataCaptor.getValue();
        assertEquals(messageType, SyncService.ROUTES_SYNC_DATA_WHAT);
        assertEquals(data, routesInfoData);
    }

    @Test
    public void should_read_from_disk_if_there_and_use_it_if_not_expired() {
        final long prefTsp = 11111111;
        final long nowTsp = prefTsp + 1;
        JSONArray routesInfoJSON = TestHelpers.generateRoutesInfo();
        final RoutesInfoData routesInfoData = new RoutesInfoData(routesInfoJSON);
        ArgumentCaptor<Integer> messageTypeCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<RoutesInfoData> dataCaptor = ArgumentCaptor.forClass(RoutesInfoData.class);

        when(pref.getLong(SyncService.ROUTES_INFO_TSP, 0L)).thenReturn(prefTsp);
        when(utils.getUnixTsp()).thenReturn(nowTsp);
        when(storageService.getRoutesInfo()).thenReturn(routesInfoData);

        syncService.syncRoutesInfo().run();

        verify(httpService, never()).getRoutesInfo(prefTsp);
        verify(handler).obtainMessage(messageTypeCaptor.capture(), dataCaptor.capture());
        int messageType = messageTypeCaptor.getValue();
        RoutesInfoData data = dataCaptor.getValue();
        assertEquals(messageType, SyncService.ROUTES_SYNC_DATA_WHAT);
        assertEquals(data, routesInfoData);
    }

    @Test
    public void should_read_from_disk_and_get_from_http_if_expired() {
        final long prefTsp = 11111111;
        final long nowTsp = prefTsp + SyncService.SYNC_VALID_FOR * 2;
        final SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        JSONArray routesInfoJSON = TestHelpers.generateRoutesInfo();
        final RoutesInfoData localRoutesInfoData = new RoutesInfoData(routesInfoJSON);
        final RoutesInfoData httpRoutesInfoData = new RoutesInfoData(routesInfoJSON);
        ArgumentCaptor<Integer> messageTypeCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<RoutesInfoData> dataCaptor = ArgumentCaptor.forClass(RoutesInfoData.class);

        when(pref.getLong(SyncService.ROUTES_INFO_TSP, 0L)).thenReturn(prefTsp);
        when(utils.getUnixTsp()).thenReturn(nowTsp);
        when(storageService.getRoutesInfo()).thenReturn(localRoutesInfoData);
        when(httpService.getRoutesInfo(prefTsp)).thenReturn(httpRoutesInfoData);
        when(pref.edit()).thenReturn(editor);

        syncService.syncRoutesInfo().run();

        verify(pref, times(1)).edit();
        verify(editor, times(1)).putLong(SyncService.ROUTES_INFO_TSP, nowTsp);
        verify(editor, times(1)).commit();
        verify(storageService, times(1)).setRoutesInfo(httpRoutesInfoData);
        verify(handler).obtainMessage(messageTypeCaptor.capture(), dataCaptor.capture());
        int messageType = messageTypeCaptor.getValue();
        RoutesInfoData data = dataCaptor.getValue();
        assertEquals(messageType, SyncService.ROUTES_SYNC_DATA_WHAT);
        assertEquals(data, httpRoutesInfoData);
    }

    @Test
    public void should_read_from_disk_and_get_from_http_if_expired_but_use_local_if_http_failed() {
        final long prefTsp = 11111111;
        final long nowTsp = prefTsp + SyncService.SYNC_VALID_FOR * 2;
        JSONArray routesInfoJSON = TestHelpers.generateRoutesInfo();
        final RoutesInfoData localRoutesInfoData = new RoutesInfoData(routesInfoJSON);
        ArgumentCaptor<Integer> messageTypeCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<RoutesInfoData> dataCaptor = ArgumentCaptor.forClass(RoutesInfoData.class);

        when(pref.getLong(SyncService.ROUTES_INFO_TSP, 0L)).thenReturn(prefTsp);
        when(utils.getUnixTsp()).thenReturn(nowTsp);
        when(storageService.getRoutesInfo()).thenReturn(localRoutesInfoData);
        when(httpService.getRoutesInfo(prefTsp)).thenReturn(null);

        syncService.syncRoutesInfo().run();

        verify(pref, never()).edit();
        verify(handler).obtainMessage(messageTypeCaptor.capture(), dataCaptor.capture());
        int messageType = messageTypeCaptor.getValue();
        RoutesInfoData data = dataCaptor.getValue();
        assertEquals(messageType, SyncService.ROUTES_SYNC_DATA_WHAT);
        assertEquals(data, localRoutesInfoData);
    }
}
