package info.nskgortrans.maps.Services;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import info.nskgortrans.maps.DataClasses.HistoryData;
import info.nskgortrans.maps.DataClasses.RoutesInfoData;
import info.nskgortrans.maps.DataClasses.Way;
import info.nskgortrans.maps.DataClasses.WayData;

public class StorageService implements IStorageService {
    public static final String ROUTES_INFO_FILE_NAME = "routes_info";
    public static final String SEARCH_HISTORY_FILE = "search_history";

    public final Context ctx;

    public StorageService(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public RoutesInfoData getRoutesInfo() {
        Object data = getData(ROUTES_INFO_FILE_NAME);
        if (data != null) {
            return (RoutesInfoData) data;
        } else {
            return null;
        }
    }

    @Override
    public void setRoutesInfo(RoutesInfoData routesInfoData) {
        saveObjectToDisk(ROUTES_INFO_FILE_NAME, routesInfoData);
    }

    @Override
    public HistoryData getSearchHistory() {
        Object data = getData(SEARCH_HISTORY_FILE);
        try {
            return (HistoryData) data;
        } catch (Exception e) {
            e.printStackTrace();
            return new HistoryData();
        }
    }

    @Override
    public void setSearchHistory(HistoryData history) {
        saveObjectToDisk(SEARCH_HISTORY_FILE, history);
    }

    private Object getData(String fileName) {
        Object data = null;
        Object maybeData = readObjectFromDisk(fileName);
        if (maybeData != null) {
            data = maybeData;
        }
        return data;
    }

    private void saveObjectToDisk(String path, Object data) {
        OutputStream file = null;
        OutputStream buffer = null;
        ObjectOutput output = null;

        try {
            file = ctx.openFileOutput(path, Context.MODE_PRIVATE);
            buffer = new BufferedOutputStream(file);
            output = new ObjectOutputStream(buffer);
            output.writeObject(data);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(buffer);
            close(file);
            close(output);
        }
    }

    private Object readObjectFromDisk(String path) {
        Object result = null;
        InputStream file = null;
        InputStream buffer = null;
        ObjectInput input = null;

        try {
            file = ctx.openFileInput(path);
            buffer = new BufferedInputStream(file);
            input = new ObjectInputStream(buffer);
            result = input.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(buffer);
            close(file);
            close(input);
        }

        return result;
    }

    private void close(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void close(ObjectOutput resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void close(ObjectInput resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
