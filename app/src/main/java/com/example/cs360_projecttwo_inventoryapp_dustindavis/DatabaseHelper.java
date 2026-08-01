package com.example.cs360_projecttwo_inventoryapp_dustindavis;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Basic database settings
    private static final String DATABASE_NAME = "inventory_app.db";
    private static final int DATABASE_VERSION = 5;

    // Users table (login plus optional profile info)
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";
    private static final String COL_FIRST_NAME = "first_name";
    private static final String COL_LAST_NAME = "last_name";
    private static final String COL_TITLE = "title";
    private static final String COL_EMAIL = "email";

    // Normalized lookup tables
    private static final String TABLE_MANUFACTURERS = "manufacturers";
    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_LOCATIONS = "locations";
    private static final String TABLE_INVENTORY_ITEMS = "inventory_items";

    // Inventory item table columns
    private static final String COL_ITEM_ID = "id";
    private static final String COL_ITEM_NAME = "item_name";
    private static final String COL_ITEM_QTY = "quantity";
    private static final String COL_ITEM_THRESHOLD = "low_threshold";

    // Additional inventory item detail columns
    private static final String COL_ITEM_SERIAL = "serial_number";
    private static final String COL_ITEM_SCU = "scu_number";
    private static final String COL_ITEM_NOTES = "notes";

    // Manufacturer table columns
    private static final String COL_MANUFACTURER_ID = "manufacturer_id";
    private static final String COL_MANUFACTURER_NAME = "manufacturer_name";

    // Category table columns
    private static final String COL_CATEGORY_ID = "category_id";
    private static final String COL_CATEGORY_NAME = "category_name";

    // Warehouse location table columns
    private static final String COL_LOCATION_ID = "location_id";
    private static final String COL_WAREHOUSE_ROW = "warehouse_row";
    private static final String COL_WAREHOUSE_SHELF = "warehouse_shelf";

    // Additional normalized inventory item columns
    private static final String COL_ITEM_ACTIVE = "is_active";
    private static final String COL_MODEL_NUMBER = "model_number";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);

        // Turn on foreign key checks so inventory items cannot reference
        // manufacturers, categories, or locations that do not exist.
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Users table rules:
        // Required: username, password, email
        // Optional: first name, last name, title
        String createUsersTable =
                "CREATE TABLE " + TABLE_USERS + " (" +
                        COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_USERNAME + " TEXT NOT NULL UNIQUE, " +
                        COL_PASSWORD + " TEXT NOT NULL, " +
                        COL_FIRST_NAME + " TEXT, " +
                        COL_LAST_NAME + " TEXT, " +
                        COL_TITLE + " TEXT, " +
                        COL_EMAIL + " TEXT NOT NULL" +
                        ");";

        // These lookup tables let us normalize the inventory database instead of
        // storing the same manufacturer, category, and location information over
        // and over again with every inventory item.

        String createManufacturersTable =
                "CREATE TABLE " + TABLE_MANUFACTURERS + " (" +
                        COL_MANUFACTURER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_MANUFACTURER_NAME +
                        " TEXT NOT NULL UNIQUE COLLATE NOCASE" +
                        ");";

        String createCategoriesTable =
                "CREATE TABLE " + TABLE_CATEGORIES + " (" +
                        COL_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_CATEGORY_NAME +
                        " TEXT NOT NULL UNIQUE COLLATE NOCASE" +
                        ");";

        // Store warehouse locations one time and let inventory items reference them.
        // This keeps the database cleaner and makes locations easier to manage later.
        String createLocationsTable =
                "CREATE TABLE " + TABLE_LOCATIONS + " (" +
                        COL_LOCATION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_WAREHOUSE_ROW + " " +
                        "INTEGER NOT NULL CHECK (" + COL_WAREHOUSE_ROW + " > 0), " +
                        COL_WAREHOUSE_SHELF + " " +
                        "INTEGER NOT NULL CHECK (" + COL_WAREHOUSE_SHELF + " > 0), " +
                        "UNIQUE (" + COL_WAREHOUSE_ROW + ", " +
                        COL_WAREHOUSE_SHELF + ")" +
                        ");";

        // This is the normalized replacement for the original inventory table.
        // Manufacturer, category, and warehouse location are connected through
        // foreign keys instead of repeating the same text for every item.
        String createInventoryItemsTable =
                "CREATE TABLE " + TABLE_INVENTORY_ITEMS + " (" +
                        COL_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_MANUFACTURER_ID + " INTEGER NOT NULL, " +
                        COL_CATEGORY_ID + " INTEGER NOT NULL, " +
                        COL_LOCATION_ID + " INTEGER, " +
                        COL_ITEM_NAME + " TEXT NOT NULL, " +
                        COL_MODEL_NUMBER + " TEXT NOT NULL, " +
                        COL_ITEM_SERIAL + " TEXT NOT NULL UNIQUE, " +
                        COL_ITEM_SCU + " TEXT NOT NULL UNIQUE, " +
                        COL_ITEM_QTY + " INTEGER NOT NULL DEFAULT 0 " +
                        "CHECK (" + COL_ITEM_QTY + " >= 0), " +
                        COL_ITEM_THRESHOLD + " INTEGER NOT NULL DEFAULT 0 " +
                        "CHECK (" + COL_ITEM_THRESHOLD + " >= 0), " +
                        COL_ITEM_NOTES + " TEXT, " +
                        COL_ITEM_ACTIVE + " INTEGER NOT NULL DEFAULT 1 " +
                        "CHECK (" + COL_ITEM_ACTIVE + " IN (0, 1)), " +

                        "FOREIGN KEY (" + COL_MANUFACTURER_ID + ") REFERENCES " +
                        TABLE_MANUFACTURERS + "(" + COL_MANUFACTURER_ID + "), " +

                        "FOREIGN KEY (" + COL_CATEGORY_ID + ") REFERENCES " +
                        TABLE_CATEGORIES + "(" + COL_CATEGORY_ID + "), " +

                        "FOREIGN KEY (" + COL_LOCATION_ID + ") REFERENCES " +
                        TABLE_LOCATIONS + "(" + COL_LOCATION_ID + ")" +
                        ");";

        db.execSQL(createUsersTable);

        // Create the normalized lookup tables that future inventory records will use.
        db.execSQL(createManufacturersTable);
        db.execSQL(createCategoriesTable);
        db.execSQL(createLocationsTable);
        db.execSQL(createInventoryItemsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Drop the inventory item table first because it will eventually depend on
        // the manufacturer, category, and location tables through foreign keys.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY_ITEMS);

        // Remove the normalized lookup tables before rebuilding the database.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MANUFACTURERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);

        // Recreate the database using the newest table definitions.
        onCreate(db);
    }

    private long getOrCreateManufacturerId(SQLiteDatabase db, String manufacturerName) {
        // Look for an existing manufacturer before adding a new one.
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
                        cursor.getColumnIndexOrThrow(COL_MANUFACTURER_ID)
                );
            }
        }

        // Add the manufacturer only when it is not already in the table.
        ContentValues values = new ContentValues();
        values.put(COL_MANUFACTURER_NAME, manufacturerName);

        return db.insertOrThrow(TABLE_MANUFACTURERS, null, values);
    }

    private long getOrCreateCategoryId(SQLiteDatabase db, String categoryName) {
        // Look for an existing category before adding a new one.
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
                        cursor.getColumnIndexOrThrow(COL_CATEGORY_ID)
                );
            }
        }

        // Add the category only when it is not already in the table.
        ContentValues values = new ContentValues();
        values.put(COL_CATEGORY_NAME, categoryName);

        return db.insertOrThrow(TABLE_CATEGORIES, null, values);
    }

    private long getOrCreateLocationId(
            SQLiteDatabase db,
            int warehouseRow,
            int warehouseShelf
    ) {
        // Look for the exact row and shelf combination before creating a new location.
        try (Cursor cursor = db.query(
                TABLE_LOCATIONS,
                new String[]{COL_LOCATION_ID},
                COL_WAREHOUSE_ROW + " = ? AND " + COL_WAREHOUSE_SHELF + " = ?",
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
                        cursor.getColumnIndexOrThrow(COL_LOCATION_ID)
                );
            }
        }

        // Add the warehouse location only when that row and shelf do not already exist.
        ContentValues values = new ContentValues();
        values.put(COL_WAREHOUSE_ROW, warehouseRow);
        values.put(COL_WAREHOUSE_SHELF, warehouseShelf);

        return db.insertOrThrow(TABLE_LOCATIONS, null, values);
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
            String notes
    ) {
        // Stop the insert when any required inventory information is missing.
        if (isBlank(name)
                || isBlank(manufacturer)
                || isBlank(category)
                || isBlank(modelNumber)
                || isBlank(serialNumber)
                || isBlank(scuNumber)) {
            return -1;
        }

        // Quantities and warehouse positions should never use negative or zero values.
        if (quantity < 0
                || lowThreshold < 0
                || warehouseRow <= 0
                || warehouseShelf <= 0) {
            return -1;
        }

        SQLiteDatabase db = getWritableDatabase();
        long newItemId = -1;

        // The lookup records and inventory item all belong to the same operation.
        // Using a transaction prevents a partial record if one of the inserts fails.
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
            values.put(COL_MANUFACTURER_ID, manufacturerId);
            values.put(COL_CATEGORY_ID, categoryId);
            values.put(COL_LOCATION_ID, locationId);
            values.put(COL_ITEM_NAME, name.trim());
            values.put(COL_MODEL_NUMBER, modelNumber.trim());
            values.put(COL_ITEM_SERIAL, serialNumber.trim());
            values.put(COL_ITEM_SCU, scuNumber.trim());
            values.put(COL_ITEM_QTY, quantity);
            values.put(COL_ITEM_THRESHOLD, lowThreshold);

            // Notes are optional, so store an empty value when nothing was entered.
            values.put(
                    COL_ITEM_NOTES,
                    notes == null ? "" : notes.trim()
            );

            newItemId = db.insertOrThrow(
                    TABLE_INVENTORY_ITEMS,
                    null,
                    values
            );

            // Only keep the lookup records and item when every step succeeded.
            db.setTransactionSuccessful();
        } catch (RuntimeException exception) {
            // Leave the result as -1 so the screen can report that the item was not saved.
            newItemId = -1;
        } finally {
            db.endTransaction();
            db.close();
        }

        return newItemId;
    }
    public ArrayList<InventoryItem> getAllNormalizedInventoryItems() {
        ArrayList<InventoryItem> items = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        // Join the lookup tables so each item comes back with complete display values.
        String query =
                "SELECT " +
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

                        "INNER JOIN " + TABLE_MANUFACTURERS + " m ON " +
                        "i." + COL_MANUFACTURER_ID + " = " +
                        "m." + COL_MANUFACTURER_ID + " " +

                        "INNER JOIN " + TABLE_CATEGORIES + " c ON " +
                        "i." + COL_CATEGORY_ID + " = " +
                        "c." + COL_CATEGORY_ID + " " +

                        "LEFT JOIN " + TABLE_LOCATIONS + " l ON " +
                        "i." + COL_LOCATION_ID + " = " +
                        "l." + COL_LOCATION_ID + " " +

                        // Only return active items for the normal inventory view.
                        "WHERE i." + COL_ITEM_ACTIVE + " = 1 " +

                        // Keep the list consistent by sorting item names without case sensitivity.
                        "ORDER BY i." + COL_ITEM_NAME + " COLLATE NOCASE ASC";

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                items.add(readNormalizedInventoryItemFromCursor(cursor));
            }
        } finally {
            db.close();
        }

        return items;
    }
    public InventoryItem getNormalizedInventoryItemById(int itemId) {
        // Item IDs should always be positive before we query the database.
        if (itemId <= 0) {
            return null;
        }

        SQLiteDatabase db = getReadableDatabase();

        // Join the lookup tables so the details screen gets the full item record.
        String query =
                "SELECT " +
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

                        "INNER JOIN " + TABLE_MANUFACTURERS + " m ON " +
                        "i." + COL_MANUFACTURER_ID + " = " +
                        "m." + COL_MANUFACTURER_ID + " " +

                        "INNER JOIN " + TABLE_CATEGORIES + " c ON " +
                        "i." + COL_CATEGORY_ID + " = " +
                        "c." + COL_CATEGORY_ID + " " +

                        "LEFT JOIN " + TABLE_LOCATIONS + " l ON " +
                        "i." + COL_LOCATION_ID + " = " +
                        "l." + COL_LOCATION_ID + " " +

                        "WHERE i." + COL_ITEM_ID + " = ?";

        InventoryItem item = null;

        try (Cursor cursor = db.rawQuery(
                query,
                new String[] { String.valueOf(itemId) }
        )) {
            if (cursor.moveToFirst()) {
                item = readNormalizedInventoryItemFromCursor(cursor);
            }
        } finally {
            db.close();
        }

        return item;
    }
    public boolean updateNormalizedInventoryDetails(
            int itemId,
            String name,
            String manufacturer,
            String category,
            String modelNumber,
            String serialNumber,
            String scuNumber,
            int lowThreshold,
            int warehouseRow,
            int warehouseShelf,
            String notes
    ) {
        // Do not run an update without a valid inventory item ID.
        if (itemId <= 0) {
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

        // Prevent invalid threshold and warehouse position values.
        if (lowThreshold < 0
                || warehouseRow <= 0
                || warehouseShelf <= 0) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
        boolean wasUpdated = false;

        // The lookup records and inventory update need to succeed together.
        // The transaction prevents the item from being left partly updated.
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
            values.put(COL_MANUFACTURER_ID, manufacturerId);
            values.put(COL_CATEGORY_ID, categoryId);
            values.put(COL_LOCATION_ID, locationId);
            values.put(COL_ITEM_NAME, name.trim());
            values.put(COL_MODEL_NUMBER, modelNumber.trim());
            values.put(COL_ITEM_SERIAL, serialNumber.trim());
            values.put(COL_ITEM_SCU, scuNumber.trim());
            values.put(COL_ITEM_THRESHOLD, lowThreshold);

            // Notes are optional, so keep an empty value when nothing was entered.
            values.put(
                    COL_ITEM_NOTES,
                    notes == null ? "" : notes.trim()
            );

            int rows = db.update(
                    TABLE_INVENTORY_ITEMS,
                    values,
                    COL_ITEM_ID + " = ?",
                    new String[] { String.valueOf(itemId) }
            );

            wasUpdated = rows > 0;

            if (wasUpdated) {
                db.setTransactionSuccessful();
            }
        } catch (RuntimeException exception) {
            // Return false when a duplicate or another database error blocks the update.
            wasUpdated = false;
        } finally {
            db.endTransaction();
            db.close();
        }

        return wasUpdated;
    }
    public boolean updateNormalizedInventoryQuantity(
            int itemId,
            int newQuantity
    ) {
        // Do not update an inventory item without a valid ID.
        if (itemId <= 0) {
            return false;
        }

        // Inventory quantities should never be negative.
        if (newQuantity < 0) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_ITEM_QTY, newQuantity);

        int rows = db.update(
                TABLE_INVENTORY_ITEMS,
                values,
                COL_ITEM_ID + " = ?",
                new String[] { String.valueOf(itemId) }
        );

        db.close();

        return rows > 0;
    }
    public boolean deactivateNormalizedInventoryItem(int itemId) {
        // Do not change a record without a valid inventory item ID.
        if (itemId <= 0) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        // Keep the inventory record in the database, but hide it from active views.
        values.put(COL_ITEM_ACTIVE, 0);

        int rows = db.update(
                TABLE_INVENTORY_ITEMS,
                values,
                COL_ITEM_ID + " = ?",
                new String[] { String.valueOf(itemId) }
        );

        db.close();

        return rows > 0;
    }
    public boolean reactivateNormalizedInventoryItem(int itemId) {
        // Do not change a record without a valid inventory item ID.
        if (itemId <= 0) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        // Put the item back into the active inventory without recreating the record.
        values.put(COL_ITEM_ACTIVE, 1);

        int rows = db.update(
                TABLE_INVENTORY_ITEMS,
                values,
                COL_ITEM_ID + " = ?",
                new String[] { String.valueOf(itemId) }
        );

        db.close();

        return rows > 0;
    }
    public ArrayList<InventoryItem> getInactiveNormalizedInventoryItems() {
        ArrayList<InventoryItem> items = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        // Join the lookup tables so archived items still include all of their
        // manufacturer, category, and warehouse location information.
        String query =
                "SELECT " +
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

                        "INNER JOIN " + TABLE_MANUFACTURERS + " m ON " +
                        "i." + COL_MANUFACTURER_ID + " = " +
                        "m." + COL_MANUFACTURER_ID + " " +

                        "INNER JOIN " + TABLE_CATEGORIES + " c ON " +
                        "i." + COL_CATEGORY_ID + " = " +
                        "c." + COL_CATEGORY_ID + " " +

                        "LEFT JOIN " + TABLE_LOCATIONS + " l ON " +
                        "i." + COL_LOCATION_ID + " = " +
                        "l." + COL_LOCATION_ID + " " +

                        // Only return records that have been removed from active inventory.
                        "WHERE i." + COL_ITEM_ACTIVE + " = 0 " +

                        // Keep the archived list predictable for the user.
                        "ORDER BY i." + COL_ITEM_NAME + " COLLATE NOCASE ASC";

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                items.add(readNormalizedInventoryItemFromCursor(cursor));
            }
        } finally {
            db.close();
        }

        return items;
    }
    public ArrayList<InventoryItem> getLowNormalizedInventoryItems() {
        ArrayList<InventoryItem> items = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        // Join the lookup tables so low inventory records include the complete
        // manufacturer, category, and warehouse location information.
        String query =
                "SELECT " +
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

                        "INNER JOIN " + TABLE_MANUFACTURERS + " m ON " +
                        "i." + COL_MANUFACTURER_ID + " = " +
                        "m." + COL_MANUFACTURER_ID + " " +

                        "INNER JOIN " + TABLE_CATEGORIES + " c ON " +
                        "i." + COL_CATEGORY_ID + " = " +
                        "c." + COL_CATEGORY_ID + " " +

                        "LEFT JOIN " + TABLE_LOCATIONS + " l ON " +
                        "i." + COL_LOCATION_ID + " = " +
                        "l." + COL_LOCATION_ID + " " +

                        // Only show active items that are at or below their reorder threshold.
                        "WHERE i." + COL_ITEM_ACTIVE + " = 1 AND " +
                        "i." + COL_ITEM_QTY + " <= i." + COL_ITEM_THRESHOLD + " " +

                        // Sort by quantity first so the items needing attention appear first.
                        "ORDER BY i." + COL_ITEM_QTY + " ASC, " +
                        "i." + COL_ITEM_NAME + " COLLATE NOCASE ASC";

        try (Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                items.add(readNormalizedInventoryItemFromCursor(cursor));
            }
        } finally {
            db.close();
        }

        return items;
    }
    private InventoryItem readNormalizedInventoryItemFromCursor(Cursor cursor) {
        // Keep the normalized cursor parsing in one place so every join stays consistent.
        int id = cursor.getInt(
                cursor.getColumnIndexOrThrow(COL_ITEM_ID)
        );

        long manufacturerId = cursor.getLong(
                cursor.getColumnIndexOrThrow(COL_MANUFACTURER_ID)
        );

        String manufacturerName = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_MANUFACTURER_NAME)
        );

        long categoryId = cursor.getLong(
                cursor.getColumnIndexOrThrow(COL_CATEGORY_ID)
        );

        String categoryName = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_CATEGORY_NAME)
        );

        long locationId = cursor.isNull(
                cursor.getColumnIndexOrThrow(COL_LOCATION_ID)
        )
                ? 0
                : cursor.getLong(
                cursor.getColumnIndexOrThrow(COL_LOCATION_ID)
        );

        int warehouseRow = cursor.isNull(
                cursor.getColumnIndexOrThrow(COL_WAREHOUSE_ROW)
        )
                ? 0
                : cursor.getInt(
                cursor.getColumnIndexOrThrow(COL_WAREHOUSE_ROW)
        );

        int warehouseShelf = cursor.isNull(
                cursor.getColumnIndexOrThrow(COL_WAREHOUSE_SHELF)
        )
                ? 0
                : cursor.getInt(
                cursor.getColumnIndexOrThrow(COL_WAREHOUSE_SHELF)
        );

        String name = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_ITEM_NAME)
        );

        String modelNumber = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_MODEL_NUMBER)
        );

        String serialNumber = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_ITEM_SERIAL)
        );

        String scuNumber = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_ITEM_SCU)
        );

        int quantity = cursor.getInt(
                cursor.getColumnIndexOrThrow(COL_ITEM_QTY)
        );

        int lowThreshold = cursor.getInt(
                cursor.getColumnIndexOrThrow(COL_ITEM_THRESHOLD)
        );

        String notes = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_ITEM_NOTES)
        );

        boolean isActive = cursor.getInt(
                cursor.getColumnIndexOrThrow(COL_ITEM_ACTIVE)
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
    // User login helpers
    // -------------------------

    public boolean createUser(String username,
                              String password,
                              String firstName,
                              String lastName,
                              String title,
                              String email) {

        // Required fields for account creation
        if (isBlank(username) || isBlank(password) || isBlank(email)) {
            return false;
        }

        // Stop duplicate usernames before attempting the insert
        if (doesUsernameExist(username.trim())) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username.trim());
        values.put(COL_PASSWORD, password.trim());
        values.put(COL_EMAIL, email.trim());

        // Optional fields, store empty string when left blank
        values.put(COL_FIRST_NAME, firstName == null ? "" : firstName.trim());
        values.put(COL_LAST_NAME, lastName == null ? "" : lastName.trim());
        values.put(COL_TITLE, title == null ? "" : title.trim());

        long result = db.insert(TABLE_USERS, null, values);
        db.close();

        return result != -1;
    }

    public boolean checkUserCredentials(String username, String password) {

        // Skip the DB call if required inputs are missing
        if (isBlank(username) || isBlank(password)) {
            return false;
        }

        SQLiteDatabase db = getReadableDatabase();

        String selection = COL_USERNAME + " = ? AND " + COL_PASSWORD + " = ?";
        String[] selectionArgs = {username.trim(), password.trim()};

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COL_USER_ID},
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        boolean isValid = cursor != null && cursor.moveToFirst();

        if (cursor != null) {
            cursor.close();
        }
        db.close();

        return isValid;
    }

    private boolean doesUsernameExist(String username) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COL_USER_ID},
                COL_USERNAME + " = ?",
                new String[]{username.trim()},
                null,
                null,
                null
        );

        boolean exists = cursor != null && cursor.moveToFirst();

        if (cursor != null) {
            cursor.close();
        }
        db.close();

        return exists;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // -------------------------
    // Model object
    // -------------------------

    public static class InventoryItem {

        public int id;

        // These IDs connect the item back to the normalized lookup tables.
        public long manufacturerId;
        public long categoryId;
        public long locationId;

        // These values are returned from the lookup tables for display in the app.
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

        /**
         * This constructor supports inventory records returned from the new
         * normalized database structure.
         */
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
}
