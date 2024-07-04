package com.yjfcasting.qrcode.postprocess;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.gson.Gson;
import com.yjfcasting.qrcode.postprocess.model.LoginModel;
import com.yjfcasting.qrcode.postprocess.util.Constant;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class LoginActivity extends BaseActivity {
    private final OkHttpClient okHttpClient = new OkHttpClient().newBuilder().addInterceptor(
            new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)
    ).build();
    private boolean canLogin = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        canLogin = false;
    }

    public void doLogin(View view) {
        EditText workerNumber = findViewById(R.id.workerNumber);
        Request r = new Request.Builder().url(Constant.url + "/api/OutsourceLogin?account="+workerNumber.getText()).build();
        Call call = okHttpClient.newCall(r);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d("OkHttp result:", e.getMessage() == null ? "" : e.getMessage());
                makeMessage(e.getMessage(), Toast.LENGTH_SHORT);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.body() != null) {
                        LoginModel result = new Gson().fromJson(response.body().string(), LoginModel.class);
                        if (result.WorkStatus.equals("OK")) {
                            canLogin = true;
                        }
                        Log.d("LoginResponse", "" + result);
                        if (canLogin) {
                            redirectMain(workerNumber.getText().toString());
                        } else {
                            makeMessage("輸入的帳號不存在或密碼有誤", Toast.LENGTH_SHORT);
                        }
                    } else {
                        makeMessage("回傳的資料是空的", Toast.LENGTH_SHORT);
                    }
                } catch (IOException e) {
                    Log.d("Error",""+e);
                } catch (Exception e) {
                    Log.d("Error",""+e);
                }
            }
        });

    }



    private void redirectMain(String workerNumber) {
        try {
            Intent mainProgram = new Intent(this, MainActivity.class);
            mainProgram.putExtra("workerNumber", workerNumber);
            startActivity(mainProgram);
        } catch (Exception ex){
            if (ex.getMessage() != null) {
                Log.e("Error", ex.getMessage().toString());
            } else {
                Log.e("Error", "ex.getMessage() == null");
            }
        }
    }
}