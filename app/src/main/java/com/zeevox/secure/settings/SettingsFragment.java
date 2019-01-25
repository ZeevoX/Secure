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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.ListView;

import com.zeevox.secure.Flags;
import com.zeevox.secure.R;
import com.zeevox.secure.cryptography.Crypto;

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
