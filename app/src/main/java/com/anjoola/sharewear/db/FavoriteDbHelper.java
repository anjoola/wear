package com.anjoola.sharewear.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.anjoola.sharewear.db.FavoriteContactContract.FavoriteEntry;

public class FavoriteDbHelper extends SQLiteOpenHelper {
    public static final int DB_VERSION = 1;
    public static final String DB_NAME = "sharewear.db";

    // Used for creating the table.
    private static final String DB_CREATE =
            "CREATE TABLE " + FavoriteEntry.TABLE_NAME + " (" +
                FavoriteEntry._ID + " INTEGER PRIMARY KEY," +
                FavoriteEntry.COLUMN_NAME_NAME + " STRING," +
                FavoriteEntry.COLUMN_NAME_PHONE + " STRING," +
                FavoriteEntry.COLUMN_NAME_EMAIL + " STRING," +
                FavoriteEntry.COLUMN_NAME_PHOTO_URI + " STRING," +
                "UNIQUE(" +
                    FavoriteEntry.COLUMN_NAME_NAME + ", " +
                    FavoriteEntry.COLUMN_NAME_PHONE + "," +
                    FavoriteEntry.COLUMN_NAME_EMAIL + ")" +
            ");";
    private static final String DB_DELETE =
        "DROP TABLE IF EXISTS " + FavoriteEntry.TABLE_NAME;

    // Used to select all columns in the table.
    public static final String[] PROJECTION = {
            FavoriteEntry._ID,
            FavoriteEntry.COLUMN_NAME_NAME,
            FavoriteEntry.COLUMN_NAME_PHONE,
            FavoriteEntry.COLUMN_NAME_EMAIL,
            FavoriteEntry.COLUMN_NAME_PHOTO_URI
    };

    // Used for selecting a specific user from the table.
    public static final String PHONE_SELECTION =
            FavoriteEntry.COLUMN_NAME_NAME + " LIKE ? AND " +
            FavoriteEntry.COLUMN_NAME_PHONE + " = ?";
    public static final String EMAIL_SELECTION =
            FavoriteEntry.COLUMN_NAME_NAME + " LIKE ? AND " +
            FavoriteEntry.COLUMN_NAME_EMAIL + " = ?";


    public FavoriteDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DB_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DB_DELETE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
