package com.zeevox.secure.ui.dialog;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;

import com.zeevox.secure.Flags;

import java.util.Objects;

public class CustomDimDialog {
    private View alertLayout;

    public AppCompatDialog dialog(@NonNull Activity activity, @LayoutRes int layout, boolean dismissOnTouchOutside) {
        // Create a dialog requesting the master password
        LayoutInflater inflater = activity.getLayoutInflater();

        // Create the dialog
        final AppCompatDialog dialog = new AppCompatDialog(activity);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Inflate the elements of the dialog
        alertLayout = inflater.inflate(layout, null, false);
        dialog.setContentView(alertLayout);

        // Get the dialog window to prevent repeating dialog.getWindow() each time.
        Window dialogWindow = dialog.getWindow();

        // Make rounded corners visible
        Objects.requireNonNull(dialogWindow).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Set background dimming according to percentage set in Flags.java
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.dimAmount = Flags.DIALOG_BACKGROUND_DIM;
        dialogWindow.setAttributes(lp);
        dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // Disable dismissing on back button or touch outside
        dialog.setCanceledOnTouchOutside(dismissOnTouchOutside);
        dialog.setCancelable(dismissOnTouchOutside);

        return dialog;
    }

    public View getAlertLayout() {
        return alertLayout;
    }
}
