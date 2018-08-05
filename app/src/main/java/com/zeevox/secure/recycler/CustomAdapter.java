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

package com.zeevox.secure.recycler;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.zeevox.secure.Flags;
import com.zeevox.secure.R;
import com.zeevox.secure.cryptography.Crypto;
import com.zeevox.secure.cryptography.Encryptor;
import com.zeevox.secure.cryptography.Entry;
import com.zeevox.secure.ui.EditEntryActivity;
import com.zeevox.secure.ui.MainActivity;
import com.zeevox.secure.ui.PasswordsBottomModalSheet;
import com.zeevox.secure.util.LogUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Provide views to RecyclerView with data from mDataSet.
 */
public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {
    private static final String TAG = "CustomAdapter";
    private String[] mDataSet;

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet String[] containing the data to populate views to be used by RecyclerView.
     */
    public CustomAdapter(String[] dataSet) {
        mDataSet = dataSet;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.recycler_item, viewGroup, false);

        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        //LogUtils.d(TAG, "Element " + position + " set.");

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        viewHolder.getTextView().setText(mDataSet[position]);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (mDataSet == null) {
            return 0;
        } else {
            return mDataSet.length;
        }
    }

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder implements ActionMode.Callback {
        private final TextView textView;
        private ActionMode mActionMode;
        private AppCompatActivity activity = null;

        public ViewHolder(View v) {
            super(v);
            textView = v.findViewById(R.id.textView);

            // Define click listener for the ViewHolder's View.
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity = ((AppCompatActivity) v.getContext());
                    try {
                        showMasterDialog(MASTER_DIALOG_KEY_INFO, getAdapterPosition());
                        LogUtils.d(TAG, "Element " + getAdapterPosition() + " clicked.");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // Define long click listener and contextual action bar.
            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    activity = ((AppCompatActivity) view.getContext());
                    LogUtils.d(TAG, "Element " + getAdapterPosition() + " long clicked.");

                    if (mActionMode != null) {
                        return false;
                    }

                    // Start the CAB using the ActionMode.Callback defined above
                    mActionMode = activity.startSupportActionMode(mActionModeCallback());
                    view.setSelected(true);
                    return true;
                }
            });
        }

        public ActionMode.Callback mActionModeCallback() {
            return this;
        }

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_cab_main, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            try {
                mode.setTitle(Crypto.getEntries().getEntryAt(getAdapterPosition()).key);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_item_edit:
                    try {
                        showMasterDialog(MASTER_DIALOG_EDIT_ENTRY, getAdapterPosition());
                        // Hide the CAB once action selected
                        mode.finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                case R.id.action_item_delete:
                    try {
                        showMasterDialog(MASTER_DIALOG_DELETE_ENTRY, getAdapterPosition());
                        LogUtils.d(TAG, "Delete item with id " + getAdapterPosition());
                        // Hide the CAB once action selected
                        mode.finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
        }

        public TextView getTextView() {
            return textView;
        }

        PasswordsBottomModalSheet passwordsBottomModalSheet = new PasswordsBottomModalSheet();

        static final int MASTER_DIALOG_KEY_INFO = 1;
        static final int MASTER_DIALOG_EDIT_ENTRY = 2;
        static final int MASTER_DIALOG_DELETE_ENTRY = 3;

        /**
         * Master password dialog
         */
        private void showMasterDialog(final int command, final int adapterPosition) {
            try {
                Crypto.init();
                if (MainActivity.masterKey != null && Crypto.verifyMasterPass(MainActivity.masterKey) &&
                        !PreferenceManager.getDefaultSharedPreferences(activity)
                                .getBoolean(Flags.ASK_MASTER_PASS_EACH_TIME, true)) {
                    // Don't continue any further
                    Entry entry = Crypto.getEntries().getEntryAt(adapterPosition);
                    switch (command) {
                        case MASTER_DIALOG_KEY_INFO:
                            String keyNotes = null;
                            if (entry.notes != null) {
                                keyNotes = Encryptor.decrypt(entry.notes, MainActivity.masterKey.toCharArray());
                            }
                            passwordsBottomModalSheet.setKeyInfo(entry.key, Encryptor.decrypt(entry.name, MainActivity.masterKey.toCharArray()),
                                    Encryptor.decrypt(entry.pass, MainActivity.masterKey.toCharArray()), keyNotes);
                            passwordsBottomModalSheet.show(activity.getSupportFragmentManager(), "Password BottomSheet");
                            break;
                        case MASTER_DIALOG_EDIT_ENTRY:
                            Intent intent = new Intent(activity, EditEntryActivity.class);
                            intent.putExtra("entryKey", entry.key);
                            intent.putExtra("entryName", Encryptor.decrypt(entry.name, MainActivity.masterKey.toCharArray()));
                            intent.putExtra("entryPass", Encryptor.decrypt(entry.pass, MainActivity.masterKey.toCharArray()));
                            if (entry.notes != null) {
                                intent.putExtra("entryNotes", Encryptor.decrypt(entry.notes, MainActivity.masterKey.toCharArray()));
                            }
                            intent.putExtra("adapterPosition", adapterPosition);
                            activity.startActivity(intent);
                            break;
                        case MASTER_DIALOG_DELETE_ENTRY:
                            Crypto.getEntries().removeEntryAt(getAdapterPosition());
                            //TODO refresh recyclerview to notify user of changes
                            break;
                    }
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Create a dialog requesting the master password
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            // Prepare the layout
            LayoutInflater inflater = activity.getLayoutInflater();

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
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
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
                        MainActivity.masterKey = masterKeyInput.getText().toString();
                        // Verify that the password is correct
                        if (Crypto.verifyMasterPass(MainActivity.masterKey)) {
                            // Reset attempts counter
                            MainActivity.attempts[0] = 0;
                            // Dismiss the dialog
                            dialog.dismiss();
                            // Show key info
                            Entry entry = Crypto.getEntries().getEntryAt(adapterPosition);
                            switch (command) {
                                case MASTER_DIALOG_KEY_INFO:
                                    String keyNotes = null;
                                    if (entry.notes != null) {
                                        keyNotes = Encryptor.decrypt(entry.notes, MainActivity.masterKey.toCharArray());
                                    }
                                    passwordsBottomModalSheet.setKeyInfo(entry.key, Encryptor.decrypt(entry.name, MainActivity.masterKey.toCharArray()),
                                            Encryptor.decrypt(entry.pass, MainActivity.masterKey.toCharArray()), keyNotes);
                                    passwordsBottomModalSheet.show(activity.getSupportFragmentManager(), "Password BottomSheet");
                                    break;
                                case MASTER_DIALOG_EDIT_ENTRY:
                                    Intent intent = new Intent(activity, EditEntryActivity.class);
                                    intent.putExtra("entryKey", entry.key);
                                    intent.putExtra("entryName", Encryptor.decrypt(entry.name, MainActivity.masterKey.toCharArray()));
                                    intent.putExtra("entryPass", Encryptor.decrypt(entry.pass, MainActivity.masterKey.toCharArray()));
                                    if (entry.notes != null) {
                                        intent.putExtra("entryNotes", Encryptor.decrypt(entry.notes, MainActivity.masterKey.toCharArray()));
                                    }
                                    intent.putExtra("adapterPosition", adapterPosition);
                                    activity.startActivity(intent);
                                    break;
                                case MASTER_DIALOG_DELETE_ENTRY:
                                    Crypto.getEntries().removeEntryAt(getAdapterPosition());
                                    //TODO refresh recyclerview to notify user of changes
                                    break;
                            }
                        } else {
                            // Wrong password, show the dialog again with an e.
                            final TextInputLayout masterKeyLayout = alertLayout.findViewById(R.id.dialog_master_password_layout);
                            masterKeyLayout.setErrorEnabled(true);
                            masterKeyLayout.setError(activity.getResources().getString(R.string.error_wrong_master_pass));
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
                            MainActivity.attempts[0] = MainActivity.attempts[0] + 1;
                            // If more than three wrong attempts have been made, block the user.
                            if (MainActivity.attempts[0] >= Flags.MAX_ATTEMPTS) {
                                // Show an e as a toast message
                                Toast.makeText(activity, R.string.error_wrong_master_pass_thrice, Toast.LENGTH_SHORT).show();
                                // Close ALL running activities of this app
                                activity.finishAffinity();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        dialog.dismiss();
                    }
                }
            });
        }
    }
}
