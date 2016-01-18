/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.elgan;

import android.os.RemoteException;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
//import com.android.server.am.ActivityManagerService;
import android.app.IActivityManager;

import com.android.internal.os.BinderInternal;
import android.os.IBinder;
import android.os.Binder;
import com.android.internal.app.IAppDisplayOpsService;
import android.app.AppDisplayOpsManager;
//import com.android.server.AppDisplayOpsService;

import android.os.ServiceManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Handler;
import android.os.UserHandle;

//import android.elgan.AppDisplayBackend;
//import android.elgan.AppDisplayRow;


import android.app.ActivityThread;
import android.app.ActivityManager;

import android.util.DisplayMetrics;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.content.SharedPreferences;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.SwitchPreference;
import android.preference.EditTextPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;
import android.text.InputType;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.AppHeader;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.AppInfoWithHeader;
//import com.android.settings.notification.NotificationBackend.AppRow;
//import com.android.settings.notification.NotificationBackend.Row;

import java.lang.Double;
import java.lang.String;
import java.util.List;
import java.util.Map;




/** These settings are per app, so should not be returned in global search results. */
public class AppDisplaySettings extends SettingsPreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {


    private static final String TAG = "AppDisplaySettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String KEY_DPI = "dpi_setting";
    private static final String KEY_FONT = "font_setting";

    private EditTextPreference mDPIPreference;
    private EditTextPreference mFontPreference;

    private Context mContext;
    private boolean mCreated;
    private boolean mIsSystemPackage;
    private int mUid;
     String pkgname;

    SharedPreferences prefs;

    private AppDisplayOpsManager.AppDisplayRow mAppSettings;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG) Log.d(TAG, "onActivityCreated mCreated=" + mCreated);
        if (mCreated) {
            Log.w(TAG, "onActivityCreated: ignoring duplicate call");
            return;
        }
        mCreated = true;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_APP_NOTIFICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity();
        Intent intent = getActivity().getIntent();
        Bundle args = getArguments();

        if (DEBUG) Log.d(TAG, "onCreate getIntent()=" + intent);
        if (intent == null && args == null) {
            Log.w(TAG, "No intent");
            toastAndFinish();
            return;
        }

        final String pkg = args != null && args.containsKey(AppInfoBase.ARG_PACKAGE_NAME)
                ? args.getString(AppInfoBase.ARG_PACKAGE_NAME)
                : intent.getStringExtra(Settings.EXTRA_APP_PACKAGE);
        mUid = args != null && args.containsKey(AppInfoBase.ARG_PACKAGE_UID)
                ? args.getInt(AppInfoBase.ARG_PACKAGE_UID)
                : intent.getIntExtra(Settings.EXTRA_APP_UID, -1);
        if (mUid == -1 || TextUtils.isEmpty(pkg)) {
            Log.w(TAG, "Missing extras: " + Settings.EXTRA_APP_PACKAGE + " was " + pkg + ", "
                    + Settings.EXTRA_APP_UID + " was " + mUid);
            toastAndFinish();
            return;
        }


        if (DEBUG) Log.d(TAG, "Load details for pkg=" + pkg + " uid=" + mUid);
        final PackageManager pm = getPackageManager();
        final PackageInfo info = findPackageInfo(pm, pkg, mUid);
        if (info == null) {
            Log.w(TAG, "Failed to find package info: " + Settings.EXTRA_APP_PACKAGE + " was " + pkg
                    + ", " + Settings.EXTRA_APP_UID + " was " + mUid);
            toastAndFinish();
            return;
        }

        addPreferencesFromResource(R.xml.app_display_settings);
        mDPIPreference = (EditTextPreference) findPreference(KEY_DPI);
        mDPIPreference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);

        mFontPreference = (EditTextPreference) findPreference(KEY_FONT);
        mFontPreference.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);


        Context myContext;
        pkgname = pkg;
        Log.w(TAG, "ELGAN SAYER pkg " + pkg);

        ActivityThread at = ActivityThread.systemMain();
        Context context = at.getSystemContext();

/*
        AppDisplayBackend backed = AppDisplayBackend.getInstance();
        //backed.addRow(pkgname, newDpi, 0);
        backed.open(context);

        this.mAppSettings = backed.getAppDisplayRow(pkgname);

      //  int dpi = backed.getDPI(pkgname);
        String dpi = Integer.toString(this.mAppSettings.getDPI()); //this.mAppSettings.getDPI().toString();
        mDPIPreference.setText( dpi );

      //  int font = backed.getFontScale(pkgname);
        String font = Integer.toString(this.mAppSettings.getFontScale()); ////.toString();
        mFontPreference.setText( font );

        //backed.close();

        */
        
                
        
        IBinder b = ServiceManager.getService(Context.APP_DISPLAY_OPS_SERVICE);        
        IAppDisplayOpsService dooda = IAppDisplayOpsService.Stub.asInterface(b);
        
        try
        {
	  this.mAppSettings = dooda.getAppDisplayRow(pkgname);
        }
        catch(RemoteException ex)
        {
        }
        
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {

        String value = sharedPreferences.getString(key, "");

        if (key.equals(KEY_DPI))
        {
            int newDpi = Integer.parseInt(value);

            Log.w(TAG, "ELGAN SAYER saving KEY_DPI :" + Integer.toString(newDpi) + " for " + pkgname );
            this.mAppSettings.setDPI( newDpi );

        //    updateDPI(value);
        }
        else if (key.equals(KEY_FONT)) {

            int newFontScale = Integer.parseInt(value);
            Log.w(TAG, "ELGAN SAYER saving KEY_FONTSCALE :" + Integer.toString(newFontScale) + " for " + pkgname );
	    this.mAppSettings.setFontScale(newFontScale);

          //  updateFont(value);
        }

        IActivityManager am = ActivityManagerNative.getDefault();
        
        
        
        IBinder b = ServiceManager.getService(Context.APP_DISPLAY_OPS_SERVICE);        
        IAppDisplayOpsService dooda = IAppDisplayOpsService.Stub.asInterface(b);
        
        try
        {

        
        dooda.insertRow(pkgname, this.mAppSettings);
        
        }
        catch(RemoteException ex)
        {
        }
        
        /*
        ActivityThread at = ActivityThread.systemMain();
        Context context = at.getSystemContext();
        AppDisplayBackend backed = AppDisplayBackend.getInstance();
        backed.open(context);

        backed.insertRow(pkgname, this.mAppSettings);

     //   backed.close();
     */
    }

/*
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if (preference == KEY_DPI) {
            updateDPI(newValue);
        }else if (preference == KEY_FONT) {
            updateDPI(newValue);
        }

        return true;
    }
*/





/*
      //  SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
      //  String syncConnPref = sharedPref.getString(SettingsActivity.KEY_DPI, "");

      //  mDPIPreference.setText(getPersistedString(180));

        //mDPIPreference.
      //  File prefsdir = new File(info.dataDir,"shared_prefs");

      //  if(prefsdir.exists() && prefsdir.isDirectory()){
         //   String[] list = prefsdir.list();
//

      //      ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1, android.R.id.text1,list);
      //      Spinner sp = (Spinner) findViewById(R.id.spinner1);
///

        //    sp.setAdapter(adapter);


          //  String preffile = pkg + "_preferences";
          //  SharedPreferences sp2 = getSharedPreferences(preffile, MODE_PRIVATE);
          //  Map<String, ?> map = sp2.getAll();


       //     final ArrayList<HashMap<String,String>> LIST = new ArrayList<HashMap<String,String>>();

            Context myContext = createPackageContext(pkg,
                    Context.MODE_WORLD_WRITEABLE); // where com.example is the owning  app containing the preferences

            SharedPreferences prefs = myContext.getSharedPreferences
                    ("_preferences", Context.MODE_WORLD_READABLE);

       //     SharedPreferences.Editor editor = prefs.edit();
       //     editor.putString(KEY_DPI, strShareValue);
       //     editor.commit();
       //

            Map<String, ?> items = testPrefs .getAll();

            for(String s : items.keySet()){
                //do somthing like String value=  items.get(s).toString());
                if( s.equals(KEY_DPI) ) {

                    mDPIPreference.setText(items.get(s).toString());
                }

            }



            mDPIPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final int dpi = (int) newValue;

                    SharedPreferences.Editor editor = prefs.edit();
                         editor.putInt(KEY_DPI, newValue);
                         editor.commit();
                    return true;
                }
            });
*/


    private static PackageInfo findPackageInfo(PackageManager pm, String pkg, int uid) {
        final String[] packages = pm.getPackagesForUid(uid);
        if (packages != null && pkg != null) {
            final int N = packages.length;
            for (int i = 0; i < N; i++) {
                final String p = packages[i];
                if (pkg.equals(p)) {
                    try {
                        return pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES);
                    } catch (NameNotFoundException e) {
                        Log.w(TAG, "Failed to load package " + pkg, e);
                    }
                }
            }
        }
        return null;
    }

    private void toastAndFinish() {
        Toast.makeText(mContext, R.string.app_not_found_dlg_text, Toast.LENGTH_SHORT).show();
        getActivity().finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

}


//
//
// PackageInfo datainfo;
//try{


        //     datainfo = getPackageInfoNoCheck(info, compatInfo);
        //final Context tempAppContext = mContext.createAppContext(null, info);


 //       UserHandle handle = new UserHandle(ActivityManager.getCurrentUser());

//Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
 //       myContext = mContext.createPackageContext(pkg,
 //       Context.CONTEXT_IGNORE_SECURITY); // where com.example is the owning  app containing the preferences

        /// FileOutputStream fos = myContext.openFileOutput("_preferences.xml", Context.MODE_PRIVATE);
        //  fos.close();


 //       Log.w(TAG, "ELGAN SAYER getPackageCodePath:" + myContext.getPackageCodePath() );
 ///       //   Log.w(TAG, "ELGAN SAYER getFilesDir:" + ContextImpl.getFilesDir() );
 ///       } catch (PackageManager.NameNotFoundException ex)
 //       {
 //       Log.w(TAG, "ELGAN SAYER PackageManager.NameNotFoundException:");
 //       toastAndFinish();
 //       return;
//        }

        /*
        catch (FileNotFoundException ex)
        {
            Log.w(TAG, "ELGAN SAYER PackageManager.FileNotFoundException:");
            toastAndFinish();
            return;
        }
        catch (IOException ex)
        {
            Log.w(TAG, "ELGAN SAYER PackageManager.IOException:");
            toastAndFinish();
            return;
        }*/

 //       prefs = myContext.getSharedPreferences
  //      ("_preferences", Context.MODE_PRIVATE);

        //     SharedPreferences.Editor editor = prefs.edit();
        //     editor.putString(KEY_DPI, strShareValue);
        //     editor.commit();dd
        //

   //     Map<String, ?> items = prefs .getAll();/

 //       if(items.get(KEY_DPI) != null) {
 ///       mDPIPreference.setText(items.get(KEY_DPI).toString());
//        }
    //    else
   //     {
  //      DisplayMetrics metrics = myContext.getResources().getDisplayMetrics();
 //       //DisplayMetrics metrics = myContext.getDisplayMetrics();
//        mDPIPreference.setText( Integer.toString( metrics.densityDpi )  );
  //      }
//
 //       if(items.get(KEY_FONT) != null) {
 //       mFontPreference.setText(items.get(KEY_FONT).toString());
 //       }
 //       else
 //       {
 //       mFontPreference.setText("1.0");
 //       }
//
 //       getPreferenceScreen().getSharedPreferences()
 //       .registerOnSharedPreferenceChangeListener(this);
//
/*
        for(String s : items.keySet()){
            //do somthing like String value=  items.get(s).toString());
            if( s.equals(KEY_DPI) ) {

                mDPIPreference.setText(items.get(s).toString());
            }

            if( s.equals(KEY_FONT) ) {

                mFontPreference.setText(items.get(s).toString());
            }
        }
*/

        // mFontPreference.setOnPreferenceChangeListener(AppDisplaySettings.this);
        // mDPIPreference.setOnPreferenceChangeListener(AppDisplaySettings.this);

    /*
    mDPIPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            int newDpi = Integer.parseInt(newValue.toString());
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_DPI, newDpi);
            editor.commit();


            Log.w(TAG, "ELGAN SAYER saving KEY_DPI :" + Integer.toString(newDpi) );

            return true;
        }
    });

*/
//
//