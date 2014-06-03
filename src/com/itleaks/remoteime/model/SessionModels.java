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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;

import android.util.Log;

public interface SessionModels {
	public static final String TAG = "NetUtils";
	
	public class tcpSessionClient extends NetSession {
		OutputStream ops;
		InputStream ips;
		Socket mSocket = null;

		public tcpSessionClient(String ip, int port, INetSessionListener listener) {
		    super(null, listener);
		    try {
		    	mSocket = new Socket(ip, port);//创建Socket实例，并绑定连接远端IP地址和端口
			    init(mSocket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				close();
				e.printStackTrace();
			}
		}
	}
	
	class NetSession extends Thread {
		Socket mSocket = null;
		private boolean mRunning = true;
		OutputStream ops;
		InputStream ips;
		INetSessionListener mListener;
		String mIp, mTag;
		public NetSession(Socket socket, INetSessionListener listener) {
			setListener(listener);
			init(socket);
		}
		
		public void init(Socket socket) {
			if (socket == null) {
				return;
			}
			mRunning = true;
			mSocket = socket;
			try {
				ops = mSocket.getOutputStream();
				ips = mSocket.getInputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				mRunning = false;
				if (mListener != null) {
					mListener.onError(this, 0);
				}
				e.printStackTrace();
			}
			mIp = mSocket.getInetAddress().toString().substring(1);
			mTag = mIp + ":" + mSocket.getPort();
		}
		
		public void setListener(INetSessionListener listener) {
			mListener = listener;
		}
		
		@Override
		public void run(){
			while(mRunning) {
				byte[] data = new byte[512];
				int length = read(data);
				mListener.onDataRecieved(this, data, length);
			}
			try {
				mSocket.close();
				mSocket = null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public int read(byte[] data) {
			int length = -1;
			try {
				length = ips.read(data);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return length;
		}
		
		public int write(byte[] data) {
			int length = -1;
			try {
				ops.write(data);
				ops.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				if (mListener != null) {
					mListener.onError(this, 0);
				}
				e.printStackTrace();
			}
			return length;
		}
		
		public void close(){
			mRunning = false;
		}
		
		public String getTag() {
			return mTag;
		}

		public String getIp() {
			// TODO Auto-generated method stub
			return mIp;
		}
		
		public boolean isActive() {
			return mRunning;
		}
	}
	
	public interface INetSessionListener {
		void onDataRecieved(NetSession session, byte[] data, int length);
		void onError(NetSession session, int code);
	}
	
	public class tcpSessionServer {
		OutputStream ops;
		InputStream ips;
		ServerSocket mSocket;
		HashMap<Integer, NetSession> mSessions;
		boolean isActive;
		int mCurIndex = 0;
		IClientEventListener mListener;
		
		interface IClientEventListener {
			void onSessionCreated(NetSession session);
		}
		
		public tcpSessionServer(int port, IClientEventListener listener) {
			mListener = listener;
	        try {
	        	mSocket = new ServerSocket(port);
	        	isActive = true;
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		}
		
		public boolean listen() {
			try {
				 Socket socket = mSocket.accept();
				 createSession(socket);
				 return true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				destroy();
	        	isActive = false;
				e.printStackTrace();
			}
			return false;
		}
		
		public void destroy() {
        	if (mSocket == null) {
        		return;
        	}
			try {
				mSocket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		private void createSession(Socket socket) {
			NetSession session = new NetSession(socket, null);
			session.start();
			if (mListener != null) {
				mListener.onSessionCreated(session);
			}
		}
		
		public boolean isActive() {
			return isActive;
		}
	}
	
	public class udpSessionClient {
		static DatagramSocket socket = null;
		public udpSessionClient() {
		}
	
		public static void send(String ip, int port, byte[] data) {
			try {
				if (socket == null) {
					socket = new DatagramSocket();
				}
		        InetAddress serverAddress = InetAddress.getByName(ip); 
				DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, port);
				socket.send(packet);
				//socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				if (socket != null) {
					socket.close();
					socket = null;
				}
				e.printStackTrace();
			}
		}
	}
	
	public class udpSessionServer {
		boolean isActive;
		DatagramSocket ds = null;

		public udpSessionServer(int port) {
		    try {
		    	ds = new DatagramSocket(port);
				isActive = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				close();
				e.printStackTrace();
			}
		}
	
		public int read(byte[] data) {
			try {
				DatagramPacket dp = new DatagramPacket(data, data.length-1);
				ds.receive(dp);
				if (SessionModels.Utils.getLocalIpAddress().equals(dp.getAddress().toString().substring(1))) {
					return 0;
				} else {
					return dp.getLength();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				close();
				Log.d(TAG, "error", e);
			}
			return 0;
		}
		
		public boolean isActive() {
			return isActive;
		}
		
		public void close() {
			isActive = false;
			if (ds != null) {
				ds.close();
				ds = null;
			}
		}
	}
	
	public class Utils {
		private static String mIp = "";
		public static String getLocalIpAddress() {
			return mIp;
		}
		
		public static void updateLocalIpAddress() {
		    try {
		        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
		            NetworkInterface intf = en.nextElement();
		            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
		                InetAddress inetAddress = enumIpAddr.nextElement();
		                if (!inetAddress.isLoopbackAddress()) {
		                    mIp = inetAddress.getHostAddress().toString();
		                }
		            }
		        }
		    } catch (SocketException ex) {
		        Log.e(TAG, ex.toString());
		    }
		}
	}
}
