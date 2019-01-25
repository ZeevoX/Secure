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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.zeevox.secure.R;
import com.zeevox.secure.core.PasswordGenerator;
import com.zeevox.secure.core.SecureAppCompatActivity;

import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;

public class PassgenActivity extends SecureAppCompatActivity {

    private TextView tv;

    @Override
    @RequiresApi(api = Build.VERSION_CODES.N)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passgen);

        final Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        tv = findViewById(R.id.test_textview);

        generatePassword();

        tv.setOnClickListener(v -> generatePassword());
        tv.setOnLongClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Generated Password", tv.getText().toString());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(v.getContext(), "Password copied successfully.", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Toast.makeText(v.getContext(), "Error occurred copying this password. Please try again.", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void generatePassword() {
        try {
            PasswordGenerator passwordGenerator = new PasswordGenerator();
            passwordGenerator.setUseLowerCase(true);
            passwordGenerator.setUseUpperCase(true);
            passwordGenerator.setUseNumbers(true);
            tv.setText(String.valueOf(passwordGenerator.generatePassword(12)));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
