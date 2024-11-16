package com.example.plantdiseasedetection;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "PlantDisease.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "diseases";
    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_DESCRIPTION = "description";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create table to store disease information
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " TEXT, " +
                COL_DESCRIPTION + " TEXT)";
        db.execSQL(createTable);

        // Insert some sample data
        db.execSQL("INSERT INTO " + TABLE_NAME + " (name, description) VALUES ('Maize Leaf Blight', 'First symptoms on maize plants appear on the lower leaves. Spots that occur later, caused by spores distributed by wind, show on upper leaves. At the beginning of the infestation small, longish, watery stains arise which can grow into elongated bands of grey-green to light brown lesions.')");
        db.execSQL("INSERT INTO " + TABLE_NAME + " (name, description) VALUES ('Coconut Yellowing Leaf', 'Yellowing leaves on your coconut palm can be a red flag for nutrient deficiencies. Magnesium shortages turn older leaves yellow, while a lack of potassium causes yellowing at the tips and edges. Manganese deficiency is a bit of a sneakier villain, resulting in yellow specks or stripes.')");
        db.execSQL("INSERT INTO " + TABLE_NAME + " (name, description) VALUES ('Rice Bacterial Leaf Blight', 'rice bacterial blight, deadly bacterial disease that is among the most destructive afflictions of cultivated rice (Oryza sativa and O. glaberrima). In severe epidemics, crop loss may be as high as 75 percent, and millions of hectares of rice are infected annually.')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Fetch the description of a disease by its name
    public String getDiseaseDescription(String diseaseName) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        String description = null;

        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery("SELECT description FROM " + TABLE_NAME + " WHERE name = ?",
                    new String[]{diseaseName});

            if (cursor != null && cursor.moveToFirst()) {
                description = cursor.getString(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
        return description;
    }

    public boolean isDatabaseOutdated() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);

        boolean isOutdated = true;
        if (cursor != null && cursor.moveToFirst()) {
            int count = cursor.getInt(0);
            isOutdated = count < 3; // Current number of diseases
        }

        if (cursor != null) cursor.close();
        return isOutdated;
    }
}
