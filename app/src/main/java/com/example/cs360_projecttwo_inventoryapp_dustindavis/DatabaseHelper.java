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

    // Inventory table
    private static final String TABLE_INVENTORY = "inventory";

    // Normalized lookup tables
    private static final String TABLE_MANUFACTURERS = "manufacturers";
    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_LOCATIONS = "locations";
    private static final String TABLE_INVENTORY_ITEMS = "inventory_items";

    // Existing inventory columns
    private static final String COL_ITEM_ID = "id";
    private static final String COL_ITEM_NAME = "item_name";
    private static final String COL_ITEM_QTY = "quantity";
    private static final String COL_ITEM_THRESHOLD = "low_threshold";

    // Item detail fields used on the details screen
    // Column name stays as "model" to avoid a schema migration in this class project
    private static final String COL_ITEM_MODEL = "model";
    private static final String COL_ITEM_SERIAL = "serial_number";
    private static final String COL_ITEM_SCU = "scu_number";
    private static final String COL_ITEM_LOCATION = "location";
    private static final String COL_ITEM_NOTES = "notes";

    // Manufacturer table columns
    private static final String COL_MANUFACTURER_ID = "manufacturer_id";
    private static final String COL_MANUFACTURER_NAME = "manufacturer_name";

    // Category location columns
    private static final String COL_CATEGORY_ID = "category_id";
    private static final String COL_CATEGORY_NAME = "category_name";

    // Warehouse location table columns
    private static final String COL_LOCATION_ID = "location_id";
    private static final String COL_WAREHOUSE_ROW = "warehouse_row";
    private static final String COL_WAREHOUSE_SHELF = "warehouse_shelf";

    // Additional normalized inventory item columns
    private static final String COL_ITEM_ACTIVE = "is_active";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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

        // Inventory table includes both list screen fields and details screen fields
        String createInventoryTable =
                "CREATE TABLE " + TABLE_INVENTORY + " (" +
                        COL_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_ITEM_NAME + " TEXT NOT NULL, " +
                        COL_ITEM_QTY + " INTEGER NOT NULL, " +
                        COL_ITEM_THRESHOLD + " INTEGER NOT NULL, " +
                        COL_ITEM_MODEL + " TEXT NOT NULL, " +
                        COL_ITEM_SERIAL + " TEXT NOT NULL, " +
                        COL_ITEM_SCU + " TEXT NOT NULL, " +
                        COL_ITEM_LOCATION + " TEXT, " +
                        COL_ITEM_NOTES + " TEXT" +
                        ");";

        // These lookup tables let us normalize the inventory database instead of
        // storing the same manufacturer, category, and location information over
        // and over again with every inventory item.

        String createManufacturersTable =
                "CREATE TABLE " + TABLE_MANUFACTURERS + " (" +
                        COL_MANUFACTURER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_MANUFACTURER_NAME + " TEXT NOT NULL UNIQUE" +
                        ");";

        String createCategoriesTable =
                "CREATE TABLE " + TABLE_CATEGORIES + " (" +
                        COL_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_CATEGORY_NAME + " TEXT NOT NULL UNIQUE" +
                        ");";

        // Store warehouse locations one time and let inventory items reference them.
        // This keeps the database cleaner and makes locations easier to manage later.
        String createLocationsTable =
                "CREATE TABLE " + TABLE_LOCATIONS + " (" +
                        COL_LOCATION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_WAREHOUSE_ROW + " INTEGER NOT NULL, " +
                        COL_WAREHOUSE_SHELF + " INTEGER NOT NULL" +
                        ");";

        db.execSQL(createUsersTable);
        db.execSQL(createInventoryTable);

        // Create the normalized lookup tables that future inventory records will use.
        db.execSQL(createManufacturersTable);
        db.execSQL(createCategoriesTable);
        db.execSQL(createLocationsTable);
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

        // Keep the original tables in the upgrade process while the app is being
        // moved over to the normalized database structure.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);

        // Recreate the database using the newest table definitions.
        onCreate(db);
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
        String[] selectionArgs = { username.trim(), password.trim() };

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[] { COL_USER_ID },
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
                new String[] { COL_USER_ID },
                COL_USERNAME + " = ?",
                new String[] { username.trim() },
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
    // Inventory CRUD
    // -------------------------

    public long createInventoryItem(String name,
                                    int quantity,
                                    int lowThreshold,
                                    String manufacturer,
                                    String serialNumber,
                                    String scuNumber,
                                    String location,
                                    String notes) {

        // Required fields for inventory items
        if (isBlank(name) || isBlank(manufacturer) || isBlank(serialNumber) || isBlank(scuNumber)) {
            return -1;
        }

        // Keep numeric values from going negative
        if (quantity < 0) {
            quantity = 0;
        }
        if (lowThreshold < 0) {
            lowThreshold = 0;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_ITEM_NAME, name.trim());
        values.put(COL_ITEM_QTY, quantity);
        values.put(COL_ITEM_THRESHOLD, lowThreshold);

        // "model" column is used as manufacturer in the UI
        values.put(COL_ITEM_MODEL, manufacturer.trim());

        values.put(COL_ITEM_SERIAL, serialNumber.trim());
        values.put(COL_ITEM_SCU, scuNumber.trim());

        // Optional fields, store empty string when left blank
        values.put(COL_ITEM_LOCATION, location == null ? "" : location.trim());
        values.put(COL_ITEM_NOTES, notes == null ? "" : notes.trim());

        long newId = db.insert(TABLE_INVENTORY, null, values);
        db.close();

        return newId;
    }

    public ArrayList<InventoryItem> getAllInventoryItems() {
        ArrayList<InventoryItem> items = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        // Sort by name so the list feels consistent for the user
        Cursor cursor = db.query(
                TABLE_INVENTORY,
                null,
                null,
                null,
                null,
                null,
                COL_ITEM_NAME + " COLLATE NOCASE ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                items.add(readInventoryItemFromCursor(cursor));
            }
            cursor.close();
        }

        db.close();
        return items;
    }

    public InventoryItem getInventoryItemById(int itemId) {

        // Item IDs should always be positive
        if (itemId <= 0) {
            return null;
        }

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_INVENTORY,
                null,
                COL_ITEM_ID + " = ?",
                new String[] { String.valueOf(itemId) },
                null,
                null,
                null
        );

        InventoryItem item = null;

        if (cursor != null && cursor.moveToFirst()) {
            item = readInventoryItemFromCursor(cursor);
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();

        return item;
    }

    public boolean updateInventoryQuantity(int itemId, int newQuantity) {

        // This is the only edit allowed from the inventory list screen
        if (itemId <= 0) {
            return false;
        }

        if (newQuantity < 0) {
            newQuantity = 0;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_ITEM_QTY, newQuantity);

        int rows = db.update(
                TABLE_INVENTORY,
                values,
                COL_ITEM_ID + " = ?",
                new String[] { String.valueOf(itemId) }
        );

        db.close();
        return rows > 0;
    }

    public boolean updateInventoryDetails(int itemId,
                                          String name,
                                          int lowThreshold,
                                          String manufacturer,
                                          String serialNumber,
                                          String scuNumber,
                                          String location,
                                          String notes) {

        // Details screen edits should always have a valid item id
        if (itemId <= 0) {
            return false;
        }

        // Required fields for the details screen
        if (isBlank(name) || isBlank(manufacturer) || isBlank(serialNumber) || isBlank(scuNumber)) {
            return false;
        }

        if (lowThreshold < 0) {
            lowThreshold = 0;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_ITEM_NAME, name.trim());
        values.put(COL_ITEM_THRESHOLD, lowThreshold);

        // "model" column is used as manufacturer in the UI
        values.put(COL_ITEM_MODEL, manufacturer.trim());
        values.put(COL_ITEM_SERIAL, serialNumber.trim());
        values.put(COL_ITEM_SCU, scuNumber.trim());

        values.put(COL_ITEM_LOCATION, location == null ? "" : location.trim());
        values.put(COL_ITEM_NOTES, notes == null ? "" : notes.trim());

        int rows = db.update(
                TABLE_INVENTORY,
                values,
                COL_ITEM_ID + " = ?",
                new String[] { String.valueOf(itemId) }
        );

        db.close();
        return rows > 0;
    }

    public boolean deleteInventoryItem(int itemId) {

        // Protect against accidental deletes with a bad id
        if (itemId <= 0) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();

        int rows = db.delete(
                TABLE_INVENTORY,
                COL_ITEM_ID + " = ?",
                new String[] { String.valueOf(itemId) }
        );

        db.close();
        return rows > 0;
    }

    public ArrayList<InventoryItem> getLowInventoryItems() {
        ArrayList<InventoryItem> items = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        // Low inventory means quantity is at or below the reorder threshold
        String selection = COL_ITEM_QTY + " <= " + COL_ITEM_THRESHOLD;

        Cursor cursor = db.query(
                TABLE_INVENTORY,
                null,
                selection,
                null,
                null,
                null,
                COL_ITEM_NAME + " COLLATE NOCASE ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                items.add(readInventoryItemFromCursor(cursor));
            }
            cursor.close();
        }

        db.close();
        return items;
    }

    private InventoryItem readInventoryItemFromCursor(Cursor cursor) {

        // Keep cursor parsing in one spot so every query stays consistent
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ITEM_ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_ITEM_NAME));
        int qty = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ITEM_QTY));
        int threshold = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ITEM_THRESHOLD));

        // Stored in the "model" column, shown as manufacturer in the app
        String manufacturer = cursor.getString(cursor.getColumnIndexOrThrow(COL_ITEM_MODEL));

        String serial = cursor.getString(cursor.getColumnIndexOrThrow(COL_ITEM_SERIAL));
        String scu = cursor.getString(cursor.getColumnIndexOrThrow(COL_ITEM_SCU));
        String location = cursor.getString(cursor.getColumnIndexOrThrow(COL_ITEM_LOCATION));
        String notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_ITEM_NOTES));

        return new InventoryItem(id, name, qty, threshold, manufacturer, serial, scu, location, notes);
    }

    // -------------------------
    // Model object
    // -------------------------

    public static class InventoryItem {

        public int id;
        public String name;
        public int quantity;
        public int lowThreshold;

        public String manufacturer;
        public String serialNumber;
        public String scuNumber;
        public String location;
        public String notes;

        public InventoryItem(int id,
                             String name,
                             int quantity,
                             int lowThreshold,
                             String manufacturer,
                             String serialNumber,
                             String scuNumber,
                             String location,
                             String notes) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
            this.lowThreshold = lowThreshold;
            this.manufacturer = manufacturer;
            this.serialNumber = serialNumber;
            this.scuNumber = scuNumber;
            this.location = location;
            this.notes = notes;
        }
    }
}
