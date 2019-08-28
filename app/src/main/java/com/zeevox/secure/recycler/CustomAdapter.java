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

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.RecyclerView;

import com.zeevox.secure.App;
import com.zeevox.secure.R;
import com.zeevox.secure.core.SecureAppCompatActivity;
import com.zeevox.secure.cryptography.Crypto;
import com.zeevox.secure.cryptography.Encryptor;
import com.zeevox.secure.cryptography.Entry;
import com.zeevox.secure.ui.EditEntryActivity;
import com.zeevox.secure.ui.PasswordsBottomModalSheet;
import com.zeevox.secure.ui.dialog.AuthenticationDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provide views to RecyclerView with data from mDataSet.
 */
public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {
    private static final String TAG = "CustomAdapter";
    private final List<String> mDataSet;
    private RecyclerView mRecyclerView;
    private SecureAppCompatActivity activity;

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet String[] containing the data to populate views to be used by RecyclerView.
     */
    public CustomAdapter(SecureAppCompatActivity activity, String[] dataSet) {
        this.activity = activity;
        mDataSet = new ArrayList<>(Arrays.asList(dataSet));
    }

    /**
     * Called by RecyclerView when it starts observing this Adapter.
     * <p>
     * Keep in mind that same adapter may be observed by multiple RecyclerViews.
     *
     * @param recyclerView The RecyclerView instance which started observing this adapter.
     * @see #onDetachedFromRecyclerView(RecyclerView)
     */
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.recycler_item, viewGroup, false);

        return new ViewHolder(v, activity, mDataSet, mRecyclerView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int position) {
        //Log.d(TAG, "Element " + position + " set.");

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        viewHolder.getTextView().setText(mDataSet.get(position));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (mDataSet == null) {
            return 0;
        } else {
            return mDataSet.size();
        }
    }

    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder implements ActionMode.Callback, AuthenticationDialog.Callback {
        private final TextView textView;
        private ActionMode mActionMode;
        private SecureAppCompatActivity activity;
        private List<String> mDataSet;
        private RecyclerView mRecyclerView;
        private Crypto crypto;

        ViewHolder(View v, SecureAppCompatActivity activity, List<String> dataSet, RecyclerView recyclerView) {
            super(v);

            this.activity = activity;
            this.mDataSet = dataSet;
            this.mRecyclerView = recyclerView;
            try {
                crypto = new Crypto(activity);
            } catch (Exception e) {
                e.printStackTrace();
            }

            textView = v.findViewById(R.id.textView);

            // Define click listener for the ViewHolder's View.
            v.setOnClickListener(v1 -> {
                try {
                    new AuthenticationDialog(activity, crypto, MASTER_DIALOG_KEY_INFO, getAdapterPosition(), this);
                    Log.d(TAG, "Element " + getAdapterPosition() + " clicked.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // Define long click listener and contextual action bar.
            v.setOnLongClickListener(view -> {
                Log.d(TAG, "Element " + getAdapterPosition() + " long clicked.");

                if (mActionMode != null) {
                    return false;
                } else {
                    Log.w(TAG, "mActionMode is not null");
                }

                // Start the CAB using the ActionMode.Callback defined above
                mActionMode = ((AppCompatActivity) view.getContext()).startSupportActionMode(mActionModeCallback());
                view.setSelected(true);
                return true;
            });
        }

        ActionMode.Callback mActionModeCallback() {
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
                mode.setTitle(crypto.getEntries().getEntryAt(getAdapterPosition()).key);
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
                        new AuthenticationDialog(activity, crypto, MASTER_DIALOG_EDIT_ENTRY, getAdapterPosition(), this);
                        // Hide the CAB once action selected
                        mode.finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                case R.id.action_item_delete:
                    try {
                        new AuthenticationDialog(activity, crypto, MASTER_DIALOG_DELETE_ENTRY, getAdapterPosition(), this);
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

        TextView getTextView() {
            return textView;
        }

        static final int MASTER_DIALOG_KEY_INFO = 1;
        static final int MASTER_DIALOG_EDIT_ENTRY = 2;
        static final int MASTER_DIALOG_DELETE_ENTRY = 3;

        @Override
        public void onAuthenticationComplete(int resultCode, int requestCode, int adapterPosition) {
            try {
                Entry entry = crypto.getEntries().getEntryAt(adapterPosition);

                if (resultCode == AuthenticationDialog.RESULT_ACCEPT) {
                    switch (requestCode) {
                        case MASTER_DIALOG_KEY_INFO:
                            String keyNotes = null;
                            if (entry.notes != null) {
                                keyNotes = Encryptor.decrypt(entry.notes, App.masterKey.toCharArray());
                            }
                            new PasswordsBottomModalSheet(activity.getSupportFragmentManager(), entry.key, Encryptor.decrypt(entry.name, App.masterKey.toCharArray()),
                                    Encryptor.decrypt(entry.pass, App.masterKey.toCharArray()), keyNotes);
                            break;
                        case MASTER_DIALOG_EDIT_ENTRY:
                            Intent intent = new Intent(activity, EditEntryActivity.class);
                            intent.putExtra("entryKey", entry.key);
                            intent.putExtra("entryName", Encryptor.decrypt(entry.name, App.masterKey.toCharArray()));
                            intent.putExtra("entryPass", Encryptor.decrypt(entry.pass, App.masterKey.toCharArray()));
                            if (entry.notes != null) {
                                intent.putExtra("entryNotes", Encryptor.decrypt(entry.notes, App.masterKey.toCharArray()));
                            }
                            intent.putExtra("adapterPosition", adapterPosition);
                            activity.startActivity(intent);
                            break;
                        case MASTER_DIALOG_DELETE_ENTRY:
                            crypto.getEntries().removeEntryAt(getAdapterPosition());
                            mDataSet.remove(getAdapterPosition());
                            try {
                                mRecyclerView.getAdapter().notifyItemRemoved(getAdapterPosition());
                                mRecyclerView.getAdapter().notifyDataSetChanged();
                            } catch (NullPointerException npe) {
                                npe.printStackTrace();
                            }
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
