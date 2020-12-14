package com.cfdemo.receiptreader;

import android.content.Context;
import android.content.SharedPreferences;

public class StorageHelper {
    private static final String PrefKey = "receiptReaderPrefKey";
    private static final String CredKey = "receiptCredit";
    private static final String NameKey = "profileName";

    public static SharedPreferences getSharedPreference(Context context) {
        return context.getSharedPreferences(
                PrefKey, Context.MODE_PRIVATE);
    }

    public static float getCredit (Context context) {
        return getSharedPreference(context).getFloat(CredKey, 0);
    }

    public static String getName (Context context) {
        return getSharedPreference(context).getString(NameKey, null);
    }

    public static void saveName (Context context, String name) {
        SharedPreferences.Editor editor = getSharedPreference(context).edit();
        editor.putString(NameKey, name);
        editor.apply();
    }

    public static void addCredit (Context context, float increment) {
        SharedPreferences.Editor editor = getSharedPreference(context).edit();
        float currCredit = getCredit(context);
        editor.putFloat(CredKey, currCredit + increment);
        editor.apply();

    }
}
