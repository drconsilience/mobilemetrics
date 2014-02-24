package com.mobilemetric.main;
	import ti.android.ble.sensortag.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
//Test Comment here
public class DataChoose extends Activity{


		@Override
		protected void onCreate(Bundle savedInstanceState) {
			// TODO Auto-generated method stub
			super.onCreate(savedInstanceState);
			setContentView(R.layout.datachoose);
			
			final Button connect = (Button) findViewById(R.id.connect);
			final Button history = (Button) findViewById(R.id.history);

			connect.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					Intent MainActivity = new Intent("android.intent.action.MAINACTIVITY");
					startActivity(MainActivity);
				}
			});
			history.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent History = new Intent("android.intent.action.HISTORY");
					startActivity(History);
				}
			});
		}
		
		//Test comment

	}

