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
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.zeevox.secure.App;
import com.zeevox.secure.BuildConfig;
import com.zeevox.secure.Flags;
import com.zeevox.secure.R;
import com.zeevox.secure.core.SecureAppCompatActivity;
import com.zeevox.secure.cryptography.Crypto;
import com.zeevox.secure.cryptography.Entries;
import com.zeevox.secure.recycler.CustomAdapter;
import com.zeevox.secure.settings.SettingsActivity;
import com.zeevox.secure.ui.dialog.CustomDimDialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

import static com.zeevox.secure.App.masterKey;

public class MainActivity extends SecureAppCompatActivity implements SearchView.OnQueryTextListener {

    private static final int DATASET_COUNT = 60;
    // Identify the permissions request
    private static final int PERMISSIONS_REQUEST = 2302;
    private static final int READ_REQUEST_CODE = 7436;
    CoordinatorLayout layout;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private NavigationView navigationView;
    private String[] mDataSet;
    private LinearLayoutManager mLayoutManager;
    private boolean displayingDialog = false;

    private static String generateRandomWord() {
        Random random = new Random();
        char[] word = new char[random.nextInt(8) + 3]; // words of length 3 through 10. (1 and 2 letter words are boring.)
        for (int j = 0; j < word.length; j++) {
            word[j] = (char) ('a' + random.nextInt(26));
        }
        return new String(word);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(getClass().getSimpleName(), "onCreate called");

        super.onCreate(savedInstanceState);

        // Inflate the layout
        setContentView(R.layout.activity_main);
        layout = findViewById(R.id.root_main);

        // Find the RecyclerView in the layout
        mRecyclerView = findViewById(R.id.recycler_view);

        // Improve performance since changes in content do
        // not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(false);

        // Select to use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);

        // Apply the LLM to the RecyclerView
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Initialise Crypto
        try {
            Crypto.init(MainActivity.this);
            if (Crypto.isNewUser()) {
                Log.d(getClass().getSimpleName(), "New user!");
                newMasterDialog();
            } else {
                setupRecycler();
            }
        } catch (Exception e) {
            showErrorSnackBar();
        }

        // Add onClick functionality for the FloatingActionButton
        final FloatingActionButton floatingActionButton = findViewById(R.id.main_fab);
        floatingActionButton.setOnClickListener(view -> {
            // Show the dialog requesting the master password
            showMasterDialog();
        });

        // Enable a contextual menu for recyclerView items. For more info, see
        // https://developer.android.com/guide/topics/ui/menus.html#context-menu
        registerForContextMenu(mRecyclerView);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu);
        setSupportActionBar(toolbar);

        final DrawerLayout mDrawerLayout = findViewById(R.id.drawer_layout);
        toolbar.setNavigationOnClickListener(v -> {
            // Handle the navigation click by showing a BottomDrawer etc.
            mDrawerLayout.openDrawer(GravityCompat.START);
        });

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    // close drawer when item is tapped
                    mDrawerLayout.closeDrawers();

                    switch (menuItem.getItemId()) {
                        case R.id.nav_passgen:
                            startActivity(new Intent(MainActivity.this, PassgenActivity.class));
                            break;
                        case R.id.nav_settings:
                            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                            break;
                        case R.id.nav_help:
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://zeevox.net/secure/")));
                            break;
                        case R.id.nav_send_feedback:
                            startActivity(new Intent(MainActivity.this, FeedbackActivity.class));
                            break;
                        case R.id.nav_tos:
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://zeevox.net/secure/privacy.html")));
                            break;
                    }

                    // Add code here to update the UI based on the item selected
                    // For example, swap UI fragments here

                    return true;
                });

        TextView navHeaderAppVersionText = navigationView.getHeaderView(0).findViewById(R.id.nav_header_app_version);
        navHeaderAppVersionText.setText(String.format(getString(R.string.nav_header_app_version), BuildConfig.VERSION_NAME));
    }

    private void setupRecycler() {
        // Initialize dataset, this data would usually come
        // from a local content provider or remote server.
        try {
            initDataset();
        } catch (Exception e) {
            showErrorSnackBar();
            e.printStackTrace();
        }

        // Specify our CustomAdapter
        mAdapter = new CustomAdapter(MainActivity.this, mDataSet);
        mRecyclerView.setAdapter(mAdapter);

        // Add item separators
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(), mLayoutManager.getOrientation());

        mRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_cab_main, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.action_item_edit:
                //editNote(info.id);
                return true;
            case R.id.action_item_delete:
                try {
                    Crypto.getEntries().removeEntryAt((int) info.id);
                    mAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
//        switch (item.getItemId()) {
//            case R.id.action_settings:
//                startActivity(new Intent(this, com.zeevox.secure.settings.SettingsActivity.class));
//                break;
//        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Generates Strings for RecyclerView's adapter. This data would usually come
     * from a local content provider or remote server.
     */
    private void initDataset() throws Exception {
        Crypto.init(MainActivity.this);

        if (!Flags.SAMPLE_DATA) {
            int numberOfEntries = Crypto.getEntries().getEntries().size();
            Entries mEntries = Crypto.getEntries();
            mDataSet = new String[numberOfEntries];
            for (int i = 0; i < numberOfEntries; i++) {
                mDataSet[i] = mEntries.getEntryAt(i).key;
            }
        } else {
            mDataSet = new String[DATASET_COUNT];
            for (int i = 0; i < DATASET_COUNT; i++) {
                mDataSet[i] = generateRandomWord(); // "This is element #" + i;
            }
            Arrays.sort(mDataSet);
        }
        // If there's nothing in the password database, help the user get started
        if (mDataSet.length == 0 && Crypto.isNewUser() && !displayingDialog) {
            // Create the intent
            Intent newActivityIntent = new Intent(MainActivity.this, EditEntryActivity.class);
            // Send the master key to the new entry activity too
            newActivityIntent.putExtra("MASTER_KEY", masterKey);
            newActivityIntent.putExtra("display_back_button", false);
            // Start the EditEntryActivity
            startActivity(newActivityIntent);
        }
    }

    /**
     * Master password dialog
     */
    private void showMasterDialog() {
        displayingDialog = true;
        try {
            String securityLevel = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(Flags.SECURITY_LEVEL, "fingerprint");
            if (masterKey != null && Crypto.verifyMasterPass(masterKey) && !Objects.equals(securityLevel, "password")) {
                assert securityLevel != null;
                switch (securityLevel) {
                    case "none":
                    case "appfingerprint":
                        // Create the intent
                        Intent newActivityIntent = new Intent(MainActivity.this, EditEntryActivity.class);
                        // Send the master key to the new entry activity too
                        newActivityIntent.putExtra("MASTER_KEY", masterKey);
                        // Start the EditEntryActivity
                        startActivity(newActivityIntent);
                        // Don't continue any further
                        return;
                    case "fingerprint":
                        KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
                        Intent credentialsIntent;
                        assert keyguardManager != null;
                        credentialsIntent = keyguardManager.createConfirmDeviceCredentialIntent("Verification required", "To use this application, first please verify that it's you.");
                        assert credentialsIntent != null;
                        startActivityForResult(credentialsIntent, 1042);
                        return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        CustomDimDialog customDimDialog = new CustomDimDialog();
        AppCompatDialog dialog = customDimDialog.dialog(this, R.layout.dialog_master_key, true);
        View alertLayout = customDimDialog.getAlertLayout();

        // Find the input field
        final TextInputEditText masterKeyInput = alertLayout.findViewById(R.id.dialog_master_password_input);

        // Show the keyboard
        masterKeyInput.requestFocus();
        /*InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(masterKeyInput, InputMethodManager.SHOW_FORCED);
        }*/

        // Handle clicks on the "OK" button
        alertLayout.findViewById(R.id.dialog_master_key_button_ok).setOnClickListener(view -> {
            try {
                Crypto.init(MainActivity.this);
                masterKey = masterKeyInput.getText().toString();
                // Verify that the password is correct
                if (Crypto.verifyMasterPass(masterKey)) {
                    // Dismiss the dialog
                    dialog.dismiss();
                    displayingDialog = false;
                    // Reset attempts count
                    App.attempts[0] = 0;
                    // Create the intent
                    Intent newActivityIntent = new Intent(MainActivity.this, EditEntryActivity.class);
                    // Send the master key to the new entry activity too
                    newActivityIntent.putExtra("MASTER_KEY", masterKey);
                    // Start the EditEntryActivity
                    startActivity(newActivityIntent);
                } else {
                    // Wrong password, show the dialog again with an e.
                    final TextInputLayout masterKeyLayout = alertLayout.findViewById(R.id.dialog_master_password_layout);
                    masterKeyLayout.setErrorEnabled(true);
                    masterKeyLayout.setError(getString(R.string.error_wrong_master_pass));
                    masterKeyInput.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            masterKeyLayout.setErrorEnabled(false);
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                        }
                    });
                    App.attempts[0] = App.attempts[0] + 1;
                    // If more than three wrong attempts have been made, block the user.
                    if (App.attempts[0] >= Flags.MAX_ATTEMPTS) {
                        // Show an e as a toast message
                        Toast.makeText(MainActivity.this, R.string.error_wrong_master_pass_thrice, Toast.LENGTH_SHORT).show();
                        // Close ALL running activities of this app
                        finishAffinity();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                dialog.dismiss();
                displayingDialog = false;
                showErrorSnackBar();
            }
        });

        // Handle clicks on the "Cancel" button
        alertLayout.findViewById(R.id.dialog_master_key_button_cancel).setOnClickListener(view -> dialog.dismiss());

        // Show the dialog
        dialog.show();
    }

    private void newMasterDialog() {
        displayingDialog = true;

        CustomDimDialog customDimDialog = new CustomDimDialog();
        AppCompatDialog dialog = customDimDialog.dialog(this, R.layout.dialog_new_master_key, false);
        View alertLayout = customDimDialog.getAlertLayout();

        // Find the input field
        final TextInputEditText masterKeyInput = alertLayout.findViewById(R.id.dialog_new_master_password_input);
        final TextInputEditText masterKeyRepeat = alertLayout.findViewById(R.id.dialog_repeat_master_password_input);

        // Show the keyboard
        /*masterKeyInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(masterKeyInput, InputMethodManager.SHOW_FORCED);
        }*/

        alertLayout.findViewById(R.id.dialog_new_master_key_button_ok).setOnClickListener(view -> {
            if (Objects.equals(masterKeyInput.getText().toString(), masterKeyRepeat.getText().toString())) {
                // Save the master key that was entered
                masterKey = masterKeyInput.getText().toString();
                // Dismiss the dialog
                dialog.dismiss();
                displayingDialog = false;
                // Create the intent
                Intent newActivityIntent = new Intent(MainActivity.this, EditEntryActivity.class);
                // Send the master key to the new entry activity too
                newActivityIntent.putExtra("MASTER_KEY", masterKey);
                newActivityIntent.putExtra("display_back_button", false);
                // Start the EditEntryActivity
                startActivity(newActivityIntent);
            } else {
                final TextInputLayout masterRepeatLayout = alertLayout.findViewById(R.id.dialog_repeat_master_password_layout);
                masterRepeatLayout.setErrorEnabled(true);
                masterRepeatLayout.setError("The two passwords you entered do not match.");
                masterKeyRepeat.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        masterRepeatLayout.setErrorEnabled(false);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });
            }
        });

        alertLayout.findViewById(R.id.dialog_new_master_key_button_neutral).setOnClickListener(v -> {
            // Check application required permissions
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {


                // Permission is not granted; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST);

                // PERMISSIONS_REQUEST is an app-defined int constant.
                // The callback method gets the result of the request.
            }

            // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
            // browser.
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            // Filter to show only images, using the image MIME data type.
            // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
            // To search for all documents available via installed storage providers,
            // it would be "*/*".
            intent.setType("text/xml");

            startActivityForResult(intent, READ_REQUEST_CODE);
            dialog.dismiss();
        });

        // Show the dialog
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                Log.i(getClass().getSimpleName(), "Uri: " + Objects.requireNonNull(uri).toString());
                //File from = new File(uri.toString());
                File destination = new File(getFilesDir(), Entries.FILENAME);
                ContentResolver resolver = getContentResolver();
                try {
                    InputStream inputStream = resolver.openInputStream(uri);
                    OutputStream outputStream = new FileOutputStream(destination);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = Objects.requireNonNull(inputStream).read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }

                    outputStream.close();
                    inputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.i(getClass().getSimpleName(), "Importing passwords successful!");
                Snackbar.make(layout, "Passwords successfully imported", Snackbar.LENGTH_SHORT).show();
            }
        }

        if (requestCode == 1042) {
            if (resultCode == RESULT_OK) {
                // Create the intent
                Intent newActivityIntent = new Intent(this, EditEntryActivity.class);
                newActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                // Send the master key to the new entry activity too
                newActivityIntent.putExtra("MASTER_KEY", masterKey);
                // Start the EditEntryActivity
                startActivity(newActivityIntent);
            } else {
                Toast.makeText(this, "Access denied, sorry.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 1043) {
            if (resultCode == RESULT_OK) {

            } else {
                Toast.makeText(this, "Access denied, sorry.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showErrorSnackBar() {
        Snackbar.make(findViewById(R.id.root_main), R.string.error_occurred_miscellaneous, Snackbar.LENGTH_LONG)
                .setActionTextColor(getResources().getColor(R.color.secure_accent, getTheme()))
                .setAction("RESTART APP", v -> {
                    finish();
                    startActivity(getIntent());
                }).show();
    }

    @Override
    protected void onStart() {
        Log.d(getClass().getSimpleName(), "onStart called");
        super.onStart();
        try {
            Crypto.init(MainActivity.this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!Crypto.isNewUser()) {
            setupRecycler();
        }
    }

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.  This means
     * that in some cases the previous state may still be saved, not allowing
     * fragment transactions that modify the state.  To correctly interact
     * with fragments in their proper state, you should instead override
     * {@link #onResumeFragments()}.
     */
    @Override
    public void onResume() {
        Log.d(getClass().getSimpleName(), "onResume called");
        try {
            navigationView.setCheckedItem(R.id.nav_home);
        } catch (NullPointerException npe) {}
        super.onResume();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }
}
