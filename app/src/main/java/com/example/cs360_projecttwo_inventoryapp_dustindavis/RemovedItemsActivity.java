package com.example.cs360_projecttwo_inventoryapp_dustindavis;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RemovedItemsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    private RecyclerView removedItemsRecyclerView;
    private TextView emptyRemovedItemsText;

    // Holds the inactive inventory records shown on this screen.
    private ArrayList<DatabaseHelper.InventoryItem> removedItems;

    private RemovedItemsAdapter adapter;

    // The signed-in user ID is saved with each restore history record.
    private int loggedInUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_removed_items);

        dbHelper = new DatabaseHelper(this);

        // Load the current user before allowing an item to be restored.
        loadUserSession();

        if (loggedInUserId <= 0) {
            Toast.makeText(
                    this,
                    "Your login session could not be loaded.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        // Connect the removed items list and empty message to the layout.
        removedItemsRecyclerView = findViewById(
                R.id.removedItemsRecyclerView
        );

        emptyRemovedItemsText = findViewById(
                R.id.emptyRemovedItemsText
        );

        removedItemsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        removedItems = new ArrayList<>();
        adapter = new RemovedItemsAdapter(removedItems);

        removedItemsRecyclerView.setAdapter(adapter);

        // Return to the active warehouse inventory screen.
        LinearLayout backContainer = findViewById(
                R.id.backContainer
        );

        backContainer.setOnClickListener(v -> finish());

        loadRemovedItems();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload the list in case an item changed while this screen was hidden.
        if (adapter != null) {
            loadRemovedItems();
        }
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
    }

    private void loadRemovedItems() {
        removedItems.clear();

        // Only load records that have been marked inactive.
        removedItems.addAll(
                dbHelper.getInactiveNormalizedInventoryItems()
        );

        adapter.notifyDataSetChanged();

        boolean hasRemovedItems =
                !removedItems.isEmpty();

        removedItemsRecyclerView.setVisibility(
                hasRemovedItems
                        ? View.VISIBLE
                        : View.GONE
        );

        emptyRemovedItemsText.setVisibility(
                hasRemovedItems
                        ? View.GONE
                        : View.VISIBLE
        );
    }

    private class RemovedItemsAdapter
            extends RecyclerView.Adapter<RemovedItemsAdapter.Holder> {

        private final ArrayList<DatabaseHelper.InventoryItem> data;

        RemovedItemsAdapter(
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
                    R.layout.item_removed_inventory_row,
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

            holder.qtyText.setText(
                    String.valueOf(item.quantity)
            );

            // Restore the item and record which signed-in user did it.
            holder.restoreButton.setOnClickListener(v -> {
                boolean restored =
                        dbHelper.reactivateNormalizedInventoryItem(
                                item.id,
                                loggedInUserId
                        );

                if (restored) {
                    Toast.makeText(
                            RemovedItemsActivity.this,
                            "Item returned to active inventory.",
                            Toast.LENGTH_SHORT
                    ).show();

                    loadRemovedItems();
                } else {
                    Toast.makeText(
                            RemovedItemsActivity.this,
                            "Could not restore item.",
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

            TextView itemNameText;
            TextView qtyText;
            Button restoreButton;

            Holder(View itemView) {
                super(itemView);

                itemNameText = itemView.findViewById(
                        R.id.itemNameText
                );

                qtyText = itemView.findViewById(
                        R.id.qtyText
                );

                restoreButton = itemView.findViewById(
                        R.id.restoreButton
                );
            }
        }
    }
}