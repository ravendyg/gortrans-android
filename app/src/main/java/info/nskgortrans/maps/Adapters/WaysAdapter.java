package info.nskgortrans.maps.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import info.nskgortrans.maps.Data.WayData;
import info.nskgortrans.maps.R;

/**
 * Created by me on 4/12/16.
 */

public class WaysAdapter extends BaseAdapter {

    private Context ctx;
    private LayoutInflater inflater;
    private ArrayList<WayData> data;

    public WaysAdapter(Context ctx, ArrayList<WayData> data) {
        this.ctx = ctx;
        this.data = data;
        inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }
  
    public WayData getElem(int position) {
        return (WayData) getItem(position);
    }

    @Override
    public long getItemId (int position) {
        return position;
    }

    @Override
    public View getView(int position, View _view, ViewGroup parent) {
        View view = _view;

        final WayData element = getElem(position);

        if (view == null) {
            view = inflater.inflate(R.layout.adapter_way_group_item, parent, false);
        }

        if (position % 2 == 0) {
            view.setBackgroundColor(ctx.getResources().getColor(R.color.busBg));
        } else {
            view.setBackgroundColor(ctx.getResources().getColor(R.color.whitish));
        }

        TextView name = ((TextView) view.findViewById(R.id.busName));
        name.setText(element.getName());

        TextView stopb = ((TextView) view.findViewById(R.id.stopb));
        stopb.setText(element.stopb);

        TextView stope = ((TextView) view.findViewById(R.id.stope));
        stope.setText(element.stope);

        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }
}
