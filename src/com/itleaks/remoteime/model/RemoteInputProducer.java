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

import android.os.Handler;
import android.util.Log;

import com.itleaks.remoteime.model.RemoteImePolicy.CommandData;

public class RemoteInputProducer extends ImeSessionManager {
	private static final String TAG = "RemoteInputProducer";
	private static final boolean DEBUG = true;

	ImeSession mServeSession = null;
	private Handler mHandler;
	
	public RemoteInputProducer(RemoteImePolicy policy) {
		super(policy);
		mHandler = new Handler();
	}
	
	@Override
	protected void handleCommandInner(ImeSession session, CommandData command) {
		// TODO Auto-generated method stub
		if (DEBUG) Log.d(TAG, "handleCommand " + session + " " + command);
		if (RemoteImePolicy.COMMAND_SEARCH_REMOTE_IME.equals(command.command)) {
			if (mServeSession == null) {
				//Only support one remote inputMethod
				session = createClientSession(command.ip);
				session.setStatus(ImeSession.STATUS_CONNECTING);
				writeCommand(session, RemoteImePolicy.COMMAND_CONNECTING);
				mServeSession = session;
				mHandler.removeCallbacks(mTimeOutRunnable);
				mHandler.postDelayed(mTimeOutRunnable, 2000);
			}
			return;
		}
		if(session != null && RemoteImePolicy.COMMAND_CONNECTED.equals(command.command)){
			if (session.getStatus() == ImeSession.STATUS_CONNECTING) {
				mHandler.removeCallbacks(mTimeOutRunnable);
				session.setStatus(ImeSession.STATUS_CONNECTED);
			}
			return;
		}
		//Handle command by common api
		super.handleCommandInner(session, command);
	}
	
	public void writeData(byte[] value){
		writeData(mServeSession, value);
	}
	
	public void writeCommand(byte[] value){
		writeCommand(mServeSession, value);
	}
	
	public boolean isConnected() {
		return mServeSession != null && 
				mServeSession.getStatus() == ImeSession.STATUS_CONNECTED;
	}
	
	Runnable mTimeOutRunnable = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (mServeSession.getStatus() != ImeSession.STATUS_CONNECTING) {
				Log.d(TAG, "Connecting Timeout after 2s");
				removeSession(mServeSession);
			}
		}
	};
	
	@Override
	public void removeSession(ImeSession session) {
		if (session == mServeSession) {
			mServeSession = null;
		}
		super.removeSession(session);
	}

	public int getStatus() {
		// TODO Auto-generated method stub
		return mServeSession == null ? ImeSession.STATUS_INVALID : mServeSession.getStatus();
	}
}
