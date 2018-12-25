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

package com.zeevox.secure.core;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.zeevox.secure.App;
import com.zeevox.secure.R;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("Registered")
public class SecureAppCompatActivity extends AppCompatActivity {

    private static int activities_num = 0;
    private boolean shouldExecuteOnResume;

    protected boolean passwordProtect() {
        return false;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            getWindow().setNavigationBarColor(getResources().getColor(R.color.secure_primary, getTheme()));
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }
        // Activity created for the first time
        shouldExecuteOnResume = false;
        // Don't show app snapshot in overview and don't allow screenshots
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    /* START Close the activity when exiting, don't just switch from it. */
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    /* *** */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }
    /* END */

    /* START Handle password protecting the app */
    @Override
    protected void onStart() {
        super.onStart();
        activities_num++;
        if (activities_num == 1) {
            // User back in the application
            Log.i(getClass().getSimpleName(), "User has returned to the application");
            if (shouldExecuteOnResume && passwordProtect()) {
                shouldExecuteOnResume = false;
                startActivity(new Intent(this, App.class));
                finish();
            }
        }
    }

    /* *** */

    @Override
    protected void onStop() {
        super.onStop();
        activities_num--;
        if (activities_num == 0) {
            // User not in the application
            shouldExecuteOnResume = true;
            Log.i(getClass().getSimpleName(), "User not longer in the application");
        }
    }
    /* END */
}
