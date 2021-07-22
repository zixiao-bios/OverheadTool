package com.zixiao_bios.overheadtool;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.widget.Toast;

import static android.content.Context.VIBRATOR_SERVICE;

public class MyDisplay {
    private static Toast toast=null;
    @SuppressLint("StaticFieldLeak")
    private static Activity activity=null;
    private static Vibrator vibrator;

    @SuppressLint("ShowToast")
    public static void createToast(Context c, Activity a) {
        activity=a;
        toast=Toast.makeText(c, "", Toast.LENGTH_SHORT);
        vibrator=(Vibrator)activity.getSystemService(VIBRATOR_SERVICE);
    }

    public static void toast(String message) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toast.setText(message);
                toast.show();
            }
        });
    }

    public static void vibrate(int time) {
        vibrator.vibrate(time);
    }
}
