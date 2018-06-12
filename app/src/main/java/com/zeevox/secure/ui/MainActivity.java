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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.zeevox.secure.Flags;
import com.zeevox.secure.R;
import com.zeevox.secure.core.SecureAppCompatActivity;
import com.zeevox.secure.cryptography.Crypto;
import com.zeevox.secure.cryptography.Entries;
import com.zeevox.secure.recycler.CustomAdapter;
import com.zeevox.secure.util.LogUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends SecureAppCompatActivity {

    public static final int[] attempts = {0};
    private static final int DATASET_COUNT = 60;
    public static String masterKey = null;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private String[] mDataSet;
    private LinearLayoutManager mLayoutManager;
    private boolean wasNewUser = false;
    private boolean displayingDialog = false;
    private boolean mttpshown = false;

    public static String generateRandomWord() {
        Random random = new Random();
        char[] word = new char[random.nextInt(8) + 3]; // words of length 3 through 10. (1 and 2 letter words are boring.)
        for (int j = 0; j < word.length; j++) {
            word[j] = (char) ('a' + random.nextInt(26));
        }
        return new String(word);
    }

    @Override
    public boolean passwordProtect() {
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        LogUtils.d(getClass().getSimpleName(), "onCreate called");

        super.onCreate(savedInstanceState);

        // Inflate the layout
        setContentView(R.layout.activity_main);

        // Set the toolbar having inflated layout
        //setSupportActionBar((Toolbar) findViewById(R.id.toolbar_main));

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
            Crypto.init();
            if (Crypto.isNewUser()) {
                LogUtils.d(getClass().getSimpleName(), "New user!");
                newMasterDialog();
            } else {
                setupRecycler();
            }
        } catch (Exception e) {
            showErrorSnackBar();
        }

        // Add onClick functionality for the FloatingActionButton
        final FloatingActionButton floatingActionButton = findViewById(R.id.main_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Show the dialog requesting the master password
                showMasterDialog();
            }
        });

        // Enable a contextual menu for recyclerView items. For more info, see
        // https://developer.android.com/guide/topics/ui/menus.html#context-menu
        registerForContextMenu(mRecyclerView);

        // Hide bottom navigation bar when scrolling
        final BottomAppBar mBottomAppBar = findViewById(R.id.bottom_app_bar);
        setSupportActionBar(mBottomAppBar);
    }

    public void setupRecycler() {
        // Initialize dataset, this data would usually come
        // from a local content provider or remote server.
        try {
            initDataset();
        } catch (Exception e) {
            showErrorSnackBar();
            e.printStackTrace();
        }

        // Specify our CustomAdapter
        mAdapter = new CustomAdapter(mDataSet);
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
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, com.zeevox.secure.settings.SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Generates Strings for RecyclerView's adapter. This data would usually come
     * from a local content provider or remote server.
     */
    private void initDataset() throws Exception {
        Crypto.init();

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
            showIntroMTTP();
        }
    }

    /**
     * Master password dialog
     */
    private void showMasterDialog() {
        displayingDialog = true;
        try {
            if (masterKey != null && Crypto.verifyMasterPass(masterKey) &&
                    !PreferenceManager.getDefaultSharedPreferences(this)
                            .getBoolean(Flags.ASK_MASTER_PASS_EACH_TIME, true)) {
                // Create the intent
                Intent newActivityIntent = new Intent(MainActivity.this, EditEntryActivity.class);
                // Send the master key to the new entry activity too
                newActivityIntent.putExtra("MASTER_KEY", masterKey);
                // Start the EditEntryActivity
                startActivity(newActivityIntent);
                // Don't continue any further
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create a dialog requesting the master password
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Prepare the layout
        LayoutInflater inflater = getLayoutInflater();

        // Create a null parent since there really is no parent - this is a dialog!
        final ViewGroup nullParent = null;

        // Create the layout
        final View alertLayout = inflater.inflate(R.layout.dialog_master_key, nullParent, false);

        // Inflate the layout
        builder.setView(alertLayout);

        // Find the input field
        final TextInputEditText masterKeyInput = alertLayout.findViewById(R.id.dialog_master_password_input);

        // Show the keyboard
        masterKeyInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(masterKeyInput, InputMethodManager.SHOW_FORCED);
        }

        // Set up the positive "OK" action button
        builder.setPositiveButton(R.string.action_ok, null);

        // Set up the negative "CANCEL" action button
        builder.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                displayingDialog = false;
                dialog.cancel();
            }
        });

        // Disable dismissing on back button or touch outside
        setFinishOnTouchOutside(false);
        builder.setCancelable(false);

        // Finalize the dialog
        final AlertDialog dialog = builder.create();

        // Show the dialog
        dialog.show();

        // Overriding the handler immediately after show
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Crypto.init();
                    masterKey = masterKeyInput.getText().toString();
                    // Verify that the password is correct
                    if (Crypto.verifyMasterPass(masterKey)) {
                        // Dismiss the dialog
                        dialog.dismiss();
                        displayingDialog = false;
                        // Reset attempts count
                        attempts[0] = 0;
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
                        attempts[0] = attempts[0] + 1;
                        // If more than three wrong attempts have been made, block the user.
                        if (attempts[0] >= Flags.MAX_ATTEMPTS) {
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
            }
        });
    }

    private void newMasterDialog() {
        displayingDialog = true;
        // Create a dialog requesting the master password
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Prepare the layout
        LayoutInflater inflater = getLayoutInflater();

        // Create a null parent since there really is no parent - this is a dialog!
        final ViewGroup nullParent = null;

        // Create the layout
        final View alertLayout = inflater.inflate(R.layout.dialog_new_master_key, nullParent, false);

        // Inflate the layout
        builder.setView(alertLayout);

        // Find the input field
        final TextInputEditText masterKeyInput = alertLayout.findViewById(R.id.dialog_new_master_password_input);
        final TextInputEditText masterKeyRepeat = alertLayout.findViewById(R.id.dialog_repeat_master_password_input);

        // Show the keyboard
        masterKeyInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(masterKeyInput, InputMethodManager.SHOW_FORCED);
        }

        // Set up the positive "OK" action button
        builder.setPositiveButton(R.string.action_ok, null);

        // Set up the negative "CANCEL" action button
        builder.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Disable dismissing on back button or touch outside
        setFinishOnTouchOutside(false);
        builder.setCancelable(false);

        // Finalize the dialog
        final AlertDialog dialog = builder.create();

        // Show the dialog
        dialog.show();

        // Overriding the handler immediately after show
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Objects.equals(masterKeyInput.getText().toString(), masterKeyRepeat.getText().toString())) {
                    // Save the master key that was entered
                    masterKey = masterKeyInput.getText().toString();
                    // Dismiss the dialog
                    dialog.dismiss();
                    displayingDialog = false;
                    wasNewUser = true;
                    // Help the user get started
                    showIntroMTTP();
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
            }
        });
    }

    @Override
    public void onBackPressed() {

        super.onBackPressed();
    }

    private void showErrorSnackBar() {
        Snackbar.make(findViewById(R.id.root_main), R.string.error_occurred_miscellaneous, Snackbar.LENGTH_LONG)
                .setActionTextColor(getResources().getColor(R.color.google_blue_300, getTheme()))
                .setAction("RESTART APP", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                        startActivity(getIntent());
                    }
                }).show();
    }

    @Override
    protected void onStart() {
        LogUtils.d(getClass().getSimpleName(), "onStart called");
        super.onStart();
        try {
            Crypto.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!Crypto.isNewUser()) {
            wasNewUser = false;
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
    protected void onResume() {
        LogUtils.d(getClass().getSimpleName(), "onResume called");
        super.onResume();
    }

    public void refreshRecyclerView() {
        try {
            mRecyclerView.getAdapter().notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showIntroMTTP() {
        // Create the intent
        Intent newActivityIntent = new Intent(MainActivity.this, EditEntryActivity.class);
        // Send the master key to the new entry activity too
        newActivityIntent.putExtra("MASTER_KEY", masterKey);
        // Start the EditEntryActivity
        startActivity(newActivityIntent);
        /*new MaterialTapTargetPrompt.Builder(MainActivity.this)
                .setTarget(findViewById(R.id.main_fab))
                .setPrimaryText("Add a password to your database")
                .setSecondaryText("Get started by adding an entry to your password list")
                .setBackgroundColour(getResources().getColor(R.color.colorAccent, getTheme()))
                .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                    @Override
                    public void onPromptStateChanged(MaterialTapTargetPrompt prompt, int state) {
                        if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED) {
                            // User has pressed the prompt target
                            prompt.dismiss();
                            // Create the intent
                            Intent newActivityIntent = new Intent(MainActivity.this, EditEntryActivity.class);
                            // Send the master key to the new entry activity too
                            newActivityIntent.putExtra("MASTER_KEY", masterKey);
                            // Start the EditEntryActivity
                            startActivity(newActivityIntent);
                            mttpshown = true;
                        } else if (state == MaterialTapTargetPrompt.STATE_DISMISSED && !mttpshown) {
                            showIntroMTTP();
                        }
                    }
                })
                .show();*/
    }
}
