/*
 * Copyright (C) 2019 Timothy "ZeevoX" Langer
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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.zeevox.secure.backup.BackupRestoreHelper;
import com.zeevox.secure.core.SecureAppCompatActivity;
import com.zeevox.secure.cryptography.Crypto;
import com.zeevox.secure.ui.MainActivity;
import com.zeevox.secure.ui.dialog.AuthenticationDialog;
import com.zeevox.secure.util.PermissionUtils;

public class App extends SecureAppCompatActivity implements AuthenticationDialog.Callback {

    // Total number of attempts in the application
    public static final int[] attempts = {0};
    // Identify the permissions request
    private static final int PERMISSIONS_REQUEST = 2302;
    private static final int AUTHENTICATION_REQUEST = 3323;
    // MasterKey in memory
    public static String masterKey = null;
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

        try {
            Crypto crypto = new Crypto(this);
            if (!crypto.getEntries().isEmpty())
                new AuthenticationDialog(this, crypto, AUTHENTICATION_REQUEST, this);
            else {
                startActivity(new Intent(App.this, MainActivity.class));
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Requests the storage permission. If a rationale with an additional explanation should
     * be shown to the user, displays a dialog that triggers the request.
     */
    private void requestStoragePermission(int requestCode) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BackupRestoreHelper.REQUEST_CODE_SIGN_IN) {
            backupRestoreHelper.setup();
        }
    }

    @Override
    public void onAuthenticationComplete(int resultCode, int requestCode, int adapterPosition) {
        if (requestCode == AUTHENTICATION_REQUEST) {
            if (resultCode == AuthenticationDialog.RESULT_ACCEPT)
                startActivity(new Intent(App.this, MainActivity.class));
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission was granted, yay!
                setResult(Activity.RESULT_OK, new Intent());
            } else {
                setResult(Activity.RESULT_CANCELED, new Intent());
            }
            finish();
        }
    }
}
