package com.example.cs360_projecttwo_inventoryapp_dustindavis;

import android.content.Intent;
import android.content.SharedPreferences;
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

    // Database helper used for account and login checks
    private DatabaseHelper dbHelper;

    // SharedPreferences keeps the active user available after login.
    public static final String USER_SESSION_PREFS = "user_session";
    public static final String KEY_LOGGED_IN_USER_ID = "logged_in_user_id";
    public static final String KEY_LOGGED_IN_USERNAME = "logged_in_username";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connect the login screen fields and buttons to the code.
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        createAccountButton = findViewById(R.id.createAccountButton);

        // This helper reads the saved user records from SQLite.
        dbHelper = new DatabaseHelper(this);

        loginButton.setOnClickListener(v -> attemptLogin());
        createAccountButton.setOnClickListener(v -> openCreateAccount());
    }

    private void attemptLogin() {
        String username = getText(usernameEditText);
        String password = getText(passwordEditText);

        // Stop before querying the database when either field is blank.
        if (!isValidInput(username, password)) {
            Toast.makeText(
                    this,
                    "Enter a username and password.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Return the user ID and saved username in one database query.
        DatabaseHelper.UserSession userSession =
                dbHelper.getUserSession(username, password);

        if (userSession == null) {
            Toast.makeText(
                    this,
                    "Invalid login.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Save the active user so inventory changes can record who made them.
        saveUserSession(userSession);

        // Open the inventory screen after the account is confirmed.
        Intent intent = new Intent(
                this,
                InventoryActivity.class
        );

        // Keep these extras available for screens that may need them right away.
        intent.putExtra(
                KEY_LOGGED_IN_USER_ID,
                userSession.userId
        );

        intent.putExtra(
                KEY_LOGGED_IN_USERNAME,
                userSession.username
        );

        startActivity(intent);

        // Prevent the Back button from returning to the signed-in login screen.
        finish();
    }

    private void saveUserSession(
            DatabaseHelper.UserSession userSession
    ) {
        SharedPreferences preferences = getSharedPreferences(
                USER_SESSION_PREFS,
                MODE_PRIVATE
        );

        // Store both values because the ID is used for database history,
        // while the username is better for messages shown to the user.
        preferences.edit()
                .putInt(
                        KEY_LOGGED_IN_USER_ID,
                        userSession.userId
                )
                .putString(
                        KEY_LOGGED_IN_USERNAME,
                        userSession.username
                )
                .apply();
    }

    private void openCreateAccount() {
        // Open the registration screen for a new user.
        Intent intent = new Intent(
                this,
                CreateAccountActivity.class
        );

        startActivity(intent);
    }

    private boolean isValidInput(
            String username,
            String password
    ) {
        return !username.isEmpty()
                && !password.isEmpty();
    }

    private String getText(EditText field) {
        // Trim the value so accidental spaces do not break login.
        return field.getText()
                .toString()
                .trim();
    }
}