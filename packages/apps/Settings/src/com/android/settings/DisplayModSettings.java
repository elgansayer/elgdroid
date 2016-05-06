/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.view.RotationPolicy;
import com.android.settings.DropDownPreference.Callback;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import static android.provider.Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED;
import static android.provider.Settings.Secure.CAMERA_GESTURE_DISABLED;
import static android.provider.Settings.Secure.DOUBLE_TAP_TO_WAKE;
import static android.provider.Settings.Secure.DOZE_ENABLED;
import static android.provider.Settings.Secure.WAKE_GESTURE_ENABLED;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;


//import static android.provider.Settings.System.STATUSBAR_CLOCK_FONT_STYLE;
//import static android.provider.Settings.System.STATUSBAR_CLOCK_LOCATION;
//import static android.provider.Settings.System.STATUSBAR_CLOCK_FONT_SCALE;


import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.UiModeManager;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;


import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DisplayModSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener, Indexable {
    private static final String TAG = "DisplayModSettings";



    // Intent actions for Settings
    /**
     * Font size for clock
     * @hide
     */
    public static final String STATUSBAR_CLOCK_FONT_SCALE = "statusbar_clock_font_scale";
    /* Settings for clock font style
    * 0 - Normal
    * 1 - Bold
    * 2 - italic
    * @hide
    */
    public static final String STATUSBAR_CLOCK_FONT_STYLE = "statusbar_clock_font_style";
    /**
     * Style of clock
     * 0 - Hide Clock
     * 1 - Right Clock
     * 2 - Center Clock
     * @hide
     */
    public static final String STATUSBAR_CLOCK_LOCATION = "statusbar_clock_location";




    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;


    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";


    private static final String KEY_CLOCK_FONT_SIZE = "clock_font_size";
    private static final String KEY_CLOCK_FONT_STYLE = "clock_font_style";

    /*
    private static final String KEY_SCREEN_SAVER = "screensaver";
    private static final String KEY_LIFT_TO_WAKE = "lift_to_wake";
    private static final String KEY_DOZE = "doze";
    private static final String KEY_TAP_TO_WAKE = "tap_to_wake";
    private static final String KEY_AUTO_BRIGHTNESS = "auto_brightness";
    private static final String KEY_AUTO_ROTATE = "auto_rotate";
    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_CAMERA_GESTURE = "camera_gesture";
    private static final String KEY_CAMERA_DOUBLE_TAP_POWER_GESTURE
            = "camera_double_tap_power_gesture";
    */

    private static final int DLG_GLOBAL_CHANGE_WARNING_FONT_SIZE = 1;
    private static final int DLG_GLOBAL_CHANGE_WARNING_FONT_STYLE = 2;

    private WarnedListPreference mFontSizePref;
    private WarnedListPreference mFontStylePref;

    private final Configuration mCurConfig = new Configuration();

    /*
    private ListPreference mScreenTimeoutPreference;
    private ListPreference mNightModePreference;
    private Preference mScreenSaverPreference;
    private SwitchPreference mLiftToWakePreference;
    private SwitchPreference mDozePreference;
    private SwitchPreference mTapToWakePreference;
    private SwitchPreference mAutoBrightnessPreference;
    private SwitchPreference mCameraGesturePreference;
    private SwitchPreference mCameraDoubleTapPowerGesturePreference;
    */

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DISPLAY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();

        addPreferencesFromResource(R.xml.displaymod_settings);

        mFontSizePref = (WarnedListPreference) findPreference(KEY_CLOCK_FONT_SIZE);
        mFontSizePref.setOnPreferenceChangeListener(this);
        mFontSizePref.setOnPreferenceClickListener(this);

        mFontStylePref = (WarnedListPreference) findPreference(KEY_CLOCK_FONT_STYLE);
        mFontStylePref.setOnPreferenceChangeListener(this);
        mFontStylePref.setOnPreferenceClickListener(this);

        readFontSizePreference(mFontSizePref);
        readFontStylePreference(mFontStylePref);


        }


    int floatToIndex(float val) {
        String[] indices = getResources().getStringArray(R.array.entryvalues_clock_font_size);
        float lastVal = Float.parseFloat(indices[0]);
        for (int i=1; i<indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < (lastVal + (thisVal-lastVal)*.5f)) {
                return i-1;
            }
            lastVal = thisVal;
        }
        return indices.length-1;
    }

    public void readFontSizePreference(ListPreference pref) {


        float fontFloat = Settings.System.getFloat(getContentResolver(), STATUSBAR_CLOCK_FONT_SCALE,
                0);

        // mark the appropriate item in the preferences list
        int index = floatToIndex(fontFloat);
        pref.setValueIndex(index);

        // report the current size in the summary text
        final Resources res = getResources();
        String[] fontSizeNames = res.getStringArray(R.array.entries_clock_font_size);
        pref.setSummary(String.format(res.getString(R.string.clock_summary_font_size),
                fontSizeNames[index]));
    }

    public void readFontStylePreference(ListPreference pref) {


        int fontIndex = Settings.System.getInt(getContentResolver(), STATUSBAR_CLOCK_FONT_STYLE,
                0);

        pref.setValueIndex(fontIndex);

        // report the current size in the summary text
        final Resources res = getResources();
        String[] fontSizeNames = res.getStringArray(R.array.entries_clock_font_style);
        pref.setSummary(String.format(res.getString(R.string.clock_summary_font_style),
                fontSizeNames[fontIndex]));
    }

    @Override
    public void onResume() {
        super.onResume();
        //updateState();
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DLG_GLOBAL_CHANGE_WARNING_FONT_SIZE) {
            return Utils.buildGlobalChangeWarningDialog(getActivity(),
                    R.string.global_font_change_title,
                    new Runnable() {
                        public void run() {
                            mFontSizePref.click();
                        }
                    });
        }
        if (dialogId == DLG_GLOBAL_CHANGE_WARNING_FONT_STYLE) {
            return Utils.buildGlobalChangeWarningDialog(getActivity(),
                    R.string.global_font_change_style_title,
                    new Runnable() {
                        public void run() {
                            mFontStylePref.click();
                        }
                    });
        }
        return null;
    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();

        if (KEY_CLOCK_FONT_SIZE.equals(key)) {
            try {
                float value = Float.parseFloat((String) objValue);
                Settings.System.putFloat(getContentResolver(), STATUSBAR_CLOCK_FONT_SCALE, value);
//                updateTimeoutPreferenceDescription(value);

                readFontSizePreference(mFontSizePref);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist font size setting", e);
            }
        }

        if (KEY_CLOCK_FONT_STYLE.equals(key)) {
            try {
                int value = Integer.parseInt((String) objValue);
                Settings.System.putInt(getContentResolver(), STATUSBAR_CLOCK_FONT_STYLE, value);
//                updateTimeoutPreferenceDescription(value);

                readFontStylePreference(mFontStylePref);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist font style setting", e);
            }
        }
        //if (KEY_FONT_SIZE.equals(key)) {
       //     writeFontSizePreference(objValue);
        //}

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mFontSizePref) {
            if (Utils.hasMultipleUsers(getActivity())) {
                showDialog(DLG_GLOBAL_CHANGE_WARNING_FONT_SIZE);
                return true;
            } else {
                mFontSizePref.click();
            }
        }
        if (preference == mFontStylePref) {
            if (Utils.hasMultipleUsers(getActivity())) {
                showDialog(DLG_GLOBAL_CHANGE_WARNING_FONT_STYLE);
                return true;
            } else {
                mFontStylePref.click();
            }
        }
        return false;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_display;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.displaymod_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();

                    return result;
                }
            };
}
