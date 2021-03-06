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

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.zeevox.secure.R;

/**
 * Show a bottom sheet with entry information, including the key, username, password and notes.
 */
public class PasswordsBottomModalSheet extends BottomSheetDialogFragment {

    private final BottomSheetBehavior.BottomSheetCallback bottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View view, int newState) {
            switch (newState) {
                case BottomSheetBehavior.STATE_COLLAPSED: {
                    Log.d("BSB", "collapsed");
                    //dismiss();
                }
                case BottomSheetBehavior.STATE_SETTLING: {
                    Log.d("BSB", "settling");
                }
                case BottomSheetBehavior.STATE_EXPANDED: {
                    Log.d("BSB", "expanded");
                }
                case BottomSheetBehavior.STATE_HIDDEN: {
                    Log.d("BSB", "hidden");
                    //dismiss();
                }
                case BottomSheetBehavior.STATE_DRAGGING: {
                    Log.d("BSB", "dragging");
                }
                case BottomSheetBehavior.STATE_HALF_EXPANDED: {
                    Log.d("BSB", "half-expanded");
                }
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            Log.d("BSB", "sliding " + slideOffset);
        }
    };
    private final String entryName;
    private final String keyUsername;
    private final String keyPassword;
    private final String keyNotes;

    public PasswordsBottomModalSheet(@NonNull FragmentManager fragmentManager, @NonNull String entryName, @NonNull String keyUsername, @NonNull String keyPassword, @Nullable String keyNotes) {
        super();
        this.entryName = entryName;
        this.keyUsername = keyUsername;
        this.keyPassword = keyPassword;
        this.keyNotes = keyNotes;
        show(fragmentManager, getClass().getSimpleName());
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        View contentView = View.inflate(getContext(), R.layout.sheet_key_info, null);
        dialog.setContentView(contentView);

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();

        // Populate the dialog with information
        TextView entryNameTextView = contentView.findViewById(R.id.sheet_key_info_entry_name);
        entryNameTextView.setText(entryName);
        TextView keyUsernameTextView = contentView.findViewById(R.id.sheet_key_info_username);
        keyUsernameTextView.setText(keyUsername);
        TextView keyPasswordTextView = contentView.findViewById(R.id.sheet_key_info_password);
        keyPasswordTextView.setText(keyPassword);
        TextView keyNotesTextView = contentView.findViewById(R.id.sheet_key_info_notes);
        if (keyNotes == null) {
            keyNotesTextView.setVisibility(View.GONE);
        } else {
            keyNotesTextView.setText(keyNotes);
        }
        // Handle [long] clicking the elements of the layout
        entryNameTextView.setOnLongClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Account URL", entryName);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(view.getContext(), "Account URL copied successfully.", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Toast.makeText(view.getContext(), "Error occurred copying account URL. Please try again.", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        keyUsernameTextView.setOnLongClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Account username", keyUsername);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(view.getContext(), "Username copied successfully.", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Toast.makeText(view.getContext(), "Error occurred copying username. Please try again.", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        keyPasswordTextView.setOnLongClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Account password", keyPassword);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(view.getContext(), "Password copied successfully.", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Toast.makeText(view.getContext(), "Error occurred copying password. Please try again.", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        keyNotesTextView.setOnLongClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Extra account information", keyNotes);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(view.getContext(), "Notes copied successfully.", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Toast.makeText(view.getContext(), "Error occurred copying the notes. Please try again.", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        if (behavior instanceof BottomSheetBehavior) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(bottomSheetCallback);
        }
    }
}
