package com.yjfcasting.qrcode.postprocess;

import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    protected void makeMessage(String message, int interval) {
        try {
            Looper.prepare();
            Toast toast = Toast.makeText(this, message, interval);
            toast.show();
            Looper.loop();
        }
        catch (Exception ex){
            Log.e("Error", ex.getMessage().toString());
        }
    }
}