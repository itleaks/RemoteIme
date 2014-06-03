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

import com.itleaks.remoteime.model.SessionManager.ISessionEventListener;
import com.itleaks.remoteime.model.SessionManager.Session;

import android.util.Log;

public class RemoteImePolicy implements ISessionEventListener {
	public final static String TAG = "RemoteImePolicy";
	public final static boolean DEBUG = true;

	public final static String MAGIC_HEADER = "REMOTEIME";
	public final static String SPLIT_CHAR = ":";

	public final static String COMMAND_SEARCH_REMOTE_IME   = "searchime";
	public final static String COMMAND_BUSY        = "busy";
	public final static String COMMAND_CONNECTING  = "connecting";
	public final static String COMMAND_CONNECTED   = "connected";
	public final static String COMMAND_OFFLINE     = "offline";
	public final static String COMMAND_ONLINE      = "online";
	public final static String COMMAND_KEY         =  "key";
	public final static String COMMAND_INVALID_STATUS =  "invalid-status";

	private SessionManager mSessionManager;
	private IRemoteImeEventListener mListener;
	private RemoteInputConsumer mInputConsumer;
	private RemoteInputProducer mInputProducer;
	public HashMap<String, Session> mClientSessions;
	public HashMap<String, Session> mServerSessions;
	
	public class CommandData {
		String ip;
		String command;
		String content;

		public CommandData(String value) {
			parse(value);
		}
		
		private void parse(String value) {
			String[] datas = value.split(SPLIT_CHAR);
			if (datas != null && datas.length >= 3 && datas[0].equals(MAGIC_HEADER)) {
				ip = datas[1];
				command = datas[2];
				if (datas.length > 3) {
					content = datas[3];
				}
			}
		}
		
		@Override
		public String toString() {
			return "Command:" + command + " ip:" + ip + " content:" + content;
		}
	}

	public RemoteImePolicy() {
		init(new SessionManager(this));
	}

	public void init(SessionManager session) {
		// TODO Auto-generated method stub
		mSessionManager = session;
		mClientSessions = new HashMap<String, Session>();
		mServerSessions = new HashMap<String, Session>();
		mInputConsumer = new RemoteInputConsumer(this);
		mInputProducer = new RemoteInputProducer(this);
	}
	
	public void online() {
		if (DEBUG) Log.d(TAG, "online");
		broadcast(COMMAND_ONLINE);
	}

	public void offline() {
		if (DEBUG) Log.d(TAG, "offline");
		broadcast(COMMAND_OFFLINE);
	}
	
	public void searchRemoteIme() {
		if (DEBUG) Log.d(TAG, "search remote ime");
		broadcast(COMMAND_SEARCH_REMOTE_IME);
	}
	
	public void setListener(IRemoteImeEventListener listener) {
		mListener = listener;
	}

	public void writeData(byte[] value){
		mInputProducer.writeData(value);
	}

	public void writeData(Session session, byte[] value) {
		mSessionManager.writeData(session, value);
	}
	
	public void writeCommand(String command) {
		String data = MAGIC_HEADER + SPLIT_CHAR + SessionModels.Utils.getLocalIpAddress();
		data += SPLIT_CHAR + command;
		mInputProducer.writeCommand(data.getBytes());
	}
	
	public void writeCommand(String command, String Content) {
		String data = MAGIC_HEADER + SPLIT_CHAR + SessionModels.Utils.getLocalIpAddress();
		data += SPLIT_CHAR + command;
		data += SPLIT_CHAR + Content;
		mInputProducer.writeCommand(data.getBytes());
	}

	public void broadcast(String command) {
		String data = MAGIC_HEADER + SPLIT_CHAR + SessionModels.Utils.getLocalIpAddress();
		data += SPLIT_CHAR + command;
		mSessionManager.broadcast(data.getBytes());
	}

	public void writeCommand(Session session, String command) {
		String data = MAGIC_HEADER + SPLIT_CHAR + SessionModels.Utils.getLocalIpAddress();
		data += SPLIT_CHAR + command;
		mSessionManager.writeCommand(session, data.getBytes());
	}
	
	public void writeCommand(Session session, byte[] buf) {
		mSessionManager.writeCommand(session, buf);
	}

	@Override
	public void onBroadcastRecieved(Session session, byte[] data, int length) {
		// TODO Auto-generated method stub
		if (length <= 0) {
			Log.e(TAG, "o lenght");
			return;
		}
		String content = new String(data, 0, length);
		if(DEBUG) Log.e(TAG, "onBroadcastRecieved " + content);
		handleCommand(session, content);
	}

	@Override
	public void onCommandRecieved(Session session, byte[] data, int length) {
		// TODO Auto-generated method stub
		if (length <= 0) {
			Log.e(TAG, "o lenght");
			return;
		}
		String content = new String(data, 0, length);
		if(DEBUG) Log.e(TAG, "onCommandRecieved " + content);
		handleCommand(session, content);
	}

	@Override
	public void onDataRecieved(Session session, byte[] data, int length) {
		// TODO Auto-generated method stub
		if (length <= 0) {
			Log.e(TAG, "o lenght");
			return;
		}
		String content = new String(data, 0, length);
		if(DEBUG) Log.d(TAG, "onDataRecieved " + content);
		if (mListener != null) {
			mListener.onTextRecieved(content);
		}
	}

	@Override
	public void onError(Session session, int code) {
		// TODO Auto-generated method stub
		if(DEBUG) Log.e(TAG, "onError " + session);
		if (session != null) {
			mInputProducer.onError(session, code);
			mInputConsumer.onError(session, code);
			removeSession(session);
		}
		//TODO
		if (mListener != null) {
			mListener.onError(code);
			mListener.onStatusChanged(getProducerStatus(), getConsumerStatus());
		}
	}

	private void handleCommand(Session session, String data) {
		CommandData command = new CommandData(data);
		if(DEBUG) Log.e(TAG, "handleCommand " + command);
		if (COMMAND_SEARCH_REMOTE_IME.equals(command)) {
			mInputConsumer.handleCommand(session, command);
		} else {
			mInputProducer.handleCommand(session, command);
			mInputConsumer.handleCommand(session, command);
		}
		if(COMMAND_OFFLINE.equals(command)) {
			removeSession(command.ip);
		}
		if (mListener != null) {
			mListener.onCommandRecieved(command.command, command.content);
			mListener.onStatusChanged(getProducerStatus(), getConsumerStatus());
		}
	}

	public void destroy() {
		if (mSessionManager != null) {
			mSessionManager.destroy();
			mSessionManager = null;
		}
	}
	
	public Session getClientSession(String serveIp) {
		//One target ip, one client session
		if (mClientSessions.get(serveIp) != null) {
			//TODO:Current, serveIp is same as session's tag
			String tag = serveIp;
			return mClientSessions.get(tag);
		} else {
			Session session = mSessionManager.createClientSession(serveIp);
			return session;
		}
	}

	@Override
	public void onSessionCreated(Session session) {
		if (session.isClient()) {
			mClientSessions.put(session.getTag(), session);
		} else {
			mServerSessions.put(session.getTag(), session);
		}
	}
	
	public void removeSession(Session session) {
		// TODO Auto-generated method stub
		mClientSessions.remove(session.getTag());
		mServerSessions.remove(session.getTag());
		mSessionManager.removeSession(session);
	}
	
	public void removeSession(String ip) {
		// TODO Auto-generated method stub
		List<Session> removes = new ArrayList<Session>();
		Iterator iter = mClientSessions.entrySet().iterator();
		while (iter.hasNext()) {
		    Map.Entry entry = (Map.Entry) iter.next();
		    Session session = (Session)entry.getKey();
		    if (session.getServeIp().equals(ip)) {
		    	removes.add(session);
		    }
		}
		iter = mServerSessions.entrySet().iterator();
		while (iter.hasNext()) {
		    Map.Entry entry = (Map.Entry) iter.next();
		    Session session = (Session)entry.getKey();
		    if (session.getServeIp().equals(ip)) {
		    	removes.add(session);
		    }
		}
		for (int i=0; i<removes.size(); i++) {
			removeSession(removes.get(i));
		}
	}

	public boolean isConnected() {
		// TODO Auto-generated method stub
		return mInputProducer.isConnected();
	}

	public int getProducerStatus() {
		// TODO Auto-generated method stub
		return mInputProducer.getStatus();
	}
	
	public int getConsumerStatus() {
		// TODO Auto-generated method stub
		return mInputConsumer.getStatus();
	}
}
