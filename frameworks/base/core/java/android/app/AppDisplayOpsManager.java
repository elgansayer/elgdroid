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

package android.app;

import android.Manifest;
import android.annotation.SystemApi;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.media.AudioAttributes.AttributeUsage;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;

import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsService;

import com.android.internal.app.IAppDisplayOpsService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

 
public class AppDisplayOpsManager {
  
    final Context mContext;
    final IAppDisplayOpsService mService;
 
    
  /** These settings are per app, so should not be returned in global search results. */
  public static class AppDisplayRow implements Parcelable{

      private String pkgName;
      private int dpi;
      private int fontScale;

      public AppDisplayRow()
      {
	  this.pkgName = "android";
	  this.fontScale = 0;
	  this.dpi = 0;
      }

      public void setPackageName(String name)
      {
	  this.pkgName = new String(name);
      }
      public String getPackageName()
      {
	  return this.pkgName;
      }

      public void setDPI(int newDPI)
      {
	  this.dpi = new Integer(newDPI);
      }
      public int getDPI()
      {
	  return this.dpi;
      }

      public void setFontScale(int fontScale)
      {
	  this.fontScale = new Integer(fontScale);;
      }
      public int getFontScale()
      {
	  return this.fontScale;
      }

      public boolean isValid()
      {
	    int total = this.dpi + this.fontScale;
        return (total > 0);
      }





      @Override
      public int describeContents() {
          return 0;
      }

      @Override
      public void writeToParcel(Parcel dest, int flags) {


          dest.writeString(pkgName);
          dest.writeInt(fontScale);
          dest.writeInt(dpi);

      }

      AppDisplayRow(Parcel source) {


          pkgName = source.readString();
          fontScale = source.readInt();
          dpi = source.readInt();


      }

      public static final Creator<AppDisplayRow> CREATOR = new Creator<AppDisplayRow>() {
          @Override public AppDisplayRow createFromParcel(Parcel source) {
              return new AppDisplayRow(source);
          }

          @Override public AppDisplayRow[] newArray(int size) {
              return new AppDisplayRow[size];
          }
      };      
  }
  
  
    AppDisplayOpsManager(Context context, IAppDisplayOpsService service) {
        mContext = context;
        mService = service;
    }

    
}
 