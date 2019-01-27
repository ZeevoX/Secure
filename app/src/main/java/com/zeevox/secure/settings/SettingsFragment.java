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

package com.zeevox.secure.settings;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.google.android.material.snackbar.Snackbar;
import com.zeevox.secure.Flags;
import com.zeevox.secure.R;
import com.zeevox.secure.cryptography.Crypto;
import com.zeevox.secure.cryptography.Entries;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        try {
            Crypto.init(getActivity());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Preference exportPasswordsPreference = findPreference(Flags.EXPORT_PASSWORDS);
        exportPasswordsPreference.setOnPreferenceClickListener(preference -> {
            Uri uri = Uri.fromFile(new File(getActivity().getFilesDir(), Entries.FILENAME));
            Log.i(getClass().getSimpleName(), "Uri: " + Objects.requireNonNull(uri).toString());
            File destination = new File(Environment.getExternalStorageDirectory(),  Entries.FILENAME);
            ContentResolver resolver = getActivity().getContentResolver();
            try {
                InputStream inputStream = resolver.openInputStream(uri);
                OutputStream outputStream = new FileOutputStream(destination);
                byte buffer[] = new byte[1024];
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
            Log.i(getClass().getSimpleName(), "Exporting passwords successful!");
            Snackbar.make(Objects.requireNonNull(getView()), "Passwords successfully exported to " + destination.toString(), Snackbar.LENGTH_LONG).show();

            return true;
        });

        Preference deleteDbPreference = findPreference(Flags.CLEAR_DATABASE);

        deleteDbPreference.setEnabled(Crypto.getFile().exists());
        deleteDbPreference.setOnPreferenceClickListener(preference -> {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
            alertDialog.setTitle("Delete database?");
            alertDialog.setMessage("This will delete ALL passwords in the database, permanently. There is no way back after this! Are you sure you want to continue?");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "DELETE", (dialog, which) -> {
                Crypto.getFile().delete();
                preference.setEnabled(false);
                dialog.dismiss();
            });
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "CANCEL", (dialog, which) -> dialog.dismiss());
            alertDialog.show();
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Hide divider line between preferences
        View rootView = getView();
        if (rootView != null) {
            ListView list = rootView.findViewById(android.R.id.list);
            list.setDivider(null);
        }
    }
}
