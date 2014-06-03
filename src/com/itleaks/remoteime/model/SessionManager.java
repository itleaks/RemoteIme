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

import java.util.HashMap;
import java.util.Iterator;

import com.itleaks.remoteime.model.SessionModels.NetSession;
import com.itleaks.remoteime.model.SessionModels.tcpSessionClient;
import com.itleaks.remoteime.model.SessionModels.tcpSessionServer;
import com.itleaks.remoteime.model.SessionModels.udpSessionServer;
import com.itleaks.remoteime.model.SessionModels.tcpSessionServer.IClientEventListener;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class SessionManager {
	public final static String TAG  = "NetSession";
	public final static boolean DEBUG  = true;


	private boolean mRunning;
	static int BROADCAST_PORT = 8001;
	static int COMMAND_PORT   = 8002;
	static int DATA_PORT      = 8003;

	final static int MSG_DATA_RECIEVE      = 1;
	final static int MSG_COMMAND_RECIEVE   = 2;
	final static int MSG_BROADCAST_RECIEVE = 3;
	final static int MSG_DATA_WRITE        = 4;
	final static int MSG_COMMAND_WRITE     = 5;
	final static int MSG_BROADCAST_WRITE   = 6;
	final static int MSG_ERROR             = 7;
	final static int MSG_RAW_RECIEVE       = 8;

	final static int ERROR_TCP_LOCAL       = 1;
	final static int ERROR_TCP_REMOTE      = 2;

	public volatile Handler mHandler;

	private class WriteData {
		Session session;
		byte[] data;
		int msg;
		public WriteData(Session session, byte[] data, int msg) {
			this.session = session;
			this.data = data;
			this.msg = msg;
		}

		public void write() {
			if (session != null) {
				if (this.msg == MSG_DATA_WRITE) {
					session.writeData(this.data);
				} else if (this.msg == MSG_COMMAND_WRITE) {
					session.writeCommand(this.data);
				}
			}
		}
	}

	private class ReadData {
		Session session;
		byte[] data;
		int msg;
		int length;
		public ReadData(int msg, Session session, byte[] data, int length) {
			this.session = session;
			this.data = data;
			this.msg = msg;
			this.length = length;
		}
	}
	
	public abstract class Session {
		//the ip this object serves
		private String mServeIp;
		private IDataEventListener mListener;
		private final boolean mIsClient;
		
		public Session(String ip, boolean isClient) {
			this(ip, null, isClient);
		}
		
		public Session(String ip, IDataEventListener listener, boolean isClient) {
			mServeIp = ip;
			mListener = listener;
			mIsClient = isClient;
		}
		
		public boolean isClient() {
			return mIsClient;
		}
		
		public IDataEventListener getListener() {
			return mListener;
		}
		
		public void setListener(IDataEventListener listener) {
			mListener = listener;
		}
		
		public String getServeIp() {
			return mServeIp;
		}
		
		public String getTag() {
			return getServeIp();
		}
		
		public void remove() {
			SessionManager.this.removeSession(getTag());
		}
		
		public abstract void writeData(byte[] data);
		public abstract void writeCommand(byte[] data);
		public abstract void destroy();
	}
	
	private interface IDataEventListener {
		public void onDataRecieved(Session session, byte[] data, int length);
		public void onCommandRecieved(Session session, byte[] data, int length);
		public void onError(Session session, int code);
	}

	private class ClientSession extends Session 
		implements SessionModels.INetSessionListener{
		tcpSessionClient mDataClient;
		tcpSessionClient mCommandClient;

		public ClientSession(String ip, IDataEventListener listener) {
			super(ip, listener, true);
			//Connect remote server
			mCommandClient = new tcpSessionClient(ip, COMMAND_PORT, this);
			mDataClient = new tcpSessionClient(ip, DATA_PORT, this);
			mCommandClient.start();
			mDataClient.start();
		}

		public void writeData(byte[] data) {
			mDataClient.write(data);
			if (!mDataClient.isActive()) {
				remove();
			}
		}

		public void writeCommand(byte[] data) {
			mCommandClient.write(data);
			if (!mCommandClient.isActive()) {
				remove();
			}
		}

		public void destroy() {
			if (mDataClient != null) {
				mDataClient.close();
				mDataClient = null;
			}
			if (mCommandClient != null) {
				mCommandClient.close();
				mCommandClient = null;
			}
		}
		
		@Override
		public void onDataRecieved(NetSession session, byte[] data, int length) {
			// TODO Auto-generated method stub
			IDataEventListener listener = getListener();
			if (listener != null) {
				if (DEBUG) Log.d(TAG, "on data received from session " + session);
				if (session == mDataClient) {
					listener.onDataRecieved(this, data, length);
				} else if (session == mCommandClient){
					listener.onCommandRecieved(this, data, length);
				}
			}
		}

		@Override
		public void onError(NetSession session, int code) {
			// TODO Auto-generated method stub
			IDataEventListener listener = getListener();
			if (listener != null) {
				listener.onError(this, code);
			}
		}
	}
	
	private class ServerSession extends Session
		implements SessionModels.INetSessionListener {
		NetSession mNetSession;
		boolean isDataSession;
	    
		public ServerSession(NetSession session, boolean isDataSession, IDataEventListener listener) {
			super(session.getIp(), listener, false);
			//restore the net session
	    	this.isDataSession = isDataSession;
			mNetSession = session;
			mNetSession.setListener(this);
		}

		public void writeData(byte[] data) {
			mNetSession.write(data);
		}

		public void writeCommand(byte[] data) {
			mNetSession.write(data);
		}

		public void destroy() {
			mNetSession.close();
		}
		
		@Override
		public void onDataRecieved(NetSession session, byte[] data, int length) {
			// TODO Auto-generated method stub
			IDataEventListener listener = getListener();
			if (listener != null) {
				if (isDataSession) {
					listener.onDataRecieved(this, data, length);
				} else {
					listener.onCommandRecieved(this, data, length);
				}
			}
		}

		@Override
		public void onError(NetSession session, int code) {
			// TODO Auto-generated method stub
			IDataEventListener listener = getListener();
			if (listener != null) {
				listener.onError(this, code);
			}
		}
		
		public String getTag() {
			return mNetSession.getTag();
		}
	}
	
	public interface ISessionEventListener extends IDataEventListener {
		void onSessionCreated(Session session);
		void onBroadcastRecieved(Session session, byte[] data, int length);
		void onCommandRecieved(Session session, byte[] data, int length);
		void onDataRecieved(Session session, byte[] data, int length);
	}
	
	HashMap<String, Session> mSessions;
	tcpSessionServer mDataServer = null;
	tcpSessionServer mCommandServer = null;
	udpSessionServer mBroadcastServer = null;
	private ISessionEventListener mListener;

	public SessionManager(ISessionEventListener listener) {
		mRunning = true;
		mListener = listener;
		mSessions = new HashMap<String, Session>();
		init();
	}

	void init() {
		class Worker extends Thread {
			final static String TAG = "Netsession Worker";

			public void run() {
				Looper.prepare();
				mHandler = new Handler() {
					public void handleMessage(android.os.Message msg) {
						if(DEBUG) Log.e(TAG, "Message:" + msg.what + " Data length:" + msg.arg1);
						if (msg.arg1 < 0) {
							Log.e(TAG, "Length <0 exception");
						}
						if (mListener == null) {
							Log.e(TAG, "command channel no listener");
							return;
						}
						int what = msg.what;
						switch(what) {
						case MSG_DATA_WRITE:
							((WriteData)msg.obj).write();
							break;
						case MSG_COMMAND_WRITE:
							((WriteData)msg.obj).write();
							break;
						case MSG_RAW_RECIEVE:
							if (msg.obj == null) {
								break;
							}
							ReadData data = (ReadData)msg.obj;
							int command = data.msg;
							if (command == MSG_DATA_RECIEVE) {
								mListener.onDataRecieved(data.session, data.data, data.length);
							} else if (command == MSG_COMMAND_RECIEVE){
								mListener.onCommandRecieved(data.session, data.data, data.length);
							} else if (command == MSG_BROADCAST_RECIEVE){
								mListener.onBroadcastRecieved(data.session, data.data, data.length);
							}
							break;
						case MSG_DATA_RECIEVE:
							if (msg.obj == null) {
								break;
							}
							mListener.onDataRecieved(null, (byte[])msg.obj, msg.arg1);
							break;
						case MSG_COMMAND_RECIEVE:
							if (msg.obj == null) {
								break;
							}
							mListener.onCommandRecieved(null, (byte[])msg.obj, msg.arg1);
							break;
						case MSG_BROADCAST_RECIEVE:
							if (msg.obj == null) {
								break;
							}
							mListener.onBroadcastRecieved(null, (byte[])msg.obj, msg.arg1);
							break;
						case MSG_BROADCAST_WRITE:
							if (msg.obj == null) {
								break;
							}
							SessionModels.udpSessionClient.send("255.255.255.255", BROADCAST_PORT, (byte[])msg.obj);
							break;
						case MSG_ERROR:
							if (msg.obj != null) {
								Session session = (Session) msg.obj;
								mListener.onError(session, msg.arg1);
							} else {
								mListener.onError(null, msg.arg1);
							}
							break;
						default:
							break;
						}
					}
				};
				Looper.loop();
			}
		}

		new Worker().start();

		new Thread(new Runnable() {
			private String TAG = "dataServer";

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(mRunning) {
					mDataServer = new tcpSessionServer(DATA_PORT, mDataClientSessionListener);
					while (mDataServer.isActive()){
						mDataServer.listen();
					}
					Log.d(TAG , "Server is not active");
					mDataServer.destroy();
					postError(null, ERROR_TCP_LOCAL);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (mDataServer != null) {
					mDataServer.destroy();
					mDataServer = null;
				}
			}
		}).start();

		new Thread(new Runnable() {
			private String TAG = "commandServer";

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(mRunning) {
					mCommandServer = new tcpSessionServer(COMMAND_PORT, mCommandClientSessionListener);
					while (mCommandServer.isActive()){
						mCommandServer.listen();
					}
					Log.d(TAG , "Server is not active");
					mCommandServer.destroy();
					postError(null, ERROR_TCP_LOCAL);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (mCommandServer != null) {
					mCommandServer.destroy();
					mCommandServer = null;
				}
			}
		}).start();

		new Thread(new Runnable() {
			private String TAG = "broadcastServer";

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(mRunning) {
					mBroadcastServer = new udpSessionServer(BROADCAST_PORT);
					while(mBroadcastServer.isActive()){
						byte[] data = new byte[512];
						int length = mBroadcastServer.read(data);
						mHandler.sendMessage(mHandler.obtainMessage(MSG_BROADCAST_RECIEVE, length, 0, data));
					}
					Log.d(TAG , "server is not active");
					mBroadcastServer.close();
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (mBroadcastServer != null) {
					mBroadcastServer.close();
					mBroadcastServer = null;
				}
			}
		}).start();
	}

	public Session createClientSession(String ip) {
		ClientSession session = new ClientSession(ip, mDataEventListener);
		addSession(session);
		return session;
	}
	
	public Session createServerSession(NetSession netSession, boolean isDataSession) {
		ServerSession session = new ServerSession(netSession,
				isDataSession, mDataEventListener);
		addSession(session);
		return session;
	}
	
	private void addSession(Session session) {
		mSessions.put(session.getTag(), session);
		mListener.onSessionCreated(session);
	}

	public void removeSession(String tag) {
		if (!mSessions.containsKey(tag)) {
			return;
		}
		mSessions.get(tag).destroy();
		mSessions.remove(tag);
	}
	
	public void removeSession(Session session) {
		if (session == null) {
			return;
		}
		removeSession(session.getTag());
	}

	public void writeData(Session session, byte[] data) {
		WriteData obj = new WriteData(session, data, MSG_DATA_WRITE);
		mHandler.sendMessage(mHandler.obtainMessage(MSG_DATA_WRITE, data.length, 0, obj));
	}

	public void writeCommand(Session session, byte[] data) {
		WriteData obj = new WriteData(session, data, MSG_COMMAND_WRITE);
		mHandler.sendMessage(mHandler.obtainMessage(MSG_COMMAND_WRITE, data.length, 0, obj));
	}

	public void broadcast(byte[] data) {
		mHandler.sendMessage(mHandler.obtainMessage(MSG_BROADCAST_WRITE, data.length, 0, data));
	}

	private void postError(Session session, int code) {
		mHandler.sendMessage(mHandler.obtainMessage(MSG_ERROR, code, 0, session));
	}

	protected void finalize() throws Throwable {
		destroy();
		super.finalize();
	}

	public void destroy() {
		mRunning = false;
		Iterator<String> iter = mSessions.keySet().iterator(); 
		while (iter.hasNext()) { 
			String tag = iter.next(); 
			removeSession(tag);
		}
	}
	
	IClientEventListener mDataClientSessionListener = new IClientEventListener() {

		@Override
		public void onSessionCreated(NetSession session) {
			// TODO Auto-generated method stub
			createServerSession(session, true);
		}
	};
	
	IClientEventListener mCommandClientSessionListener = new IClientEventListener() {

		@Override
		public void onSessionCreated(NetSession session) {
			// TODO Auto-generated method stub
			createServerSession(session, false);
		}
	};
	
	IDataEventListener mDataEventListener = new IDataEventListener() {

		@Override
		public void onDataRecieved(Session session, byte[] data, int length) {
			// TODO Auto-generated method stub
			if (length < 0) {
			    onError(session, 0);
				return;
			}
			//Current, Only server session can receive data
			ReadData msgData = new ReadData(MSG_DATA_RECIEVE, session, data, length);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_RAW_RECIEVE, length, 0, msgData));
		}
		
		@Override
		public void onCommandRecieved(Session session, byte[] data, int length) {
			// TODO Auto-generated method stub
			if (length < 0) {
			    onError(session, 0);
				return;
			}
			//Current, Only server session can receive data
			ReadData msgData = new ReadData(MSG_COMMAND_RECIEVE, session, data, length);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_RAW_RECIEVE, length, 0, msgData));
		}

		@Override
		public void onError(Session session, int code) {
			// TODO Auto-generated method stub
		    postError(session, code);
		}
		
	};
}
