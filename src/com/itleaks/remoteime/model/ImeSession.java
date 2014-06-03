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

package com.itleaks.remoteime.model;

import android.util.Log;

import com.itleaks.remoteime.model.SessionManager.Session;

public class ImeSession {
	private static final String TAG = "ImeSession";
	private static final boolean DEBUG = true;
	
	public final static int STATUS_INVALID    = 0;
	public final static int STATUS_IDLE       = 1;
	public final static int STATUS_SEARCH     = 2;
	public final static int STATUS_CONNECTING = 3;
	public final static int STATUS_CONNECTED  = 4;
	
	Session mSession;
	int mStatus;

	public ImeSession(Session session) {
		mStatus = STATUS_IDLE;
		mSession = session;
	}
	
	public int getStatus() {
		return mStatus;
	}
	 
	public void setStatus(int status) {
		if (DEBUG) Log.d(TAG, "new status:" + getStatusStr(status));
		mStatus = status;
	}
	
	public static String getStatusStr(int status) {
		switch(status) {
		case STATUS_IDLE:
			return "idler";
		case STATUS_SEARCH:
			return "search";
		case STATUS_CONNECTING:
			return "connecting";
		case STATUS_CONNECTED:
			return "connected";
		default:
			return "invalid status";
		}
	}
}
