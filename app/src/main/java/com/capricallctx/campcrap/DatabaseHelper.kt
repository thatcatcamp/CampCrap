package com.capricallctx.campcrap

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    
    companion object {
        private const val DATABASE_NAME = "campcrap.db"
        private const val DATABASE_VERSION = 6
        
        // People table
        const val TABLE_PEOPLE = "people"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_REAL_NAME = "real_name"
        const val COLUMN_ENTRY_DATE = "entry_date"
        const val COLUMN_EXIT_DATE = "exit_date"
        const val COLUMN_CAMP_NAME = "camp_name"
        const val COLUMN_NOTES = "notes"
        const val COLUMN_YEAR = "year"
        const val COLUMN_SKIPPING = "skipping"
        const val COLUMN_IS_INFRASTRUCTURE = "is_infrastructure"
        
        // Locations table
        const val TABLE_LOCATIONS = "locations"
        const val LOCATION_ID = "id"
        const val LOCATION_NAME = "name"
        const val LOCATION_DESCRIPTION = "description"
        const val LOCATION_YEAR = "year"
        
        // Items table
        const val TABLE_ITEMS = "items"
        const val ITEM_ID = "id"
        const val ITEM_NAME = "name"
        const val ITEM_DESCRIPTION = "description"
        const val ITEM_CAMPER_ID = "camper_id"
        const val ITEM_LOCATION_ID = "location_id"
        const val ITEM_PHOTO_PATH = "photo_path"
        const val ITEM_YEAR = "year"
        const val ITEM_CREATED_DATE = "created_date"
        const val ITEM_REMOVAL_STATUS = "removal_status"
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        val createPeopleTable = """
            CREATE TABLE $TABLE_PEOPLE (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_EMAIL TEXT,
                $COLUMN_REAL_NAME TEXT,
                $COLUMN_ENTRY_DATE TEXT,
                $COLUMN_EXIT_DATE TEXT,
                $COLUMN_CAMP_NAME TEXT,
                $COLUMN_NOTES TEXT,
                $COLUMN_YEAR TEXT NOT NULL,
                $COLUMN_SKIPPING INTEGER DEFAULT 0,
                $COLUMN_IS_INFRASTRUCTURE INTEGER DEFAULT 0
            )
        """.trimIndent()
        
        db.execSQL(createPeopleTable)
        
        val createLocationsTable = """
            CREATE TABLE $TABLE_LOCATIONS (
                $LOCATION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $LOCATION_NAME TEXT NOT NULL,
                $LOCATION_DESCRIPTION TEXT,
                $LOCATION_YEAR TEXT NOT NULL
            )
        """.trimIndent()
        
        db.execSQL(createLocationsTable)
        
        val createItemsTable = """
            CREATE TABLE $TABLE_ITEMS (
                $ITEM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $ITEM_NAME TEXT NOT NULL,
                $ITEM_DESCRIPTION TEXT,
                $ITEM_CAMPER_ID INTEGER NOT NULL,
                $ITEM_LOCATION_ID INTEGER NOT NULL,
                $ITEM_PHOTO_PATH TEXT,
                $ITEM_YEAR TEXT NOT NULL,
                $ITEM_CREATED_DATE TEXT NOT NULL,
                $ITEM_REMOVAL_STATUS TEXT DEFAULT 'active',
                FOREIGN KEY($ITEM_CAMPER_ID) REFERENCES $TABLE_PEOPLE($COLUMN_ID),
                FOREIGN KEY($ITEM_LOCATION_ID) REFERENCES $TABLE_LOCATIONS($LOCATION_ID)
            )
        """.trimIndent()
        
        db.execSQL(createItemsTable)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Add skipping column if upgrading from version 1
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_PEOPLE ADD COLUMN $COLUMN_SKIPPING INTEGER DEFAULT 0")
        }
        // Add infrastructure column if upgrading from version 2
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_PEOPLE ADD COLUMN $COLUMN_IS_INFRASTRUCTURE INTEGER DEFAULT 0")
        }
        // Add locations table if upgrading from version 3
        if (oldVersion < 4) {
            val createLocationsTable = """
                CREATE TABLE $TABLE_LOCATIONS (
                    $LOCATION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $LOCATION_NAME TEXT NOT NULL,
                    $LOCATION_DESCRIPTION TEXT,
                    $LOCATION_YEAR TEXT NOT NULL
                )
            """.trimIndent()
            db.execSQL(createLocationsTable)
        }
        // Add items table if upgrading from version 4
        if (oldVersion < 5) {
            val createItemsTable = """
                CREATE TABLE $TABLE_ITEMS (
                    $ITEM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $ITEM_NAME TEXT NOT NULL,
                    $ITEM_DESCRIPTION TEXT,
                    $ITEM_CAMPER_ID INTEGER NOT NULL,
                    $ITEM_LOCATION_ID INTEGER NOT NULL,
                    $ITEM_PHOTO_PATH TEXT,
                    $ITEM_YEAR TEXT NOT NULL,
                    $ITEM_CREATED_DATE TEXT NOT NULL,
                    FOREIGN KEY($ITEM_CAMPER_ID) REFERENCES $TABLE_PEOPLE($COLUMN_ID),
                    FOREIGN KEY($ITEM_LOCATION_ID) REFERENCES $TABLE_LOCATIONS($LOCATION_ID)
                )
            """.trimIndent()
            db.execSQL(createItemsTable)
        }
        // Add removal status column if upgrading from version 5
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE $TABLE_ITEMS ADD COLUMN $ITEM_REMOVAL_STATUS TEXT DEFAULT 'active'")
        }
    }
    
    fun addPerson(
        name: String,
        email: String = "",
        realName: String = "",
        entryDate: String = "",
        exitDate: String = "",
        campName: String = "",
        notes: String = "",
        year: String,
        isInfrastructure: Boolean = false
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_EMAIL, email)
            put(COLUMN_REAL_NAME, realName)
            put(COLUMN_ENTRY_DATE, entryDate)
            put(COLUMN_EXIT_DATE, exitDate)
            put(COLUMN_CAMP_NAME, campName)
            put(COLUMN_NOTES, notes)
            put(COLUMN_YEAR, year)
            put(COLUMN_IS_INFRASTRUCTURE, if (isInfrastructure) 1 else 0)
        }
        
        return db.insert(TABLE_PEOPLE, null, values)
    }
    
    fun hasPeopleForYear(year: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PEOPLE,
            arrayOf(COLUMN_ID),
            "$COLUMN_YEAR = ? AND $COLUMN_IS_INFRASTRUCTURE = 0",
            arrayOf(year),
            null,
            null,
            null,
            "1"
        )
        
        val hasData = cursor.count > 0
        cursor.close()
        return hasData
    }
    
    fun getPeopleForYear(year: String): List<Person> {
        val people = mutableListOf<Person>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PEOPLE,
            null,
            "$COLUMN_YEAR = ? AND $COLUMN_IS_INFRASTRUCTURE = 0",
            arrayOf(year),
            null,
            null,
            "$COLUMN_NAME ASC"
        )
        
        cursor.use {
            while (it.moveToNext()) {
                people.add(
                    Person(
                        id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                        email = it.getString(it.getColumnIndexOrThrow(COLUMN_EMAIL)) ?: "",
                        realName = it.getString(it.getColumnIndexOrThrow(COLUMN_REAL_NAME)) ?: "",
                        entryDate = it.getString(it.getColumnIndexOrThrow(COLUMN_ENTRY_DATE)) ?: "",
                        exitDate = it.getString(it.getColumnIndexOrThrow(COLUMN_EXIT_DATE)) ?: "",
                        campName = it.getString(it.getColumnIndexOrThrow(COLUMN_CAMP_NAME)) ?: "",
                        notes = it.getString(it.getColumnIndexOrThrow(COLUMN_NOTES)) ?: "",
                        year = it.getString(it.getColumnIndexOrThrow(COLUMN_YEAR)),
                        skipping = it.getInt(it.getColumnIndexOrThrow(COLUMN_SKIPPING)) == 1,
                        isInfrastructure = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_INFRASTRUCTURE)) == 1
                    )
                )
            }
        }
        
        return people
    }
    
    fun updatePerson(
        id: Long,
        name: String,
        email: String = "",
        realName: String = "",
        entryDate: String = "",
        exitDate: String = "",
        campName: String = "",
        notes: String = "",
        skipping: Boolean = false
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_EMAIL, email)
            put(COLUMN_REAL_NAME, realName)
            put(COLUMN_ENTRY_DATE, entryDate)
            put(COLUMN_EXIT_DATE, exitDate)
            put(COLUMN_CAMP_NAME, campName)
            put(COLUMN_NOTES, notes)
            put(COLUMN_SKIPPING, if (skipping) 1 else 0)
        }
        
        val result = db.update(TABLE_PEOPLE, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        return result > 0
    }
    
    fun setPersonSkipping(id: Long, skipping: Boolean): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SKIPPING, if (skipping) 1 else 0)
        }
        
        val result = db.update(TABLE_PEOPLE, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        return result > 0
    }
    
    fun getPersonById(id: Long): Person? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PEOPLE,
            null,
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        
        cursor.use {
            if (it.moveToFirst()) {
                return Person(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                    email = it.getString(it.getColumnIndexOrThrow(COLUMN_EMAIL)) ?: "",
                    realName = it.getString(it.getColumnIndexOrThrow(COLUMN_REAL_NAME)) ?: "",
                    entryDate = it.getString(it.getColumnIndexOrThrow(COLUMN_ENTRY_DATE)) ?: "",
                    exitDate = it.getString(it.getColumnIndexOrThrow(COLUMN_EXIT_DATE)) ?: "",
                    campName = it.getString(it.getColumnIndexOrThrow(COLUMN_CAMP_NAME)) ?: "",
                    notes = it.getString(it.getColumnIndexOrThrow(COLUMN_NOTES)) ?: "",
                    year = it.getString(it.getColumnIndexOrThrow(COLUMN_YEAR)),
                    skipping = it.getInt(it.getColumnIndexOrThrow(COLUMN_SKIPPING)) == 1,
                    isInfrastructure = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_INFRASTRUCTURE)) == 1
                )
            }
        }
        return null
    }
    
    fun ensureInfrastructureCamper(year: String): Long {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PEOPLE,
            arrayOf(COLUMN_ID),
            "$COLUMN_YEAR = ? AND $COLUMN_IS_INFRASTRUCTURE = 1",
            arrayOf(year),
            null,
            null,
            null,
            "1"
        )
        
        val exists = cursor.count > 0
        cursor.close()
        
        return if (exists) {
            // Infrastructure camper already exists, get its ID
            val infCursor = db.query(
                TABLE_PEOPLE,
                arrayOf(COLUMN_ID),
                "$COLUMN_YEAR = ? AND $COLUMN_IS_INFRASTRUCTURE = 1",
                arrayOf(year),
                null,
                null,
                null,
                "1"
            )
            var id = -1L
            if (infCursor.moveToFirst()) {
                id = infCursor.getLong(infCursor.getColumnIndexOrThrow(COLUMN_ID))
            }
            infCursor.close()
            id
        } else {
            // Create infrastructure camper
            addPerson(
                name = "Camp Infrastructure",
                notes = "Items that belong to the camp overall",
                year = year,
                isInfrastructure = true
            )
        }
    }
    
    fun getInfrastructureCamper(year: String): Person? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PEOPLE,
            null,
            "$COLUMN_YEAR = ? AND $COLUMN_IS_INFRASTRUCTURE = 1",
            arrayOf(year),
            null,
            null,
            null
        )
        
        cursor.use {
            if (it.moveToFirst()) {
                return Person(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                    email = it.getString(it.getColumnIndexOrThrow(COLUMN_EMAIL)) ?: "",
                    realName = it.getString(it.getColumnIndexOrThrow(COLUMN_REAL_NAME)) ?: "",
                    entryDate = it.getString(it.getColumnIndexOrThrow(COLUMN_ENTRY_DATE)) ?: "",
                    exitDate = it.getString(it.getColumnIndexOrThrow(COLUMN_EXIT_DATE)) ?: "",
                    campName = it.getString(it.getColumnIndexOrThrow(COLUMN_CAMP_NAME)) ?: "",
                    notes = it.getString(it.getColumnIndexOrThrow(COLUMN_NOTES)) ?: "",
                    year = it.getString(it.getColumnIndexOrThrow(COLUMN_YEAR)),
                    skipping = it.getInt(it.getColumnIndexOrThrow(COLUMN_SKIPPING)) == 1,
                    isInfrastructure = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_INFRASTRUCTURE)) == 1
                )
            }
        }
        return null
    }
    
    // Location methods
    fun addLocation(name: String, description: String = "", year: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(LOCATION_NAME, name)
            put(LOCATION_DESCRIPTION, description)
            put(LOCATION_YEAR, year)
        }
        return db.insert(TABLE_LOCATIONS, null, values)
    }
    
    fun getLocationsForYear(year: String): List<Location> {
        val locations = mutableListOf<Location>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_LOCATIONS,
            null,
            "$LOCATION_YEAR = ?",
            arrayOf(year),
            null,
            null,
            "$LOCATION_NAME ASC"
        )
        
        cursor.use {
            while (it.moveToNext()) {
                locations.add(
                    Location(
                        id = it.getLong(it.getColumnIndexOrThrow(LOCATION_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(LOCATION_NAME)),
                        description = it.getString(it.getColumnIndexOrThrow(LOCATION_DESCRIPTION)) ?: "",
                        year = it.getString(it.getColumnIndexOrThrow(LOCATION_YEAR))
                    )
                )
            }
        }
        return locations
    }
    
    fun hasLocationsForYear(year: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_LOCATIONS,
            arrayOf(LOCATION_ID),
            "$LOCATION_YEAR = ?",
            arrayOf(year),
            null,
            null,
            null,
            "1"
        )
        
        val hasData = cursor.count > 0
        cursor.close()
        return hasData
    }
    
    fun ensureCampStorageLocation(year: String): Long {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_LOCATIONS,
            arrayOf(LOCATION_ID),
            "$LOCATION_YEAR = ? AND $LOCATION_NAME = 'Camp Storage'",
            arrayOf(year),
            null,
            null,
            null,
            "1"
        )
        
        val exists = cursor.count > 0
        cursor.close()
        
        return if (exists) {
            // Camp Storage already exists, get its ID
            val storageCursor = db.query(
                TABLE_LOCATIONS,
                arrayOf(LOCATION_ID),
                "$LOCATION_YEAR = ? AND $LOCATION_NAME = 'Camp Storage'",
                arrayOf(year),
                null,
                null,
                null,
                "1"
            )
            var id = -1L
            if (storageCursor.moveToFirst()) {
                id = storageCursor.getLong(storageCursor.getColumnIndexOrThrow(LOCATION_ID))
            }
            storageCursor.close()
            id
        } else {
            // Create Camp Storage location
            addLocation(
                name = "Camp Storage",
                description = "Central storage area for camp items",
                year = year
            )
        }
    }
    
    fun updateLocation(id: Long, name: String, description: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(LOCATION_NAME, name)
            put(LOCATION_DESCRIPTION, description)
        }
        
        val result = db.update(TABLE_LOCATIONS, values, "$LOCATION_ID = ?", arrayOf(id.toString()))
        return result > 0
    }
    
    fun getLocationById(id: Long): Location? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_LOCATIONS,
            null,
            "$LOCATION_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        
        cursor.use {
            if (it.moveToFirst()) {
                return Location(
                    id = it.getLong(it.getColumnIndexOrThrow(LOCATION_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(LOCATION_NAME)),
                    description = it.getString(it.getColumnIndexOrThrow(LOCATION_DESCRIPTION)) ?: "",
                    year = it.getString(it.getColumnIndexOrThrow(LOCATION_YEAR))
                )
            }
        }
        return null
    }
    
    // Item methods
    fun addItem(
        name: String,
        description: String = "",
        camperId: Long,
        locationId: Long,
        photoPath: String? = null,
        year: String
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(ITEM_NAME, name)
            put(ITEM_DESCRIPTION, description)
            put(ITEM_CAMPER_ID, camperId)
            put(ITEM_LOCATION_ID, locationId)
            put(ITEM_PHOTO_PATH, photoPath)
            put(ITEM_YEAR, year)
            put(ITEM_CREATED_DATE, System.currentTimeMillis().toString())
        }
        return db.insert(TABLE_ITEMS, null, values)
    }
    
    fun getItemsForYear(year: String): List<Item> {
        val items = mutableListOf<Item>()
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT 
                i.$ITEM_ID,
                i.$ITEM_NAME,
                i.$ITEM_DESCRIPTION,
                i.$ITEM_CAMPER_ID,
                i.$ITEM_LOCATION_ID,
                i.$ITEM_PHOTO_PATH,
                i.$ITEM_YEAR,
                i.$ITEM_CREATED_DATE,
                i.$ITEM_REMOVAL_STATUS,
                p.$COLUMN_NAME as camper_name,
                l.$LOCATION_NAME as location_name
            FROM $TABLE_ITEMS i
            LEFT JOIN $TABLE_PEOPLE p ON i.$ITEM_CAMPER_ID = p.$COLUMN_ID
            LEFT JOIN $TABLE_LOCATIONS l ON i.$ITEM_LOCATION_ID = l.$LOCATION_ID
            WHERE i.$ITEM_YEAR = ? AND i.$ITEM_REMOVAL_STATUS = 'active'
            ORDER BY i.$ITEM_CREATED_DATE DESC
        """, arrayOf(year))
        
        cursor.use {
            while (it.moveToNext()) {
                items.add(
                    Item(
                        id = it.getLong(it.getColumnIndexOrThrow(ITEM_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(ITEM_NAME)),
                        description = it.getString(it.getColumnIndexOrThrow(ITEM_DESCRIPTION)) ?: "",
                        camperId = it.getLong(it.getColumnIndexOrThrow(ITEM_CAMPER_ID)),
                        locationId = it.getLong(it.getColumnIndexOrThrow(ITEM_LOCATION_ID)),
                        photoPath = it.getString(it.getColumnIndexOrThrow(ITEM_PHOTO_PATH)),
                        year = it.getString(it.getColumnIndexOrThrow(ITEM_YEAR)),
                        createdDate = it.getString(it.getColumnIndexOrThrow(ITEM_CREATED_DATE)),
                        camperName = it.getString(it.getColumnIndexOrThrow("camper_name")) ?: "",
                        locationName = it.getString(it.getColumnIndexOrThrow("location_name")) ?: "",
                        removalStatus = it.getString(it.getColumnIndexOrThrow(ITEM_REMOVAL_STATUS)) ?: "active"
                    )
                )
            }
        }
        return items
    }
    
    fun getAllItemsForYear(year: String): List<Item> {
        val items = mutableListOf<Item>()
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT 
                i.$ITEM_ID,
                i.$ITEM_NAME,
                i.$ITEM_DESCRIPTION,
                i.$ITEM_CAMPER_ID,
                i.$ITEM_LOCATION_ID,
                i.$ITEM_PHOTO_PATH,
                i.$ITEM_YEAR,
                i.$ITEM_CREATED_DATE,
                i.$ITEM_REMOVAL_STATUS,
                p.$COLUMN_NAME as camper_name,
                l.$LOCATION_NAME as location_name
            FROM $TABLE_ITEMS i
            LEFT JOIN $TABLE_PEOPLE p ON i.$ITEM_CAMPER_ID = p.$COLUMN_ID
            LEFT JOIN $TABLE_LOCATIONS l ON i.$ITEM_LOCATION_ID = l.$LOCATION_ID
            WHERE i.$ITEM_YEAR = ?
            ORDER BY i.$ITEM_CREATED_DATE DESC
        """, arrayOf(year))
        
        cursor.use {
            while (it.moveToNext()) {
                items.add(
                    Item(
                        id = it.getLong(it.getColumnIndexOrThrow(ITEM_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(ITEM_NAME)),
                        description = it.getString(it.getColumnIndexOrThrow(ITEM_DESCRIPTION)) ?: "",
                        camperId = it.getLong(it.getColumnIndexOrThrow(ITEM_CAMPER_ID)),
                        locationId = it.getLong(it.getColumnIndexOrThrow(ITEM_LOCATION_ID)),
                        photoPath = it.getString(it.getColumnIndexOrThrow(ITEM_PHOTO_PATH)),
                        year = it.getString(it.getColumnIndexOrThrow(ITEM_YEAR)),
                        createdDate = it.getString(it.getColumnIndexOrThrow(ITEM_CREATED_DATE)),
                        camperName = it.getString(it.getColumnIndexOrThrow("camper_name")) ?: "",
                        locationName = it.getString(it.getColumnIndexOrThrow("location_name")) ?: "",
                        removalStatus = it.getString(it.getColumnIndexOrThrow(ITEM_REMOVAL_STATUS)) ?: "active"
                    )
                )
            }
        }
        return items
    }
    
    fun getAllCampersAndInfrastructureForYear(year: String): List<Person> {
        val people = mutableListOf<Person>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PEOPLE,
            null,
            "$COLUMN_YEAR = ?",
            arrayOf(year),
            null,
            null,
            "$COLUMN_NAME ASC"
        )
        
        cursor.use {
            while (it.moveToNext()) {
                people.add(
                    Person(
                        id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                        email = it.getString(it.getColumnIndexOrThrow(COLUMN_EMAIL)) ?: "",
                        realName = it.getString(it.getColumnIndexOrThrow(COLUMN_REAL_NAME)) ?: "",
                        entryDate = it.getString(it.getColumnIndexOrThrow(COLUMN_ENTRY_DATE)) ?: "",
                        exitDate = it.getString(it.getColumnIndexOrThrow(COLUMN_EXIT_DATE)) ?: "",
                        campName = it.getString(it.getColumnIndexOrThrow(COLUMN_CAMP_NAME)) ?: "",
                        notes = it.getString(it.getColumnIndexOrThrow(COLUMN_NOTES)) ?: "",
                        year = it.getString(it.getColumnIndexOrThrow(COLUMN_YEAR)),
                        skipping = it.getInt(it.getColumnIndexOrThrow(COLUMN_SKIPPING)) == 1,
                        isInfrastructure = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_INFRASTRUCTURE)) == 1
                    )
                )
            }
        }
        return people
    }
    
    fun updateItem(
        id: Long,
        name: String,
        description: String,
        camperId: Long,
        locationId: Long,
        photoPath: String? = null,
        removalStatus: String? = null
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(ITEM_NAME, name)
            put(ITEM_DESCRIPTION, description)
            put(ITEM_CAMPER_ID, camperId)
            put(ITEM_LOCATION_ID, locationId)
            if (photoPath != null) {
                put(ITEM_PHOTO_PATH, photoPath)
            }
            if (removalStatus != null) {
                put(ITEM_REMOVAL_STATUS, removalStatus)
            }
        }
        
        val result = db.update(TABLE_ITEMS, values, "$ITEM_ID = ?", arrayOf(id.toString()))
        return result > 0
    }
    
    fun getItemById(id: Long): Item? {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT 
                i.$ITEM_ID,
                i.$ITEM_NAME,
                i.$ITEM_DESCRIPTION,
                i.$ITEM_CAMPER_ID,
                i.$ITEM_LOCATION_ID,
                i.$ITEM_PHOTO_PATH,
                i.$ITEM_YEAR,
                i.$ITEM_CREATED_DATE,
                i.$ITEM_REMOVAL_STATUS,
                p.$COLUMN_NAME as camper_name,
                l.$LOCATION_NAME as location_name
            FROM $TABLE_ITEMS i
            LEFT JOIN $TABLE_PEOPLE p ON i.$ITEM_CAMPER_ID = p.$COLUMN_ID
            LEFT JOIN $TABLE_LOCATIONS l ON i.$ITEM_LOCATION_ID = l.$LOCATION_ID
            WHERE i.$ITEM_ID = ?
        """, arrayOf(id.toString()))
        
        cursor.use {
            if (it.moveToFirst()) {
                return Item(
                    id = it.getLong(it.getColumnIndexOrThrow(ITEM_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(ITEM_NAME)),
                    description = it.getString(it.getColumnIndexOrThrow(ITEM_DESCRIPTION)) ?: "",
                    camperId = it.getLong(it.getColumnIndexOrThrow(ITEM_CAMPER_ID)),
                    locationId = it.getLong(it.getColumnIndexOrThrow(ITEM_LOCATION_ID)),
                    photoPath = it.getString(it.getColumnIndexOrThrow(ITEM_PHOTO_PATH)),
                    year = it.getString(it.getColumnIndexOrThrow(ITEM_YEAR)),
                    createdDate = it.getString(it.getColumnIndexOrThrow(ITEM_CREATED_DATE)),
                    camperName = it.getString(it.getColumnIndexOrThrow("camper_name")) ?: "",
                    locationName = it.getString(it.getColumnIndexOrThrow("location_name")) ?: "",
                    removalStatus = it.getString(it.getColumnIndexOrThrow(ITEM_REMOVAL_STATUS)) ?: "active"
                )
            }
        }
        return null
    }
}

data class Person(
    val id: Long,
    val name: String,
    val email: String,
    val realName: String,
    val entryDate: String,
    val exitDate: String,
    val campName: String,
    val notes: String,
    val year: String,
    val skipping: Boolean = false,
    val isInfrastructure: Boolean = false
)

data class Location(
    val id: Long,
    val name: String,
    val description: String,
    val year: String
)

data class Item(
    val id: Long,
    val name: String,
    val description: String,
    val camperId: Long,
    val locationId: Long,
    val photoPath: String?,
    val year: String,
    val createdDate: String,
    val camperName: String,
    val locationName: String,
    val removalStatus: String = "active"
)