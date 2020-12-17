package com.cfdemo.receiptreader;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.mlsdk.MLAnalyzerFactory;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.text.MLLocalTextSetting;
import com.huawei.hms.mlsdk.text.MLText;
import com.huawei.hms.mlsdk.text.MLTextAnalyzer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.List;

import com.cfdemo.receiptreader.StorageHelper.*;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "ReceiptReaderLogTag";
    protected String pushToken;

    private MLTextAnalyzer analyzer;

    private static final int GET_IMAGE_REQUEST_CODE = 1222;

    ImageView imgBitmap;
    TextView txtResult;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        getToken();

        imgBitmap = findViewById(R.id.img_bitmap);
        txtResult = findViewById(R.id.txt_result);
        Log.d(TAG+"bg", StorageHelper.getCredit(this)+"");
        if (StorageHelper.getName(this) == null) {
            getName();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.analyzer != null) {
            try {
                this.analyzer.stop();
            } catch (IOException e) {
                Log.e(TAG, "Stop failed: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GET_IMAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imgBitmap.setImageBitmap(selectedBitmap);
                asyncAnalyzeText(selectedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getName () {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("What is your name?");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setCancelable(false);
        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                StorageHelper.saveName(context, input.getText().toString());

            }
        });

        builder.show();
    }

    public void onScan(View view) {
        Log.d(TAG, "onScan");

        getImage();

    }

    public void onProfile(View view) {
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
    }

    private void createMLTextAnalyzer() {
        MLLocalTextSetting setting = new MLLocalTextSetting.Factory()
                .setOCRMode(MLLocalTextSetting.OCR_DETECT_MODE)
                .setLanguage("en")
                .create();

        analyzer = MLAnalyzerFactory.getInstance().getLocalTextAnalyzer(setting);
    }

    private String stringCleanUp (String input) {
        return input.replaceAll(":", "").replaceAll("hkd|HKD", "").trim().toLowerCase();
    }

    private boolean isAmountDescription(String[] input) {
        boolean result = false;
        for (int i = 0; i < input.length; i++) {
            Log.d(TAG+"ad", input[i] );
            String processedString = stringCleanUp(input[i]);
            if (processedString.indexOf("amount")>=0 || processedString.indexOf("total")>=0) {
                result = true;
                break;
            }
        }
        return result;
    }

    private float getAmount(String[] input) {
        float result = -1;
        for (int i = 0; i < input.length; i++) {
            Log.d(TAG+"am", input[i]);
            try {
                result = Float.parseFloat(stringCleanUp(input[i]));
                Log.d(TAG+"am", result+"");
                break;
            }
            catch (Exception e) {
                Log.d(TAG+"am", "Not Double");
            }
        }
        return result;
    }

    private void asyncAnalyzeText(Bitmap bitmap) {

        if (analyzer == null) {
            createMLTextAnalyzer();
        }

        MLFrame frame = MLFrame.fromBitmap(bitmap);

        Task<MLText> task = analyzer.asyncAnalyseFrame(frame);
        task.addOnSuccessListener(new OnSuccessListener<MLText>() {
            @Override
            public void onSuccess(MLText text) {
                List<MLText.Block> blocks = text.getBlocks();
                String resultString = "Amount not found";
                boolean amountRecognised = false;
                for ( int i = 0; i < blocks.size(); i++ ) {
                    String[] blockString = text.getBlocks().get(i).getStringValue().split(" ");

                    if (!amountRecognised ) {
                        amountRecognised = isAmountDescription(blockString);
                    }
                    else {
                        Log.d(TAG, i-1+"");
                        float amount = getAmount(blockString);
                        if (amount >= 0) {
                            resultString = amount+"";
                            StorageHelper.addCredit(context, amount);
                        }
                    }
                }
                txtResult.setText(resultString);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                txtResult.setText(e.getMessage());
            }
        });
    }

    private void getImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, GET_IMAGE_REQUEST_CODE);
    }

    private void getToken() {
        Log.d(TAG, "get token: begin");

        // get token
        new Thread() {
            @Override
            public void run() {
                try {
                    String appId = AGConnectServicesConfig.fromContext(MainActivity.this).getString("client/app_id");
                    pushToken = HmsInstanceId.getInstance(MainActivity.this).getToken(appId, "HCM");
                    if(!TextUtils.isEmpty(pushToken)) {
                        Log.d(TAG, "get token:" + pushToken);
                        onTokenReceived(pushToken);
                    }
                } catch (Exception e) {
                    Log.d(TAG,"getToken failed, " + e);
                }
            }
        }.start();
    }

    private void onTokenReceived(String token) {
        Log.d(TAG,"Token: " + token);
    }

}

