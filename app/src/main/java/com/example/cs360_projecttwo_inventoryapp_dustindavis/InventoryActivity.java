package com.example.cs360_projecttwo_inventoryapp_dustindavis;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class InventoryActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    private RecyclerView inventoryRecyclerView;
    private InventoryAdapter adapter;

    // Backing list for the active inventory records shown in the RecyclerView.
    private ArrayList<DatabaseHelper.InventoryItem> items;

    // These values identify the user who is currently signed in.
    private int loggedInUserId;
    private String loggedInUsername;

    // SharedPreferences hold the unread SMS badge and latest alert text.
    private static final String SMS_PREFS = "sms_alerts";
    private static final String KEY_HAS_UNREAD_ALERT = "has_unread_alert";

    private View smsNotificationDot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        dbHelper = new DatabaseHelper(this);

        // Load the active user before allowing access to inventory features.
        loadUserSession();

        // Return to login if the saved session cannot identify the user.
        if (loggedInUserId <= 0 || loggedInUsername.isEmpty()) {
            Toast.makeText(
                    this,
                    "Your login session could not be loaded.",
                    Toast.LENGTH_LONG
            ).show();

            returnToLogin();
            return;
        }

        // Bottom navigation back button.
        LinearLayout backContainer = findViewById(
                R.id.backContainer
        );

        backContainer.setOnClickListener(v -> finish());

        // Open the inventory transaction history screen.
        LinearLayout historyContainer = findViewById(
                R.id.historyContainer
        );

        historyContainer.setOnClickListener(v -> {
            Intent intent = new Intent(
                    InventoryActivity.this,
                    HistoryActivity.class
            );

            startActivity(intent);
        });

        // Open the in-app SMS alert screen.
        ImageButton smsButton = findViewById(
                R.id.navSmsButton
        );

        smsButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    InventoryActivity.this,
                    SmsActivity.class
            );

            startActivity(intent);
        });

        // Red dot shown when a low-inventory alert has not been read.
        smsNotificationDot = findViewById(
                R.id.smsNotificationDot
        );

        // Set up the RecyclerView that displays active inventory records.
        inventoryRecyclerView = findViewById(
                R.id.inventoryRecyclerView
        );

        inventoryRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        items = new ArrayList<>();
        adapter = new InventoryAdapter(items);

        inventoryRecyclerView.setAdapter(adapter);

        // Open the list of inactive inventory records.
        Button removedItemsButton = findViewById(
                R.id.removedItemsButton
        );

        removedItemsButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    InventoryActivity.this,
                    RemovedItemsActivity.class
            );

            startActivity(intent);
        });

        // Add Item opens the details form in NEW mode.
        Button addItemButton = findViewById(
                R.id.addItemButton
        );

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

        // Match the unread badge with the saved SMS alert state.
        updateSmsUnreadBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // These objects are not ready if onCreate returned after a session failure.
        if (adapter == null || items == null) {
            return;
        }

        // Reload after returning from the details, history, or SMS screen.
        refreshList();

        // The unread alert state may have changed while this screen was hidden.
        updateSmsUnreadBadge();
    }

    private void loadUserSession() {
        SharedPreferences preferences = getSharedPreferences(
                MainActivity.USER_SESSION_PREFS,
                MODE_PRIVATE
        );

        loggedInUserId = preferences.getInt(
                MainActivity.KEY_LOGGED_IN_USER_ID,
                -1
        );

        String savedUsername = preferences.getString(
                MainActivity.KEY_LOGGED_IN_USERNAME,
                ""
        );

        loggedInUsername =
                savedUsername == null
                        ? ""
                        : savedUsername.trim();
    }

    private void returnToLogin() {
        // Clear the current task so inventory cannot be used without a valid session.
        Intent intent = new Intent(
                this,
                MainActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void refreshList() {
        items.clear();

        // The normalized query joins manufacturers, categories, and locations.
        items.addAll(
                dbHelper.getAllNormalizedInventoryItems()
        );

        adapter.notifyDataSetChanged();
    }

    private void seedIfEmpty() {
        // Only seed when the normalized inventory table has no active records.
        ArrayList<DatabaseHelper.InventoryItem> existingItems =
                dbHelper.getAllNormalizedInventoryItems();

        if (!existingItems.isEmpty()) {
            return;
        }

        // These starter records use the same normalized insert path as the form.
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
        SharedPreferences preferences = getSharedPreferences(
                SMS_PREFS,
                MODE_PRIVATE
        );

        boolean hasUnread = preferences.getBoolean(
                KEY_HAS_UNREAD_ALERT,
                false
        );

        if (smsNotificationDot != null) {
            smsNotificationDot.setVisibility(
                    hasUnread
                            ? View.VISIBLE
                            : View.GONE
            );
        }
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
            View row = LayoutInflater.from(
                    parent.getContext()
            ).inflate(
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
            DatabaseHelper.InventoryItem item =
                    data.get(position);

            holder.itemNameText.setText(
                    item.name
            );

            // Quantity is displayed here but is now edited from the details screen.
            holder.qtyText.setText(
                    String.valueOf(item.quantity)
            );

            // Show the warning icon when quantity reaches its reorder threshold.
            boolean isLow =
                    item.quantity <= item.lowThreshold;

            holder.lowStockIcon.setVisibility(
                    isLow
                            ? View.VISIBLE
                            : View.GONE
            );

            // Open the item details screen for viewing or editing.
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

            // Remove the item from active inventory after confirmation.
            holder.deleteButton.setOnClickListener(v ->
                    showDeactivateConfirmation(item)
            );
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class Holder extends RecyclerView.ViewHolder {

            ImageView lowStockIcon;
            TextView itemNameText;
            TextView qtyText;

            Button infoButton;
            ImageButton deleteButton;

            Holder(View itemView) {
                super(itemView);

                lowStockIcon = itemView.findViewById(
                        R.id.lowStockIcon
                );

                itemNameText = itemView.findViewById(
                        R.id.itemNameText
                );

                qtyText = itemView.findViewById(
                        R.id.qtyText
                );

                infoButton = itemView.findViewById(
                        R.id.infoButton
                );

                deleteButton = itemView.findViewById(
                        R.id.deleteButton
                );
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
                            // The item stays in the database but no longer
                            // appears in the active inventory list.
                            boolean deactivated =
                                    dbHelper.deactivateNormalizedInventoryItem(
                                            item.id,
                                            loggedInUserId
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
                        (dialog, which) ->
                                dialog.dismiss()
                )
                .show();
    }
}