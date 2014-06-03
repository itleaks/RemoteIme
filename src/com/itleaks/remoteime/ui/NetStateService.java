/*
 * This software is intended to be a network InputMethod of android platform
 *
 * Copyright (c) 2014 Itleaks shen itleaks@126.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.itleaks.remoteime.ui;

import com.itleaks.remoteime.model.IRemoteImeEventListener;
import com.itleaks.remoteime.model.RemoteImePolicy;
import com.itleaks.remoteime.model.SessionModels;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;

public class NetStateService extends Service {
	private final static boolean DEBUG = true;
	private final static String TAG = "NetStateService";
	
    private ConnectivityManager connectivityManager;
    private NetworkInfo info;
    private static IRemoteImeEventListener mImeEventListener;
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            	checkNetwork();
            }
        }
    };
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "service create");
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mReceiver, mFilter);
        checkNetwork();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.d(TAG, "service destroy");
        unregisterReceiver(mReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
    
    public static RemoteImePolicy mPolicy = null;
    public void checkNetwork() {
        if (DEBUG) Log.d(TAG, "network changes");
         connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
         info = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI); 
         if(info != null && info.isConnected()) {
        	 if(mPolicy == null) {
        		 mPolicy = new RemoteImePolicy();
        		 mPolicy.setListener((MApplication)getApplicationContext());
        	 }
        	 SessionModels.Utils.updateLocalIpAddress();
         	 if (DEBUG) Log.d(TAG, "wifi is connected");
         } else {
        	 if (mPolicy != null) {
        		 mPolicy.destroy();
        		 mPolicy = null;
        	 }
         	 if (DEBUG) Log.d(TAG, "wifi is disconnected");
         }
    }

	public static void start(Context context) {
		// TODO Auto-generated method stub
        if (DEBUG) Log.d(TAG, "start");
		Intent intent = new Intent();
		intent.setClass(context, NetStateService.class);
		context.startService(intent);
	}
}

