/*
 * Copyright (C) 2018 Timothy "ZeevoX" Langer
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.zeevox.secure;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.zeevox.secure.core.SecureAppCompatActivity;
import com.zeevox.secure.ui.MainActivity;
import com.zeevox.secure.util.PermissionUtils;

import java.util.Objects;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class App extends SecureAppCompatActivity {

    // Total number of attempts in the application
    public static final int[] attempts = {0};

    // MasterKey in memory
    public static String masterKey = null;

    // Generate a unique number each time
    public static final int CREDENTIALS_RESULT = new Random().nextInt(10000) + 1;

    // Identify the permissions request
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1;

    // Identify the permissions request
    private static final int PERMISSIONS_REQUEST = 2302;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the splash screen layout
        setContentView(R.layout.activity_splash);

        // Set light navigation bar on Oreo SDK 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }

        unlock();

//        // Check application required permissions
//        if (ContextCompat.checkSelfPermission(App.this,
//                Manifest.permission.READ_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(App.this,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//
//
//            // Permission is not granted; request the permission
//            ActivityCompat.requestPermissions(App.this,
//                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    PERMISSIONS_REQUEST);
//
//            // PERMISSIONS_REQUEST is an app-defined int constant.
//            // The callback method gets the result of the request.
//
//        } else {
//            // Permission has already been granted
//            unlock();
//        }

        //requestStoragePermission(STORAGE_PERMISSION_REQUEST_CODE);
    }

    private void checkCredentials() {
        KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);

        if (!keyguardManager.isKeyguardSecure()) {
            // Show a message that the user hasn't set up a lock screen.
            Toast.makeText(this,
                    "Secure lock screen hasn't set up.\n"
                            + "Go to 'Settings -> Security -> Screenlock' to set up a lock screen",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Intent credentialsIntent;
        credentialsIntent = keyguardManager.createConfirmDeviceCredentialIntent("Verification required", "To use this application, first please verify that it's you.");
        if (credentialsIntent != null) {
            startActivityForResult(credentialsIntent, CREDENTIALS_RESULT);
        }
    }

    /**
     * Requests the storage permission. If a rationale with an additional explanation should
     * be shown to the user, displays a dialog that triggers the request.
     */
    public void requestStoragePermission(int requestCode) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Display a dialog with rationale.
            PermissionUtils.RationaleDialog
                    .newInstance(requestCode, false).show(
                    getSupportFragmentManager(), "dialog");
        } else {
            // Storage permission has not been granted yet, request it.
            PermissionUtils.requestPermissions(this, requestCode,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE}, false);
        }
    }

    private void unlock() {
        // Ask for user fingerprint / password if this option is enabled in preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (Objects.equals(preferences.getString(Flags.SECURITY_LEVEL, "fingerprint"), "appfingerprint")) {
            checkCredentials();
        } else {
            startActivity(new Intent(App.this, MainActivity.class));
            finish();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CREDENTIALS_RESULT) {
            if (resultCode == RESULT_OK) {
                // User successfully unlocked, access granted.
                startActivity(new Intent(App.this, MainActivity.class));
                finish();
            } else {
                // Access was denied, close the app and exit.
                Toast.makeText(this, "Access denied, sorry.", Toast.LENGTH_SHORT).show();
                finishAffinity();
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    unlock();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }
}
