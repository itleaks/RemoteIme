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

import com.itleaks.remoteime.R;
import com.itleaks.remoteime.model.IRemoteImeEventListener;
import com.itleaks.remoteime.model.ImeSession;
import com.itleaks.remoteime.model.RemoteImePolicy;

import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.TextView;

public class RemoteInputMethod extends InputMethodService 
		implements IRemoteImeEventListener{
	private static final boolean DEBUG = true;

	private static final String TAG = "RemoteInputMethod";
	boolean isShowing = false;
	private Handler mHandler;
	private TextView mStatusView = null;
	
    @Override 
    public void onCreate() {
    	if (DEBUG) Log.d(TAG, "onCreate");
    	super.onCreate();
    	mHandler = new Handler();
		((MApplication)getApplicationContext()).addImeEventListener(this);
		//启动网络service
    	NetStateService.start(this);
    	if (DEBUG) Log.d(TAG, "onCreate");
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
		((MApplication)getApplicationContext()).removeImeEventListener(this);
    }
    
    @Override 
    public View onCreateInputView() {
    	View view = LayoutInflater.from(this).inflate(R.layout.input_method, null);
    	Button searchBtn = (Button) view.findViewById(R.id.search_btn);
		searchBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (NetStateService.mPolicy != null) {
					//开始搜索远程输入法
					NetStateService.mPolicy.searchRemoteIme();
				}
			}
		});
		mStatusView = (TextView)view.findViewById(R.id.status_view);
		return view;
    }
    
    @Override 
    public void onStartInput(EditorInfo attribute, boolean restarting) {
    	if (DEBUG) Log.d(TAG, "onStartInput");
    	super.onStartInput(attribute, restarting);
    	isShowing = true;
    }

    @Override 
    public void onFinishInput() {
    	if (DEBUG) Log.d(TAG, "onCreate");
    	isShowing = false;
    }

    private void sendText(String text) {
    	if (!isShow()) {
    		Log.d(TAG, "ime is hidden");
    		return;
    	}
        final InputConnection ic = getCurrentInputConnection();
        if(ic == null) {
    		Log.d(TAG, "ic null");
        	return;
        }
        ic.commitText(text, text.length());
    }
    
    public void sendDownUpKeyEvents(int keyEventCode) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        long eventTime = SystemClock.uptimeMillis();
        ic.sendKeyEvent(new KeyEvent(eventTime, eventTime,
                KeyEvent.ACTION_DOWN, keyEventCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD|KeyEvent.FLAG_KEEP_TOUCH_MODE));
        ic.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), eventTime,
                KeyEvent.ACTION_UP, keyEventCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD|KeyEvent.FLAG_KEEP_TOUCH_MODE));
    }
 
    public boolean isShow() {
    	return isShowing;
    }

    String mText;
	@Override
	public void onTextRecieved(String text) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onTextRecieved" + text);
		mText = text;
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				sendText(mText);
			}
		});
	}
	
	String mKey;
	private void sendKey(String key) {
		mKey = key;
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Log.d(TAG, "onKey" + mKey);
				sendDownUpKeyEvents(Integer.parseInt(mKey));
			}
		});
	}
	
	@Override
	public void onCommandRecieved(String command, String content) {
		if (RemoteImePolicy.COMMAND_KEY.equals(command)) {
			sendKey(content);
		}
	}

	@Override
	public void onError(int code) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(int status, int status2) {
		// TODO Auto-generated method stub
		setViewText("status:" + ImeSession.getStatusStr(status2));
	}
	
	String status;
	private void setViewText(String text) {
		status = text;
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (mStatusView != null) {
					mStatusView.setText(status);
				}
			}
		});
	}
}

