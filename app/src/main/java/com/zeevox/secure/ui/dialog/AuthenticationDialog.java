package com.zeevox.secure.ui.dialog;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.zeevox.secure.App;
import com.zeevox.secure.Flags;
import com.zeevox.secure.R;
import com.zeevox.secure.core.SecureAppCompatActivity;
import com.zeevox.secure.cryptography.Crypto;

import java.util.Objects;

public class AuthenticationDialog implements SecureAppCompatActivity.ResultListener {

    private static final int RESULT_REJECT = 0;
    public static final int RESULT_ACCEPT = 1;

    private static final int CREDENTIALS_RESULT = 733;

    private Callback callback;

    private int requestCode;
    private int adapterPosition;

    public AuthenticationDialog(@NonNull SecureAppCompatActivity activity, @NonNull Crypto crypto, int requestCode, @NonNull Callback callback) {
        new AuthenticationDialog(activity, crypto, requestCode, -1, callback);
    }

    public AuthenticationDialog(@NonNull SecureAppCompatActivity activity, @NonNull Crypto crypto, int requestCode, int adapterPosition, @NonNull Callback callback) {
        new AuthenticationDialog(activity, crypto, requestCode, adapterPosition, callback, false);
    }

    public AuthenticationDialog(@NonNull SecureAppCompatActivity activity, @NonNull Crypto crypto, int requestCode, int adapterPosition, @NonNull Callback callback, boolean alwaysPassword) {

        this.callback = callback;
        this.requestCode = requestCode;
        this.adapterPosition = adapterPosition;

        activity.setCallback(this);

        String securityLevel = androidx.preference.PreferenceManager.getDefaultSharedPreferences(activity)
                .getString(Flags.SECURITY_LEVEL, Flags.SELEV_FINGERPRINT);

        if (App.masterKey != null && crypto.verifyMasterPass(App.masterKey) && !alwaysPassword) {
            switch (securityLevel) {
                case Flags.SELEV_NONE:
                    callback.onAuthenticationComplete(RESULT_ACCEPT, requestCode, adapterPosition);
                    return;
                case Flags.SELEV_FINGERPRINT:
                    KeyguardManager keyguardManager = activity.getSystemService(KeyguardManager.class);

                    if (keyguardManager != null) {
                        if (!keyguardManager.isKeyguardSecure()) {
                            Log.e(getClass().getSimpleName(), "Device keyguard is not secured by a PIN, pattern or password");
                            // Show a message that the user hasn't set up a lock screen.
                            Toast.makeText(activity,
                                    "Secure lock screen hasn't set up.\n"
                                            + "Go to 'Settings -> Security -> Screenlock' to set up a lock screen",
                                    Toast.LENGTH_LONG).show();
                            activity.finish();
                            return;
                        }

                        Intent credentialsIntent;
                        credentialsIntent = keyguardManager.createConfirmDeviceCredentialIntent("Verification required", "To use this application, first please verify that it's you.");
                        if (credentialsIntent != null) {
                            activity.startActivityForResult(credentialsIntent, CREDENTIALS_RESULT);
                        } else {
                            Log.e(getClass().getSimpleName(), "Could not create Keyguard Manager device credential request");
                        }
                    } else {
                        Log.e(getClass().getSimpleName(), "KeyguardManager system service unavailable");
                    }
                    return;
            }
        }

        // In all other cases, show the master password dialog

        CustomDimDialog customDimDialog = new CustomDimDialog();
        AppCompatDialog dialog = customDimDialog.dialog(activity, R.layout.dialog_master_key, false);
        View alertLayout = customDimDialog.getAlertLayout();

        // Find the input field
        final TextInputEditText masterKeyInput = alertLayout.findViewById(R.id.dialog_master_password_input);

        // Handle clicks on the "OK" button
        alertLayout.findViewById(R.id.dialog_master_key_button_ok).setOnClickListener(view -> {
            try {
                App.masterKey = Objects.requireNonNull(masterKeyInput.getText()).toString();
                // Verify that the password is correct
                if (crypto.verifyMasterPass(App.masterKey)) {
                    // Dismiss the dialog
                    dialog.dismiss();
                    // Reset attempts count
                    App.attempts[0] = 0;
                    // Show key info
                    callback.onAuthenticationComplete(RESULT_ACCEPT, requestCode, adapterPosition);
                } else {
                    // Wrong password, show the dialog again with an e.
                    final TextInputLayout masterKeyLayout = alertLayout.findViewById(R.id.dialog_master_password_layout);
                    masterKeyLayout.setErrorEnabled(true);
                    masterKeyLayout.setError(activity.getString(R.string.error_wrong_master_pass));
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
                    App.attempts[0] = App.attempts[0] + 1;
                    // If more than three wrong attempts have been made, block the user.
                    if (App.attempts[0] >= Flags.MAX_ATTEMPTS) {
                        // Show an e as a toast message
                        Toast.makeText(activity, R.string.error_wrong_master_pass_thrice, Toast.LENGTH_SHORT).show();
                        // Close ALL running activities of this app
                        activity.finishAffinity();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                dialog.dismiss();
                callback.onAuthenticationComplete(RESULT_REJECT, requestCode, adapterPosition);
            }
        });

        // Handle clicks on the "Cancel" button
        alertLayout.findViewById(R.id.dialog_master_key_button_cancel).setOnClickListener(view -> {
            dialog.dismiss();
            callback.onAuthenticationComplete(RESULT_REJECT, requestCode, adapterPosition);
        });

        // Show the dialog
        dialog.show();
    }

    @Override
    public void onActivityResult(int code, int resultCode, @Nullable Intent data) {
        if (code == CREDENTIALS_RESULT) {
            callback.onAuthenticationComplete(resultCode == Activity.RESULT_OK ? RESULT_ACCEPT : RESULT_REJECT, requestCode, adapterPosition);
        }
    }

    public interface Callback {
        void onAuthenticationComplete(int resultCode, int requestCode, int adapterPosition);
    }
}
