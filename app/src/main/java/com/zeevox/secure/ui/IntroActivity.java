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

package com.zeevox.secure.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;
import com.zeevox.secure.R;

public class IntroActivity extends com.heinrichreimersoftware.materialintro.app.IntroActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addSlide(new SimpleSlide.Builder()
                .title("Welcome to Secure")
                .description("Store your passwords and security questions locally, digitally and securely.")
                .buttonCtaLabel("GET STARTED")
                .buttonCtaClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        nextSlide();
                    }
                })
                .image(R.drawable.book_secure)
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .build());
        if (ContextCompat.checkSelfPermission(IntroActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            addSlide(new SimpleSlide.Builder()
                    .background(R.color.google_blue_500)
                    .backgroundDark(R.color.google_blue_700)
                    .title("Storage permission")
                    .description("This app needs access to your internal storage in order to save your passwords.")
                    .permission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    .build());
        }
    }
}
