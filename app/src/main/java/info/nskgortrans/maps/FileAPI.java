package info.nskgortrans.maps;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by me on 6/11/16.
 */
public class FileAPI {
    private Context context;

    public FileAPI(Context context) {
        this.context = context;
    }

    public boolean isFileExists(String filename) {
        File file = context.getFileStreamPath(filename);
        return file.exists();
    }

    public String readFile(String filename) {
        String out = "";
        try {
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader buf = new BufferedReader(isr);
            String line;

            while ((line = buf.readLine()) != null) {
                out += line;
            }
        } catch (FileNotFoundException err) {
        } catch (IOException err) {
        }

        return out;
    }

    public void writeFile(String filename, String data) {
        try {
            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(data.getBytes());
            fos.close();
        } catch (FileNotFoundException err) {
        } catch (IOException err) {
        }
    }
}
