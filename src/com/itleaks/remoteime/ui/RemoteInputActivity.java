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

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class RemoteInputActivity extends Activity implements
		IRemoteImeEventListener {
	private static final String TAG = "RemoteInputActivity";
	private static final boolean DEBUG = true;

	TextView mInputView;
	TextView mStatusView;
	Handler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		NetStateService.start(this);
		setContentView(R.layout.input_main);
		mStatusView = (TextView) this.findViewById(R.id.status_view);
		mInputView = (TextView) this.findViewById(R.id.input_text);
		Button searchBtn = (Button) this.findViewById(R.id.search_btn);
		searchBtn.setVisibility(View.INVISIBLE);
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
		
		Button sendBtn = (Button) this.findViewById(R.id.send_btn);
		sendBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (NetStateService.mPolicy != null) {
					if (!NetStateService.mPolicy.isConnected()) {
						setViewText("No Remote ime connected");
						return;
					}
					if (mInputView.getText() != null) {
						//将输入的数据传递到远端输入法
						NetStateService.mPolicy.writeData(mInputView.getText()
								.toString().getBytes());
						mInputView.setText("");
					}
				}
			}
		});
		mHandler = new Handler();
		init();
	}
	
	private void init() {
		((MApplication)getApplicationContext()).addImeEventListener(this);
		onStatusChanged(ImeSession.STATUS_INVALID, ImeSession.STATUS_INVALID);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		((MApplication)getApplicationContext()).removeImeEventListener(this);
	}

	String mText;
	@Override
	public void onTextRecieved(String text) {
		// TODO Auto-generated method stub
		mText = text;
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				mInputView.append(mText);
			}
		});
	}

	String status;
	private void setViewText(String text) {
		status = text;
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				mStatusView.setText(status);
			}
		});
	}

	@Override
	public void onCommandRecieved(String command, String content) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onError(int code) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStatusChanged(int status, int status2) {
		// TODO Auto-generated method stub
		if (DEBUG) Log.d(TAG, "onStatusChanged " + status);
		setViewText("status:" + ImeSession.getStatusStr(status));
	}

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (DEBUG) Log.d(TAG, "onKeyDown" + keyCode);
    	if (keyCode == KeyEvent.KEYCODE_DEL) {
			if (NetStateService.mPolicy != null && NetStateService.mPolicy.isConnected()) {
				NetStateService.mPolicy.writeCommand(
						RemoteImePolicy.COMMAND_KEY, "" + keyCode);
			}
    	}
        return super.onKeyDown(keyCode, event);
    }
}
