package info.nskgortrans.maps.Services;

import info.nskgortrans.maps.FileAPI;

public class StorageService implements IStorageService {
    FileAPI fileAPI;

    public StorageService(FileAPI fileAPI) {
        this.fileAPI = fileAPI;
    }
}
