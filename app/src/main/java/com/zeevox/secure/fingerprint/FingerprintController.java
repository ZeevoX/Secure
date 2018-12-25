package com.zeevox.secure.fingerprint;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.hardware.fingerprint.FingerprintManagerCompat;

class FingerprintController extends FingerprintManagerCompat.AuthenticationCallback {
    private FingerprintManagerCompat fingerprintManager;
    private Callback callback;
    private TextView title;
    private TextView subtitle;
    private TextView errorText;
    private ImageView icon;

    interface Callback {
        void onAuthenticated();
        void onError();
    }
}
