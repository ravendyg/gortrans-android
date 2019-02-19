package info.nskgortrans.maps.UIComponents;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;

import info.nskgortrans.maps.R;

public class SettingsDialog extends Dialog {
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
    }
}
