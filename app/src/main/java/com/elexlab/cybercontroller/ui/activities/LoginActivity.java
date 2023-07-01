package com.elexlab.cybercontroller.ui.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.elexlab.cybercontroller.R;
import com.elexlab.cybercontroller.communication.BluetoothClient;
import com.elexlab.cybercontroller.communication.BluetoothKeyboard;
import com.elexlab.cybercontroller.ui.widget.AssetsAnimationImageView;
import com.elexlab.cybercontroller.ui.widget.CoolDigitalClock;
import com.elexlab.cybercontroller.utils.AssetsUtils;
import com.elexlab.cybercontroller.utils.DeviceUtil;
import com.elexlab.cybercontroller.utils.PermissionUtil;

public class LoginActivity extends Activity {

    private BluetoothKeyboard bluetoothKeyboard;
    private FrameLayout mPreviewContainer;
    private View rlScanFace;
    private TextView tvInfo;
    private View llMain;
    private View llSleep;
    private CoolDigitalClock digitalClock;
    AssetsAnimationImageView tvImageView;

    public static void startMe(Activity activity, boolean directUnlock){
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.putExtra("directUnlock",directUnlock);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.login_transition, R.anim.login_transition);
        DeviceUtil.acquireWakeLock(activity,1000);
    }


    private Handler handler = new Handler();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionUtil.checkAndRequestPermissions(this,new String[]{Manifest.permission.CAMERA});
        setContentView(R.layout.activity_login);
        mPreviewContainer = findViewById(R.id.surface_layout);
        rlScanFace = findViewById(R.id.rlScanFace);
        tvInfo = findViewById(R.id.tvInfo);
        llMain = findViewById(R.id.llMain);
        digitalClock = findViewById(R.id.digitalClock);
        llSleep = findViewById(R.id.llSleep);

        bluetoothKeyboard = new BluetoothKeyboard(this);

        //initLiveDetector(savedInstanceState);

        tvImageView = findViewById(R.id.tvImageView);

        tvImageView.setImages("tv");
        tvImageView.setDeltaTime(50);
        boolean directUnlock = getIntent().getBooleanExtra("directUnlock",false);

        llMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startMe(LoginActivity.this,true);
            }
        });
        if(directUnlock){
            startDetector(savedInstanceState);
        }

        BluetoothClient.getInstance().active();

    }

    private void startDetector(Bundle savedInstanceState){
        digitalClock.setVisibility(View.INVISIBLE);
        rlScanFace.setVisibility(View.VISIBLE);
        llSleep.setVisibility(View.INVISIBLE);
        tvImageView.start();

        //Obtain MLLivenessDetectView
        DisplayMetrics outMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        int widthPixels = outMetrics.widthPixels;
    }

    private void onRecSuccess(){
        rlScanFace.setVisibility(View.GONE);
        tvImageView.stopAnim();
        tvImageView.setImageBitmap(AssetsUtils.getImageFromAssetsFile(this,"tv_happy.png"));
        tvInfo.setVisibility(View.VISIBLE);
        tvInfo.setText("欢迎回来~");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
                LoginActivity.this.overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
            }
        },1000);

    }

    private void onRecFailure(){
        rlScanFace.setVisibility(View.GONE);
        tvInfo.setVisibility(View.VISIBLE);
        tvInfo.setText("你不对劲！");
    }

    private void unLock(){
        bluetoothKeyboard.sendKey("ESC");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothKeyboard.sendKey("1");
                bluetoothKeyboard.sendKey("q");
                bluetoothKeyboard.sendKey("a");
                bluetoothKeyboard.sendKey("z");
                bluetoothKeyboard.sendKey("@");
                bluetoothKeyboard.sendKey("W");
                bluetoothKeyboard.sendKey("S");
                bluetoothKeyboard.sendKey("X");
                bluetoothKeyboard.sendKey("Enter");
            }
        },500);

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if(mlLivenessDetectView != null){
//            mlLivenessDetectView.onDestroy();
//        }
        digitalClock.destroy();

    }

    public static int dip2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }



    @Override
    protected void onPause() {
        super.onPause();
//        if(mlLivenessDetectView != null){
//            mlLivenessDetectView.onPause();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if(mlLivenessDetectView != null){
//            mlLivenessDetectView.onResume();
//        }
    }


}
