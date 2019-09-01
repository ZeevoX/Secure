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

import android.content.Intent;
import android.os.Bundle;

import com.zeevox.secure.backup.BackupRestoreHelper;
import com.zeevox.secure.core.SecureAppCompatActivity;
import com.zeevox.secure.cryptography.Crypto;
import com.zeevox.secure.ui.MainActivity;
import com.zeevox.secure.ui.dialog.AuthenticationDialog;

/**
 * The splash screen / loading activity
 */
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

        // Inflate the splash screen layout
        setContentView(R.layout.activity_splash);

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

    @Override
    public void onAuthenticationComplete(int resultCode, int requestCode, int adapterPosition) {
        if (requestCode == AUTHENTICATION_REQUEST) {
            if (resultCode == AuthenticationDialog.RESULT_ACCEPT)
                startActivity(new Intent(App.this, MainActivity.class));
            finish();
        }
    }
}
