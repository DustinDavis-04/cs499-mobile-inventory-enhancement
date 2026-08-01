package com.example.cs360_projecttwo_inventoryapp_dustindavis;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DetailedInfoActivity extends AppCompatActivity {

    // Intent extras used when opening this screen from the app
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_ITEM_ID = "itemId";

    // Screen modes
    public static final String MODE_NEW = "NEW";
    public static final String MODE_VIEW = "VIEW";

    private DatabaseHelper dbHelper;

    private EditText itemNameEditText;
    private EditText reorderValueEditText;
    private TextView qtyValueText;

    private EditText manufacturerEditText;

    private EditText serialEditText;
    private EditText scuEditText;

    private EditText locationEditText;
    private EditText notesEditText;

    private Button editButton;
    private Button saveButton;

    // Current mode and item id for this screen session
    private String mode;
    private int itemId;

    // Loaded item from the database (VIEW mode only)
    private DatabaseHelper.InventoryItem currentItem;

    // True only after a real change is made during an edit session
    private boolean isDirty = false;

    // True only when fields are unlocked and the user is allowed to edit
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_info);

        dbHelper = new DatabaseHelper(this);

        // Bind UI fields
        itemNameEditText = findViewById(R.id.itemNameEditText);
        reorderValueEditText = findViewById(R.id.reorderValueEditText);
        qtyValueText = findViewById(R.id.qtyValueText);

        manufacturerEditText = findViewById(R.id.manufacturerEditText);

        serialEditText = findViewById(R.id.serialEditText);
        scuEditText = findViewById(R.id.scuEditText);

        locationEditText = findViewById(R.id.locationEditText);
        notesEditText = findViewById(R.id.notesEditText);

        editButton = findViewById(R.id.editButton);
        saveButton = findViewById(R.id.saveButton);

        // Bottom nav back button
        LinearLayout backContainer = findViewById(R.id.backContainer);
        backContainer.setOnClickListener(v -> finish());

        // If opened from the SMS link, pull the id from the URL path
        Uri data = getIntent().getData();
        if (data != null) {
            String lastSegment = data.getLastPathSegment();
            int deepLinkId = safeInt(lastSegment);

            if (deepLinkId > 0) {
                mode = MODE_VIEW;
                itemId = deepLinkId;
            }
        }

        // If the deep link did not set a mode, fall back to extras
        if (mode == null) {
            mode = getIntent().getStringExtra(EXTRA_MODE);
            if (mode == null) {
                mode = MODE_VIEW;
            }
        }

        // Watch for changes so Save only enables after something actually changes
        attachDirtyWatchers();

        // Route into the correct setup based on the mode
        if (MODE_NEW.equals(mode)) {
            setupNewItemMode();
        } else {
            setupViewExistingMode();
        }
    }

    private void setupNewItemMode() {

        // New item starts unlocked, Save is available right away
        isEditing = true;
        isDirty = true;

        // Quantity starts at 0, inventory screen controls quantity changes
        qtyValueText.setText("0");

        // Allow typing into fields
        setFieldsEnabled(true);

        // Edit button is not used in NEW mode, keep it disabled
        setButtonEnabled(editButton, false);

        // Save button is active for NEW mode
        setButtonEnabled(saveButton, true);

        // Save inserts a brand new row into the database
        saveButton.setOnClickListener(v -> saveNewItem());

        // Edit stays visible for a consistent layout, but does nothing here
        editButton.setOnClickListener(v -> { });
    }

    private void setupViewExistingMode() {

        // If the deep link did not set an id, fall back to extras
        if (itemId <= 0) {
            itemId = getIntent().getIntExtra(EXTRA_ITEM_ID, -1);
        }

        // No valid id means there is nothing to load
        if (itemId <= 0) {
            Toast.makeText(this, "Could not load item.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Pull the item from SQLite
        currentItem = dbHelper.getInventoryItemById(itemId);
        if (currentItem == null) {
            Toast.makeText(this, "Item not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Fill the UI with the saved values
        itemNameEditText.setText(currentItem.name);
        reorderValueEditText.setText(String.valueOf(currentItem.lowThreshold));
        qtyValueText.setText(String.valueOf(currentItem.quantity));

        manufacturerEditText.setText(currentItem.manufacturer);

        serialEditText.setText(currentItem.serialNumber);
        scuEditText.setText(currentItem.scuNumber);

        locationEditText.setText(currentItem.location == null ? "" : currentItem.location);
        notesEditText.setText(currentItem.notes == null ? "" : currentItem.notes);

        // Start locked until Edit is pressed
        isEditing = false;
        isDirty = false;

        setFieldsEnabled(false);

        // Edit is enabled in VIEW mode
        setButtonEnabled(editButton, true);

        // Save stays disabled until Edit is pressed and a change is made
        setButtonEnabled(saveButton, false);

        editButton.setOnClickListener(v -> beginEditSession());
        saveButton.setOnClickListener(v -> saveExistingItemChanges());
    }

    private void beginEditSession() {

        // Unlock fields and reset dirty state for this edit session
        isEditing = true;
        isDirty = false;

        setFieldsEnabled(true);

        // Save will turn on after a real change happens
        setButtonEnabled(saveButton, false);
    }

    private void saveNewItem() {

        // Pull values from the UI
        String name = itemNameEditText.getText().toString().trim();
        String reorderText = reorderValueEditText.getText().toString().trim();
        int reorderAmount = safeInt(reorderText);

        // Manufacturer is the field name shown on the UI
        String manufacturer = manufacturerEditText.getText().toString().trim();

        String serial = serialEditText.getText().toString().trim();
        String scu = scuEditText.getText().toString().trim();

        String location = locationEditText.getText().toString().trim();
        String notes = notesEditText.getText().toString().trim();

        // Reorder has to be typed in, otherwise safeInt returns 0 and it looks valid
        if (name.isEmpty() || reorderText.isEmpty() || manufacturer.isEmpty() || serial.isEmpty() || scu.isEmpty()) {
            Toast.makeText(
                    this,
                    "Fill in item name, reorder amount, manufacturer, serial number, and SCU number.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // New items start with quantity 0, inventory screen adjusts quantity later
        long newId = dbHelper.createInventoryItem(
                name,
                0,
                reorderAmount,
                manufacturer,
                serial,
                scu,
                location,
                notes
        );

        if (newId > 0) {
            Toast.makeText(this, "Item added.", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Could not add item.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveExistingItemChanges() {

        // Nothing to save if the item never loaded
        if (currentItem == null) {
            return;
        }

        // Save should only run after Edit is pressed
        if (!isEditing) {
            return;
        }

        // If nothing changed, do not write to the database
        if (!isDirty) {
            Toast.makeText(this, "No changes to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Pull updated values from the UI
        String name = itemNameEditText.getText().toString().trim();
        int reorderAmount = safeInt(reorderValueEditText.getText().toString().trim());

        String manufacturer = manufacturerEditText.getText().toString().trim();

        String serial = serialEditText.getText().toString().trim();
        String scu = scuEditText.getText().toString().trim();

        String location = locationEditText.getText().toString().trim();
        String notes = notesEditText.getText().toString().trim();

        // Keep required fields from being saved blank
        if (name.isEmpty() || manufacturer.isEmpty() || serial.isEmpty() || scu.isEmpty()) {
            Toast.makeText(this, "Required fields cannot be blank.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update the existing row in SQLite
        boolean updated = dbHelper.updateInventoryDetails(
                currentItem.id,
                name,
                reorderAmount,
                manufacturer,
                serial,
                scu,
                location,
                notes
        );

        if (updated) {
            Toast.makeText(this, "Saved.", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Could not save changes.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setFieldsEnabled(boolean enabled) {

        // Quantity stays read only, inventory screen controls it
        itemNameEditText.setEnabled(enabled);
        reorderValueEditText.setEnabled(enabled);

        manufacturerEditText.setEnabled(enabled);
        serialEditText.setEnabled(enabled);
        scuEditText.setEnabled(enabled);

        locationEditText.setEnabled(enabled);
        notesEditText.setEnabled(enabled);

        // Dim fields when locked so it is obvious they are not editable
        float alpha = enabled ? 1.0f : 0.6f;

        itemNameEditText.setAlpha(alpha);
        reorderValueEditText.setAlpha(alpha);

        manufacturerEditText.setAlpha(alpha);
        serialEditText.setAlpha(alpha);
        scuEditText.setAlpha(alpha);

        locationEditText.setAlpha(alpha);
        notesEditText.setAlpha(alpha);
    }

    private void setButtonEnabled(Button button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1.0f : 0.45f);
    }

    private void attachDirtyWatchers() {

        // Turns on Save only after the user is in edit mode and changes something
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                // Only track changes after Edit is pressed in VIEW mode
                if (!MODE_NEW.equals(mode) && isEditing) {
                    isDirty = true;
                    setButtonEnabled(saveButton, true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        };

        itemNameEditText.addTextChangedListener(watcher);
        reorderValueEditText.addTextChangedListener(watcher);

        manufacturerEditText.addTextChangedListener(watcher);
        serialEditText.addTextChangedListener(watcher);
        scuEditText.addTextChangedListener(watcher);

        locationEditText.addTextChangedListener(watcher);
        notesEditText.addTextChangedListener(watcher);
    }

    // Converts a string to int, returns 0 if it fails
    private int safeInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return 0;
        }
    }
}
