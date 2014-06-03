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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.itleaks.remoteime.model.RemoteImePolicy.CommandData;
import com.itleaks.remoteime.model.SessionManager.Session;

public class ImeSessionManager {
	private static final String TAG = "ImeSessionManager";
	private static final boolean DEBUG = true;
	
	RemoteImePolicy mPolicy;
	HashMap<Session, ImeSession> mSessions;
	
	public ImeSessionManager(RemoteImePolicy policy) {
		mPolicy = policy;
		mSessions = new HashMap<Session, ImeSession>();
	}
	
	public void handleCommand(Session session, CommandData command) {
		if (DEBUG) Log.d(TAG, "handleCommand session " + session + " command:" + command);
		handleCommandInner(getSession(session), command);
	}
	
	protected void handleCommandInner(ImeSession session, CommandData command) {
		if(RemoteImePolicy.COMMAND_OFFLINE.equals(command)) {
			removeSession(command.ip);
			return;
		}
		return;
	}
	
	public void writeData(byte[] value){
	}
	
	public void writeCommand(byte[] value){
	}
	
	public void writeData(ImeSession session, byte[] value){
		mPolicy.writeData(session.mSession, value);
	}
	
	public void writeCommand(ImeSession session, byte[] buf) {
		mPolicy.writeCommand(session.mSession, buf);
	}
	
	public void writeCommand(ImeSession session, String command) {
		mPolicy.writeCommand(session.mSession, command);
	}
	
	public ImeSession createClientSession(String ip) {
		Session session = mPolicy.getClientSession(ip);
		return createSession(session);
	}
	
	public ImeSession createSession(Session session) {
		ImeSession imeSession = new ImeSession(session);
		mSessions.put(session, imeSession);
		return imeSession;
	}
	
	public void removeSession(ImeSession session) {
		if (session != null) {
			mSessions.remove(session.mSession);
			mPolicy.removeSession(session.mSession);
		} else {
			Log.e(TAG, "remove session null");
		}
	}
	
	public void removeSession(String ip) {
		Iterator iter = mSessions.entrySet().iterator();
		List<ImeSession> removes = new ArrayList<ImeSession>();
		while (iter.hasNext()) {
		    Map.Entry entry = (Map.Entry) iter.next();
		    ImeSession session = (ImeSession)entry.getKey();
		    if (session.mSession.getServeIp().equals(ip)) {
		    	removes.add(session);
		    }
		}
		for (int i=0; i<removes.size(); i++) {
			removeSession(removes.get(i));
		}
	}
	
	public ImeSession getSession(Session session) {
		return getSession(session, true);
	}
	
	public ImeSession getSession(Session session, boolean autoCreate) {
		if (session == null) {
			return null;
		}
		ImeSession imeSession = mSessions.get(session);
		if (imeSession == null && autoCreate) {
			imeSession = createSession(session);
		}
		return imeSession;
	}

	public void onSessionCreated(Session session) {
		// TODO Auto-generated method stub
		ImeSession imeSession = mSessions.get(session);
		if (imeSession == null) {
			imeSession = new ImeSession(session);
			mSessions.put(session, imeSession);
		}
	}
	
	public boolean isConnected() {
		return false;
	}
	
	public boolean onError(Session session, int code) {
		// TODO Auto-generated method stub
		return onError(getSession(session, false), code);
	}
	
	public boolean onError(ImeSession session, int code) {
		// TODO Auto-generated method stub
		if (session != null) {
			removeSession(session);
			return true;
		} else {
			return false;
		}
	}
}
