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

package com.zeevox.secure.settings;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.zeevox.secure.Flags;
import com.zeevox.secure.R;
import com.zeevox.secure.backup.BackupRestoreHelper;
import com.zeevox.secure.core.SecureAppCompatActivity;
import com.zeevox.secure.cryptography.Crypto;
import com.zeevox.secure.cryptography.Entries;
import com.zeevox.secure.ui.dialog.AuthenticationDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat implements AuthenticationDialog.Callback {

    private static final int PERMISSIONS_REQUEST = 2550;
    private static final int REQUEST_AUTHENTICATION = 6600;
    private ListPreference securityLevelPreference;
    private Crypto crypto;
    private BackupRestoreHelper backupRestoreHelper;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        if (getActivity() == null) return;

        try {
            crypto = new Crypto(getActivity());
        } catch (Exception e) {
            e.printStackTrace();
        }

        securityLevelPreference = findPreference(Flags.SECURITY_LEVEL);
        List<String> seLevels = Arrays.asList(getResources().getStringArray(R.array.security_level_entries));
        securityLevelPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            int valueIndex = seLevels.indexOf(newValue);
            // Ask for the master password if the new security level is lower than the current one
            if (seLevels.indexOf(securityLevelPreference.getValue()) > valueIndex) {
                new AuthenticationDialog(((SecureAppCompatActivity) getActivity()), crypto, REQUEST_AUTHENTICATION, valueIndex, SettingsFragment.this, true);
                return false;
            } else {
                return true;
            }
        });

        Preference exportPasswordsPreference = findPreference(Flags.EXPORT_PASSWORDS);
        exportPasswordsPreference.setOnPreferenceClickListener(preference -> {
            // Check application required permissions
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted; request the permission
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST);

                // PERMISSIONS_REQUEST is an app-defined int constant.
                // The callback method gets the result of the request.

            } else {
                // Permission has already been granted
                if (exportPasswords()) {
                    Snackbar.make(Objects.requireNonNull(getView()), getString(R.string.export_passwords_message_success), Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(Objects.requireNonNull(getView()), getString(R.string.export_passwords_error_miscellaneous), Snackbar.LENGTH_LONG).show();
                }
            }
            return true;
        });

        Preference backupNowPreference = findPreference(Flags.BACKUP_RESTORE_NOW);
        backupNowPreference.setOnPreferenceClickListener(preference -> {
            BackupRestoreHelper.Callback brhCallback = backupSuccess -> {
                if (backupSuccess) Snackbar.make(getView(), getString(R.string.brh_message_backup_success), BaseTransientBottomBar.LENGTH_LONG).show();
            };
            backupRestoreHelper = new BackupRestoreHelper(getActivity(), brhCallback);
            backupRestoreHelper.setup();
            return true;
        });

        Preference deleteDbPreference = findPreference(Flags.CLEAR_DATABASE);

        deleteDbPreference.setEnabled(crypto.getFile().exists());
        deleteDbPreference.setOnPreferenceClickListener(preference -> {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
            alertDialog.setTitle(getString(R.string.dialog_clear_database_warning_title));
            alertDialog.setMessage(getString(R.string.dialog_clear_database_warning_message));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.action_delete), (dialog, which) -> {
                crypto.getFile().delete();
                preference.setEnabled(false);
                dialog.dismiss();
            });
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.cancel), (dialog, which) -> dialog.dismiss());
            alertDialog.show();
            return true;
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Hide divider line between preferences
        setDivider(new ColorDrawable(Color.TRANSPARENT));
        setDividerHeight(0);
    }

    private boolean exportPasswords() {
        Uri uri = Uri.fromFile(new File(getActivity().getFilesDir(), Entries.FILENAME));
        Log.i(this.getClass().getSimpleName(), "Uri: " + Objects.requireNonNull(uri).toString());
        File destination = new File(Environment.getExternalStorageDirectory(), Entries.FILENAME);
        ContentResolver resolver = getActivity().getContentResolver();
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
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission was granted, yay!
                exportPasswords();

            } else {
                // permission denied, boo!
                Snackbar.make(Objects.requireNonNull(getView()), getString(R.string.export_passwords_error_storage_perm_denied), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.action_try_again), view -> {
                            // Permission is not granted; request the permission
                            ActivityCompat.requestPermissions(getActivity(),
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    PERMISSIONS_REQUEST);
                        }).show();
            }
        }
    }

    @Override
    public void onAuthenticationComplete(int resultCode, int requestCode, int adapterPosition) {
        if (requestCode == REQUEST_AUTHENTICATION && resultCode == AuthenticationDialog.RESULT_ACCEPT) {
            securityLevelPreference.setValueIndex(adapterPosition);
        }
    }
}
