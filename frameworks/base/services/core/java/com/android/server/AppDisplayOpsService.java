/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.server;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//import android.app.AppDisplayOpsManager.AppDisplayRow;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AppGlobals;

import android.app.AppDisplayOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioAttributes;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.storage.MountServiceInternal;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.Xml;
//

import com.android.internal.app.IAppDisplayOpsService;

//import com.android.internal.app.IAppDisplayOpsCallback;

import com.android.internal.os.Zygote;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;

import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
import android.os.Environment;
import java.lang.Integer;
import java.lang.Object;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.*;
import java.util.Map.*;
import java.util.HashMap.*;

import android.util.Log;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.SQLException;
import android.app.ActivityThread;


import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.*;

import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import com.android.internal.util.*;








public class AppDisplayOpsService extends IAppDisplayOpsService.Stub	 {

    static final String TAG = "ELGAN SAYER AppDisplayOps";
    static final boolean DEBUG = false;

    // Write at most every 30 minutes.
    static final long WRITE_DELAY = DEBUG ? 1000 : 30*60*1000;

    Context mContext;
    final AtomicFile mFile;
    final Handler mHandler;

    boolean mWriteScheduled;
    boolean mFastWriteScheduled;
    final Runnable mWriteRunner = new Runnable() {
        public void run() {
            synchronized (AppDisplayOpsService.this) {
                mWriteScheduled = false;
                mFastWriteScheduled = false;
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    @Override protected Void doInBackground(Void... params) {
                        writeState();
                        return null;
                    }
                };
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
            }
        }
    };

    final SparseArray<UidState> mUidStates = new SparseArray<>();

  
    private static final class UidState {
        public final int uid;
        Ops pkgDisplayOps;

        public UidState(int uid) {
            this.uid = uid;
        }

        public void clear() {
            pkgDisplayOps = null;
        }

        public boolean isDefault() {
            return (true);
        }
    }

    public final static class Ops extends SparseArray<Op> {
        public final String packageName;
        public final UidState uidState;
        public final boolean isPrivileged;

        public Ops(String _packageName, UidState _uidState, boolean _isPrivileged) {
            packageName = _packageName;
            uidState = _uidState;
            isPrivileged = _isPrivileged;
        }
    }

    public final static class Op {
        public final int uid;
        public final String packageName;
        public int dpi;
        public int fontScale;
        
        public Op(int _uid, String _packageName,int _dpi, int _fontScale) {
            uid = _uid;
            packageName = _packageName;
            dpi = _dpi;
            fontScale = _fontScale;
        }
    }
 
    public AppDisplayOpsService(File storagePath, Handler handler) {
        mFile = new AtomicFile(storagePath);
        mHandler = handler;
        readState();
    }

    public void publish(Context context) {
        mContext = context;
        ServiceManager.addService(Context.APP_DISPLAY_OPS_SERVICE, asBinder() );
    }


    public void systemReady() {
        synchronized (this) {
	  
        }
    }

    public void packageRemoved(int uid, String packageName) {
    
        synchronized (this) {

        }
    }

    public void uidRemoved(int uid) {
        synchronized (this) {
            if (mUidStates.indexOfKey(uid) >= 0) {
                mUidStates.remove(uid);
                scheduleFastWriteLocked();
            }
        }
    }

    public void shutdown() {
        Slog.w(TAG, "Writing app ops before shutdown...");
        boolean doWrite = false;
        synchronized (this) {
            if (mWriteScheduled) {
                mWriteScheduled = false;
                doWrite = true;
            }
        }
        if (doWrite) {
            writeState();
        }
    }


    private void scheduleWriteLocked() {
        if (!mWriteScheduled) {
            mWriteScheduled = true;
            mHandler.postDelayed(mWriteRunner, WRITE_DELAY);
        }
    }

    private void scheduleFastWriteLocked() {
        if (!mFastWriteScheduled) {
            mWriteScheduled = true;
            mFastWriteScheduled = true;
            mHandler.removeCallbacks(mWriteRunner);
            mHandler.postDelayed(mWriteRunner, 10*1000);
        }
    }
 
  
    void readState() {
        synchronized (mFile) {
            synchronized (this) {
	    }
        }
    }

    void writeState() {
        synchronized (mFile) {
       
        }
    }


    private void checkSystemUid(String function) {
        int uid = Binder.getCallingUid();
        if (uid != Process.SYSTEM_UID) {
            throw new SecurityException(function + " must by called by the system");
        }
    }

    private static String[] getPackagesForUid(int uid) {
        String[] packageNames = null;
        try {
            packageNames= AppGlobals.getPackageManager().getPackagesForUid(uid);
        } catch (RemoteException e) {
            /* ignore - local call */
        }
        if (packageNames == null) {
            return EmptyArray.STRING;
        }
        return packageNames;
    }
    
    
    
    
    
    
    
    
    
    

    class AppContainer
    {
        String hello= "hello";
    }

    String fuckYou;
    
    private static HashMap<String, AppDisplayOpsManager.AppDisplayRow> pkgMap = new HashMap<String, AppDisplayOpsManager.AppDisplayRow>();
   // private HashMap<String, AppDisplayOpsManager.AppDisplayRow> pkgMap = new HashMap<String, AppDisplayOpsManager.AppDisplayRow>();;
   private static List<AppContainer> apptest = new ArrayList<AppContainer>();
 
 

 boolean isOpen = false;
    public void open() {

        if(isOpen)
        {
            Log.w(TAG, "SKIPPED OPEN isOpen != true");
            return ;
        }

        isOpen = true;
 

        Log.w(TAG, "OPENED mDbHelper");

        readData();

 
        try {

            File dataDir = Environment.getDataDirectory();
            File systemDir = new File(dataDir, "system");
            systemDir.mkdirs();

            File file = new File(systemDir, "mytestDB.xml");
            //FileInputStream fileIn = systemContext.openFileInput("mytestDB.xml");
            FileInputStream fileIn = new FileInputStream(file);

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);

            XmlPullParser parser = factory.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(fileIn, null);
            parser.nextTag();


            parser.require(XmlPullParser.START_TAG, "settings", "settings");

            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }

                String name = parser.getName();

                // Starts by looking for the entry tag
                if (name.equals("package")) {

                    AppDisplayOpsManager.AppDisplayRow row = readPackage(parser);

                    synchronized (pkgMap) {
                        pkgMap.put(new String(row.getPackageName()), row);

                    }

                    synchronized (apptest) {
                        apptest.add(new AppContainer());

                    }

                    Log.w(TAG, "read in row : DPI " + row.getDPI() + " font " + row.getFontScale() + " for " + row.getPackageName());


                } else {
                    // skip(parser);
                }
            }

        }
        catch (XmlPullParserException ex) {
            Log.e(TAG, "ELGAN SAYER >> " + ex.toString());
        }
        catch (IOException ex) {
            Log.e(TAG, "ELGAN SAYER >> " + ex.toString());
        }
        finally{
            //fileIn.close();
        }

        Log.e(TAG, "ELGAN SAYER FINISHED OPEN ");

        Log.w(TAG, "fuckYou " + fuckYou );

        Log.w(TAG, "readData size " + pkgMap.size() ); 
    }



    // Adding new contact
    public void insertRow(String pkgname, AppDisplayOpsManager.AppDisplayRow appDisplaySettings) {

        if(!appDisplaySettings.isValid())
        {
            Log.w(TAG, "FAILED KEY_DPI : " + Integer.toString( appDisplaySettings.getDPI() ) + " for " + pkgname);
            Log.w(TAG, "FAILED KEY_FONTSCALE : " + Integer.toString( appDisplaySettings.getFontScale() ) + " for " + pkgname);


            Log.e(TAG, ">> FAILED insertWithOnConflict " + pkgname);
            return ;
        }

        pkgMap.put(new String(pkgname), appDisplaySettings );
        apptest.add(new AppContainer());

        synchronized (pkgMap) {
            pkgMap.put(new String(pkgname), appDisplaySettings);

        }

        synchronized (apptest) {
            apptest.add(new AppContainer());

        }

        Log.w(TAG, " saving AppDisplayOpsManager.AppDisplayRow for " + pkgname);
        Log.w(TAG, " Size now " + pkgMap.size());


        Log.w(TAG, " apptest Size now " + apptest.size());

        writeData();

        return;
    }


    // Getting single contact
    public AppDisplayOpsManager.AppDisplayRow getAppDisplayRow(String pkgName) {

        if(pkgMap == null)
        {
            Log.w(TAG, "pkgMap == null " );

            return new AppDisplayOpsManager.AppDisplayRow();
        }
        else
        {
            Log.w(TAG, "pkgMap IS NOT NULL ");
        }

        Log.w(TAG, "pkgMap size " + pkgMap.size());
        Log.w(TAG, "size " + pkgMap.size());
        Log.w(TAG, "fuckYou " + fuckYou);
        Log.w(TAG, " apptest Size now " + apptest.size());


        synchronized (apptest) {
            Log.w(TAG, "synchronized apptest Size now " + apptest.size());
        }

        synchronized (pkgMap) {
            Log.w(TAG, "synchronized pkgMap Size now " + pkgMap.size());
        }


        AppDisplayOpsManager.AppDisplayRow row = (AppDisplayOpsManager.AppDisplayRow) pkgMap.get(pkgName);
        if(row == null)
        {
            Log.w(TAG, "row == null " + pkgMap.size() );
            row = new AppDisplayOpsManager.AppDisplayRow();

            for (Map.Entry<String, AppDisplayOpsManager.AppDisplayRow> item : pkgMap.entrySet()) {
                String pkgNames = item.getKey();
                //String value = item.getValue();

                AppDisplayOpsManager.AppDisplayRow rowD = (AppDisplayOpsManager.AppDisplayRow) item.getValue();

                Log.w(TAG, "print row : DPI " + rowD.getDPI() + " font " + rowD.getFontScale() + " for " + pkgNames);
            }

        }

        Log.w(TAG, "getAppDisplayOpsManager.AppDisplayRow row : DPI " + row.getDPI() + " font " + row.getFontScale() + " for " + pkgName);

        return row;
    }

    public void writeData()
    {

        try {
            File dataDir = Environment.getDataDirectory();
            File systemDir = new File(dataDir, "system");
            systemDir.mkdirs();

            File file = new File(systemDir, "mytestDB.xml");
            FileOutputStream fos = new FileOutputStream(file); //systemContext.openFileOutput("mytestDB.xml", Context.MODE_PRIVATE );


          //  fos = new FileOutputStream( fileIn );

            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fos, StandardCharsets.UTF_8.name());
            out.startDocument(null, true);
            out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            out.startTag(null, "settings");

            // for (Map.Entry<V, Integer> service : user.persistentServices.////entrySet())
            // {

            for (Map.Entry<String, AppDisplayOpsManager.AppDisplayRow> item : pkgMap.entrySet())
            {
                String pkgNames = item.getKey();
                AppDisplayOpsManager.AppDisplayRow row = item.getValue();

                out.startTag(null, "package");
                out.attribute(null, "packageName", row.getPackageName());
                out.attribute(null, "dpi", Integer.toString( row.getDPI() ));
                out.attribute(null, "fontScale", Integer.toString( row.getFontScale() ));
                // mSerializerAndParser.writeAsXml(service.getKey(), out);
                out.endTag(null, "package");
            }

            out.endTag(null, "settings");
            out.endDocument();

            Log.w(TAG, "ELGAN SAYER document written");

            //    atomicFile.finishWrite(fos);
        } catch (IOException e1) {
            Log.e(TAG, "ELGAN SAYER Error writing file", e1);

        }

    }


    public  void readData()
    {
        fuckYou = "fuckYou";

    }


    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
// to their respective "read" methods for processing. Otherwise, skips the tag.
    /* @hide */
    private AppDisplayOpsManager.AppDisplayRow readPackage(XmlPullParser parser) throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, "settings", "package");

        String packageName = null;
        int dpi = 0;
        int fontScale = 0;

        while (parser.next() != XmlPullParser.END_TAG)
        {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals("packageName")) {
                packageName = readPackageName(parser);
            } else if (name.equals("dpi")) {
                dpi = readDPI(parser);
            } else if (name.equals("link")) {
                fontScale = readFontScale(parser);
            } else {
                //skip(parser);
            }
        }

        AppDisplayOpsManager.AppDisplayRow row = new AppDisplayOpsManager.AppDisplayRow();

        row.setPackageName(packageName);
        row.setFontScale(fontScale);
        row.setDPI(dpi);
        return row;
    }

    // Processes title tags in the feed.
    /* @hide */
    private String readPackageName(XmlPullParser parser) throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, "settings", "packageName");

        String packageName = readText(parser);
        parser.require(XmlPullParser.END_TAG, "settings", "packageName");
        return packageName;
    }

    // Processes link tags in the feed.
    /* @hide */
    private int readDPI(XmlPullParser parser) throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, "settings", "dpi");

        String dpi = readText(parser);
        parser.require(XmlPullParser.END_TAG, "settings", "dpi");
        return Integer.valueOf(dpi);
    }

    // Processes summary tags in the feed.
    /* @hide */
    private int readFontScale(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, "settings", "fontScale");

        String fontScale = readText(parser);
        parser.require(XmlPullParser.END_TAG, "settings", "fontScale");
        return Integer.valueOf(fontScale);
    }

    // For the tags title and summary, extracts their text values.
    /* @hide */
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }


}
