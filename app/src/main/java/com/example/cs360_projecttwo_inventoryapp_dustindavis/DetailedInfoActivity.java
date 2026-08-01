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
    private EditText categoryEditText;
    private EditText modelNumberEditText;

    private EditText serialEditText;
    private EditText scuEditText;

    private EditText warehouseRowEditText;
    private EditText warehouseShelfEditText;
    private EditText notesEditText;

    private Button editButton;
    private Button saveButton;

    // Current mode and item ID for this screen session
    private String mode;
    private int itemId;

    // Loaded item from the normalized database in VIEW mode
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

        // Bind the general inventory fields.
        itemNameEditText = findViewById(R.id.itemNameEditText);
        reorderValueEditText = findViewById(R.id.reorderValueEditText);
        qtyValueText = findViewById(R.id.qtyValueText);

        // These fields now match the normalized manufacturer and category tables.
        manufacturerEditText = findViewById(R.id.manufacturerEditText);
        categoryEditText = findViewById(R.id.categoryEditText);
        modelNumberEditText = findViewById(R.id.modelNumberEditText);

        serialEditText = findViewById(R.id.serialEditText);
        scuEditText = findViewById(R.id.scuEditText);

        // Warehouse row and shelf replace the old combined location field.
        warehouseRowEditText = findViewById(R.id.warehouseRowEditText);
        warehouseShelfEditText = findViewById(R.id.warehouseShelfEditText);
        notesEditText = findViewById(R.id.notesEditText);

        editButton = findViewById(R.id.editButton);
        saveButton = findViewById(R.id.saveButton);

        // Bottom navigation back button
        LinearLayout backContainer = findViewById(R.id.backContainer);
        backContainer.setOnClickListener(v -> finish());

        // If opened from the SMS link, pull the ID from the URL path.
        Uri data = getIntent().getData();
        if (data != null) {
            String lastSegment = data.getLastPathSegment();
            int deepLinkId = safeInt(lastSegment);

            if (deepLinkId > 0) {
                mode = MODE_VIEW;
                itemId = deepLinkId;
            }
        }

        // If the deep link did not set a mode, fall back to Intent extras.
        if (mode == null) {
            mode = getIntent().getStringExtra(EXTRA_MODE);

            if (mode == null) {
                mode = MODE_VIEW;
            }
        }

        // Watch for changes so Save only enables after something changes.
        attachDirtyWatchers();

        // Set up the screen based on whether an item is new or already exists.
        if (MODE_NEW.equals(mode)) {
            setupNewItemMode();
        } else {
            setupViewExistingMode();
        }
    }

    private void setupNewItemMode() {

        // New items start unlocked, and Save is available right away.
        isEditing = true;
        isDirty = true;

        // Quantity starts at zero because the inventory screen controls changes.
        qtyValueText.setText("0");

        setFieldsEnabled(true);

        // Edit is not needed while creating a new item.
        setButtonEnabled(editButton, false);

        // Save stays available so the new record can be added.
        setButtonEnabled(saveButton, true);

        saveButton.setOnClickListener(v -> saveNewItem());

        // Keep the button visible for a consistent layout, but do nothing here.
        editButton.setOnClickListener(v -> {
            // No action is needed in NEW mode.
        });
    }

    private void setupViewExistingMode() {

        // If a deep link did not set the ID, fall back to the Intent extra.
        if (itemId <= 0) {
            itemId = getIntent().getIntExtra(EXTRA_ITEM_ID, -1);
        }

        // There is nothing to load without a valid item ID.
        if (itemId <= 0) {
            Toast.makeText(
                    this,
                    "Could not load item.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        // Load the complete item through the normalized JOIN query.
        currentItem = dbHelper.getNormalizedInventoryItemById(itemId);

        if (currentItem == null) {
            Toast.makeText(
                    this,
                    "Item not found.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        // Fill the screen with values returned from the normalized database.
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

        // Existing records begin locked until the user selects Edit.
        isEditing = false;
        isDirty = false;

        setFieldsEnabled(false);

        setButtonEnabled(editButton, true);
        setButtonEnabled(saveButton, false);

        editButton.setOnClickListener(v -> beginEditSession());
        saveButton.setOnClickListener(v -> saveExistingItemChanges());
    }

    private void beginEditSession() {

        // Unlock the form and start watching for an actual change.
        isEditing = true;
        isDirty = false;

        setFieldsEnabled(true);

        // Save turns on after the user changes one of the fields.
        setButtonEnabled(saveButton, false);
    }

    private void saveNewItem() {

        // Pull the required text values from the form.
        String name = itemNameEditText.getText().toString().trim();
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

        // Check text fields before converting the numeric values.
        if (name.isEmpty()
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

        int reorderAmount = safeInt(reorderText);
        int warehouseRow = safeInt(warehouseRowText);
        int warehouseShelf = safeInt(warehouseShelfText);

        // Row and shelf numbers must identify a valid warehouse position.
        if (warehouseRow <= 0 || warehouseShelf <= 0) {
            Toast.makeText(
                    this,
                    "Warehouse row and shelf must be greater than zero.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // New items begin with zero quantity until the inventory screen updates it.
        long newId = dbHelper.createNormalizedInventoryItem(
                name,
                manufacturer,
                category,
                modelNumber,
                serial,
                scu,
                0,
                reorderAmount,
                warehouseRow,
                warehouseShelf,
                notes
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

        // Nothing can be saved if the item did not load correctly.
        if (currentItem == null) {
            return;
        }

        // Updates should only run after the Edit button is selected.
        if (!isEditing) {
            return;
        }

        // Avoid writing to SQLite when nothing was changed.
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

        // Keep required data from being saved as blank values.
        if (name.isEmpty()
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

        int reorderAmount = safeInt(reorderText);
        int warehouseRow = safeInt(warehouseRowText);
        int warehouseShelf = safeInt(warehouseShelfText);

        if (warehouseRow <= 0 || warehouseShelf <= 0) {
            Toast.makeText(
                    this,
                    "Warehouse row and shelf must be greater than zero.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // Update the item and its lookup-table relationships together.
        boolean updated = dbHelper.updateNormalizedInventoryDetails(
                currentItem.id,
                name,
                manufacturer,
                category,
                modelNumber,
                serial,
                scu,
                reorderAmount,
                warehouseRow,
                warehouseShelf,
                notes
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

        // Quantity remains read only because it is managed on the inventory screen.
        itemNameEditText.setEnabled(enabled);
        reorderValueEditText.setEnabled(enabled);

        manufacturerEditText.setEnabled(enabled);
        categoryEditText.setEnabled(enabled);
        modelNumberEditText.setEnabled(enabled);

        serialEditText.setEnabled(enabled);
        scuEditText.setEnabled(enabled);

        warehouseRowEditText.setEnabled(enabled);
        warehouseShelfEditText.setEnabled(enabled);
        notesEditText.setEnabled(enabled);

        // Dim locked fields so it is obvious that they cannot be changed.
        float alpha = enabled ? 1.0f : 0.6f;

        itemNameEditText.setAlpha(alpha);
        reorderValueEditText.setAlpha(alpha);

        manufacturerEditText.setAlpha(alpha);
        categoryEditText.setAlpha(alpha);
        modelNumberEditText.setAlpha(alpha);

        serialEditText.setAlpha(alpha);
        scuEditText.setAlpha(alpha);

        warehouseRowEditText.setAlpha(alpha);
        warehouseShelfEditText.setAlpha(alpha);
        notesEditText.setAlpha(alpha);
    }

    private void setButtonEnabled(Button button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1.0f : 0.45f);
    }

    private void attachDirtyWatchers() {

        // Turn on Save only after the user enters edit mode and changes something.
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence text,
                    int start,
                    int count,
                    int after
            ) {
                // Nothing is needed before the text changes.
            }

            @Override
            public void onTextChanged(
                    CharSequence text,
                    int start,
                    int before,
                    int count
            ) {
                // Only track changes after Edit is selected in VIEW mode.
                if (!MODE_NEW.equals(mode) && isEditing) {
                    isDirty = true;
                    setButtonEnabled(saveButton, true);
                }
            }

            @Override
            public void afterTextChanged(Editable text) {
                // Nothing is needed after the text changes.
            }
        };

        itemNameEditText.addTextChangedListener(watcher);
        reorderValueEditText.addTextChangedListener(watcher);

        manufacturerEditText.addTextChangedListener(watcher);
        categoryEditText.addTextChangedListener(watcher);
        modelNumberEditText.addTextChangedListener(watcher);

        serialEditText.addTextChangedListener(watcher);
        scuEditText.addTextChangedListener(watcher);

        warehouseRowEditText.addTextChangedListener(watcher);
        warehouseShelfEditText.addTextChangedListener(watcher);
        notesEditText.addTextChangedListener(watcher);
    }

    // Return an empty string instead of passing a null value to an EditText.
    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    // Convert a string to an integer and return zero when it is not valid.
    private int safeInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}