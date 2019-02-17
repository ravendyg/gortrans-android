package info.nskgortrans.maps.Adapters;

/**
 * Created by me on 22/01/17.
 */

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import info.nskgortrans.maps.DataClasses.BusListElementData;
import info.nskgortrans.maps.R;

//package info.nskgortrans.maps.Adapters;


/**
 * Created by me on 4/12/16.
 */

public class BusListAdapter extends BaseAdapter {
    private Context ctx;
    private LayoutInflater inflater;
    private ArrayList<BusListElementData> data;

    public BusListAdapter(Context context, ArrayList<BusListElementData> _data) {
        ctx = context;
        data = _data;
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

    public BusListElementData getElem(int position) {
        return (BusListElementData) getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View _view, ViewGroup parent) {
        View view = _view;

        BusListElementData element = getElem(position);

        view = inflater.inflate(R.layout.bus_list_item, parent, false);
        TextView name = (TextView) view.findViewById(R.id.busName);
        name.setText(element.getName());
        name.setTextColor(ContextCompat.getColor(ctx, element.getColor()));
        ImageView icon = (ImageView) view.findViewById(R.id.busIcon);
        icon.setImageResource(element.getIcon());

        return view;
    }
}

