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

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.zeevox.secure.Flags;
import com.zeevox.secure.R;
import com.zeevox.secure.cryptography.Crypto;
import com.zeevox.secure.cryptography.Encryptor;
import com.zeevox.secure.cryptography.Entry;
import com.zeevox.secure.ui.MainActivity;

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
        //Log.d(TAG, "Element " + position + " set.");

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
                        showMasterDialog();
                        Log.d(TAG, "Element " + getAdapterPosition() + " clicked.");
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
                    Log.d(TAG, "Element " + getAdapterPosition() + " long clicked.");

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
                        Log.d(TAG, "Edit item with id " + getAdapterPosition());
                        // Hide the CAB once action selected
                        mode.finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                case R.id.action_item_delete:
                    try {
                        Crypto.getEntries().removeEntryAt(getAdapterPosition());
                        new MainActivity().refreshRecyclerView();
                        Log.d(TAG, "Delete item with id " + getAdapterPosition());
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

        /**
         * @param activity    The parameter that specifies the activity and the context in which this dialog should be shown
         * @param entryName   The parameter that specifies the dialog title to be used, this is the entry name.
         * @param keyUsername The parameter that specifies the key's username to show to the user
         * @param keyPassword The parameter that specifies the key's password to show to the user
         */
        void showKeyInfoDialog(Activity activity, final String entryName, final String keyUsername, final String keyPassword) {
            // Create the dialog
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            // Prepare the layout
            LayoutInflater inflater = activity.getLayoutInflater();

            // Create a null parent since there really is no parent - this is a dialog!
            final ViewGroup nullParent = null;

            // Create the layout
            final View alertLayout = inflater.inflate(R.layout.dialog_key_info, nullParent, false);

            // Inflate the layout
            builder.setView(alertLayout);

            // Populate the dialog with information
            TextView entryNameTextView = alertLayout.findViewById(R.id.dialog_key_info_entry_name);
            entryNameTextView.setText(entryName);
            TextView keyUsernameTextView = alertLayout.findViewById(R.id.dialog_key_info_username);
            keyUsernameTextView.setText(keyUsernameTextView.getText().toString().replace("%s", keyUsername));
            TextView keyPasswordTextView = alertLayout.findViewById(R.id.dialog_key_info_password);
            keyPasswordTextView.setText(keyPasswordTextView.getText().toString().replace("%s", keyPassword));

            // Handle [long] clicking the elements of the layout
            entryNameTextView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
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
                }
            });
            keyUsernameTextView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
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
                }
            });
            keyPasswordTextView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
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
                }
            });

            // Set up the positive "OK" action button
            builder.setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });

            // Finalize the dialog
            final AlertDialog dialog = builder.create();

            // Show the dialog
            dialog.show();
        }

        /**
         * Master password dialog
         */
        private void showMasterDialog() {
            try {
                Crypto.init();
                if (MainActivity.masterKey != null && Crypto.verifyMasterPass(MainActivity.masterKey) &&
                        !PreferenceManager.getDefaultSharedPreferences(activity)
                                .getBoolean(Flags.ASK_MASTER_PASS_EACH_TIME, true)) {
                    // Don't continue any further
                    Entry entry = Crypto.getEntries().getEntryAt(getAdapterPosition());
                    showKeyInfoDialog(activity, entry.key, Encryptor.decrypt(entry.name,
                            MainActivity.masterKey.toCharArray()), Encryptor.decrypt(entry.pass, MainActivity.masterKey.toCharArray()));
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
                            Entry entry = Crypto.getEntries().getEntryAt(getAdapterPosition());
                            showKeyInfoDialog(activity, entry.key, Encryptor.decrypt(entry.name,
                                    MainActivity.masterKey.toCharArray()), Encryptor.decrypt(entry.pass, MainActivity.masterKey.toCharArray()));
                        } else {
                            // Wrong password, show the dialog again with an error.
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
                                // Show an error as a toast message
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
