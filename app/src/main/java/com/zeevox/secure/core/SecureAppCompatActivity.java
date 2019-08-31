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

package com.zeevox.secure.core;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.zeevox.secure.R;

@SuppressLint("Registered")
public class SecureAppCompatActivity extends AppCompatActivity {

    private ResultListener callback;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            getWindow().setNavigationBarColor(getResources().getColor(
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                            ? R.color.secure_primary : android.R.color.transparent, getTheme()));
        }

        // Don't show app snapshot in overview and don't allow screenshots
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (callback != null) callback.onActivityResult(requestCode, resultCode, data);
    }

    public void setCallback(ResultListener callback) {
        this.callback = callback;
    }

    public interface ResultListener {
        void onActivityResult(int requestCode, int resultCode, @Nullable Intent data);
    }
}
