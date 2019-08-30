package com.zeevox.secure.backup;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

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
import com.zeevox.secure.cryptography.Entries;

import java.util.Collections;

public class BackupRestoreHelper {

    public final static int REQUEST_CODE_SIGN_IN = 2208;
    private final String TAG = getClass().getSimpleName();
    private final Activity context;
    private DriveServiceHelper mDriveServiceHelper;
    private final GoogleSignInClient mGoogleSignInClient;
    private final GoogleSignInAccount googleAccount;
    private final SharedPreferences preferences;

    public BackupRestoreHelper(Activity activity) {
        this.context = activity;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions);
        googleAccount = GoogleSignIn.getLastSignedInAccount(context);
    }

    public boolean setup() {
        if (googleAccount == null) {
            Log.w(TAG, "No previously signed in Google Account available");
            context.startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
            return false;
        } else {
            Log.d(TAG, "Signed in as " + googleAccount.getEmail());

            // Use the authenticated account to sign in to the Drive service.
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
            // Its instantiation is required before handling any onClick actions.
            mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
            return true;
        }
    }

    public void backup() {
        if (mDriveServiceHelper == null) {
            Log.e(TAG, "DriveServiceHelper unavailable");
        }
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

    private boolean updateFile() {
        final boolean[] result = {false};
        String fileID = preferences.getString(Entries.FILENAME, null);
        if (mDriveServiceHelper != null) {
            if (fileID != null) {
                Log.d(TAG, "Updating " + fileID);

                mDriveServiceHelper.updateFile(context, fileID)
                        .addOnSuccessListener(aVoid -> result[0] = true)
                        .addOnFailureListener(exception -> Log.e(TAG, "Unable to update file via REST.", exception));
            } else {
                Log.w(TAG, "No previous file ID saved");
            }
        } else {
            Log.e(TAG, "DriveServiceHelper unavailable");
        }
        return result[0];
    }

    private boolean createFile() {
//        if (preferences.getString(Entries.FILENAME, null) != null) {
//            Log.d(getClass().getSimpleName(), "Secure has previously backed up passwords to Google Drive");
//            return updateFile();
//        }

        Log.d(TAG, "Creating new backup");

        boolean[] result = {false};
        if (mDriveServiceHelper != null) {
            mDriveServiceHelper.createFile(context)
                    .addOnSuccessListener(fileID -> {
                        Log.d(TAG, "Created file with ID " + fileID);
                        preferences.edit().putString(Entries.FILENAME, fileID).apply();
                        result[0] = true;
                    })
                    .addOnFailureListener(exception -> Log.e(TAG, "Couldn't create file", exception));
        } else {
            Log.e(TAG, "DriveServiceHelper unavailable");
        }
        return result[0];
    }

}
