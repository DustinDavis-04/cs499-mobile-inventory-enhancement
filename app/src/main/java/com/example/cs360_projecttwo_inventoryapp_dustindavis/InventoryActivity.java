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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class InventoryActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    private RecyclerView inventoryRecyclerView;
    private InventoryAdapter adapter;

    // Backing list for the active inventory records shown in the RecyclerView.
    private ArrayList<DatabaseHelper.InventoryItem> items;

    // SharedPreferences hold the unread SMS badge and latest alert text.
    private static final String SMS_PREFS = "sms_alerts";
    private static final String KEY_HAS_UNREAD_ALERT = "has_unread_alert";
    private static final String KEY_LAST_ALERT_TEXT = "last_alert_text";

    // SmsActivity recognizes this prefix and opens the matching item details.
    private static final String DETAILS_LINK_PREFIX =
            "https://warehouse.app/item/";

    private View smsNotificationDot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        dbHelper = new DatabaseHelper(this);

        // Bottom navigation back button.
        LinearLayout backContainer = findViewById(R.id.backContainer);
        backContainer.setOnClickListener(v -> finish());

        // Bottom navigation SMS button.
        ImageButton smsButton = findViewById(R.id.navSmsButton);
        smsButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    InventoryActivity.this,
                    SmsActivity.class
            );
            startActivity(intent);
        });

        // Red dot shown when a low-inventory alert has not been read.
        smsNotificationDot = findViewById(R.id.smsNotificationDot);

        // Set up the RecyclerView that displays the active inventory records.
        inventoryRecyclerView = findViewById(R.id.inventoryRecyclerView);
        inventoryRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        items = new ArrayList<>();
        adapter = new InventoryAdapter(items);
        inventoryRecyclerView.setAdapter(adapter);

        // Add Item opens the normalized details form in NEW mode.
        Button addItemButton = findViewById(R.id.addItemButton);
        addItemButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    InventoryActivity.this,
                    DetailedInfoActivity.class
            );

            intent.putExtra(
                    DetailedInfoActivity.EXTRA_MODE,
                    DetailedInfoActivity.MODE_NEW
            );

            startActivity(intent);
        });

        // Add starter records before the first list refresh.
        seedIfEmpty();

        // Load the active inventory records from the normalized database.
        refreshList();

        // Match the unread badge with the saved alert state.
        updateSmsUnreadBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload after returning from the details or SMS screen.
        refreshList();

        // The unread state may have changed while this screen was hidden.
        updateSmsUnreadBadge();
    }

    private void refreshList() {
        items.clear();

        // The normalized query joins manufacturers, categories, and locations.
        items.addAll(dbHelper.getAllNormalizedInventoryItems());

        adapter.notifyDataSetChanged();
    }

    private void seedIfEmpty() {
        // Only seed when the normalized inventory table has no active records.
        ArrayList<DatabaseHelper.InventoryItem> existingItems =
                dbHelper.getAllNormalizedInventoryItems();

        if (!existingItems.isEmpty()) {
            return;
        }

        // These records use the same normalized insert path as the details form.
        dbHelper.createNormalizedInventoryItem(
                "Pallet Jack",
                "Crown Equipment",
                "Material Handling",
                "PTH-50",
                "SN-112233",
                "SCU-445566",
                4,
                2,
                1,
                1,
                "Heavy duty pallet jack."
        );

        dbHelper.createNormalizedInventoryItem(
                "Shipping Labels",
                "Zebra Technologies",
                "Shipping Supplies",
                "Z-Perform 1000D",
                "SN-778899",
                "SCU-001122",
                250,
                100,
                1,
                2,
                "Standard outgoing labels."
        );
    }

    private void updateSmsUnreadBadge() {
        SharedPreferences preferences =
                getSharedPreferences(SMS_PREFS, MODE_PRIVATE);

        boolean hasUnread = preferences.getBoolean(
                KEY_HAS_UNREAD_ALERT,
                false
        );

        if (smsNotificationDot != null) {
            smsNotificationDot.setVisibility(
                    hasUnread ? View.VISIBLE : View.GONE
            );
        }
    }

    private void setUnreadSmsAlert(String alertText) {
        SharedPreferences preferences =
                getSharedPreferences(SMS_PREFS, MODE_PRIVATE);

        preferences.edit()
                .putBoolean(KEY_HAS_UNREAD_ALERT, true)
                .putString(KEY_LAST_ALERT_TEXT, alertText)
                .apply();

        updateSmsUnreadBadge();
    }

    private void trySendLowInventorySms(String messageBody) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
        ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();

            // This placeholder number is used only for the class project.
            String phoneNumber = "5551234567";

            smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    messageBody,
                    null,
                    null
            );
        } catch (Exception exception) {
            Toast.makeText(
                    this,
                    "This device may not be able to send SMS messages.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private String buildLowInventoryAlertText(
            DatabaseHelper.InventoryItem item
    ) {
        // The item ID in this link lets SmsActivity open the details screen.
        String detailsLink = DETAILS_LINK_PREFIX + item.id;

        return "Alert: Item (" + item.name +
                ") is low, consider reorder. Details: " +
                detailsLink;
    }

    private class InventoryAdapter
            extends RecyclerView.Adapter<InventoryAdapter.Holder> {

        private final ArrayList<DatabaseHelper.InventoryItem> data;

        InventoryAdapter(
                ArrayList<DatabaseHelper.InventoryItem> data
        ) {
            this.data = data;
        }

        @Override
        public Holder onCreateViewHolder(
                ViewGroup parent,
                int viewType
        ) {
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(
                            R.layout.item_inventory_row,
                            parent,
                            false
                    );

            return new Holder(row);
        }

        @Override
        public void onBindViewHolder(
                Holder holder,
                int position
        ) {
            DatabaseHelper.InventoryItem item = data.get(position);

            holder.itemNameText.setText(item.name);
            holder.qtyText.setText(
                    String.valueOf(item.quantity)
            );

            // Show the warning icon when the quantity reaches its threshold.
            boolean isLow =
                    item.quantity <= item.lowThreshold;

            holder.lowStockIcon.setVisibility(
                    isLow ? View.VISIBLE : View.GONE
            );

            // Open the normalized item details in VIEW mode.
            holder.infoButton.setOnClickListener(v -> {
                Intent intent = new Intent(
                        InventoryActivity.this,
                        DetailedInfoActivity.class
                );

                intent.putExtra(
                        DetailedInfoActivity.EXTRA_MODE,
                        DetailedInfoActivity.MODE_VIEW
                );

                intent.putExtra(
                        DetailedInfoActivity.EXTRA_ITEM_ID,
                        item.id
                );

                startActivity(intent);
            });

            // Deactivate the item instead of permanently deleting its record.
            holder.deleteButton.setOnClickListener(v ->
                    showDeactivateConfirmation(item)
            );

            // Decrease the quantity by one without allowing a negative value.
            holder.qtyMinusButton.setOnClickListener(v -> {
                v.performHapticFeedback(
                        android.view.HapticFeedbackConstants.KEYBOARD_TAP
                );

                int oldQuantity = item.quantity;
                int newQuantity = Math.max(
                        0,
                        oldQuantity - 1
                );

                boolean updated =
                        dbHelper.updateNormalizedInventoryQuantity(
                                item.id,
                                newQuantity
                        );

                if (!updated) {
                    Toast.makeText(
                            InventoryActivity.this,
                            "Could not update quantity.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                // Only create an alert when the quantity crosses the threshold.
                if (oldQuantity > item.lowThreshold
                        && newQuantity <= item.lowThreshold) {

                    // Use the new quantity in the local object for this alert.
                    item.quantity = newQuantity;

                    String alertText =
                            buildLowInventoryAlertText(item);

                    setUnreadSmsAlert(alertText);
                    trySendLowInventorySms(alertText);
                }

                refreshList();
            });

            // Increase the quantity by one through the normalized table.
            holder.qtyPlusButton.setOnClickListener(v -> {
                v.performHapticFeedback(
                        android.view.HapticFeedbackConstants.KEYBOARD_TAP
                );

                int newQuantity = item.quantity + 1;

                boolean updated =
                        dbHelper.updateNormalizedInventoryQuantity(
                                item.id,
                                newQuantity
                        );

                if (updated) {
                    refreshList();
                } else {
                    Toast.makeText(
                            InventoryActivity.this,
                            "Could not update quantity.",
                            Toast.LENGTH_SHORT
                    ).show();
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

                lowStockIcon =
                        itemView.findViewById(R.id.lowStockIcon);

                itemNameText =
                        itemView.findViewById(R.id.itemNameText);

                qtyText =
                        itemView.findViewById(R.id.qtyText);

                qtyMinusButton =
                        itemView.findViewById(R.id.qtyMinusButton);

                qtyPlusButton =
                        itemView.findViewById(R.id.qtyPlusButton);

                infoButton =
                        itemView.findViewById(R.id.infoButton);

                deleteButton =
                        itemView.findViewById(R.id.deleteButton);
            }
        }
    }

    private void showDeactivateConfirmation(
            DatabaseHelper.InventoryItem item
    ) {
        new AlertDialog.Builder(this)
                .setTitle("Remove item")
                .setMessage(
                        "Are you sure you want to remove " +
                                item.name +
                                " from active inventory?"
                )
                .setPositiveButton(
                        "Remove",
                        (dialog, which) -> {
                            boolean deactivated =
                                    dbHelper
                                            .deactivateNormalizedInventoryItem(
                                                    item.id
                                            );

                            if (deactivated) {
                                Toast.makeText(
                                        this,
                                        "Item removed from active inventory.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                refreshList();
                            } else {
                                Toast.makeText(
                                        this,
                                        "Could not remove item.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .setNegativeButton(
                        "Cancel",
                        (dialog, which) -> dialog.dismiss()
                )
                .show();
    }
}