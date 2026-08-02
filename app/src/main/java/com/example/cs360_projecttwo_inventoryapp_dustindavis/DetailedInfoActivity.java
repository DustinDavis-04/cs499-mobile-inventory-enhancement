package com.example.cs360_projecttwo_inventoryapp_dustindavis;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DetailedInfoActivity extends AppCompatActivity {

    // Intent extras used when opening this screen from the app.
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_ITEM_ID = "itemId";

    // Screen modes.
    public static final String MODE_NEW = "NEW";
    public static final String MODE_VIEW = "VIEW";

    private DatabaseHelper dbHelper;

    private EditText itemNameEditText;
    private EditText reorderValueEditText;
    private EditText qtyValueText;

    private EditText manufacturerEditText;
    private EditText categoryEditText;
    private EditText modelNumberEditText;

    private EditText serialEditText;
    private EditText scuEditText;

    private EditText warehouseRowEditText;
    private EditText warehouseShelfEditText;
    private EditText notesEditText;

    private Button editButton;
    private Button saveButton;

    // Current mode and item ID for this screen session.
    private String mode;
    private int itemId;

    // The user ID is needed when a saved change is added to history.
    private int loggedInUserId;

    // Loaded item from the normalized database in VIEW mode.
    private DatabaseHelper.InventoryItem currentItem;

    // True only after a real change is made during an edit session.
    private boolean isDirty = false;

    // True only when fields are unlocked and the user can edit them.
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_info);

        dbHelper = new DatabaseHelper(this);

        // Load the signed-in user before allowing inventory changes.
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

        // Bind the main inventory fields.
        itemNameEditText = findViewById(R.id.itemNameEditText);
        reorderValueEditText = findViewById(R.id.reorderValueEditText);
        qtyValueText = findViewById(R.id.qtyValueText);

        // These fields match the normalized manufacturer and category tables.
        manufacturerEditText = findViewById(R.id.manufacturerEditText);
        categoryEditText = findViewById(R.id.categoryEditText);
        modelNumberEditText = findViewById(R.id.modelNumberEditText);

        serialEditText = findViewById(R.id.serialEditText);
        scuEditText = findViewById(R.id.scuEditText);

        // Warehouse row and shelf are stored as separate location values.
        warehouseRowEditText = findViewById(R.id.warehouseRowEditText);
        warehouseShelfEditText = findViewById(R.id.warehouseShelfEditText);
        notesEditText = findViewById(R.id.notesEditText);

        editButton = findViewById(R.id.editButton);
        saveButton = findViewById(R.id.saveButton);

        // Bottom navigation back button.
        LinearLayout backContainer = findViewById(R.id.backContainer);
        backContainer.setOnClickListener(v -> finish());

        // If the screen opened from an SMS link, pull the item ID from the URL.
        Uri data = getIntent().getData();

        if (data != null) {
            String lastSegment = data.getLastPathSegment();
            int deepLinkId = safeInt(lastSegment);

            if (deepLinkId > 0) {
                mode = MODE_VIEW;
                itemId = deepLinkId;
            }
        }

        // If a deep link did not set the mode, use the Intent extras.
        if (mode == null) {
            mode = getIntent().getStringExtra(EXTRA_MODE);

            if (mode == null) {
                mode = MODE_VIEW;
            }
        }

        // Watch the form so Save only turns on after a real change.
        attachDirtyWatchers();

        if (MODE_NEW.equals(mode)) {
            setupNewItemMode();
        } else {
            setupViewExistingMode();
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

    private void setupNewItemMode() {
        // New items start unlocked and ready to save.
        isEditing = true;
        isDirty = true;

        // Start at zero, but allow the user to enter an opening quantity.
        qtyValueText.setText("0");

        setFieldsEnabled(true);

        // Edit is not needed while a new record is being created.
        setButtonEnabled(editButton, false);

        // Save stays available so the new item can be added.
        setButtonEnabled(saveButton, true);

        saveButton.setOnClickListener(v -> saveNewItem());

        editButton.setOnClickListener(v -> {
            // No Edit action is needed while creating a new item.
        });
    }

    private void setupViewExistingMode() {
        // If a deep link did not provide an ID, use the Intent extra.
        if (itemId <= 0) {
            itemId = getIntent().getIntExtra(
                    EXTRA_ITEM_ID,
                    -1
            );
        }

        if (itemId <= 0) {
            Toast.makeText(
                    this,
                    "Could not load item.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        // Load the full item through the normalized JOIN query.
        currentItem = dbHelper.getNormalizedInventoryItemById(
                itemId
        );

        if (currentItem == null) {
            Toast.makeText(
                    this,
                    "Item not found.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        // Fill the form with the current database values.
        itemNameEditText.setText(currentItem.name);

        reorderValueEditText.setText(
                String.valueOf(currentItem.lowThreshold)
        );

        qtyValueText.setText(
                String.valueOf(currentItem.quantity)
        );

        manufacturerEditText.setText(
                valueOrEmpty(currentItem.manufacturerName)
        );

        categoryEditText.setText(
                valueOrEmpty(currentItem.categoryName)
        );

        modelNumberEditText.setText(
                valueOrEmpty(currentItem.modelNumber)
        );

        serialEditText.setText(
                valueOrEmpty(currentItem.serialNumber)
        );

        scuEditText.setText(
                valueOrEmpty(currentItem.scuNumber)
        );

        warehouseRowEditText.setText(
                currentItem.warehouseRow > 0
                        ? String.valueOf(currentItem.warehouseRow)
                        : ""
        );

        warehouseShelfEditText.setText(
                currentItem.warehouseShelf > 0
                        ? String.valueOf(currentItem.warehouseShelf)
                        : ""
        );

        notesEditText.setText(
                valueOrEmpty(currentItem.notes)
        );

        // Existing items start locked until Edit is selected.
        isEditing = false;
        isDirty = false;

        setFieldsEnabled(false);

        setButtonEnabled(editButton, true);
        setButtonEnabled(saveButton, false);

        editButton.setOnClickListener(v -> beginEditSession());
        saveButton.setOnClickListener(v -> saveExistingItemChanges());
    }

    private void beginEditSession() {
        // Unlock the form and wait for an actual change.
        isEditing = true;
        isDirty = false;

        setFieldsEnabled(true);

        // Save turns on after one of the watched fields changes.
        setButtonEnabled(saveButton, false);
    }

    private void saveNewItem() {
        String name =
                itemNameEditText.getText().toString().trim();

        String quantityText =
                qtyValueText.getText().toString().trim();

        String reorderText =
                reorderValueEditText.getText().toString().trim();

        String manufacturer =
                manufacturerEditText.getText().toString().trim();

        String category =
                categoryEditText.getText().toString().trim();

        String modelNumber =
                modelNumberEditText.getText().toString().trim();

        String serial =
                serialEditText.getText().toString().trim();

        String scu =
                scuEditText.getText().toString().trim();

        String warehouseRowText =
                warehouseRowEditText.getText().toString().trim();

        String warehouseShelfText =
                warehouseShelfEditText.getText().toString().trim();

        String notes =
                notesEditText.getText().toString().trim();

        // Check required fields before converting numeric values.
        if (name.isEmpty()
                || quantityText.isEmpty()
                || reorderText.isEmpty()
                || manufacturer.isEmpty()
                || category.isEmpty()
                || modelNumber.isEmpty()
                || serial.isEmpty()
                || scu.isEmpty()
                || warehouseRowText.isEmpty()
                || warehouseShelfText.isEmpty()) {

            Toast.makeText(
                    this,
                    "Fill in all required item and warehouse fields.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        int quantity = safeInt(quantityText);
        int reorderAmount = safeInt(reorderText);
        int warehouseRow = safeInt(warehouseRowText);
        int warehouseShelf = safeInt(warehouseShelfText);

        // Quantity and reorder amount cannot be negative.
        if (quantity < 0 || reorderAmount < 0) {
            Toast.makeText(
                    this,
                    "Quantity and reorder amount cannot be negative.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // Row and shelf numbers must identify a real warehouse position.
        if (warehouseRow <= 0 || warehouseShelf <= 0) {
            Toast.makeText(
                    this,
                    "Warehouse row and shelf must be greater than zero.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        long newId = dbHelper.createNormalizedInventoryItem(
                name,
                manufacturer,
                category,
                modelNumber,
                serial,
                scu,
                quantity,
                reorderAmount,
                warehouseRow,
                warehouseShelf,
                notes,
                loggedInUserId
        );

        if (newId > 0) {
            Toast.makeText(
                    this,
                    "Item added.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
        } else {
            Toast.makeText(
                    this,
                    "Could not add item. Check for duplicate serial or SCU numbers.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void saveExistingItemChanges() {
        if (currentItem == null || !isEditing) {
            return;
        }

        // Avoid writing another database row when nothing changed.
        if (!isDirty) {
            Toast.makeText(
                    this,
                    "No changes to save.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String name =
                itemNameEditText.getText().toString().trim();

        String quantityText =
                qtyValueText.getText().toString().trim();

        String reorderText =
                reorderValueEditText.getText().toString().trim();

        String manufacturer =
                manufacturerEditText.getText().toString().trim();

        String category =
                categoryEditText.getText().toString().trim();

        String modelNumber =
                modelNumberEditText.getText().toString().trim();

        String serial =
                serialEditText.getText().toString().trim();

        String scu =
                scuEditText.getText().toString().trim();

        String warehouseRowText =
                warehouseRowEditText.getText().toString().trim();

        String warehouseShelfText =
                warehouseShelfEditText.getText().toString().trim();

        String notes =
                notesEditText.getText().toString().trim();

        // Required values should never be saved as blank text.
        if (name.isEmpty()
                || quantityText.isEmpty()
                || reorderText.isEmpty()
                || manufacturer.isEmpty()
                || category.isEmpty()
                || modelNumber.isEmpty()
                || serial.isEmpty()
                || scu.isEmpty()
                || warehouseRowText.isEmpty()
                || warehouseShelfText.isEmpty()) {

            Toast.makeText(
                    this,
                    "Required fields cannot be blank.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        int quantity = safeInt(quantityText);
        int reorderAmount = safeInt(reorderText);
        int warehouseRow = safeInt(warehouseRowText);
        int warehouseShelf = safeInt(warehouseShelfText);

        if (quantity < 0 || reorderAmount < 0) {
            Toast.makeText(
                    this,
                    "Quantity and reorder amount cannot be negative.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (warehouseRow <= 0 || warehouseShelf <= 0) {
            Toast.makeText(
                    this,
                    "Warehouse row and shelf must be greater than zero.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // Save the details, quantity, and one matching history record together.
        boolean updated = dbHelper.updateNormalizedInventoryDetails(
                currentItem.id,
                name,
                manufacturer,
                category,
                modelNumber,
                serial,
                scu,
                quantity,
                reorderAmount,
                warehouseRow,
                warehouseShelf,
                notes,
                loggedInUserId
        );

        if (updated) {
            Toast.makeText(
                    this,
                    "Saved.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
        } else {
            Toast.makeText(
                    this,
                    "Could not save changes. Check for duplicate serial or SCU numbers.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void setFieldsEnabled(boolean enabled) {
        // All inventory changes now happen through this controlled form.
        itemNameEditText.setEnabled(enabled);
        reorderValueEditText.setEnabled(enabled);
        qtyValueText.setEnabled(enabled);

        manufacturerEditText.setEnabled(enabled);
        categoryEditText.setEnabled(enabled);
        modelNumberEditText.setEnabled(enabled);

        serialEditText.setEnabled(enabled);
        scuEditText.setEnabled(enabled);

        warehouseRowEditText.setEnabled(enabled);
        warehouseShelfEditText.setEnabled(enabled);
        notesEditText.setEnabled(enabled);

        // Dim locked fields so view mode is easy to recognize.
        float alpha = enabled ? 1.0f : 0.6f;

        itemNameEditText.setAlpha(alpha);
        reorderValueEditText.setAlpha(alpha);
        qtyValueText.setAlpha(alpha);

        manufacturerEditText.setAlpha(alpha);
        categoryEditText.setAlpha(alpha);
        modelNumberEditText.setAlpha(alpha);

        serialEditText.setAlpha(alpha);
        scuEditText.setAlpha(alpha);

        warehouseRowEditText.setAlpha(alpha);
        warehouseShelfEditText.setAlpha(alpha);
        notesEditText.setAlpha(alpha);
    }

    private void setButtonEnabled(
            Button button,
            boolean enabled
    ) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1.0f : 0.45f);
    }

    private void attachDirtyWatchers() {
        // Save only turns on after the user changes something in edit mode.
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence text,
                    int start,
                    int count,
                    int after
            ) {
                // Nothing is needed before the value changes.
            }

            @Override
            public void onTextChanged(
                    CharSequence text,
                    int start,
                    int before,
                    int count
            ) {
                if (!MODE_NEW.equals(mode) && isEditing) {
                    isDirty = true;
                    setButtonEnabled(saveButton, true);
                }
            }

            @Override
            public void afterTextChanged(Editable text) {
                // Nothing is needed after the value changes.
            }
        };

        itemNameEditText.addTextChangedListener(watcher);
        reorderValueEditText.addTextChangedListener(watcher);
        qtyValueText.addTextChangedListener(watcher);

        manufacturerEditText.addTextChangedListener(watcher);
        categoryEditText.addTextChangedListener(watcher);
        modelNumberEditText.addTextChangedListener(watcher);

        serialEditText.addTextChangedListener(watcher);
        scuEditText.addTextChangedListener(watcher);

        warehouseRowEditText.addTextChangedListener(watcher);
        warehouseShelfEditText.addTextChangedListener(watcher);
        notesEditText.addTextChangedListener(watcher);
    }

    // Return an empty string instead of passing null into an EditText.
    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    // Convert text to an integer and return zero when it is not valid.
    private int safeInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}