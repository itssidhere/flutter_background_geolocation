package com.transistorsoft.flutter.backgroundgeolocation.streams;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.transistorsoft.locationmanager.logger.TSLog;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "LocationCache.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "location_cache";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_UUID = "uuid";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_CREATED_AT = "created_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_UUID + " TEXT, "
                + COLUMN_TIMESTAMP + " TEXT, "
                + COLUMN_CREATED_AT + " INTEGER)";
        db.execSQL(createTable);
    }

    public void getAllEntries() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            TSLog.logger.debug("[DatabaseHelper] Entry: " + cursor.getString(1) + " - " + cursor.getString(2));
            cursor.moveToNext();
        }
        cursor.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean isDuplicate(String value, String column) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE " + column + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{value});
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count > 0;
    }

    public void addEntry(String uuid, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_UUID, uuid);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_CREATED_AT, System.currentTimeMillis());
        db.insert(TABLE_NAME, null, values);
    }

    public void maintainCacheSize(int maxSize) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();

        if (count > maxSize) {
            int deleteCount = count - maxSize;
            db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " IN (SELECT " + 
                      COLUMN_ID + " FROM " + TABLE_NAME + " ORDER BY " + COLUMN_CREATED_AT + 
                      " ASC LIMIT " + deleteCount + ")");
        }
    }
} 