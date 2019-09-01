/*
 *  Copyright (C) 2019 Timothy "ZeevoX" Langer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 *  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.zeevox.secure.backup;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.zeevox.secure.core.SecureAppCompatActivity;
import com.zeevox.secure.cryptography.Entries;

import java.util.Collections;

/**
 * @author ZeevoX
 * @since 2019-08-23
 *
 * <em>BackupRestoreHelper</em> is used to manage the Google Drive™ backup feature of this app.
 *
 * It communicates with {@link DriveServiceHelper} to manage signing into the user's Google Account
 * and uploading the password database to Drive.
 */
public class BackupRestoreHelper implements SecureAppCompatActivity.ResultListener {

    private final static int REQUEST_CODE_SIGN_IN = 2208;
    private final String TAG = getClass().getSimpleName();
    private final Activity context;
    private DriveServiceHelper mDriveServiceHelper;
    private final GoogleSignInClient mGoogleSignInClient;
    private final GoogleSignInAccount googleAccount;
    private final SharedPreferences preferences;
    private Callback callback;

    /**
     * Initialise a new BackupRestoreHelper
     * @param activity {@link Activity} used to provide the necessary {@link android.content.Context context} for certain methods
     * @param callback {@link Callback} used to notify the user when a backup has completed
     */
    public BackupRestoreHelper(@NonNull Activity activity, @NonNull Callback callback) {
        this.context = activity;
        this.callback = callback;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions);
        googleAccount = GoogleSignIn.getLastSignedInAccount(context);
    }

    /**
     * Manage signing into the user's Google Account.
     */
    public void setup() {
        if (googleAccount == null) { // If the user hasn't signed in before, we show a prompt to select a Google Account.
            Log.w(TAG, "No previously signed in Google Account available");
            context.startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
        } else {
            Log.d(TAG, "Signed in as " + googleAccount.getEmail());

            // Use the previously authenticated account to sign in to the Drive service.
            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            context, Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(googleAccount.getAccount());
            Drive googleDriveService =
                    new Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName("Secure - Password Manager")
                            .build();

            // The DriveServiceHelper encapsulates all REST API and SAF functionality.
            // Its instantiation is required before handling any backup actions.
            mDriveServiceHelper = new DriveServiceHelper(googleDriveService);

            // Now that sign-in is complete, we can backup the database
            backup();
        }
    }

    /**
     * Manages backing up the password database to Google Drivel
     */
    private void backup() {
        if (mDriveServiceHelper == null) {
            Log.e(TAG, "DriveServiceHelper unavailable");
            return;
        }

        /*
        * We save the file ID string in the {@link SharedPreferences} of the app
        * If we have previously uploaded to Google Drive, then the saved String will not be null
        */
        String fileID = preferences.getString(Entries.FILENAME, null);

        if (fileID != null) {
            Log.d(TAG, "Fetching file metadata for file " + fileID);
            mDriveServiceHelper.getFile(fileID)
                    .addOnSuccessListener(file -> {
                        if (file != null) {
                            Log.d(TAG, "Discovered backup from " + file.getModifiedTime());
                            updateFile();
                        } else {
                            createFile();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Unable to retrieve requested file.", e);
                        createFile();
                    });
        } else {
            Log.w(TAG, "No previous file ID saved");
            createFile();
        }
    }

    /**
     * If there already exists a backup in Google Drive, we use this method to update
     * the remote backup with the newer local version
     */
    private void updateFile() {
        String fileID = preferences.getString(Entries.FILENAME, null);
        if (mDriveServiceHelper != null) {
            if (fileID != null) {
                Log.d(TAG, "Updating " + fileID);

                mDriveServiceHelper.updateFile(context, fileID)
                        .addOnSuccessListener(aVoid -> callback.onBackupComplete(true))
                        .addOnFailureListener(exception -> {
                            Log.e(TAG, "Unable to update file via REST.", exception);
                            callback.onBackupComplete(false);
                        });
            } else {
                Log.w(TAG, "No previous file ID saved");
            }
        } else {
            Log.e(TAG, "DriveServiceHelper unavailable");
        }

        callback.onBackupComplete(false);
    }

    /**
     * If there is no previous backup in Google Drive that we know of, we create
     * a new Google Drive file where we will upload the password database from now on.
     */
    private void createFile() {
        Log.d(TAG, "Creating new backup");

        if (mDriveServiceHelper != null) {
            mDriveServiceHelper.createFile(context)
                    .addOnSuccessListener(fileID -> {
                        Log.d(TAG, "Created file with ID " + fileID);
                        preferences.edit().putString(Entries.FILENAME, fileID).apply();
                        callback.onBackupComplete(true);
                    })
                    .addOnFailureListener(exception -> {
                        Log.e(TAG, "Couldn't create file", exception);
                        callback.onBackupComplete(false);
                    });
        } else {
            Log.e(TAG, "DriveServiceHelper unavailable");
        }
        callback.onBackupComplete(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            setup();
        }
    }

    public interface Callback {
        void onBackupComplete(boolean backupSuccess);
    }
}
