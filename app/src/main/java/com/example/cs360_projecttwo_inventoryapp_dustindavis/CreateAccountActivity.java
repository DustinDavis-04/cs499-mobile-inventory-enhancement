package com.example.cs360_projecttwo_inventoryapp_dustindavis;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CreateAccountActivity extends AppCompatActivity {

    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private EditText titleEditText;
    private EditText emailEditText;
    private EditText usernameEditText;
    private EditText passwordEditText;

    private TextView emailErrorText;
    private Button createButton;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        // Database access for saving a new user
        dbHelper = new DatabaseHelper(this);

        // Connect layout IDs to code
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        titleEditText = findViewById(R.id.titleEditText);
        emailEditText = findViewById(R.id.emailEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        emailErrorText = findViewById(R.id.emailErrorText);
        createButton = findViewById(R.id.createButton);

        // Bottom nav back button, this cancels account creation
        LinearLayout backContainer = findViewById(R.id.backContainer);
        backContainer.setOnClickListener(v -> finish());

        // Start disabled so an empty form cannot be submitted
        setCreateButtonEnabled(false);

        // Watch required fields and enable Create only when valid
        attachValidationWatchers();

        // Run once so the button and email error start in the right state
        validateRequiredFields();

        // Create the account only when the form passes validation
        createButton.setOnClickListener(v -> attemptCreateAccount());
    }

    private void attachValidationWatchers() {

        // Only the required fields control the Create button state
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateRequiredFields();
            }
        };

        emailEditText.addTextChangedListener(watcher);
        usernameEditText.addTextChangedListener(watcher);
        passwordEditText.addTextChangedListener(watcher);
    }

    private void validateRequiredFields() {

        String email = emailEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Email is required and has to look like a real email address
        boolean emailValid = !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();

        // Show the email error only after the user starts typing
        boolean showEmailError = !email.isEmpty() && !emailValid;
        emailErrorText.setVisibility(showEmailError ? View.VISIBLE : View.GONE);

        // Create is enabled only when required fields are filled in and valid
        boolean enable = emailValid && !username.isEmpty() && !password.isEmpty();
        setCreateButtonEnabled(enable);
    }

    private void setCreateButtonEnabled(boolean enabled) {
        createButton.setEnabled(enabled);
        createButton.setAlpha(enabled ? 1.0f : 0.45f);
    }

    private void attemptCreateAccount() {

        // Optional fields
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String title = titleEditText.getText().toString().trim();

        // Required fields
        String email = emailEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Last check, this prevents a bad submit if something slips through
        if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fields marked with * are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Email format check
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid work email.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save the user in SQLite
        // Returns false if the username already exists
        boolean created = dbHelper.createUser(
                username,
                password,
                firstName,
                lastName,
                title,
                email
        );

        if (!created) {
            Toast.makeText(this, "Username already exists.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Return to login screen after success
        Toast.makeText(this, "Account created. Please log in.", Toast.LENGTH_SHORT).show();
        finish();
    }
}
