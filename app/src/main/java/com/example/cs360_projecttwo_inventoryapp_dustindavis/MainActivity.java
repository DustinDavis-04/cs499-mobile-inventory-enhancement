package com.example.cs360_projecttwo_inventoryapp_dustindavis;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // Login input fields
    private EditText usernameEditText;
    private EditText passwordEditText;

    // Buttons on the login screen
    private Button loginButton;
    private Button createAccountButton;

    // Database helper for user login checks
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connect layout IDs to code
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        createAccountButton = findViewById(R.id.createAccountButton);

        // Used to validate login against the users table
        dbHelper = new DatabaseHelper(this);

        // Try to sign in
        loginButton.setOnClickListener(v -> attemptLogin());

        // Go to the create account screen
        createAccountButton.setOnClickListener(v -> openCreateAccount());
    }

    private void attemptLogin() {
        String username = getText(usernameEditText);
        String password = getText(passwordEditText);

        // Stop early if the fields are empty
        if (!isValidInput(username, password)) {
            Toast.makeText(this, "Enter a username and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check username and password in SQLite
        boolean isValid = dbHelper.checkUserCredentials(username, password);
        if (!isValid) {
            Toast.makeText(this, "Invalid login.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Login passed, open the inventory screen
        Intent intent = new Intent(this, InventoryActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);

        // Prevents backing into the login screen after sign in
        finish();
    }

    private void openCreateAccount() {

        // This opens the registration screen for a new user
        Intent intent = new Intent(this, CreateAccountActivity.class);
        startActivity(intent);
    }

    // Simple input check before hitting the database
    private boolean isValidInput(String username, String password) {
        return !username.isEmpty() && !password.isEmpty();
    }

    // Helper so input reads are consistent everywhere
    private String getText(EditText field) {
        return field.getText().toString().trim();
    }
}
