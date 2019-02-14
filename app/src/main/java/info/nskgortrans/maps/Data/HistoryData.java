package info.nskgortrans.maps.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import info.nskgortrans.maps.DataClasses.Way;

public class HistoryData implements Serializable {
    final static int HISTORY_SIZE = 5;
    private ArrayList<WayData>[] history;

    public HistoryData() {
        this.history = new ArrayList[]{
                new ArrayList<>(Arrays.asList(new Way[0])),
                new ArrayList<>(Arrays.asList(new Way[0])),
                new ArrayList<>(Arrays.asList(new Way[0])),
                new ArrayList<>(Arrays.asList(new Way[0]))
        };
    }

    public void save(WayData element, int pos) {
        try {
            for (int i = 0; i < history[pos].size(); i++) {
                if (history[pos].get(i).getCode().equals(element.getCode())) {
                    history[pos].remove(i);
                }
            }
            history[pos].add(0, element);
            while (history[pos].size() > HISTORY_SIZE) {
                history[pos].remove(HISTORY_SIZE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<WayData> getType(int pos) {
        try {
            return this.history[pos];
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}

