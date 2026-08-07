# Mobile Inventory App

**CS-499 Computer Science Capstone**  
**Artifact Category:** Databases

The Mobile Inventory App is an Android inventory management application originally developed in CS-360 Mobile Architecture and Programming. The application uses SQLite to store user and inventory information and allows users to log in, create inventory items, update inventory, and receive low inventory notifications.

For my CS-499 capstone, I returned to the original application and redesigned major parts of its database structure while improving security, inventory tracking, and data integrity.

## Capstone Enhancement

The enhanced version focuses on improving the database design and making the application more secure and maintainable.

Major improvements include:

- Normalized relational database structure
- Separate manufacturer, category, and warehouse location tables
- Foreign key relationships between inventory and lookup data
- Inventory transaction history
- Soft deletion and restoration of inventory items
- Improved warehouse location tracking
- PBKDF2 password hashing with unique salts
- Automatic migration of legacy user passwords
- Improved low inventory notification workflow
- Bundled sample inventory data for initial setup

## Technologies Used

- Java
- Android
- SQLite
- XML
- Android Studio

## Original and Enhanced Versions

The original version of this project was created during CS-360 Mobile Architecture and Programming.

The `main` branch contains the enhanced CS-499 capstone version of the application.

The original version has been preserved with the `v1.0-original` tag so the changes made during the capstone can be reviewed separately.

## Portfolio

A full overview of the artifact, enhancement process, and reflection is available through my CS-499 ePortfolio.

[View Mobile Inventory App in the Project Archive](https://dustindavis-04.github.io/projects/mobile.html)
