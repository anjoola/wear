package com.anjoola.sharewear.db;

import android.provider.BaseColumns;

/**
 * Columns in the favorites table.
 */
public class FavoriteContactContract {
    public FavoriteContactContract() { }

    public static abstract class FavoriteEntry implements BaseColumns {
        public static final String TABLE_NAME = "favorites";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_PHONE = "phone";
        public static final String COLUMN_NAME_EMAIL = "email";
        public static final String COLUMN_NAME_PHOTO_URI = "photo_uri";
    }
}
