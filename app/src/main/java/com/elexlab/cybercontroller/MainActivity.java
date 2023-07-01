package com.elexlab.cybercontroller;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.elexlab.cybercontroller.communication.BluetoothKeyboard;
import com.elexlab.cybercontroller.communication.TcpClient;
import com.elexlab.cybercontroller.pojo.CommandMessage;
import com.elexlab.cybercontroller.pojo.ScriptMessage;
import com.elexlab.cybercontroller.ui.activities.LoginActivity;
import com.elexlab.cybercontroller.ui.widget.InfoBoxView;
import com.elexlab.cybercontroller.ui.widget.TouchboardView;
import com.elexlab.cybercontroller.ui.widget.TranslationView;
import com.elexlab.cybercontroller.utils.AssetsUtils;
import com.elexlab.cybercontroller.utils.DeviceUtil;
import com.elexlab.cybercontroller.utils.PermissionUtil;
import com.elexlab.cybercontroller.utils.SharedPreferencesUtil;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private final static String TAG = MainActivity.class.getSimpleName();
    private static TcpClient tcpClient = new TcpClient();
    private TranslationView tvTranslation;
    private InfoBoxView ivInfoBoxView;
    private TouchboardView touchboardView;
    private BluetoothKeyboard bluetoothKeyboard;
    private Handler handler = new Handler();
    public static void startMe(Activity activity){
        Intent intent = new Intent(activity, MainActivity.class);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.login_transition, R.anim.login_transition);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionUtil.checkAndRequestPermissions(this,new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO});

        setContentView(R.layout.activity_main);
        tvTranslation = findViewById(R.id.tvTranslation);
        touchboardView = findViewById(R.id.touchboardView);
        ivInfoBoxView = findViewById(R.id.ivInfoBoxView);
        ivInfoBoxView.dismiss(0);

        initSetting();
        initTcpClient();
        initTouchboard();

        getApplicationContext();
    }

    private void setImage(Bitmap bitmap){
        //test
        ImageView ivPreview = findViewById(R.id.ivPreview);
        ivPreview.setImageBitmap(bitmap);
    }

    public void shutDown(View view) throws InterruptedException {
        String script = AssetsUtils.loadCommandScripts(MainActivity.this,"shut_down.py");
        ScriptMessage scriptMessage = new ScriptMessage();
        scriptMessage.setScript(script);
        String params = "";
        scriptMessage.setParams(params);
        tcpClient.send(new Gson().toJson(scriptMessage));
//        System.out.println();
    }

    public void checkTcp(View view) throws InterruptedException {
        if (tcpClient.connected) {
            Toast.makeText(MainActivity.this,"已建立TCP连接",Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MainActivity.this,"还未建立TCP连接",Toast.LENGTH_LONG).show();
        }
    }

    private void initSetting(){
        findViewById(R.id.rlSettings).setOnClickListener((View view)->{
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View settingView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_settings, null,false);
            builder.setView(settingView);
            final Dialog dialog = builder.create();
            //dialog.setContentView(settingView);
            dialog.show();
            ViewGroup.LayoutParams layoutParams = settingView.getLayoutParams();
            layoutParams.width = (int) (DeviceUtil.getDeiveSize(MainActivity.this).widthPixels*0.8);
            layoutParams.height = (int) (DeviceUtil.getDeiveSize(MainActivity.this).heightPixels*0.8);
            settingView.setLayoutParams(layoutParams);

            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                }
            });
            EditText etHostIp = settingView.findViewById(R.id.etHostIp);
            EditText etHostPort = settingView.findViewById(R.id.etHostPort);
            etHostIp.setText(SharedPreferencesUtil.getPreference(MainActivity.this,"settings","hostIp","not set yet"));
            int port = SharedPreferencesUtil.getPreference(MainActivity.this,"settings","hostPort",2233);
            etHostPort.setText(String.valueOf(port));

            settingView.findViewById(R.id.btnSave).setOnClickListener((View v)->{
                Map<String,Object> preferences = new HashMap<String,Object>();

                preferences.put("hostIp",etHostIp.getText().toString());
                try{
                    preferences.put("hostPort",Integer.parseInt(etHostPort.getText().toString()));
                }catch (NumberFormatException e){
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this,"端口号必须是数字",Toast.LENGTH_SHORT).show();

                }
                SharedPreferencesUtil.setPreferences(MainActivity.this,"settings",preferences);
                Toast.makeText(MainActivity.this,"设置已更新,重启App生效",Toast.LENGTH_LONG).show();
                dialog.dismiss();

            });

            settingView.findViewById(R.id.btnCancel).setOnClickListener((View v)->{
                dialog.dismiss();
            });

        });
    }

    private void onTranslateSuccess(String source, String result){
        tvTranslation.setContent(source,result);
        DeviceUtil.acquireWakeLock(MainActivity.this,TranslationView.AUTO_DISMISS_TIME);
    }


    private void initTcpClient() {
        tcpClient.onReceive((int type, byte[] data) -> {
                    Log.d("!isInLoginActivity", "" + !isInLoginActivity());
                    String text = null;
                    try {
                        text = new String(data, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        return;
                    }
                    Log.d(TAG, text);
                    CommandMessage commandMessage = new Gson().fromJson(text, CommandMessage.class);
                    if (commandMessage.getCommand() == CommandMessage.CommandType.LOCK && !isInLoginActivity()) {
                        LoginActivity.startMe(MainActivity.this, false);
                    }
                }
        );

    }


    private void initTouchboard(){
        touchboardView.setTouchCallback(new TouchboardView.TouchCallback() {
            @Override
            public void onSwitchWindow(int direction) {
                String script = AssetsUtils.loadCommandScripts(MainActivity.this,"key_event.py");
                String direct = direction==0?"left_arrow":"right_arrow";
                String params = "ctrl,win,"+direct;
                ScriptMessage scriptMessage = new ScriptMessage();
                scriptMessage.setScript(script);
                scriptMessage.setParams(params);
                tcpClient.send(new Gson().toJson(scriptMessage));
            }

            @Override
            public void onWindowTab() {
                String script = AssetsUtils.loadCommandScripts(MainActivity.this,"key_event.py");
                String params = "win,tab";
                ScriptMessage scriptMessage = new ScriptMessage();
                scriptMessage.setScript(script);
                scriptMessage.setParams(params);
                tcpClient.send(new Gson().toJson(scriptMessage));
            }
        });
    }



    private Bitmap getImageFromAssetsFile(Context context, String fileName) {
        Bitmap image = null;
        AssetManager am = context.getResources().getAssets();
        try {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    private boolean isInLoginActivity()
    {
        ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        return cn.getClassName().contains(LoginActivity.class.getSimpleName());
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onBackPressed() {

    }
}