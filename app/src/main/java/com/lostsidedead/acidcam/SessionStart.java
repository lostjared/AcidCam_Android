package com.lostsidedead.acidcam;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.widget.Toast;
import android.content.Intent;

public class SessionStart extends Activity {
    @Override
    protected void onStart() {
        super.onStart();
    }


    private static final String[] INITIAL_PERMS={
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static final int INITIAL_REQUEST=2517;

    private boolean hasPermission(String perm) {
        return(PackageManager.PERMISSION_GRANTED == checkSelfPermission(perm));
    }

    public void requestPermissions() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            boolean passed = true;
            for(int i = 0; i < 3; ++i) {
                if (!hasPermission(INITIAL_PERMS[i])) {
                    requestPermissions(INITIAL_PERMS, INITIAL_REQUEST);
                    passed = false;
                    break;
                }
            }
            if(passed == true)
                launchApp();
        }
    }

    public void launchApp() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode == INITIAL_REQUEST){
            boolean passed = true;
            for(int i = 0; i < 3; ++i) {
                if (!hasPermission(INITIAL_PERMS[i])) {
                    passed = false;
                    break;
                }
            }
            if(passed == true)
                launchApp();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
    }
    @Override
    protected void onStop() {
        super.onStop();
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}