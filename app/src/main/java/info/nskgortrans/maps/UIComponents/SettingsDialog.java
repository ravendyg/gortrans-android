package info.nskgortrans.maps.UIComponents;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import info.nskgortrans.maps.MainActivity;
import info.nskgortrans.maps.R;

public class SettingsDialog extends Dialog {
    static public final String MARKER_TYPE = "marker_type";

    private final SharedPreferences pref;
    private final RadioButton newMarkerTypeBtn;
    private final LinearLayout newMarkerTypeWrapper;
    private final RadioButton oldMarkerTypeBtn;
    private final LinearLayout oldMarkerTypeWrapper;
    private int markerType;

    public SettingsDialog(final Context context) {
        super(context);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.settings_menu);
        getWindow().setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
        );
        getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.rgb(255, 255, 255))
        );
        show();


        pref = PreferenceManager.getDefaultSharedPreferences(context);
        markerType = pref.getInt(MARKER_TYPE, 1);

        newMarkerTypeBtn = (RadioButton) findViewById(R.id.new_markers);
        newMarkerTypeWrapper = (LinearLayout) findViewById(R.id.new_markers_wrapper);
        oldMarkerTypeBtn = (RadioButton) findViewById(R.id.old_markers);
        oldMarkerTypeWrapper = (LinearLayout) findViewById(R.id.old_markers_wrapper);
        setBtns();

        newMarkerTypeWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (markerType == 1) {
                    return;
                }
                markerType = 1;
                SharedPreferences.Editor editor = pref.edit();
                editor.putInt(MARKER_TYPE, markerType);
                editor.commit();
                setBtns();
                ((MainActivity) context).changeMarkerType(1);
            }
        });
        oldMarkerTypeWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (markerType == 2) {
                    return;
                }
                markerType = 2;
                SharedPreferences.Editor editor = pref.edit();
                editor.putInt(MARKER_TYPE, markerType);
                editor.commit();
                ((MainActivity) context).changeMarkerType(2);
                setBtns();
            }
        });
    }

    private void setBtns() {
        newMarkerTypeBtn.setChecked(markerType == 1);
        oldMarkerTypeBtn.setChecked(markerType != 1);
    }
}
