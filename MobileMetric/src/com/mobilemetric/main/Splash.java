package com.mobilemetric.main;

import ti.android.ble.sensortag.R;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;

public class Splash extends Activity{
	//MediaPlayer ourSound;
	@Override
	protected void onCreate(Bundle GoSplash) {
		// TODO Auto-generated method stub
		super.onCreate(GoSplash);
		setContentView(R.layout.splash);
		//ourSound = MediaPlayer.create(Splash.this, R.raw.beep);
		//ourSound.start();
		Thread timer = new Thread(){
			public void run(){
				try{
					sleep(2500);
					Intent openDataChoose = new Intent("android.intent.action.MAINACTIVITY");
					startActivity(openDataChoose);
				} catch (ActivityNotFoundException a){
					a.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}finally{

				}
			}
		};
		timer.start();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		//ourSound.release();
		finish();
	}
	

}
