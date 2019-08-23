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
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.zeevox.secure.backup.BackupRestoreHelper;
import com.zeevox.secure.core.SecureAppCompatActivity;
import com.zeevox.secure.ui.MainActivity;
import com.zeevox.secure.util.PermissionUtils;

import java.util.Objects;

public class App extends SecureAppCompatActivity {

    // Total number of attempts in the application
    public static final int[] attempts = {0};

    // MasterKey in memory
    public static String masterKey = null;

    // Generate a unique number each time
    public static final int CREDENTIALS_RESULT = 733;

    // Identify the permissions request
    public static final int PERMISSIONS_REQUEST = 2302;

    private BackupRestoreHelper backupRestoreHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getBooleanExtra("permissions_request", false)) {
            requestStoragePermission(PERMISSIONS_REQUEST);
        }

        // Inflate the splash screen layout
        setContentView(R.layout.activity_splash);

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Flags.BACKUP_RESTORE, false)) {
            backupRestoreHelper = new BackupRestoreHelper(this);
            backupRestoreHelper.setup();
        }

        unlock();
    }

    private void checkCredentials() {
        KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);

        if (keyguardManager != null) {
            if (!keyguardManager.isKeyguardSecure()) {
                Log.e(getClass().getSimpleName(), "Device keyguard is not secured by a PIN, pattern or password");
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
            } else {
                Log.e(getClass().getSimpleName(), "Could not create Keyguard Manager device credential request");
            }
        } else {
            Log.e(getClass().getSimpleName(), "KeyguardManager system service unavailable");
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
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CREDENTIALS_RESULT:
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
                break;
            case BackupRestoreHelper.REQUEST_CODE_SIGN_IN:
                backupRestoreHelper.setup();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    setResult(Activity.RESULT_OK, new Intent());
                } else {
                    setResult(Activity.RESULT_CANCELED, new Intent());
                }
                finish();
                break;
            }
        }
    }
}
