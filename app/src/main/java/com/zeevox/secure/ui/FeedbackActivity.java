package com.zeevox.secure.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.zeevox.secure.BuildConfig;
import com.zeevox.secure.R;
import com.zeevox.secure.ui.dialog.CustomDimDialog;

import java.util.Objects;

public class FeedbackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        final TextInputEditText issueTitleInput = findViewById(R.id.feedback_issue_input);
        final TextInputEditText issueReproduceInput = findViewById(R.id.feedback_issue_reproduce_input);
        final TextInputEditText expectedResultInput = findViewById(R.id.feedback_expected_result);
        final TextInputEditText actualResultInput = findViewById(R.id.feedback_actual_result);

        issueTitleInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        validateIssueTitle();
                    }
                });

        issueReproduceInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        validateReproductionSteps();
                    }
                });
        expectedResultInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        validateExpectedResult();
                    }
                });
        actualResultInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        validateActualResult();
                    }
                });

        //FEEDBACK BUTTON
        final Button feedbackButton = findViewById(R.id.feedback_send_button);
        feedbackButton.setOnClickListener(
                v -> {
                    //Set who to send email to
                    Uri uri = Uri.parse("mailto:zeevox.dev@gmail.com");
                    //Set up email intent
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, uri);

                    //Init feedback message
                    String feedbackMessage = getString(R.string.feedback_prefill_content);

                    //Set email title
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, issueTitleInput.getText().toString());

                    //Get reproduction steps
                    String issueReproduce = issueReproduceInput.getText().toString();
                    //Replace placeholder with data
                    feedbackMessage = feedbackMessage.replace("REPRODUCTION_STEPS", issueReproduce);

                    //Get expected result
                    String expectedResult = expectedResultInput.getText().toString();
                    //Replace placeholder with data
                    feedbackMessage = feedbackMessage.replace("EXPECTED_RESULT", expectedResult);

                    //Get actual result
                    String actualResult = actualResultInput.getText().toString();
                    //Replace placeholder with data
                    feedbackMessage = feedbackMessage.replace("ACTUAL_RESULT", actualResult);

                    //Replace device info placeholders with data
                    feedbackMessage =
                            feedbackMessage.replace("secure_version_name", BuildConfig.VERSION_NAME);
                    feedbackMessage = feedbackMessage.replace("device_fingerprint", Build.FINGERPRINT);

                    //Set email intent
                    emailIntent.putExtra(Intent.EXTRA_TEXT, feedbackMessage);

                    CheckBox checkNoDuplicate = findViewById(R.id.check_no_duplicate);
                    CheckBox checkLatestVersion = findViewById(R.id.check_latest_version);
                    CheckBox checkGenericTitle = findViewById(R.id.check_generic_title);
                    CheckBox checkDeviceInfo = findViewById(R.id.check_device_info);

                    if (checkNoDuplicate.isChecked()
                            && checkLatestVersion.isChecked()
                            && checkGenericTitle.isChecked()
                            && checkDeviceInfo.isChecked()
                            && validateIssueTitle()
                            && validateReproductionSteps()
                            && validateExpectedResult()
                            && validateActualResult()) {
                        try {
                            //Start default email client
                            startActivity(emailIntent);
                            finish();
                        } catch (ActivityNotFoundException activityNotFoundException) {
                            dialogNoEmailApp();
                        }
                    } else {
                        if (!checkNoDuplicate.isChecked()
                                || !checkLatestVersion.isChecked()
                                || !checkGenericTitle.isChecked()
                                || !checkDeviceInfo.isChecked()) {
                            dialogInvalid();
                        }
                    }
                });
    }

    private boolean validateIssueTitle() {
        final TextInputLayout issueTitleInputLayout = findViewById(R.id.feedback_issue_input_layout);
        final TextInputEditText issueTitleInput = findViewById(R.id.feedback_issue_input);
        try {
            //IF/ELSE to prevent no title if user deletes all characters
            if (issueTitleInput.getText().toString().isEmpty()) {
                Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.action_send_feedback);
                issueTitleInputLayout.setError(getString(R.string.feedback_error_empty));
                return false;
            } else {
                Objects.requireNonNull(getSupportActionBar()).setTitle(issueTitleInput.getText().toString());
                issueTitleInputLayout.setErrorEnabled(false);
                return true;
            }
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }
        return true;
    }

    private boolean validateReproductionSteps() {
        final TextInputLayout issueReproduceInputLayout =
                findViewById(R.id.feedback_issue_reproduce_input_layout);
        final TextInputEditText issueReproduceInput = findViewById(R.id.feedback_issue_reproduce_input);
        if (issueReproduceInput.getText().toString().isEmpty()) {
            issueReproduceInputLayout.setError(getString(R.string.feedback_error_empty));
            return false;
        } else {
            issueReproduceInputLayout.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateExpectedResult() {
        final TextInputLayout expectedResultInputLayout =
                findViewById(R.id.feedback_expected_result_layout);
        final TextInputEditText expectedResultInput = findViewById(R.id.feedback_expected_result);
        if (expectedResultInput.getText().toString().isEmpty()) {
            expectedResultInputLayout.setError(getString(R.string.feedback_error_empty));
            return false;
        } else {
            expectedResultInputLayout.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateActualResult() {
        final TextInputLayout actualResultInputLayout =
                findViewById(R.id.feedback_actual_result_layout);
        final TextInputEditText actualResultInput = findViewById(R.id.feedback_actual_result);
        if (actualResultInput.getText().toString().isEmpty()) {
            actualResultInputLayout.setError(getString(R.string.feedback_error_empty));
            return false;
        } else {
            actualResultInputLayout.setErrorEnabled(false);
            return true;
        }
    }

    private void dialogInvalid() {
        showDialog("Please make sure you have checked all the boxes.");
    }

    private void dialogNoEmailApp() {
        showDialog("No email application found on your device.\nPlease install a supported email app and try again.");
    }

    private void showDialog(String message) {
        CustomDimDialog customDimDialog = new CustomDimDialog();
        AppCompatDialog dialog = customDimDialog.dialog(this, R.layout.dialog_generic, true);
        View alertLayout = customDimDialog.getAlertLayout();
        ((TextView) alertLayout.findViewById(R.id.dialog_generic_title)).setText("Error");
        ((TextView) alertLayout.findViewById(R.id.dialog_generic_message)).setText(message);
        alertLayout.findViewById(R.id.dialog_generic_button_ok).setOnClickListener(v -> dialog.dismiss());
        alertLayout.findViewById(R.id.dialog_generic_button_cancel).setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}