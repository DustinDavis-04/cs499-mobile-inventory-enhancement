package com.example.cs360_projecttwo_inventoryapp_dustindavis;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SmsActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_REQUEST_CODE = 1001;

    // SharedPreferences used only for SMSMA alert state
    private static final String SMS_PREFS = "sms_alerts";
    private static final String KEY_HAS_UNREAD_ALERT = "has_unread_alert";
    private static final String KEY_LAST_ALERT_TEXT = "last_alert_text";
    private static final String KEY_SMS_PERMISSION_DECIDED = "sms_permission_decided";

    // Prefix used to detect and open item detail links inside chat messages
    private static final String DETAILS_LINK_PREFIX = "https://warehouse.app/item/";

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private Button sendButton;
    private Button requestPermissionButton;
    private TextView smsStatusText;

    // Simple in app message list, not real SMS history
    private final List<String> messages = new ArrayList<>();
    private ChatAdapter chatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        // Opening this screen counts as reading the alert
        clearUnreadSmsAlertFlag();

        // View references
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        requestPermissionButton = findViewById(R.id.requestPermissionButton);
        smsStatusText = findViewById(R.id.smsStatusText);

        // RecyclerView setup for chat messages
        chatAdapter = new ChatAdapter(messages, this);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Starter system message so the screen is never empty
        messages.add("System: SMS alerts will appear here.");
        chatAdapter.notifyItemInserted(messages.size() - 1);

        // Show last low inventory alert if one exists
        addLastLowInventoryAlertIfAny();

        // Set initial permission UI state
        updateSmsStatusText();
        updatePermissionUi();

        requestPermissionButton.setOnClickListener(v -> {

            // Permission already granted, mark as decided and hide prompt
            if (hasSmsPermission()) {
                Toast.makeText(this, "SMS permission is already granted.", Toast.LENGTH_SHORT).show();
                setPermissionDecided(true);
                updateSmsStatusText();
                updatePermissionUi();
                return;
            }

            // Request SMS permission from the system
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST_CODE
            );
        });

        sendButton.setOnClickListener(v -> {
            String text = messageInput.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                return;
            }

            // Without permission, messages stay inside the app
            if (!hasSmsPermission()) {
                Toast.makeText(
                        this,
                        "Cannot send SMS without permission. Messages will stay inside the app.",
                        Toast.LENGTH_LONG
                ).show();
                messages.add("You (app only): " + text);
            } else {
                // With permission, send SMS and log it in the chat
                messages.add("You (sent by SMS): " + text);
                sendSmsAlert(text);
            }

            chatAdapter.notifyItemInserted(messages.size() - 1);
            chatRecyclerView.scrollToPosition(messages.size() - 1);
            messageInput.setText("");
        });

        // Bottom nav back button
        LinearLayout backContainer = findViewById(R.id.backContainer);
        backContainer.setOnClickListener(v -> finish());
    }

    // Clears the unread alert flag used by the Inventory screen badge
    private void clearUnreadSmsAlertFlag() {
        SharedPreferences prefs = getSharedPreferences(SMS_PREFS, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_HAS_UNREAD_ALERT, false).apply();
    }

    // Adds the last saved low inventory alert to the chat, if present
    private void addLastLowInventoryAlertIfAny() {
        SharedPreferences prefs = getSharedPreferences(SMS_PREFS, MODE_PRIVATE);
        String alertText = prefs.getString(KEY_LAST_ALERT_TEXT, "");

        if (alertText != null && !alertText.trim().isEmpty()) {
            messages.add(alertText);
            chatAdapter.notifyItemInserted(messages.size() - 1);
            chatRecyclerView.scrollToPosition(messages.size() - 1);
        }
    }

    // Checks whether SEND_SMS permission is granted
    private boolean hasSmsPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    // Tracks whether the user has already answered the permission prompt
    private boolean hasPermissionDecision() {
        SharedPreferences prefs = getSharedPreferences(SMS_PREFS, MODE_PRIVATE);
        return prefs.getBoolean(KEY_SMS_PERMISSION_DECIDED, false);
    }

    // Stores that the permission prompt has been answered
    private void setPermissionDecided(boolean decided) {
        SharedPreferences prefs = getSharedPreferences(SMS_PREFS, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SMS_PERMISSION_DECIDED, decided).apply();
    }

    // Updates the permission status message shown at the top of the screen
    private void updateSmsStatusText() {

        // Once a decision is made, the status text is no longer needed
        if (hasPermissionDecision()) {
            smsStatusText.setText("");
            return;
        }

        if (hasSmsPermission()) {
            smsStatusText.setText(
                    "SMS permission is granted. Low inventory alerts can be sent by text."
            );
        } else {
            smsStatusText.setText(
                    "If you want real text alerts, tap Request SMS permission. If not, alerts stay inside the app."
            );
        }
    }

    // Shows or hides the permission prompt section
    private void updatePermissionUi() {

        boolean decided = hasPermissionDecision();

        if (decided) {
            smsStatusText.setVisibility(View.GONE);
            requestPermissionButton.setVisibility(View.GONE);
        } else {
            smsStatusText.setVisibility(View.VISIBLE);
            requestPermissionButton.setVisibility(View.VISIBLE);
        }
    }

    // Sends a basic SMS message using a placeholder phone number
    private void sendSmsAlert(String body) {
        try {
            SmsManager smsManager = SmsManager.getDefault();

            // Placeholder number for the project
            String phoneNumber = "5551234567";

            smsManager.sendTextMessage(phoneNumber, null, body, null, null);
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "This device may not be able to send SMS messages.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != SMS_PERMISSION_REQUEST_CODE) {
            return;
        }

        // User has now answered the permission prompt
        setPermissionDecided(true);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            messages.add("System: SMS permission granted. Future alerts will be sent by text.");
        } else {
            messages.add("System: SMS permission denied. Alerts will appear in the app only.");
        }

        updateSmsStatusText();
        updatePermissionUi();

        chatAdapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.scrollToPosition(messages.size() - 1);
    }

    // Adapter for displaying chat messages
    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

        private final List<String> messages;
        private final AppCompatActivity activity;

        ChatAdapter(List<String> messages, AppCompatActivity activity) {
            this.messages = messages;
            this.activity = activity;
        }

        @Override
        public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ChatViewHolder holder, int position) {
            String message = messages.get(position);

            // Detect item detail links and make them tappable
            int linkStart = message.indexOf(DETAILS_LINK_PREFIX);
            if (linkStart >= 0) {
                SpannableString spannable = new SpannableString(message);

                int linkEnd = findLinkEnd(message, linkStart);
                ClickableSpan span = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        int itemId = parseItemIdFromLink(message, linkStart);

                        if (itemId <= 0) {
                            Toast.makeText(activity, "Could not open item details.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Intent intent = new Intent(activity, DetailedInfoActivity.class);
                        intent.putExtra(DetailedInfoActivity.EXTRA_MODE, DetailedInfoActivity.MODE_VIEW);
                        intent.putExtra(DetailedInfoActivity.EXTRA_ITEM_ID, itemId);
                        activity.startActivity(intent);
                    }
                };

                spannable.setSpan(span, linkStart, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                holder.messageTextView.setText(spannable);
                holder.messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
                holder.messageTextView.setLinksClickable(true);
            } else {
                holder.messageTextView.setText(message);
                holder.messageTextView.setMovementMethod(null);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        // Finds the end of a URL inside a message
        private static int findLinkEnd(String text, int startIndex) {
            int space = text.indexOf(' ', startIndex);
            int newline = text.indexOf('\n', startIndex);

            int end = text.length();
            if (space >= 0) {
                end = Math.min(end, space);
            }
            if (newline >= 0) {
                end = Math.min(end, newline);
            }
            return end;
        }

        // Extracts the item ID from the details link
        private static int parseItemIdFromLink(String fullMessage, int linkStart) {
            int linkEnd = findLinkEnd(fullMessage, linkStart);
            String link = fullMessage.substring(linkStart, linkEnd);

            String idPart = link.replace(DETAILS_LINK_PREFIX, "").trim();

            try {
                return Integer.parseInt(idPart);
            } catch (Exception ex) {
                return -1;
            }
        }

        static class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView messageTextView;

            ChatViewHolder(View itemView) {
                super(itemView);
                messageTextView = itemView.findViewById(R.id.messageTextView);
            }
        }
    }
}
