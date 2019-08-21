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

package com.zeevox.secure.debug;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.zeevox.secure.R;
import com.zeevox.secure.core.SecureAppCompatActivity;
import com.zeevox.secure.cryptography.Encryptor;
import com.zeevox.secure.util.StringUtils;

public class TestActivity extends SecureAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        final EditText entry_edit = findViewById(R.id.entry_edit);
        final EditText mpass_edit = findViewById(R.id.mpass_edit);
        Button submitButton = findViewById(R.id.button);
        final TextView originaltx = findViewById(R.id.test_original);
        final TextView cipheredtx = findViewById(R.id.test_ciphered);

        submitButton.setOnClickListener(view -> {
            originaltx.setText(entry_edit.getText().toString());
            try {
                String entry_pass = entry_edit.getText().toString();
                char[] master_pass = mpass_edit.getText().toString().toCharArray();
                String output = StringUtils.toHex(Encryptor.encrypt(entry_pass, master_pass));
                cipheredtx.setText(output);
                Log.e("TestActivity", output);
            } catch (Exception e) {
                cipheredtx.setText("ERROR");
                e.printStackTrace();
            }
        });
    }
}
