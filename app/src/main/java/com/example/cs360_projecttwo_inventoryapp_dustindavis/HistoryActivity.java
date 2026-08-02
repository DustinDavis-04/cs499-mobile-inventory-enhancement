package com.example.cs360_projecttwo_inventoryapp_dustindavis;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class HistoryActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    private RecyclerView historyRecyclerView;
    private TextView emptyHistoryText;

    private ArrayList<DatabaseHelper.InventoryTransaction> transactions;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new DatabaseHelper(this);

        // Connect the history list and empty message to the layout.
        historyRecyclerView = findViewById(
                R.id.historyRecyclerView
        );

        emptyHistoryText = findViewById(
                R.id.emptyHistoryText
        );

        historyRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        transactions = new ArrayList<>();
        adapter = new HistoryAdapter(transactions);

        historyRecyclerView.setAdapter(adapter);

        // Return to the inventory screen.
        LinearLayout backContainer = findViewById(
                R.id.backContainer
        );

        backContainer.setOnClickListener(v -> finish());

        loadHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload in case another inventory action was recorded.
        if (adapter != null) {
            loadHistory();
        }
    }

    private void loadHistory() {
        transactions.clear();

        transactions.addAll(
                dbHelper.getInventoryTransactions()
        );

        adapter.notifyDataSetChanged();

        boolean hasHistory =
                !transactions.isEmpty();

        historyRecyclerView.setVisibility(
                hasHistory
                        ? View.VISIBLE
                        : View.GONE
        );

        emptyHistoryText.setVisibility(
                hasHistory
                        ? View.GONE
                        : View.VISIBLE
        );
    }

    private String formatTransactionDate(
            String storedDate
    ) {
        if (storedDate == null || storedDate.trim().isEmpty()) {
            return "";
        }

        // SQLite CURRENT_TIMESTAMP stores dates in UTC using this format.
        SimpleDateFormat databaseFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.US
                );

        databaseFormat.setTimeZone(
                TimeZone.getTimeZone("UTC")
        );

        // Show the date in the device's local time and a friendlier format.
        SimpleDateFormat displayFormat =
                new SimpleDateFormat(
                        "MMM d, yyyy h:mm a",
                        Locale.getDefault()
                );

        displayFormat.setTimeZone(
                TimeZone.getDefault()
        );

        try {
            Date parsedDate =
                    databaseFormat.parse(storedDate);

            if (parsedDate == null) {
                return storedDate;
            }

            return displayFormat.format(parsedDate);
        } catch (ParseException exception) {
            // Keep the original database value if the date cannot be parsed.
            return storedDate;
        }
    }

    private class HistoryAdapter
            extends RecyclerView.Adapter<HistoryAdapter.Holder> {

        private final ArrayList<DatabaseHelper.InventoryTransaction> data;

        HistoryAdapter(
                ArrayList<DatabaseHelper.InventoryTransaction> data
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
                    R.layout.item_history_row,
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
            DatabaseHelper.InventoryTransaction transaction =
                    data.get(position);

            holder.actionText.setText(
                    getReadableAction(
                            transaction.transactionType
                    )
            );

            holder.itemNameText.setText(
                    transaction.itemName
            );

            holder.userText.setText(
                    getReadableUserText(transaction)
            );

            // Quantity values are only shown when both values were recorded.
            if (transaction.oldQuantity != null
                    && transaction.newQuantity != null) {

                holder.quantityText.setText(
                        "Quantity: " +
                                transaction.oldQuantity +
                                " -> " +
                                transaction.newQuantity
                );

                holder.quantityText.setVisibility(
                        View.VISIBLE
                );
            } else if (
                    DatabaseHelper.TRANSACTION_CREATE.equals(
                            transaction.transactionType
                    )
                            && transaction.newQuantity != null
            ) {
                // New items only have an opening quantity.
                holder.quantityText.setText(
                        "Opening quantity: " +
                                transaction.newQuantity
                );

                holder.quantityText.setVisibility(
                        View.VISIBLE
                );
            } else {
                holder.quantityText.setVisibility(
                        View.GONE
                );
            }

            // Hide empty notes so the row stays compact.
            if (transaction.note == null
                    || transaction.note.trim().isEmpty()) {

                holder.noteText.setVisibility(
                        View.GONE
                );
            } else {
                holder.noteText.setText(
                        transaction.note
                );

                holder.noteText.setVisibility(
                        View.VISIBLE
                );
            }

            holder.dateText.setText(
                    formatTransactionDate(
                            transaction.transactionDate
                    )
            );
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private String getReadableAction(
                String transactionType
        ) {
            if (transactionType == null) {
                return "Inventory Updated";
            }

            switch (transactionType) {
                case DatabaseHelper.TRANSACTION_CREATE:
                    return "Item Created";

                case DatabaseHelper.TRANSACTION_UPDATE:
                    return "Item Details Updated";

                case DatabaseHelper.TRANSACTION_QUANTITY:
                    return "Quantity Updated";

                case DatabaseHelper.TRANSACTION_DEACTIVATE:
                    return "Item Removed";

                case DatabaseHelper.TRANSACTION_REACTIVATE:
                    return "Item Restored";

                default:
                    return transactionType;
            }
        }

        private String getReadableUserText(
                DatabaseHelper.InventoryTransaction transaction
        ) {
            String username =
                    transaction.username == null
                            || transaction.username.trim().isEmpty()
                            ? "Unknown user"
                            : transaction.username;

            if (DatabaseHelper.TRANSACTION_CREATE.equals(
                    transaction.transactionType
            )) {
                return "Created by: " + username;
            }

            if (DatabaseHelper.TRANSACTION_DEACTIVATE.equals(
                    transaction.transactionType
            )) {
                return "Removed by: " + username;
            }

            if (DatabaseHelper.TRANSACTION_REACTIVATE.equals(
                    transaction.transactionType
            )) {
                return "Restored by: " + username;
            }

            return "Changed by: " + username;
        }

        class Holder extends RecyclerView.ViewHolder {

            TextView actionText;
            TextView itemNameText;
            TextView userText;
            TextView quantityText;
            TextView noteText;
            TextView dateText;

            Holder(View itemView) {
                super(itemView);

                actionText = itemView.findViewById(
                        R.id.historyActionText
                );

                itemNameText = itemView.findViewById(
                        R.id.historyItemNameText
                );

                userText = itemView.findViewById(
                        R.id.historyUserText
                );

                quantityText = itemView.findViewById(
                        R.id.historyQuantityText
                );

                noteText = itemView.findViewById(
                        R.id.historyNoteText
                );

                dateText = itemView.findViewById(
                        R.id.historyDateText
                );
            }
        }
    }
}