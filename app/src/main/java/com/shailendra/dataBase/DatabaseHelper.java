package com.shailendra.dataBase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.shailendra.model.MPois;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String TABLE_NAME = "poies";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_POINAME = "poi_name";
    public static final String COLUMN_TYPE = "poi_type";
    public static final String COLUMN_OWNERNAME = "owner_name";
    public static final String COLUMN_ESTABLISHED_DATE = "Established";
    public static final String COLUMN_LAT = "latitude";
    public static final String COLUMN_LNG = "longitude";
    public static final String COLUMN_FLAG = "Synced";
    // Create table SQL query
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_POINAME + " TEXT,"
                    + COLUMN_TYPE + " TEXT,"
                    + COLUMN_OWNERNAME + " TEXT,"
                    + COLUMN_ESTABLISHED_DATE + " TEXT,"
                    + COLUMN_LAT + " TEXT,"
                    + COLUMN_LNG + " TEXT,"
                    + COLUMN_FLAG + " INTEGER"
                    + ");";

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "poies_db";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // create notes table
        sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // Drop older table if existed
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);

        // Create tables again
        onCreate(sqLiteDatabase);
    }

    public long insertNote(String poiName,String poiType,String ownerName,String establishedDate,
                           String lat,String lng,int flag) {
        // get writable database as we want to write data
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        // `id` and `timestamp` will be inserted automatically.
        // no need to add them
        values.put(COLUMN_POINAME, poiName);
        values.put(COLUMN_TYPE, poiType);
        values.put(COLUMN_OWNERNAME,ownerName);
        values.put(COLUMN_ESTABLISHED_DATE, establishedDate);
        values.put(COLUMN_LAT,lat);
        values.put(COLUMN_LNG,lng);
        values.put(COLUMN_FLAG,flag);
        // insert row
        long id = db.insert(TABLE_NAME, null, values);

        // close db connection
        db.close();

        // return newly inserted row id
        return id;
    }

    public List<MPois> getAllPoies() {
        List<MPois> mPoisList = new ArrayList<>();

        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_NAME + ";";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                MPois mPois = new MPois();
                mPois.setPoiName(cursor.getString(cursor.getColumnIndex(COLUMN_POINAME)));
                mPois.setPoiType(cursor.getString(cursor.getColumnIndex(COLUMN_TYPE)));
                mPois.setOwnerName(cursor.getString(cursor.getColumnIndex(COLUMN_OWNERNAME)));
                mPois.setEstimateDate(cursor.getString(cursor.getColumnIndex(COLUMN_ESTABLISHED_DATE)));
                mPois.setLat(cursor.getString(cursor.getColumnIndex(COLUMN_LAT)));
                mPois.setLng(cursor.getString(cursor.getColumnIndex(COLUMN_LNG)));
                mPoisList.add(mPois);
            } while (cursor.moveToNext());
        }

        // close db connection
        db.close();

        // return notes list
        return mPoisList;
    }

    public int updatePoi(MPois mPois) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_FLAG, 1);

        // updating row
        return db.update(TABLE_NAME, values, COLUMN_FLAG + " = ?",
                new String[]{String.valueOf(0)});
    }

}
