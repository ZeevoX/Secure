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

import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.zeevox.secure.R;
import com.zeevox.secure.core.SecureAppCompatActivity;
import com.zeevox.secure.cryptography.Crypto;
import com.zeevox.secure.util.LogUtils;

public class EditEntryActivity extends SecureAppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();
    protected TextInputLayout keyNameLayout;
    protected TextInputEditText keyNameInput;
    protected TextInputLayout keyNotesLayout;
    protected TextInputEditText keyNotesInput;
    protected TextInputLayout usernameLayout;
    protected TextInputEditText usernameInput;
    protected TextInputLayout passwordLayout;
    protected TextInputEditText passwordInput;
    protected String masterKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fetch the master key
        masterKey = getIntent().getStringExtra("MASTER_KEY");

        // Inflate the layout
        setContentView(R.layout.activity_edit_entry);

        // Add a 'back' button to the toolbar and set
        // the Toolbar title
        try {
            //noinspection ConstantConditions
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_activity_new_entry);
        } catch (NullPointerException npe) {
            Toast.makeText(this, R.string.error_occurred_miscellaneous, Toast.LENGTH_SHORT).show();
            npe.printStackTrace();
            finish();
        }

        // Disable Android Oreo's AutoFill service (for now?)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().getDecorView().setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        }

        // Set up the input
        keyNameLayout = findViewById(R.id.key_name_layout);
        keyNameInput = findViewById(R.id.key_name_input);
        keyNotesLayout = findViewById(R.id.key_notes_layout);
        keyNotesInput = findViewById(R.id.key_notes_input);
        usernameLayout = findViewById(R.id.username_layout);
        usernameInput = findViewById(R.id.username_input);
        passwordLayout = findViewById(R.id.password_layout);
        passwordInput = findViewById(R.id.password_input);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //new MainActivity().refreshRecyclerView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_entry, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_done_new_entry:
                try {
                    Crypto.addEntry(keyNameInput.getText().toString(), usernameInput.getText().toString(), passwordInput.getText().toString(), null, masterKey);
                    finish();
                } catch (Exception e) {
                    Snackbar.make(findViewById(R.id.root_new_entry),
                            "An entry with the name " + keyNameInput.getText().toString() + " already exists.",
                            Snackbar.LENGTH_LONG).show();
                    LogUtils.error(TAG, e);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
