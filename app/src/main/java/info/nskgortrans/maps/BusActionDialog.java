package info.nskgortrans.maps;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by me on 22/01/17.
 */

public class BusActionDialog
{

  public static Dialog showDialog(final Context contex, final String code, final int type, final String name)
  {
    final Dialog dialog = new Dialog(contex);
    dialog.setContentView(R.layout.bus_action_dialog);

    TextView header = (TextView) dialog.findViewById(R.id.route_header);
    header.setText(Utils.getTypeString(type) + ": №" + name);

    Button zoomBtn = (Button) dialog.findViewById(R.id.zoom_route_btn);
    zoomBtn.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        ((MainActivity) contex).zoomToRoute(code);
        dialog.cancel();
      }
    });

    Button removeBtn = (Button) dialog.findViewById(R.id.remove_route_btn);
    removeBtn.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        ((MainActivity) contex).removeBus(code);
        dialog.cancel();
      }
    });

    dialog.show();

    return dialog;
  }
}
