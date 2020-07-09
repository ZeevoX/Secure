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
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
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
import com.zeevox.secure.App;
import com.zeevox.secure.Flags;
import com.zeevox.secure.R;
import com.zeevox.secure.backup.BackupRestoreHelper;
import com.zeevox.secure.core.SecureAppCompatActivity;
import com.zeevox.secure.cryptography.Crypto;
import com.zeevox.secure.cryptography.Entries;
import com.zeevox.secure.ui.dialog.AuthenticationDialog;

import java.io.File;
import java.io.FileInputStream;
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
    private static final int SAVE_DATABASE_REQUEST = 734;
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
        // Export passwords on Android Q using the new storage access framework
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportPasswordsPreference.setOnPreferenceClickListener(preference -> {
                Intent shareIntent = new Intent()
                        .setAction(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("text/xml")
                        .putExtra(Intent.EXTRA_TITLE, Entries.FILENAME);
                startActivityForResult(shareIntent, SAVE_DATABASE_REQUEST);
                return true;
            });
        } else {
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
                        Snackbar.make(requireView(), getString(R.string.export_passwords_message_success), Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(requireView(), getString(R.string.export_passwords_error_miscellaneous), Snackbar.LENGTH_LONG).show();
                    }
                }
                return true;
            });
        }

        Preference backupNowPreference = findPreference(Flags.BACKUP_RESTORE_NOW);
        backupNowPreference.setOnPreferenceClickListener(preference -> {
            BackupRestoreHelper.Callback brhCallback = backupSuccess -> {
                if (backupSuccess)
                    Snackbar.make(requireView(), getString(R.string.brh_message_backup_success), BaseTransientBottomBar.LENGTH_LONG).show();
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

                // Once the database has been cleared we take the user back to the setup screen
                startActivity(new Intent(getActivity(), App.class));
                getActivity().finish();
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
                Snackbar.make(requireView(), getString(R.string.export_passwords_error_storage_perm_denied), Snackbar.LENGTH_LONG)
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case SAVE_DATABASE_REQUEST:
                if (resultCode != Activity.RESULT_OK) {
                    Snackbar.make(requireView(), "The file picker had something unexpected happen. Please try again.", Snackbar.LENGTH_LONG).show();
                    return;
                }

                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        InputStream internalDatabase = null;
                        OutputStream externalSave = null;
                        try {
                            internalDatabase = new FileInputStream(crypto.getFile());

                            ParcelFileDescriptor parcelFileDescriptor = requireContext().getContentResolver().openFileDescriptor(uri, "w");
                            externalSave = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());

                            // Copy the bits from instream to outstream
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = internalDatabase.read(buf)) > 0) {
                                externalSave.write(buf, 0, len);
                            }

                            Snackbar.make(requireView(), "Successfully exported passwords", Snackbar.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Snackbar.make(requireView(), "Exporting passwords unsuccessful.", Snackbar.LENGTH_LONG).show();
                            e.printStackTrace();
                        } finally {
                            try {
                                if (internalDatabase != null) internalDatabase.close();
                                if (externalSave != null) externalSave.close();
                            } catch (IOException e) {
                                Snackbar.make(requireView(), "Something went really wrong.", Snackbar.LENGTH_LONG).show();
                                e.printStackTrace();
                            }
                        }
                    } else {
                        Snackbar.make(requireView(), "Invalid file location picked, please try a different one", Snackbar.LENGTH_LONG).show();
                    }
                } else {
                    Snackbar.make(requireView(), "Something went wrong, please try again", Snackbar.LENGTH_LONG).show();
                }
                break;
        }
    }
}
