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

import java.util.ArrayList;

import android.app.Application;
import android.util.Log;

import com.itleaks.remoteime.model.IRemoteImeEventListener;
import com.itleaks.remoteime.model.RemoteImePolicy;

public class MApplication extends Application implements IRemoteImeEventListener {
	private ArrayList<IRemoteImeEventListener> mImeEventListeners =
				new ArrayList<IRemoteImeEventListener>();
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	public void addImeEventListener(IRemoteImeEventListener listener) {
		if (!mImeEventListeners.contains(listener)) {
			mImeEventListeners.add(listener);
		}
	}
	
	public void removeImeEventListener(IRemoteImeEventListener listener) {
		if (mImeEventListeners.contains(listener)) {
			mImeEventListeners.remove(listener);
		}
	}
	
	@Override
	public void onCommandRecieved(String command, String content) {
		for (int i=0; i<mImeEventListeners.size(); i++) {
			mImeEventListeners.get(i).onCommandRecieved(command, content);
		}
	}

	@Override
	public void onError(int code) {
		// TODO Auto-generated method stub
		for (int i=0; i<mImeEventListeners.size(); i++) {
			mImeEventListeners.get(i).onError(code);
		}
	}

	@Override
	public void onStatusChanged(int status, int status2) {
		// TODO Auto-generated method stub
		for (int i=0; i<mImeEventListeners.size(); i++) {
			mImeEventListeners.get(i).onStatusChanged(status, status2);
		}
	}

	@Override
	public void onTextRecieved(String text) {
		// TODO Auto-generated method stub
		for (int i=0; i<mImeEventListeners.size(); i++) {
			mImeEventListeners.get(i).onTextRecieved(text);
		}
	}
}
