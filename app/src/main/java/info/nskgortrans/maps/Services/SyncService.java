package info.nskgortrans.maps.Services;

public class SyncService implements ISyncService {
    private IStorageService storageService;

    public SyncService(IStorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public void syncRoutesInfo() {

    }
}
