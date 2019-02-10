package info.nskgortrans.maps.Services;

public interface IBusService {
    public void start();
    public void subscribe(String code);
    public void unsubscrive(String code);
    public void pause();
    public void resume();
}
