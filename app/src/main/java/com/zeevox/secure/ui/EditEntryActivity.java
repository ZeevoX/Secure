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
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.zeevox.secure.R;
import com.zeevox.secure.core.SecureAppCompatActivity;
import com.zeevox.secure.cryptography.Crypto;
import com.zeevox.secure.util.LogUtils;

import java.util.Objects;

public class EditEntryActivity extends SecureAppCompatActivity {

    // Identify the permissions request
    private static final int PERMISSIONS_REQUEST = 2302;

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
                // Check application required permissions
                if (ContextCompat.checkSelfPermission(EditEntryActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                        // Permission is not granted; request the permission
                        ActivityCompat.requestPermissions(EditEntryActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                PERMISSIONS_REQUEST);

                        // PERMISSIONS_REQUEST is an app-defined int constant.
                        // The callback method gets the result of the request.

                } else {
                    // Permission has already been granted
                    addEntryComplete();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addEntryComplete() {
        try {
            Crypto.init();
            String keyNotes = null;
            if (!Objects.equals(keyNotesInput.getText().toString(), "")) {
                keyNotes = keyNotesInput.getText().toString();
            }
            Crypto.addEntry(keyNameInput.getText().toString(), usernameInput.getText().toString(),
                    passwordInput.getText().toString(), keyNotes, masterKey);
            finish();
        } catch (Exception e) {
            Snackbar.make(findViewById(R.id.root_new_entry),
                    "An entry with the name " + keyNameInput.getText().toString() + " already exists.",
                    Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    addEntryComplete();

                } else {
                    // permission denied, boo!
                    Snackbar.make(findViewById(R.id.root_new_entry), "Storage permission denied; cannot save.", Snackbar.LENGTH_LONG)
                            .setAction("TRY AGAIN", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    // Permission is not granted; request the permission
                                    ActivityCompat.requestPermissions(EditEntryActivity.this,
                                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                            PERMISSIONS_REQUEST);
                                }
                            }).show();
                }
            }
        }
    }
}
