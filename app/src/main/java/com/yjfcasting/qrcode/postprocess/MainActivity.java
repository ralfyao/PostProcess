package com.yjfcasting.qrcode.postprocess;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.zxing.Result;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.yjfcasting.qrcode.postprocess.model.BaseResponse;
import com.yjfcasting.qrcode.postprocess.model.GridAdapter;
import com.yjfcasting.qrcode.postprocess.model.LoadWorkOrderMeta;
import com.yjfcasting.qrcode.postprocess.model.ReportWorkOrderMetaReq;
import com.yjfcasting.qrcode.postprocess.model.WorkOrderMeta;
import com.yjfcasting.qrcode.postprocess.util.Constant;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class MainActivity extends BaseActivity implements ZXingScannerView.ResultHandler{
    private String user_code;
    private RecyclerView recyclerView;
    //    private DataGridView dgv;
    private OkHttpClient okHttpClient = new OkHttpClient().newBuilder().addInterceptor(
            new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)
    ).build();
    private ArrayList<WorkOrderMeta> workOrderMetaList;
    private Bitmap bitmap;
    private Button btn_scan;
    private TextView QRResult;
    ZXingScannerView scannerView;
    BarcodeEncoder barcodeEncoder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            this.user_code = intent.getStringExtra("workerNumber");
        }
        workOrderMetaList = new ArrayList<WorkOrderMeta>();
        int orientation = getResources().getConfiguration().orientation;
        setRequestedOrientation(orientation);
        setContentView(R.layout.activity_main);
        btn_scan=findViewById(R.id.btn_scan);
        QRResult=findViewById(R.id.txv_result);
        scannerView = findViewById(R.id.scannerView);
        scannerView.startCamera();
//        scannerView.setResultHandler(this);
        recyclerView = findViewById(R.id.rc_data);
        recyclerView.setVerticalScrollBarEnabled(true);
        barcodeEncoder= new BarcodeEncoder();
        requestPermissions(new String[]{android.Manifest.permission.CAMERA},1);
        //按下掃描按紐
        btn_scan.setOnClickListener(view->{
            //判斷有沒有給CAMERA權限
            if(checkSelfPermission(android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED){
                //跳是否允許相機權限視窗
                requestPermissions(new String[]{android.Manifest.permission.CAMERA},1);
            }
            else {
                scannerView.startCamera();
                scannerView.setResultHandler(this);
                if (QRResult.getText() != "")
                {
                    process(QRResult.getText().toString());
                    QRResult.setText("");
                }
            }
        });
        initGridView();
    }

    private void initGridView(){
        recyclerView = findViewById(R.id.rc_data);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4)); // 設定每行 3 個項目
        List<String> data = generateSampleData(); // 生成測試數據
        GridAdapter adapter = new GridAdapter(data);
        recyclerView.setAdapter(adapter);
    }

    private void initGridViewWData(List<String> dataResult){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<String> data = generateSampleData(); // 生成測試數據
                    for(int i = 0; i < dataResult.size(); i++){
                        data.add(dataResult.get(i));
                    }
                    GridAdapter adapter = new GridAdapter(data);
                    recyclerView.setAdapter(adapter);
                    QRResult.setText("");
                }
                catch (Exception ex){
                    Log.e("initGridViewWData error", ex.getMessage());
                }
            }
        });
    }

    private List<String> generateSampleData() {
        List<String> data = new ArrayList<>();
        data.add("製令別單號");
        data.add("到站日期");
        data.add("送出者");
        data.add("簽收Flag");
        return data;
    }

    @Override
    public void handleResult(Result result) {
        QRResult.setText(result.getText());
        process(result.getText());
        QRResult.setText("");
        scannerView.startCamera();
//        scannerView.setResultHandler(this);
    }
    public void process(String result){
        try {
            String url = result;
            if (url.trim().equals("")) {
                return;
            }
            if (url.indexOf("PorductionOrderHead=") == -1 &&
                    url.indexOf("PorductionOrder=") == -1 &&
                    url.indexOf("&") == -1) {
                return;
            }
            String workOrder = url.substring(url.indexOf("PorductionOrderHead"));
            String[] workOrderArr = workOrder.split("&");
            String workOrderHead = workOrderArr[0].split("=")[1];
            String workOrderNumber = workOrderArr[1].split("=")[1];
            FormBody body = new FormBody.Builder()
                    .add("WorkOrder", workOrderHead.trim() + "-" + workOrderNumber)
                    .add("InStationDate", new SimpleDateFormat("yyyyMMdd").format(new Date()))
                    .add("Submittor", user_code)
                    .add("ReveiceFlag", "").build();
            Request request = new Request.Builder()
                    .url(Constant.url + "/api/LoadWorkOrderMeta")
                    .post(body) // 使用post連線
                    .build();
            Call call = okHttpClient.newCall(request);

            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    makeMessage(e.getMessage(), Toast.LENGTH_SHORT);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    LoadWorkOrderMeta meta = new Gson().fromJson(response.body().string(), LoadWorkOrderMeta.class);
                    if (meta.WorkStatus.equals("OK")) {
                        Log.d("call.enqueue onResponse", new Gson().toJson(meta.result).toString());
                        List<String> formatData = new ArrayList<String>();
                        boolean added = false;
                        for (int i = 0; i < workOrderMetaList.size(); i++) {
//                            if (i % 4 == 0) {
                                if (workOrderMetaList.get(i).WorkOrder.equals(meta.result.WorkOrder)){
                                    added = true;
                                    break;
                                }
//                            }
                        }
                        if (!added){
                            workOrderMetaList.add(meta.result);
                        } else {
                            makeMessage(meta.result.WorkOrder+"已加入到列表", Toast.LENGTH_SHORT);
                            return;
                        }
                        for (int i = 0; i < workOrderMetaList.size(); i++) {
                            formatData.add(workOrderMetaList.get(i).WorkOrder);
                            formatData.add(workOrderMetaList.get(i).InStationDate);
                            formatData.add(workOrderMetaList.get(i).Submittor);
                            formatData.add(workOrderMetaList.get(i).ReveiceFlag);
                        }
                        initGridViewWData(formatData);
                    } else {
                        makeMessage(meta.ErrorMsg, Toast.LENGTH_SHORT);
                    }
                }
            });

        }
        catch (Exception ex){
            makeMessage(ex.getMessage(), Toast.LENGTH_SHORT);
        }
    }

    public void submit(View view) {
        try {
            ReportWorkOrderMetaReq req = new ReportWorkOrderMetaReq();
            req.Account = this.user_code;
            req.data = workOrderMetaList;
            MediaType json = MediaType.parse("application/json; charset=utf-8");
            String jsonStr = new Gson().toJson(req);
            RequestBody postObj = RequestBody.create(jsonStr, json);
            Request request = new Request.Builder()
                    .url(Constant.url + "/api/ReportWorkOrderMeta")
                    .post(postObj)
                    .build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    makeMessage(e.getMessage(), Toast.LENGTH_SHORT);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    BaseResponse meta = new Gson().fromJson(response.body().string(), BaseResponse.class);
                    if (meta.WorkStatus.equals("OK")) {
                        makeMessage("執行成功", Toast.LENGTH_SHORT);
                        workOrderMetaList = new ArrayList<WorkOrderMeta>();
                        initGridView();
                    } else {
                        makeMessage(meta.ErrorMsg, Toast.LENGTH_SHORT);
                    }
                }
            });
        }
        catch (Exception ex){
            makeMessage(ex.getMessage(), Toast.LENGTH_SHORT);
        }
    }

    public void logout(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }
}