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

import com.itleaks.remoteime.model.RemoteImePolicy.CommandData;

public class RemoteInputConsumer extends ImeSessionManager {
	private static final String TAG = "RemoteInputConsumer";
	private static final boolean DEBUG = true;

	ImeSession mServeSession;
	
	public RemoteInputConsumer(RemoteImePolicy policy) {
		super(policy);
	}
	
	@Override
	protected void handleCommandInner(ImeSession session, CommandData command) {
		// TODO Auto-generated method stub
		if (DEBUG) Log.d(TAG, "handleCommand " + session + " " + command);
		if(RemoteImePolicy.COMMAND_ONLINE.equals(command.command) &&
				mServeSession == null) {
			session = createClientSession(command.ip);
			writeCommand(session, RemoteImePolicy.COMMAND_SEARCH_REMOTE_IME);
			return;
		}
		
		if(session != null && RemoteImePolicy.COMMAND_CONNECTING.equals(command.command)) {
			if (mServeSession != null) {
				//This consumer has found producer, so skip any command
				writeCommand(session, RemoteImePolicy.COMMAND_BUSY);
				return;
			} else if (session.getStatus() == ImeSession.STATUS_IDLE) {
				writeCommand(session, RemoteImePolicy.COMMAND_CONNECTED);
				session.setStatus(ImeSession.STATUS_CONNECTED);
				mServeSession = session;
			}
		}
		super.handleCommandInner(session, command);
	}
	
	@Override
	public void removeSession(ImeSession session) {
		if (DEBUG) Log.d(TAG, "remove session " + session);
		if (session == mServeSession) {
			mServeSession = null;
		}
		super.removeSession(session);
	}
	
	public boolean isConnected() {
		return mServeSession != null && 
				mServeSession.getStatus() == ImeSession.STATUS_CONNECTED;
	}

	public int getStatus() {
		// TODO Auto-generated method stub
		return mServeSession == null ? ImeSession.STATUS_INVALID : mServeSession.getStatus();
	}
}
