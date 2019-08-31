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

package com.zeevox.secure.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.zeevox.secure.App;
import com.zeevox.secure.R;
import com.zeevox.secure.core.SecureAppCompatActivity;
import com.zeevox.secure.cryptography.Crypto;
import com.zeevox.secure.cryptography.Entry;

import java.util.Objects;

public class EditEntryActivity extends SecureAppCompatActivity {

    public static final String ADAPTER_POSITION = "adapterPosition";
    public static final String ORIGINAL_ADAPTER_POSITION = "origAdapterPos";

    private final String TAG = this.getClass().getSimpleName();
    private TextInputEditText keyNameInput;
    private TextInputEditText keyNotesInput;
    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;

    private String entryName;
    private int adapterPosition;

    private Crypto crypto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fetch the master key
        String masterKey = getIntent().getStringExtra("MASTER_KEY");

        try {
            crypto = new Crypto(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Inflate the layout
        setContentView(R.layout.activity_edit_entry);

        // Add a 'back' button to the toolbar and set
        // the Toolbar title
        try {
            final Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(getIntent().getBooleanExtra("display_back_button", true));
            getSupportActionBar().setTitle(R.string.title_activity_new_entry);
        } catch (NullPointerException npe) {
            Toast.makeText(this, R.string.error_occurred_miscellaneous, Toast.LENGTH_SHORT).show();
            npe.printStackTrace();
            setResult(Activity.RESULT_CANCELED);
            finish();
        }

        // Disable Android Oreo's AutoFill service (for now?)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().getDecorView().setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        }

        // Set up the input
        keyNameInput = findViewById(R.id.key_name_input);
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        keyNotesInput = findViewById(R.id.key_notes_input);


        adapterPosition = getIntent().getIntExtra(ADAPTER_POSITION, -1);
        // If editing (not creating new entry), get info.
        if (adapterPosition != -1) {
            Log.d(TAG, "Editing item with id " + adapterPosition);
            entryName = getIntent().getStringExtra("entryName");
            String entryKey = getIntent().getStringExtra("entryKey");
            String entryPass = getIntent().getStringExtra("entryPass");
            String entryNotes = getIntent().getStringExtra("entryNotes");

            keyNameInput.setText(entryKey);
            usernameInput.setText(entryName);
            passwordInput.setText(entryPass);
            keyNotesInput.setText(entryNotes);
        }
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
        if (item.getItemId() == R.id.action_done_new_entry) {
            addEntryComplete();
        }
        return super.onOptionsItemSelected(item);
    }

    private void addEntryComplete() {
        try {


            String keyNotes = null;
            if (!Objects.equals(keyNotesInput.getText().toString(), "")) {
                keyNotes = keyNotesInput.getText().toString();
            }

            Entry entry = Crypto.newEntry(keyNameInput.getText().toString(), usernameInput.getText().toString(),
                    passwordInput.getText().toString(), keyNotes, App.masterKey);

            if (entryName != null && adapterPosition != -1) {
                crypto.getEntries().removeEntryAt(adapterPosition);
            }

            int newAdapterPosition = crypto.getEntries().addEntrySorted(entry);

            setResult(Activity.RESULT_OK, new Intent()
                    .putExtra(ADAPTER_POSITION, newAdapterPosition)
                    .putExtra(ORIGINAL_ADAPTER_POSITION, adapterPosition));
            finish();
        } catch (Exception e) {
            setResult(Activity.RESULT_CANCELED);
            Snackbar.make(findViewById(R.id.root_new_entry),
                    "An entry with the name " + keyNameInput.getText().toString() + " already exists.",
                    Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}
