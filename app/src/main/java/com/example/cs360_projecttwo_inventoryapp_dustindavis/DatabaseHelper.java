package com.example.cs360_projecttwo_inventoryapp_dustindavis;

import android.util.Base64;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.security.MessageDigest;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;

public class DatabaseHelper extends SQLiteOpenHelper {

    // -------------------------
    // Database settings
    // -------------------------

    private static final String DATABASE_NAME = "inventory_app.db";
    private static final int DATABASE_VERSION = 6;

    // -------------------------
    // Password security settings
    // -------------------------

    private static final String PASSWORD_ALGORITHM =
            "PBKDF2WithHmacSHA256";

    private static final int PASSWORD_ITERATIONS =
            120000;

    private static final int PASSWORD_KEY_LENGTH =
            256;

    private static final int PASSWORD_SALT_LENGTH =
            16;

    private static final String PASSWORD_SEPARATOR =
            ":";

    // -------------------------
    // Users table
    // -------------------------

    private static final String TABLE_USERS = "users";

    private static final String COL_USER_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";
    private static final String COL_FIRST_NAME = "first_name";
    private static final String COL_LAST_NAME = "last_name";
    private static final String COL_TITLE = "title";
    private static final String COL_EMAIL = "email";

    // -------------------------
    // Normalized inventory tables
    // -------------------------

    private static final String TABLE_MANUFACTURERS = "manufacturers";
    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_LOCATIONS = "locations";
    private static final String TABLE_INVENTORY_ITEMS = "inventory_items";

    // Inventory item columns
    private static final String COL_ITEM_ID = "id";
    private static final String COL_ITEM_NAME = "item_name";
    private static final String COL_ITEM_QTY = "quantity";
    private static final String COL_ITEM_THRESHOLD = "low_threshold";
    private static final String COL_ITEM_SERIAL = "serial_number";
    private static final String COL_ITEM_SCU = "scu_number";
    private static final String COL_ITEM_NOTES = "notes";
    private static final String COL_ITEM_ACTIVE = "is_active";
    private static final String COL_MODEL_NUMBER = "model_number";

    // Manufacturer columns
    private static final String COL_MANUFACTURER_ID = "manufacturer_id";
    private static final String COL_MANUFACTURER_NAME = "manufacturer_name";

    // Category columns
    private static final String COL_CATEGORY_ID = "category_id";
    private static final String COL_CATEGORY_NAME = "category_name";

    // Warehouse location columns
    private static final String COL_LOCATION_ID = "location_id";
    private static final String COL_WAREHOUSE_ROW = "warehouse_row";
    private static final String COL_WAREHOUSE_SHELF = "warehouse_shelf";

    // -------------------------
    // Inventory transaction table
    // -------------------------

    private static final String TABLE_INVENTORY_TRANSACTIONS =
            "inventory_transactions";

    private static final String COL_TRANSACTION_ID = "transaction_id";

    // The users and inventory tables both use "id" as their primary key.
    // These names keep the two foreign keys separate in the history table.
    private static final String COL_TRANSACTION_ITEM_ID = "item_id";
    private static final String COL_TRANSACTION_USER_ID = "user_id";

    private static final String COL_TRANSACTION_TYPE = "transaction_type";
    private static final String COL_OLD_QUANTITY = "old_quantity";
    private static final String COL_NEW_QUANTITY = "new_quantity";
    private static final String COL_TRANSACTION_NOTE = "transaction_note";
    private static final String COL_TRANSACTION_DATE = "transaction_date";

    // Keep transaction types consistent so the database does not end up
    // with slightly different labels for the same kind of action.
    public static final String TRANSACTION_CREATE = "CREATE";
    public static final String TRANSACTION_UPDATE = "UPDATE";
    public static final String TRANSACTION_QUANTITY = "QUANTITY";
    public static final String TRANSACTION_DEACTIVATE = "DEACTIVATE";
    public static final String TRANSACTION_REACTIVATE = "REACTIVATE";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);

        // Turn on foreign key checks so records cannot point to data
        // that does not exist in the related tables.
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String createUsersTable =
                "CREATE TABLE " + TABLE_USERS + " (" +
                        COL_USER_ID +
                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_USERNAME +
                        " TEXT NOT NULL UNIQUE, " +
                        COL_PASSWORD +
                        " TEXT NOT NULL, " +
                        COL_FIRST_NAME +
                        " TEXT, " +
                        COL_LAST_NAME +
                        " TEXT, " +
                        COL_TITLE +
                        " TEXT, " +
                        COL_EMAIL +
                        " TEXT NOT NULL" +
                        ");";

        // Manufacturers are stored once and reused by inventory items.
        String createManufacturersTable =
                "CREATE TABLE " + TABLE_MANUFACTURERS + " (" +
                        COL_MANUFACTURER_ID +
                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_MANUFACTURER_NAME +
                        " TEXT NOT NULL UNIQUE COLLATE NOCASE" +
                        ");";

        // Categories are stored once and reused by inventory items.
        String createCategoriesTable =
                "CREATE TABLE " + TABLE_CATEGORIES + " (" +
                        COL_CATEGORY_ID +
                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_CATEGORY_NAME +
                        " TEXT NOT NULL UNIQUE COLLATE NOCASE" +
                        ");";

        // Each warehouse row and shelf combination is stored one time.
        String createLocationsTable =
                "CREATE TABLE " + TABLE_LOCATIONS + " (" +
                        COL_LOCATION_ID +
                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_WAREHOUSE_ROW +
                        " INTEGER NOT NULL CHECK (" +
                        COL_WAREHOUSE_ROW + " > 0), " +
                        COL_WAREHOUSE_SHELF +
                        " INTEGER NOT NULL CHECK (" +
                        COL_WAREHOUSE_SHELF + " > 0), " +
                        "UNIQUE (" +
                        COL_WAREHOUSE_ROW + ", " +
                        COL_WAREHOUSE_SHELF +
                        ")" +
                        ");";

        // Inventory items connect to manufacturer, category, and location
        // records instead of repeating those values in every row.
        String createInventoryItemsTable =
                "CREATE TABLE " + TABLE_INVENTORY_ITEMS + " (" +
                        COL_ITEM_ID +
                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_MANUFACTURER_ID +
                        " INTEGER NOT NULL, " +
                        COL_CATEGORY_ID +
                        " INTEGER NOT NULL, " +
                        COL_LOCATION_ID +
                        " INTEGER, " +
                        COL_ITEM_NAME +
                        " TEXT NOT NULL, " +
                        COL_MODEL_NUMBER +
                        " TEXT NOT NULL, " +
                        COL_ITEM_SERIAL +
                        " TEXT NOT NULL UNIQUE, " +
                        COL_ITEM_SCU +
                        " TEXT NOT NULL UNIQUE, " +
                        COL_ITEM_QTY +
                        " INTEGER NOT NULL DEFAULT 0 " +
                        "CHECK (" + COL_ITEM_QTY + " >= 0), " +
                        COL_ITEM_THRESHOLD +
                        " INTEGER NOT NULL DEFAULT 0 " +
                        "CHECK (" + COL_ITEM_THRESHOLD + " >= 0), " +
                        COL_ITEM_NOTES +
                        " TEXT, " +
                        COL_ITEM_ACTIVE +
                        " INTEGER NOT NULL DEFAULT 1 " +
                        "CHECK (" + COL_ITEM_ACTIVE + " IN (0, 1)), " +

                        "FOREIGN KEY (" +
                        COL_MANUFACTURER_ID +
                        ") REFERENCES " +
                        TABLE_MANUFACTURERS +
                        "(" + COL_MANUFACTURER_ID + "), " +

                        "FOREIGN KEY (" +
                        COL_CATEGORY_ID +
                        ") REFERENCES " +
                        TABLE_CATEGORIES +
                        "(" + COL_CATEGORY_ID + "), " +

                        "FOREIGN KEY (" +
                        COL_LOCATION_ID +
                        ") REFERENCES " +
                        TABLE_LOCATIONS +
                        "(" + COL_LOCATION_ID + ")" +
                        ");";

        db.execSQL(createUsersTable);
        db.execSQL(createManufacturersTable);
        db.execSQL(createCategoriesTable);
        db.execSQL(createLocationsTable);
        db.execSQL(createInventoryItemsTable);

        // The history table depends on both users and inventory items,
        // so it is created after those tables are ready.
        createInventoryTransactionsTable(db);
    }

    private void createInventoryTransactionsTable(SQLiteDatabase db) {

        // This table keeps a record of what changed, which user made
        // the change, and when the change happened.
        String createTransactionsTable =
                "CREATE TABLE IF NOT EXISTS " +
                        TABLE_INVENTORY_TRANSACTIONS + " (" +

                        COL_TRANSACTION_ID +
                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        COL_TRANSACTION_ITEM_ID +
                        " INTEGER NOT NULL, " +

                        COL_TRANSACTION_USER_ID +
                        " INTEGER NOT NULL, " +

                        COL_TRANSACTION_TYPE +
                        " TEXT NOT NULL, " +

                        COL_OLD_QUANTITY +
                        " INTEGER, " +

                        COL_NEW_QUANTITY +
                        " INTEGER, " +

                        COL_TRANSACTION_NOTE +
                        " TEXT, " +

                        COL_TRANSACTION_DATE +
                        " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +

                        "FOREIGN KEY (" +
                        COL_TRANSACTION_ITEM_ID +
                        ") REFERENCES " +
                        TABLE_INVENTORY_ITEMS +
                        "(" + COL_ITEM_ID + "), " +

                        "FOREIGN KEY (" +
                        COL_TRANSACTION_USER_ID +
                        ") REFERENCES " +
                        TABLE_USERS +
                        "(" + COL_USER_ID + ")" +
                        ");";

        db.execSQL(createTransactionsTable);
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
    ) {
        // Version 6 only adds the transaction history table.
        // Existing users and inventory records stay in place.
        if (oldVersion < 6) {
            createInventoryTransactionsTable(db);
        }
    }

    // -------------------------
    // Transaction history helper
    // -------------------------

    private long recordInventoryTransaction(
            SQLiteDatabase db,
            int itemId,
            int userId,
            String transactionType,
            Integer oldQuantity,
            Integer newQuantity,
            String transactionNote
    ) {
        // A history record needs a real item, a real user, and a clear action.
        if (itemId <= 0
                || userId <= 0
                || isBlank(transactionType)) {
            return -1;
        }

        ContentValues values = new ContentValues();

        values.put(
                COL_TRANSACTION_ITEM_ID,
                itemId
        );

        values.put(
                COL_TRANSACTION_USER_ID,
                userId
        );

        values.put(
                COL_TRANSACTION_TYPE,
                transactionType.trim()
        );

        // Detail updates and status changes may not have quantity values.
        // Store null instead of pretending the quantity was zero.
        if (oldQuantity == null) {
            values.putNull(COL_OLD_QUANTITY);
        } else {
            values.put(
                    COL_OLD_QUANTITY,
                    oldQuantity
            );
        }

        if (newQuantity == null) {
            values.putNull(COL_NEW_QUANTITY);
        } else {
            values.put(
                    COL_NEW_QUANTITY,
                    newQuantity
            );
        }

        // Notes are optional, but keeping an empty value is easier to display later.
        values.put(
                COL_TRANSACTION_NOTE,
                transactionNote == null
                        ? ""
                        : transactionNote.trim()
        );

        // The timestamp is left out because SQLite adds CURRENT_TIMESTAMP.
        return db.insertOrThrow(
                TABLE_INVENTORY_TRANSACTIONS,
                null,
                values
        );
    }

    // -------------------------
    // Lookup-table helpers
    // -------------------------

    private long getOrCreateManufacturerId(
            SQLiteDatabase db,
            String manufacturerName
    ) {
        // Check for an existing manufacturer before adding another row.
        try (Cursor cursor = db.query(
                TABLE_MANUFACTURERS,
                new String[]{COL_MANUFACTURER_ID},
                COL_MANUFACTURER_NAME + " = ? COLLATE NOCASE",
                new String[]{manufacturerName},
                null,
                null,
                null
        )) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                                COL_MANUFACTURER_ID
                        )
                );
            }
        }

        ContentValues values = new ContentValues();
        values.put(
                COL_MANUFACTURER_NAME,
                manufacturerName
        );

        return db.insertOrThrow(
                TABLE_MANUFACTURERS,
                null,
                values
        );
    }

    private long getOrCreateCategoryId(
            SQLiteDatabase db,
            String categoryName
    ) {
        // Check for an existing category before adding another row.
        try (Cursor cursor = db.query(
                TABLE_CATEGORIES,
                new String[]{COL_CATEGORY_ID},
                COL_CATEGORY_NAME + " = ? COLLATE NOCASE",
                new String[]{categoryName},
                null,
                null,
                null
        )) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                                COL_CATEGORY_ID
                        )
                );
            }
        }

        ContentValues values = new ContentValues();
        values.put(
                COL_CATEGORY_NAME,
                categoryName
        );

        return db.insertOrThrow(
                TABLE_CATEGORIES,
                null,
                values
        );
    }

    private long getOrCreateLocationId(
            SQLiteDatabase db,
            int warehouseRow,
            int warehouseShelf
    ) {
        // Reuse a warehouse position when the same row and shelf
        // combination is already stored.
        try (Cursor cursor = db.query(
                TABLE_LOCATIONS,
                new String[]{COL_LOCATION_ID},
                COL_WAREHOUSE_ROW + " = ? AND " +
                        COL_WAREHOUSE_SHELF + " = ?",
                new String[]{
                        String.valueOf(warehouseRow),
                        String.valueOf(warehouseShelf)
                },
                null,
                null,
                null
        )) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                                COL_LOCATION_ID
                        )
                );
            }
        }

        ContentValues values = new ContentValues();

        values.put(
                COL_WAREHOUSE_ROW,
                warehouseRow
        );

        values.put(
                COL_WAREHOUSE_SHELF,
                warehouseShelf
        );

        return db.insertOrThrow(
                TABLE_LOCATIONS,
                null,
                values
        );
    }

    // -------------------------
    // Inventory create
    // -------------------------

    public long createNormalizedInventoryItem(
            String name,
            String manufacturer,
            String category,
            String modelNumber,
            String serialNumber,
            String scuNumber,
            int quantity,
            int lowThreshold,
            int warehouseRow,
            int warehouseShelf,
            String notes
    ) {
        // Starter records can still use the original method without creating
        // a user history entry.
        return createNormalizedInventoryItemInternal(
                name,
                manufacturer,
                category,
                modelNumber,
                serialNumber,
                scuNumber,
                quantity,
                lowThreshold,
                warehouseRow,
                warehouseShelf,
                notes,
                -1,
                false
        );
    }

    public long createNormalizedInventoryItem(
            String name,
            String manufacturer,
            String category,
            String modelNumber,
            String serialNumber,
            String scuNumber,
            int quantity,
            int lowThreshold,
            int warehouseRow,
            int warehouseShelf,
            String notes,
            int userId
    ) {
        // Items created through the form include the signed-in user
        // so the action can be added to inventory history.
        return createNormalizedInventoryItemInternal(
                name,
                manufacturer,
                category,
                modelNumber,
                serialNumber,
                scuNumber,
                quantity,
                lowThreshold,
                warehouseRow,
                warehouseShelf,
                notes,
                userId,
                true
        );
    }

    private long createNormalizedInventoryItemInternal(
            String name,
            String manufacturer,
            String category,
            String modelNumber,
            String serialNumber,
            String scuNumber,
            int quantity,
            int lowThreshold,
            int warehouseRow,
            int warehouseShelf,
            String notes,
            int userId,
            boolean recordHistory
    ) {
        // Stop the insert when required information is missing.
        if (isBlank(name)
                || isBlank(manufacturer)
                || isBlank(category)
                || isBlank(modelNumber)
                || isBlank(serialNumber)
                || isBlank(scuNumber)) {
            return -1;
        }

        // Quantities cannot be negative, and warehouse positions
        // need to use numbers greater than zero.
        if (quantity < 0
                || lowThreshold < 0
                || warehouseRow <= 0
                || warehouseShelf <= 0) {
            return -1;
        }

        // A form-created item needs a valid signed-in user for its history record.
        if (recordHistory && userId <= 0) {
            return -1;
        }

        SQLiteDatabase db = getWritableDatabase();
        long newItemId = -1;

        // The lookup records, inventory item, and history entry all belong
        // to the same operation. If one part fails, none of them are kept.
        db.beginTransaction();

        try {
            long manufacturerId = getOrCreateManufacturerId(
                    db,
                    manufacturer.trim()
            );

            long categoryId = getOrCreateCategoryId(
                    db,
                    category.trim()
            );

            long locationId = getOrCreateLocationId(
                    db,
                    warehouseRow,
                    warehouseShelf
            );

            ContentValues values = new ContentValues();

            values.put(
                    COL_MANUFACTURER_ID,
                    manufacturerId
            );

            values.put(
                    COL_CATEGORY_ID,
                    categoryId
            );

            values.put(
                    COL_LOCATION_ID,
                    locationId
            );

            values.put(
                    COL_ITEM_NAME,
                    name.trim()
            );

            values.put(
                    COL_MODEL_NUMBER,
                    modelNumber.trim()
            );

            values.put(
                    COL_ITEM_SERIAL,
                    serialNumber.trim()
            );

            values.put(
                    COL_ITEM_SCU,
                    scuNumber.trim()
            );

            values.put(
                    COL_ITEM_QTY,
                    quantity
            );

            values.put(
                    COL_ITEM_THRESHOLD,
                    lowThreshold
            );

            values.put(
                    COL_ITEM_NOTES,
                    notes == null ? "" : notes.trim()
            );

            newItemId = db.insertOrThrow(
                    TABLE_INVENTORY_ITEMS,
                    null,
                    values
            );

            if (recordHistory) {
                long transactionId = recordInventoryTransaction(
                        db,
                        (int) newItemId,
                        userId,
                        TRANSACTION_CREATE,
                        null,
                        quantity,
                        "Item created with an opening quantity of " +
                                quantity +
                                "."
                );

                if (transactionId <= 0) {
                    throw new IllegalStateException(
                            "The item history record could not be created."
                    );
                }
            }

            db.setTransactionSuccessful();
        } catch (RuntimeException exception) {
            // Return -1 so the screen can report that the item was not saved.
            newItemId = -1;
        } finally {
            db.endTransaction();
            db.close();
        }

        return newItemId;
    }

    // -------------------------
    // Inventory queries
    // -------------------------

    public ArrayList<InventoryItem> getAllNormalizedInventoryItems() {
        return getNormalizedInventoryItems(
                "i." + COL_ITEM_ACTIVE + " = 1",
                "i." + COL_ITEM_NAME +
                        " COLLATE NOCASE ASC"
        );
    }

    public ArrayList<InventoryItem> getInactiveNormalizedInventoryItems() {
        return getNormalizedInventoryItems(
                "i." + COL_ITEM_ACTIVE + " = 0",
                "i." + COL_ITEM_NAME +
                        " COLLATE NOCASE ASC"
        );
    }

    public ArrayList<InventoryItem> getLowNormalizedInventoryItems() {
        return getNormalizedInventoryItems(
                "i." + COL_ITEM_ACTIVE + " = 1 AND " +
                        "i." + COL_ITEM_QTY +
                        " <= i." + COL_ITEM_THRESHOLD,
                "i." + COL_ITEM_QTY + " ASC, " +
                        "i." + COL_ITEM_NAME +
                        " COLLATE NOCASE ASC"
        );
    }

    private ArrayList<InventoryItem> getNormalizedInventoryItems(
            String whereClause,
            String orderBy
    ) {
        ArrayList<InventoryItem> items = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String query =
                buildNormalizedInventorySelect() +
                        " WHERE " + whereClause +
                        " ORDER BY " + orderBy;

        try (Cursor cursor = db.rawQuery(
                query,
                null
        )) {
            while (cursor.moveToNext()) {
                items.add(
                        readNormalizedInventoryItemFromCursor(
                                cursor
                        )
                );
            }
        } finally {
            db.close();
        }

        return items;
    }

    public InventoryItem getNormalizedInventoryItemById(int itemId) {
        if (itemId <= 0) {
            return null;
        }

        SQLiteDatabase db = getReadableDatabase();

        String query =
                buildNormalizedInventorySelect() +
                        " WHERE i." +
                        COL_ITEM_ID +
                        " = ?";

        InventoryItem item = null;

        try (Cursor cursor = db.rawQuery(
                query,
                new String[]{String.valueOf(itemId)}
        )) {
            if (cursor.moveToFirst()) {
                item = readNormalizedInventoryItemFromCursor(
                        cursor
                );
            }
        } finally {
            db.close();
        }

        return item;
    }

    private String buildNormalizedInventorySelect() {
        // Keep the shared JOIN logic in one place so every inventory
        // query returns the same set of fields.
        return "SELECT " +
                "i." + COL_ITEM_ID + ", " +
                "i." + COL_MANUFACTURER_ID + ", " +
                "m." + COL_MANUFACTURER_NAME + ", " +
                "i." + COL_CATEGORY_ID + ", " +
                "c." + COL_CATEGORY_NAME + ", " +
                "i." + COL_LOCATION_ID + ", " +
                "l." + COL_WAREHOUSE_ROW + ", " +
                "l." + COL_WAREHOUSE_SHELF + ", " +
                "i." + COL_ITEM_NAME + ", " +
                "i." + COL_MODEL_NUMBER + ", " +
                "i." + COL_ITEM_SERIAL + ", " +
                "i." + COL_ITEM_SCU + ", " +
                "i." + COL_ITEM_QTY + ", " +
                "i." + COL_ITEM_THRESHOLD + ", " +
                "i." + COL_ITEM_NOTES + ", " +
                "i." + COL_ITEM_ACTIVE + " " +

                "FROM " + TABLE_INVENTORY_ITEMS + " i " +

                "INNER JOIN " +
                TABLE_MANUFACTURERS +
                " m ON i." +
                COL_MANUFACTURER_ID +
                " = m." +
                COL_MANUFACTURER_ID + " " +

                "INNER JOIN " +
                TABLE_CATEGORIES +
                " c ON i." +
                COL_CATEGORY_ID +
                " = c." +
                COL_CATEGORY_ID + " " +

                "LEFT JOIN " +
                TABLE_LOCATIONS +
                " l ON i." +
                COL_LOCATION_ID +
                " = l." +
                COL_LOCATION_ID;
    }

    // -------------------------
    // Inventory updates
    // -------------------------

    public boolean updateNormalizedInventoryDetails(
            int itemId,
            String name,
            String manufacturer,
            String category,
            String modelNumber,
            String serialNumber,
            String scuNumber,
            int newQuantity,
            int lowThreshold,
            int warehouseRow,
            int warehouseShelf,
            String notes,
            int userId
    ) {
        // A saved change needs a valid item, user, and quantity.
        if (itemId <= 0
                || userId <= 0
                || newQuantity < 0) {
            return false;
        }

        // These fields are required for a complete inventory record.
        if (isBlank(name)
                || isBlank(manufacturer)
                || isBlank(category)
                || isBlank(modelNumber)
                || isBlank(serialNumber)
                || isBlank(scuNumber)) {
            return false;
        }

        // Keep invalid threshold and warehouse values out of the database.
        if (lowThreshold < 0
                || warehouseRow <= 0
                || warehouseShelf <= 0) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
        boolean wasUpdated = false;

        // The item update and history record belong to the same operation.
        // If either one fails, neither change is kept.
        db.beginTransaction();

        try {
            Integer oldQuantity = null;

            // Read the current quantity before saving so history can show
            // the starting and ending values.
            try (Cursor cursor = db.query(
                    TABLE_INVENTORY_ITEMS,
                    new String[]{COL_ITEM_QTY},
                    COL_ITEM_ID + " = ?",
                    new String[]{String.valueOf(itemId)},
                    null,
                    null,
                    null
            )) {
                if (cursor.moveToFirst()) {
                    oldQuantity = cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                    COL_ITEM_QTY
                            )
                    );
                }
            }

            // Stop when the requested inventory item does not exist.
            if (oldQuantity == null) {
                return false;
            }

            long manufacturerId = getOrCreateManufacturerId(
                    db,
                    manufacturer.trim()
            );

            long categoryId = getOrCreateCategoryId(
                    db,
                    category.trim()
            );

            long locationId = getOrCreateLocationId(
                    db,
                    warehouseRow,
                    warehouseShelf
            );

            ContentValues values = new ContentValues();

            values.put(
                    COL_MANUFACTURER_ID,
                    manufacturerId
            );

            values.put(
                    COL_CATEGORY_ID,
                    categoryId
            );

            values.put(
                    COL_LOCATION_ID,
                    locationId
            );

            values.put(
                    COL_ITEM_NAME,
                    name.trim()
            );

            values.put(
                    COL_MODEL_NUMBER,
                    modelNumber.trim()
            );

            values.put(
                    COL_ITEM_SERIAL,
                    serialNumber.trim()
            );

            values.put(
                    COL_ITEM_SCU,
                    scuNumber.trim()
            );

            // Quantity is now saved from the details screen.
            values.put(
                    COL_ITEM_QTY,
                    newQuantity
            );

            values.put(
                    COL_ITEM_THRESHOLD,
                    lowThreshold
            );

            values.put(
                    COL_ITEM_NOTES,
                    notes == null ? "" : notes.trim()
            );

            int rows = db.update(
                    TABLE_INVENTORY_ITEMS,
                    values,
                    COL_ITEM_ID + " = ?",
                    new String[]{
                            String.valueOf(itemId)
                    }
            );

            if (rows <= 0) {
                return false;
            }

            long transactionId;

            if (oldQuantity != newQuantity) {
                // Save one completed quantity change instead of one row per button tap.
                transactionId = recordInventoryTransaction(
                        db,
                        itemId,
                        userId,
                        TRANSACTION_QUANTITY,
                        oldQuantity,
                        newQuantity,
                        "Quantity changed from " +
                                oldQuantity +
                                " to " +
                                newQuantity +
                                "."
                );
            } else {
                // Record a normal details update when quantity stayed the same.
                transactionId = recordInventoryTransaction(
                        db,
                        itemId,
                        userId,
                        TRANSACTION_UPDATE,
                        null,
                        null,
                        "Item details were updated."
                );
            }

            if (transactionId <= 0) {
                return false;
            }

            db.setTransactionSuccessful();
            wasUpdated = true;
        } catch (RuntimeException exception) {
            // Leave the original data unchanged if any part of the save fails.
            wasUpdated = false;
        } finally {
            db.endTransaction();
            db.close();
        }

        return wasUpdated;
    }

    public boolean updateNormalizedInventoryQuantity(
            int itemId,
            int newQuantity,
            int userId
    ) {
        // A quantity change needs a valid item, user, and nonnegative quantity.
        if (itemId <= 0
                || userId <= 0
                || newQuantity < 0) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
        boolean wasUpdated = false;

        // Update the quantity and write its history record together.
        // If either part fails, the original quantity stays unchanged.
        db.beginTransaction();

        try {
            Integer oldQuantity = null;

            // Read the current quantity before changing it so the history
            // can show exactly what the user changed.
            try (Cursor cursor = db.query(
                    TABLE_INVENTORY_ITEMS,
                    new String[]{COL_ITEM_QTY},
                    COL_ITEM_ID + " = ?",
                    new String[]{String.valueOf(itemId)},
                    null,
                    null,
                    null
            )) {
                if (cursor.moveToFirst()) {
                    oldQuantity = cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                    COL_ITEM_QTY
                            )
                    );
                }
            }

            // Do not continue when the inventory item was not found.
            if (oldQuantity == null) {
                return false;
            }

            // There is no reason to create a history row when the value did not change.
            if (oldQuantity == newQuantity) {
                return true;
            }

            ContentValues values = new ContentValues();

            values.put(
                    COL_ITEM_QTY,
                    newQuantity
            );

            int rows = db.update(
                    TABLE_INVENTORY_ITEMS,
                    values,
                    COL_ITEM_ID + " = ?",
                    new String[]{String.valueOf(itemId)}
            );

            if (rows <= 0) {
                return false;
            }

            // Record the same quantity change before finishing the transaction.
            long transactionId = recordInventoryTransaction(
                    db,
                    itemId,
                    userId,
                    TRANSACTION_QUANTITY,
                    oldQuantity,
                    newQuantity,
                    "Quantity changed from " +
                            oldQuantity +
                            " to " +
                            newQuantity +
                            "."
            );

            if (transactionId <= 0) {
                return false;
            }

            db.setTransactionSuccessful();
            wasUpdated = true;
        } catch (RuntimeException exception) {
            // Keep the database unchanged when either operation fails.
            wasUpdated = false;
        } finally {
            db.endTransaction();
            db.close();
        }

        return wasUpdated;
    }

    public boolean deactivateNormalizedInventoryItem(
            int itemId,
            int userId
    ) {
        return setInventoryItemActiveStatus(
                itemId,
                userId,
                false
        );
    }

    public boolean reactivateNormalizedInventoryItem(
            int itemId,
            int userId
    ) {
        return setInventoryItemActiveStatus(
                itemId,
                userId,
                true
        );
    }

    private boolean setInventoryItemActiveStatus(
            int itemId,
            int userId,
            boolean isActive
    ) {
        // A status change needs a valid inventory item and signed-in user.
        if (itemId <= 0 || userId <= 0) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
        boolean wasUpdated = false;

        // Save the active status and its history record together.
        // If either part fails, the original status stays unchanged.
        db.beginTransaction();

        try {
            Integer currentStatus = null;

            // Read the current value first so we do not record the same
            // status more than once.
            try (Cursor cursor = db.query(
                    TABLE_INVENTORY_ITEMS,
                    new String[]{COL_ITEM_ACTIVE},
                    COL_ITEM_ID + " = ?",
                    new String[]{String.valueOf(itemId)},
                    null,
                    null,
                    null
            )) {
                if (cursor.moveToFirst()) {
                    currentStatus = cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                    COL_ITEM_ACTIVE
                            )
                    );
                }
            }

            // Stop when the requested item does not exist.
            if (currentStatus == null) {
                return false;
            }

            int newStatus = isActive ? 1 : 0;

            // Do not add duplicate history when the item already has this status.
            if (currentStatus == newStatus) {
                return true;
            }

            ContentValues values = new ContentValues();

            values.put(
                    COL_ITEM_ACTIVE,
                    newStatus
            );

            int rows = db.update(
                    TABLE_INVENTORY_ITEMS,
                    values,
                    COL_ITEM_ID + " = ?",
                    new String[]{
                            String.valueOf(itemId)
                    }
            );

            if (rows <= 0) {
                return false;
            }

            String transactionType =
                    isActive
                            ? TRANSACTION_REACTIVATE
                            : TRANSACTION_DEACTIVATE;

            String transactionNote =
                    isActive
                            ? "Item returned to active inventory."
                            : "Item removed from active inventory.";

            long transactionId = recordInventoryTransaction(
                    db,
                    itemId,
                    userId,
                    transactionType,
                    null,
                    null,
                    transactionNote
            );

            if (transactionId <= 0) {
                return false;
            }

            db.setTransactionSuccessful();
            wasUpdated = true;
        } catch (RuntimeException exception) {
            // Keep the original item status when either update fails.
            wasUpdated = false;
        } finally {
            db.endTransaction();
            db.close();
        }

        return wasUpdated;
    }

    private boolean setInventoryItemActiveStatus(
            int itemId,
            boolean isActive
    ) {
        if (itemId <= 0) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(
                COL_ITEM_ACTIVE,
                isActive ? 1 : 0
        );

        int rows = db.update(
                TABLE_INVENTORY_ITEMS,
                values,
                COL_ITEM_ID + " = ?",
                new String[]{
                        String.valueOf(itemId)
                }
        );

        db.close();

        return rows > 0;
    }

    private InventoryItem readNormalizedInventoryItemFromCursor(
            Cursor cursor
    ) {
        int id = cursor.getInt(
                cursor.getColumnIndexOrThrow(
                        COL_ITEM_ID
                )
        );

        long manufacturerId = cursor.getLong(
                cursor.getColumnIndexOrThrow(
                        COL_MANUFACTURER_ID
                )
        );

        String manufacturerName = cursor.getString(
                cursor.getColumnIndexOrThrow(
                        COL_MANUFACTURER_NAME
                )
        );

        long categoryId = cursor.getLong(
                cursor.getColumnIndexOrThrow(
                        COL_CATEGORY_ID
                )
        );

        String categoryName = cursor.getString(
                cursor.getColumnIndexOrThrow(
                        COL_CATEGORY_NAME
                )
        );

        int locationIndex = cursor.getColumnIndexOrThrow(
                COL_LOCATION_ID
        );

        long locationId =
                cursor.isNull(locationIndex)
                        ? 0
                        : cursor.getLong(locationIndex);

        int rowIndex = cursor.getColumnIndexOrThrow(
                COL_WAREHOUSE_ROW
        );

        int warehouseRow =
                cursor.isNull(rowIndex)
                        ? 0
                        : cursor.getInt(rowIndex);

        int shelfIndex = cursor.getColumnIndexOrThrow(
                COL_WAREHOUSE_SHELF
        );

        int warehouseShelf =
                cursor.isNull(shelfIndex)
                        ? 0
                        : cursor.getInt(shelfIndex);

        String name = cursor.getString(
                cursor.getColumnIndexOrThrow(
                        COL_ITEM_NAME
                )
        );

        String modelNumber = cursor.getString(
                cursor.getColumnIndexOrThrow(
                        COL_MODEL_NUMBER
                )
        );

        String serialNumber = cursor.getString(
                cursor.getColumnIndexOrThrow(
                        COL_ITEM_SERIAL
                )
        );

        String scuNumber = cursor.getString(
                cursor.getColumnIndexOrThrow(
                        COL_ITEM_SCU
                )
        );

        int quantity = cursor.getInt(
                cursor.getColumnIndexOrThrow(
                        COL_ITEM_QTY
                )
        );

        int lowThreshold = cursor.getInt(
                cursor.getColumnIndexOrThrow(
                        COL_ITEM_THRESHOLD
                )
        );

        String notes = cursor.getString(
                cursor.getColumnIndexOrThrow(
                        COL_ITEM_NOTES
                )
        );

        boolean isActive = cursor.getInt(
                cursor.getColumnIndexOrThrow(
                        COL_ITEM_ACTIVE
                )
        ) == 1;

        return new InventoryItem(
                id,
                manufacturerId,
                manufacturerName,
                categoryId,
                categoryName,
                locationId,
                warehouseRow,
                warehouseShelf,
                name,
                modelNumber,
                serialNumber,
                scuNumber,
                quantity,
                lowThreshold,
                notes,
                isActive
        );
    }

    // -------------------------
    // Password security helpers
    // -------------------------

    private String hashPassword(String password) {
        if (isBlank(password)) {
            return null;
        }

        // Each password gets its own random salt.
        byte[] salt = new byte[PASSWORD_SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        byte[] hash = createPasswordHash(
                password.toCharArray(),
                salt,
                PASSWORD_ITERATIONS
        );

        if (hash == null) {
            return null;
        }

        String encodedSalt = Base64.encodeToString(
                salt,
                Base64.NO_WRAP
        );

        String encodedHash = Base64.encodeToString(
                hash,
                Base64.NO_WRAP
        );

        // Store the settings, salt, and hash together so login can verify it later.
        return PASSWORD_ITERATIONS
                + PASSWORD_SEPARATOR
                + encodedSalt
                + PASSWORD_SEPARATOR
                + encodedHash;
    }

    private boolean verifyPassword(
            String enteredPassword,
            String storedPassword
    ) {
        if (isBlank(enteredPassword) || isBlank(storedPassword)) {
            return false;
        }

        String[] parts = storedPassword.split(
                PASSWORD_SEPARATOR
        );

        // Older accounts may still have a plaintext password.
        if (parts.length != 3) {
            return MessageDigest.isEqual(
                    enteredPassword.getBytes(StandardCharsets.UTF_8),
                    storedPassword.getBytes(StandardCharsets.UTF_8)
            );
        }

        try {
            int iterations = Integer.parseInt(parts[0]);

            byte[] salt = Base64.decode(
                    parts[1],
                    Base64.NO_WRAP
            );

            byte[] savedHash = Base64.decode(
                    parts[2],
                    Base64.NO_WRAP
            );

            byte[] enteredHash = createPasswordHash(
                    enteredPassword.toCharArray(),
                    salt,
                    iterations
            );

            return enteredHash != null
                    && MessageDigest.isEqual(
                    enteredHash,
                    savedHash
            );

        } catch (
                IllegalArgumentException exception
        ) {
            return false;
        }
    }

    private byte[] createPasswordHash(
            char[] password,
            byte[] salt,
            int iterations
    ) {
        PBEKeySpec keySpec = new PBEKeySpec(
                password,
                salt,
                iterations,
                PASSWORD_KEY_LENGTH
        );

        try {
            SecretKeyFactory keyFactory =
                    SecretKeyFactory.getInstance(
                            PASSWORD_ALGORITHM
                    );

            return keyFactory.generateSecret(
                    keySpec
            ).getEncoded();

        } catch (
                NoSuchAlgorithmException
                | InvalidKeySpecException exception
        ) {
            return null;

        } finally {
            // Clear the password copy when hashing is finished.
            keySpec.clearPassword();
        }
    }

    private boolean isHashedPassword(String storedPassword) {
        if (storedPassword == null) {
            return false;
        }

        String[] parts = storedPassword.split(
                PASSWORD_SEPARATOR
        );

        return parts.length == 3;
    }

    private boolean replacePlaintextPassword(
            int userId,
            String password
    ) {
        String hashedPassword = hashPassword(password);

        if (userId <= 0 || hashedPassword == null) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(
                COL_PASSWORD,
                hashedPassword
        );

        int rows = db.update(
                TABLE_USERS,
                values,
                COL_USER_ID + " = ?",
                new String[]{
                        String.valueOf(userId)
                }
        );

        db.close();

        return rows > 0;
    }

    // -------------------------
    // User account and login helpers
    // -------------------------

    public boolean createUser(
            String username,
            String password,
            String firstName,
            String lastName,
            String title,
            String email
    ) {
        // Username, password, and email are required.
        if (isBlank(username)
                || isBlank(password)
                || isBlank(email)) {
            return false;
        }

        // Keep duplicate usernames out of the users table.
        if (doesUsernameExist(username.trim())) {
            return false;
        }

        // Store a salted password hash instead of the original password.
        String hashedPassword = hashPassword(password);

        if (hashedPassword == null) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(
                COL_USERNAME,
                username.trim()
        );

        values.put(
                COL_PASSWORD,
                hashedPassword
        );

        values.put(
                COL_EMAIL,
                email.trim()
        );

        values.put(
                COL_FIRST_NAME,
                firstName == null ? "" : firstName.trim()
        );

        values.put(
                COL_LAST_NAME,
                lastName == null ? "" : lastName.trim()
        );

        values.put(
                COL_TITLE,
                title == null ? "" : title.trim()
        );

        long result = db.insert(
                TABLE_USERS,
                null,
                values
        );

        db.close();

        return result != -1;
    }

    public boolean checkUserCredentials(
            String username,
            String password
    ) {
        // Keep this method available for anything that still expects
        // a basic true or false login result.
        return getUserSession(
                username,
                password
        ) != null;
    }

    public UserSession getUserSession(
            String username,
            String password
    ) {
        // Do not query the database when either login field is blank.
        if (isBlank(username) || isBlank(password)) {
            return null;
        }

        SQLiteDatabase db = getReadableDatabase();

        UserSession session = null;
        int userId = -1;
        String savedUsername = null;
        String storedPassword = null;

        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{
                        COL_USER_ID,
                        COL_USERNAME,
                        COL_PASSWORD
                },
                COL_USERNAME + " = ? COLLATE NOCASE",
                new String[]{
                        username.trim()
                },
                null,
                null,
                null
        )) {
            if (cursor.moveToFirst()) {
                userId = cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                                COL_USER_ID
                        )
                );

                savedUsername = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                COL_USERNAME
                        )
                );

                storedPassword = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                COL_PASSWORD
                        )
                );
            }
        } finally {
            db.close();
        }

        // Stop when the username does not exist.
        if (userId <= 0
                || savedUsername == null
                || storedPassword == null) {
            return null;
        }

        // Compare the entered password with either the saved hash
        // or an older plaintext password.
        boolean passwordMatches = verifyPassword(
                password,
                storedPassword
        );

        if (!passwordMatches) {
            return null;
        }

        // Older accounts are upgraded to hashed storage after a valid login.
        if (!isHashedPassword(storedPassword)) {
            replacePlaintextPassword(
                    userId,
                    password
            );
        }

        // Keep the ID and username together for the active session.
        session = new UserSession(
                userId,
                savedUsername
        );

        return session;
    }

    private boolean doesUsernameExist(String username) {
        SQLiteDatabase db = getReadableDatabase();

        boolean exists;

        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COL_USER_ID},
                COL_USERNAME + " = ? COLLATE NOCASE",
                new String[]{username.trim()},
                null,
                null,
                null
        )) {
            exists = cursor.moveToFirst();
        } finally {
            db.close();
        }

        return exists;
    }

    private boolean isBlank(String value) {
        return value == null
                || value.trim().isEmpty();
    }
    public ArrayList<InventoryTransaction> getInventoryTransactions() {
        ArrayList<InventoryTransaction> transactions = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // Join the transaction, inventory, and user tables so the history
        // screen can show readable item names and usernames instead of IDs.
        String query =
                "SELECT " +
                        "t." + COL_TRANSACTION_ID + ", " +
                        "t." + COL_TRANSACTION_ITEM_ID + ", " +
                        "t." + COL_TRANSACTION_USER_ID + ", " +
                        "t." + COL_TRANSACTION_TYPE + ", " +
                        "t." + COL_OLD_QUANTITY + ", " +
                        "t." + COL_NEW_QUANTITY + ", " +
                        "t." + COL_TRANSACTION_NOTE + ", " +
                        "t." + COL_TRANSACTION_DATE + ", " +
                        "i." + COL_ITEM_NAME + ", " +
                        "u." + COL_USERNAME + " " +

                        "FROM " + TABLE_INVENTORY_TRANSACTIONS + " t " +

                        "INNER JOIN " + TABLE_INVENTORY_ITEMS + " i ON " +
                        "t." + COL_TRANSACTION_ITEM_ID + " = " +
                        "i." + COL_ITEM_ID + " " +

                        "INNER JOIN " + TABLE_USERS + " u ON " +
                        "t." + COL_TRANSACTION_USER_ID + " = " +
                        "u." + COL_USER_ID + " " +

                        // Show the newest transaction first.
                        "ORDER BY t." + COL_TRANSACTION_ID + " DESC";

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                int oldQuantityIndex = cursor.getColumnIndexOrThrow(
                        COL_OLD_QUANTITY
                );

                int newQuantityIndex = cursor.getColumnIndexOrThrow(
                        COL_NEW_QUANTITY
                );

                // Some transaction types do not have quantity values.
                Integer oldQuantity =
                        cursor.isNull(oldQuantityIndex)
                                ? null
                                : cursor.getInt(oldQuantityIndex);

                Integer newQuantity =
                        cursor.isNull(newQuantityIndex)
                                ? null
                                : cursor.getInt(newQuantityIndex);

                InventoryTransaction transaction =
                        new InventoryTransaction(
                                cursor.getInt(
                                        cursor.getColumnIndexOrThrow(
                                                COL_TRANSACTION_ID
                                        )
                                ),
                                cursor.getInt(
                                        cursor.getColumnIndexOrThrow(
                                                COL_TRANSACTION_ITEM_ID
                                        )
                                ),
                                cursor.getInt(
                                        cursor.getColumnIndexOrThrow(
                                                COL_TRANSACTION_USER_ID
                                        )
                                ),
                                cursor.getString(
                                        cursor.getColumnIndexOrThrow(
                                                COL_TRANSACTION_TYPE
                                        )
                                ),
                                oldQuantity,
                                newQuantity,
                                cursor.getString(
                                        cursor.getColumnIndexOrThrow(
                                                COL_TRANSACTION_NOTE
                                        )
                                ),
                                cursor.getString(
                                        cursor.getColumnIndexOrThrow(
                                                COL_TRANSACTION_DATE
                                        )
                                ),
                                cursor.getString(
                                        cursor.getColumnIndexOrThrow(
                                                COL_ITEM_NAME
                                        )
                                ),
                                cursor.getString(
                                        cursor.getColumnIndexOrThrow(
                                                COL_USERNAME
                                        )
                                )
                        );

                transactions.add(transaction);
            }
        } finally {
            db.close();
        }

        return transactions;
    }

    // -------------------------
    // Inventory item model
    // -------------------------

    public static class InventoryItem {

        public int id;

        public long manufacturerId;
        public long categoryId;
        public long locationId;

        public String manufacturerName;
        public String categoryName;

        public int warehouseRow;
        public int warehouseShelf;

        public String name;
        public String modelNumber;
        public String serialNumber;
        public String scuNumber;

        public int quantity;
        public int lowThreshold;

        public String notes;
        public boolean isActive;

        public InventoryItem(
                int id,
                long manufacturerId,
                String manufacturerName,
                long categoryId,
                String categoryName,
                long locationId,
                int warehouseRow,
                int warehouseShelf,
                String name,
                String modelNumber,
                String serialNumber,
                String scuNumber,
                int quantity,
                int lowThreshold,
                String notes,
                boolean isActive
        ) {
            this.id = id;

            this.manufacturerId = manufacturerId;
            this.manufacturerName = manufacturerName;

            this.categoryId = categoryId;
            this.categoryName = categoryName;

            this.locationId = locationId;
            this.warehouseRow = warehouseRow;
            this.warehouseShelf = warehouseShelf;

            this.name = name;
            this.modelNumber = modelNumber;
            this.serialNumber = serialNumber;
            this.scuNumber = scuNumber;

            this.quantity = quantity;
            this.lowThreshold = lowThreshold;

            this.notes = notes;
            this.isActive = isActive;
        }
    }

    // -------------------------
    // Inventory history model
    // -------------------------

    public static class InventoryTransaction {

        public final int transactionId;
        public final int itemId;
        public final int userId;

        public final String transactionType;

        public final Integer oldQuantity;
        public final Integer newQuantity;

        public final String note;
        public final String transactionDate;

        public final String itemName;
        public final String username;

        public InventoryTransaction(
                int transactionId,
                int itemId,
                int userId,
                String transactionType,
                Integer oldQuantity,
                Integer newQuantity,
                String note,
                String transactionDate,
                String itemName,
                String username
        ) {
            this.transactionId = transactionId;
            this.itemId = itemId;
            this.userId = userId;

            this.transactionType = transactionType;

            this.oldQuantity = oldQuantity;
            this.newQuantity = newQuantity;

            this.note = note;
            this.transactionDate = transactionDate;

            this.itemName = itemName;
            this.username = username;
        }
    }

    // -------------------------
    // Logged-in user model
    // -------------------------

    public static class UserSession {

        public final int userId;
        public final String username;

        public UserSession(
                int userId,
                String username
        ) {
            this.userId = userId;
            this.username = username;
        }
    }
}