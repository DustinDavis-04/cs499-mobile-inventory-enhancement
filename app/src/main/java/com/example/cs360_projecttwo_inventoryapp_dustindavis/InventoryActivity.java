package com.example.cs360_projecttwo_inventoryapp_dustindavis;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class InventoryActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    private RecyclerView inventoryRecyclerView;
    private InventoryAdapter adapter;

    // Backing list for the inventory RecyclerView
    private ArrayList<DatabaseHelper.InventoryItem> items;

    // SharedPreferences used for the SMS unread badge and last alert text
    private static final String SMS_PREFS = "sms_alerts";
    private static final String KEY_HAS_UNREAD_ALERT = "has_unread_alert";
    private static final String KEY_LAST_ALERT_TEXT = "last_alert_text";

    // Link format used in alerts, SmsActivity looks for this prefix
    private static final String DETAILS_LINK_PREFIX = "https://warehouse.app/item/";

    private View smsNotificationDot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        dbHelper = new DatabaseHelper(this);

        // Bottom nav back button
        LinearLayout backContainer = findViewById(R.id.backContainer);
        backContainer.setOnClickListener(v -> finish());

        // Bottom nav SMS button
        ImageButton smsButton = findViewById(R.id.navSmsButton);
        smsButton.setOnClickListener(v -> {
            Intent intent = new Intent(InventoryActivity.this, SmsActivity.class);
            startActivity(intent);
        });

        // Red dot shown when a low stock alert is unread
        smsNotificationDot = findViewById(R.id.smsNotificationDot);

        // RecyclerView setup for the inventory list
        inventoryRecyclerView = findViewById(R.id.inventoryRecyclerView);
        inventoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        items = new ArrayList<>();
        adapter = new InventoryAdapter(items);
        inventoryRecyclerView.setAdapter(adapter);

        // Add item opens DetailedInfoActivity in NEW mode
        Button addItemButton = findViewById(R.id.addItemButton);
        addItemButton.setOnClickListener(v -> {
            Intent intent = new Intent(InventoryActivity.this, DetailedInfoActivity.class);
            intent.putExtra(DetailedInfoActivity.EXTRA_MODE, DetailedInfoActivity.MODE_NEW);
            startActivity(intent);
        });

        // Load inventory data from SQLite
        refreshList();

        // Seed a couple rows so the screen is not empty on first launch
        seedIfEmpty();

        // Sync the unread badge state on startup
        updateSmsUnreadBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload list after returning from details or SMS screens
        refreshList();

        // Badge can change while this screen is not visible
        updateSmsUnreadBadge();
    }

    // Reloads the inventory list from the database
    private void refreshList() {
        items.clear();
        items.addAll(dbHelper.getAllInventoryItems());
        adapter.notifyDataSetChanged();
    }

    // Adds starter items the first time so the UI is easy to test
    private void seedIfEmpty() {
        if (!items.isEmpty()) {
            return;
        }

        dbHelper.createInventoryItem(
                "Pallet Jack",
                4,
                2,
                "Crown Equipment",
                "SN-112233",
                "SCU-445566",
                "Bay A1",
                "Heavy duty pallet jack."
        );

        dbHelper.createInventoryItem(
                "Shipping Labels",
                250,
                100,
                "Zebra Technologies",
                "SN-778899",
                "SCU-001122",
                "Packing Station 3",
                "Standard outgoing labels."
        );

        refreshList();
    }

    // Shows or hides the unread dot on the SMS icon
    private void updateSmsUnreadBadge() {
        SharedPreferences prefs = getSharedPreferences(SMS_PREFS, MODE_PRIVATE);
        boolean hasUnread = prefs.getBoolean(KEY_HAS_UNREAD_ALERT, false);

        if (smsNotificationDot != null) {
            smsNotificationDot.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
        }
    }

    // Stores the last alert text and marks it unread so the badge can show
    private void setUnreadSmsAlert(String alertText) {
        SharedPreferences prefs = getSharedPreferences(SMS_PREFS, MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_HAS_UNREAD_ALERT, true)
                .putString(KEY_LAST_ALERT_TEXT, alertText)
                .apply();

        updateSmsUnreadBadge();
    }

    // Sends a low inventory SMS if permission is granted
    private void trySendLowInventorySms(String messageBody) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();

            // Placeholder number used for the project
            String phoneNumber = "5551234567";

            smsManager.sendTextMessage(phoneNumber, null, messageBody, null, null);
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "This device may not be able to send SMS messages.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // Builds the alert text stored in prefs and shown in the SMS screen
    private String buildLowInventoryAlertText(DatabaseHelper.InventoryItem item) {

        // This link is used for tap to open details
        String detailsLink = DETAILS_LINK_PREFIX + item.id;

        return "Alert: Item (" + item.name + ") is low, consider reorder. Details: " + detailsLink;
    }

    private class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.Holder> {

        private final ArrayList<DatabaseHelper.InventoryItem> data;

        InventoryAdapter(ArrayList<DatabaseHelper.InventoryItem> data) {
            this.data = data;
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_inventory_row, parent, false);
            return new Holder(row);
        }

        @Override
        public void onBindViewHolder(Holder h, int position) {
            DatabaseHelper.InventoryItem item = data.get(position);

            // Basic row values
            h.itemNameText.setText(item.name);
            h.qtyText.setText(String.valueOf(item.quantity));

            // Low stock icon shows when quantity is at or below reorder amount
            boolean isLow = item.quantity <= item.lowThreshold;
            h.lowStockIcon.setVisibility(isLow ? View.VISIBLE : View.GONE);

            // Info button opens details in VIEW mode
            h.infoButton.setOnClickListener(v -> {
                Intent intent = new Intent(InventoryActivity.this, DetailedInfoActivity.class);
                intent.putExtra(DetailedInfoActivity.EXTRA_MODE, DetailedInfoActivity.MODE_VIEW);
                intent.putExtra(DetailedInfoActivity.EXTRA_ITEM_ID, item.id);
                startActivity(intent);
            });

            // Delete button removes the item after a confirmation prompt
            h.deleteButton.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(InventoryActivity.this)
                        .setTitle("Remove item")
                        .setMessage("Are you sure you want to remove " + item.name + " from inventory?")
                        .setPositiveButton("Remove", (dialog, which) -> {

                            boolean deleted = dbHelper.deleteInventoryItem(item.id);
                            if (deleted) {
                                Toast.makeText(InventoryActivity.this, "Item removed.", Toast.LENGTH_SHORT).show();
                                refreshList();
                            } else {
                                Toast.makeText(InventoryActivity.this, "Could not remove item.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
            });

            // Minus button decreases quantity, stops at 0
            h.qtyMinusButton.setOnClickListener(v -> {

                // Small tap feedback so the button feels responsive
                v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);

                int oldQty = item.quantity;
                int newQty = Math.max(0, item.quantity - 1);

                if (dbHelper.updateInventoryQuantity(item.id, newQty)) {

                    // Fire alert only when crossing into low stock
                    if (oldQty > item.lowThreshold && newQty <= item.lowThreshold) {
                        String alertText = buildLowInventoryAlertText(item);

                        setUnreadSmsAlert(alertText);
                        trySendLowInventorySms(alertText);
                    }

                    refreshList();
                }
            });

            // Plus button increases quantity by 1
            h.qtyPlusButton.setOnClickListener(v -> {

                v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);

                int newQty = item.quantity + 1;
                if (dbHelper.updateInventoryQuantity(item.id, newQty)) {
                    refreshList();
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class Holder extends RecyclerView.ViewHolder {

            ImageView lowStockIcon;
            TextView itemNameText;
            TextView qtyText;

            Button qtyMinusButton;
            Button qtyPlusButton;

            Button infoButton;
            ImageButton deleteButton;

            Holder(View itemView) {
                super(itemView);

                lowStockIcon = itemView.findViewById(R.id.lowStockIcon);
                itemNameText = itemView.findViewById(R.id.itemNameText);
                qtyText = itemView.findViewById(R.id.qtyText);

                qtyMinusButton = itemView.findViewById(R.id.qtyMinusButton);
                qtyPlusButton = itemView.findViewById(R.id.qtyPlusButton);

                infoButton = itemView.findViewById(R.id.infoButton);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
        }
    }
}
