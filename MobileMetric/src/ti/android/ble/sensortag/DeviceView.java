/**************************************************************************************************
  Filename:       DeviceView.java
  Revised:        $Date: 2013-08-30 12:02:37 +0200 (fr, 30 aug 2013) $
  Revision:       $Revision: 27470 $

  Copyright 2013 Texas Instruments Incorporated. All rights reserved.

  IMPORTANT: Your use of this Software is limited to those specific rights
  granted under the terms of a software license agreement between the user
  who downloaded the software, his/her employer (which must be your employer)
  and Texas Instruments Incorporated (the "License").  You may not use this
  Software unless you agree to abide by the terms of the License. 
  The License limits your use, and you acknowledge, that the Software may not be 
  modified, copied or distributed unless used solely and exclusively in conjunction 
  with a Texas Instruments Bluetooth device. Other than for the foregoing purpose, 
  you may not use, reproduce, copy, prepare derivative works of, modify, distribute, 
  perform, display or sell this Software and/or its documentation for any purpose.

  YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
  PROVIDED “AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
  INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
  NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
  TEXAS INSTRUMENTS OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT,
  NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER
  LEGAL EQUITABLE THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES
  INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE
  OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT
  OF SUBSTITUTE GOODS, TECHNOLOGY, SERVICES, OR ANY CLAIMS BY THIRD PARTIES
  (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.

  Should you have any questions regarding your right to use this Software,
  contact Texas Instruments Incorporated at www.TI.com

 **************************************************************************************************/
package ti.android.ble.sensortag;
import static ti.android.ble.sensortag.SensorTag.UUID_ACC_DATA;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

import ti.android.util.Point3D;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.LineGraphView;

import android.view.View.OnClickListener;

// Fragment for Device View
public class DeviceView extends Fragment implements SensorEventListener,OnClickListener{
	private SensorManager sensorManager;
	TextView xCoor; // declare X axis object
	TextView yCoor; // declare Y axis object
	TextView zCoor; // declare Z axis object
	TextView dtime;
	Button viewdata;
	ArrayList<Float> data_x = new ArrayList<Float>(); // declare data array lists
	ArrayList<Float> data_y = new ArrayList<Float>();
	ArrayList<Float> data_z = new ArrayList<Float>();
	ArrayList<Double> accx = new ArrayList<Double>();
	ArrayList<Double> accy = new ArrayList<Double>();
	ArrayList<Double> accz = new ArrayList<Double>();
	double[] filtx = new double[11];
	double[] filty = new double[11];
	double[] filtz = new double[11];
	int sensorpoints=100;

	GraphViewSeries plotx; // declare GraphView objects for phone
	GraphViewSeries ploty;
	GraphViewSeries plotz;
	GraphViewData[] data;
	GraphView graphView;

	GraphViewSeries plotxs; // declare GraphView objects for SensorTag
	GraphViewSeries plotys;
	GraphViewSeries plotzs;
	GraphViewData[] datas;
	GraphView graphViews;

	private static final String TAG = "DeviceView";

	// Sensor table; the iD corresponds to row number
	private static final int ID_OFFSET = 0;
	private static final int ID_ACC = 0;

	public static DeviceView mInstance = null;

	// GUI
	private TableLayout table;
	private TableLayout dtable;
	private TextView mAccValue;
	private TextView mStatus;
	private TextView mCalcTest;

	// House-keeping
	private DecimalFormat decimal = new DecimalFormat("+0.00;-0.00");
	private DeviceActivity mActivity;
	public MediaPlayer beep;
	long t1;
	long t2;
	long dt;
	long tstep1;
	long tstep2;
	int state = 0;
	int stepCount=0;
	FileOutputStream outputStream;
	File newFile;
	String path;
	String nam;
	File dir;
	Button dataview;
	Button btn1;
	Button btn2;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");
		mInstance = this;
		mActivity = (DeviceActivity) getActivity();
		beep=MediaPlayer.create(getActivity(), R.raw.beep);
		// The last two arguments ensure LayoutParams are inflated properly.
		View view = inflater.inflate(R.layout.services_browser, container, false);

		// Hide all Sensors initially (but show the last line for status)
		table = (TableLayout) view.findViewById(R.id.services_browser_layout);

		// UI widgets
		mAccValue = (TextView) view.findViewById(R.id.accelerometerTxt);
		mStatus = (TextView) view.findViewById(R.id.status);
		mCalcTest = (TextView) view.findViewById(R.id.calctest);
		xCoor=(TextView)view.findViewById(R.id.xcoor); // create X axis object
		yCoor=(TextView)view.findViewById(R.id.ycoor); // create Y axis object
		zCoor=(TextView)view.findViewById(R.id.zcoor); // create Z axis object
		dtime=(TextView)view.findViewById(R.id.dt);
		dtable=(TableLayout)view.findViewById(R.id.datatable);
		dtable.setVisibility(View.GONE);
		mCalcTest.setText("0 steps recorded");

		sensorManager=(SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);
		// add listener. The listener will be HelloAndroid (this) class
		sensorManager.registerListener(this, 
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);

		// Notify activity that UI has been inflated
		mActivity.onViewInflated(view);

		data = new GraphViewData[] {
				new GraphViewData(0,0),
				new GraphViewData(0,0),
				new GraphViewData(0,0),
				new GraphViewData(0,0),
				new GraphViewData(0,0),
				new GraphViewData(0,0),
				new GraphViewData(0,0),
				new GraphViewData(0,0),
				new GraphViewData(0,0),
				new GraphViewData(0,0)
		};
		datas = new GraphViewData[sensorpoints];
		for(int m=0;m<sensorpoints;m++){
			datas[m]=new GraphViewData(0,0);
		};

		plotx = new GraphViewSeries("X", new GraphViewSeriesStyle(Color.RED,3), data); // create GraphViewSeries
		ploty = new GraphViewSeries("Y", new GraphViewSeriesStyle(Color.GREEN,3), data);
		plotz = new GraphViewSeries("Z", new GraphViewSeriesStyle(Color.BLUE,3), data);

		plotxs = new GraphViewSeries("X", new GraphViewSeriesStyle(Color.RED,3), data); // create GraphViewSeries for SensorTag
		plotys = new GraphViewSeries("Y", new GraphViewSeriesStyle(Color.GREEN,3), data);
		plotzs = new GraphViewSeries("Z", new GraphViewSeriesStyle(Color.BLUE,3), data);

		// Add series to LineGraphView
		graphView = new LineGraphView(
				getActivity(), // context  
				"Plots" // heading  
				);
		graphView.addSeries(plotx); // data  
		graphView.addSeries(ploty);
		graphView.addSeries(plotz);

		//Repeat for sensortag
		graphViews = new LineGraphView(
				getActivity(), // context  
				"Plots" // heading  
				);
		graphViews.addSeries(plotxs); // data  
		graphViews.addSeries(plotys);
		graphViews.addSeries(plotzs);

		// Format the view
		LinearLayout layout = (LinearLayout) view.findViewById(R.id.subLayout);  
		layout.addView(graphView);
		graphView.setManualYAxisBounds(15.0,-15.0);
		graphView.getGraphViewStyle().setNumHorizontalLabels(10);
		graphView.getGraphViewStyle().setVerticalLabelsColor(Color.BLACK);
		graphView.getGraphViewStyle().setHorizontalLabelsColor(Color.BLACK);
		graphView.getGraphViewStyle().setGridColor(Color.BLACK);
		graphView.setShowLegend(true);
		graphView.setLegendAlign(LegendAlign.BOTTOM);

		LinearLayout layouts = (LinearLayout) view.findViewById(R.id.subLayoutS);  
		layouts.addView(graphViews);
		graphViews.setManualYAxisBounds(15.0,-15.0);
		graphViews.getGraphViewStyle().setNumHorizontalLabels(10);
		graphViews.getGraphViewStyle().setVerticalLabelsColor(Color.BLACK);
		graphViews.getGraphViewStyle().setHorizontalLabelsColor(Color.BLACK);
		graphViews.getGraphViewStyle().setGridColor(Color.BLACK);
		graphViews.setShowLegend(true);
		graphViews.setLegendAlign(LegendAlign.BOTTOM);

		dataview = (Button)view.findViewById(R.id.dvbutton);
		dataview.setOnClickListener(this);

		btn1 = (Button)view.findViewById(R.id.button1);
		btn1.setOnClickListener(this);

		btn2 = (Button)view.findViewById(R.id.button2);
		btn2.setOnClickListener(this);

		Context thisContext = getActivity();
		path = thisContext.getExternalFilesDir(null).getAbsolutePath();
		dir = new File(path);
		int n = dir.list().length+1;
		nam = "/data" + n + ".txt";
		newFile = new File(path + nam);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateVisibility();
	}

	@Override
	public void onPause() {
		//beep.release();
		//beep = null;
		super.onPause();
	}

	/**
	 * Handle changes in sensor values
	 * */

	double[] coefs =new double[] {.0079,-.0263,.0694,-.1715,.6219,.6219,-.1715,.0694,-.0263,.0079,-.0015};


	public void onCharacteristicChanged(String uuidStr, byte[] rawValue) {
		Point3D v;
		String msg;
		t2=SystemClock.uptimeMillis();
		dt=t2-t1;
		t1=t2;

		if (uuidStr.equals(UUID_ACC_DATA.toString())) {
			v = TagSensor.ACCELEROMETER.convert(rawValue);
			msg = decimal.format(v.x*9.81) + "\n" + decimal.format(v.y*9.81) + "\n" + decimal.format(v.z*9.81) + "\n";
			mAccValue.setText(msg);
			double sumx=0;
			double sumy=0;
			double sumz=0;
			Vibrator vibe = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE); // create Vibrator object
			//Low pass, equiripple, 0{11}, Fs=10Hz, Fpass=3Hz, Fstop=5Hz

			//Implement algorithm here!!!!!!!

			double x = v.x*9.81; double y = v.y*9.81; double z = v.z*9.81;

			

			if(accx.size() >= sensorpoints){
				//mCalcTest.setText("Filtered x acc= " +decimal.format(accx.get(sensorpoints-1))+"\n"+ "Filtered y acc= " +decimal.format(accy.get(sensorpoints-1))+"\n"+"Filtered z acc="+decimal.format(accz.get(sensorpoints-1)));
				//for (int n=coefs.length;n>1;n--){
				//sumx+=filtx[coefs.length-n]* coefs[n-1];
				//filtx[coefs.length-n]=filtx[coefs.length-n+1];
				//sumy+=filty[coefs.length-n]* coefs[n-1];
				//filty[coefs.length-n]=filty[coefs.length-n+1];
				//sumz+=filtz[coefs.length-n]* coefs[n-1];
				//filtz[coefs.length-n]=filtz[coefs.length-n+1];
				//}
				//sumx+=filtx[coefs.length-1]*coefs[0];
				//filtx[coefs.length-1]=v.x*9.81*coefs[coefs.length-1];
				//accx.add(sumx+v.x*9.81*coefs[coefs.length-1]);
				accx.add(v.x*9.81);
				accx.remove(0);
				//sumy+=filty[coefs.length-1]*coefs[0];
				//filty[coefs.length-1]=v.y*9.81*coefs[coefs.length-1];
				//accy.add(sumy+v.y*9.81*coefs[coefs.length-1]);
				accy.add(v.y*9.81);
				accy.remove(0);
				//sumz+=filtz[coefs.length-1]*coefs[0];
				//filtz[coefs.length-1]=v.z*9.81*coefs[coefs.length-1];
				//accz.add(sumz+v.z*9.81*coefs[coefs.length-1]);
				accz.add(v.z*9.81);
				accz.remove(0);

				// reset GraphViewData with newest values
				GraphViewData[] newdataxs = new GraphViewData[sensorpoints];
				GraphViewData[] newdatays = new GraphViewData[sensorpoints];
				GraphViewData[] newdatazs = new GraphViewData[sensorpoints];
				for(int m=0;m<sensorpoints;m++){
					newdataxs[m]=new GraphViewData(m, Math.sqrt(accx.get(m)*accx.get(m)+accz.get(m)*accz.get(m)));
					newdatays[m]=new GraphViewData(m, accy.get(m));
					newdatazs[m]=new GraphViewData(m, accz.get(m));
				}
				plotxs.resetData(newdataxs);
				plotys.resetData(newdatays);
				plotzs.resetData(newdatazs);
				if(Math.sqrt(accx.get(sensorpoints-1)*accx.get(sensorpoints-1)+accz.get(sensorpoints-1)*accz.get(sensorpoints-1))>5){
					// Vibrate for 50 milliseconds
					tstep2=SystemClock.uptimeMillis();
					if(tstep2-tstep1>300){	
						beep.start(); // vibrate for 50 ms
						tstep1=tstep2;
						stepCount++;
						mCalcTest.setText(stepCount+" steps recorded.");
					}
				}
				//} else if (accx.size()<coefs.length){

				//accx.add(v.x*9.81);
				//filtx[accx.size()-1]=(accx.get(accx.size()-1));
				//accy.add(v.y*9.81);
				//filty[accy.size()-1]=(accy.get(accy.size()-1));
				//accz.add(v.z*9.81);
				//filtz[accz.size()-1]=(accz.get(accz.size()-1));
			}else {
				//for (int n=coefs.length;n>1;n--){
				//sumx+=filtx[coefs.length-n]* coefs[n-1];
				//filtx[coefs.length-n]=filtx[coefs.length-n+1];
				//sumy+=filty[coefs.length-n]* coefs[n-1];
				//filty[coefs.length-n]=filty[coefs.length-n+1];
				//sumz+=filtz[coefs.length-n]* coefs[n-1];
				//filtz[coefs.length-n]=filtz[coefs.length-n+1];
				//}
				//sumx+=filtx[coefs.length-1]*coefs[0];
				//filtx[coefs.length-1]=v.x*9.81*coefs[coefs.length-1];
				//accx.add(sumx+v.x*9.81*coefs[coefs.length-1]);
				accx.add(v.x*9.81);
				//sumy+=filty[coefs.length-1]*coefs[0];
				//filty[coefs.length-1]=v.y*9.81*coefs[coefs.length-1];
				//accy.add(sumy+v.y*9.81*coefs[coefs.length-1]);
				accy.add(v.y*9.81);
				//sumz+=filtz[coefs.length-1]*coefs[0];
				//filtz[coefs.length-1]=v.z*9.81*coefs[coefs.length-1];
				//accz.add(sumz+v.z*9.81*coefs[coefs.length-1]);
				accz.add(v.z*9.81);
				if(Math.sqrt(accx.get(accx.size()-1)*accx.get(accx.size()-1)+accz.get(accx.size()-1)*accz.get(accx.size()-1))>5){
					// Vibrate for 50 milliseconds
					tstep2=SystemClock.uptimeMillis();
					if(tstep2-tstep1>300){	
						beep.start(); // vibrate for 50 ms
						tstep1=tstep2;
						stepCount++;
						mCalcTest.setText(stepCount+" steps recorded.");
					}
			}
		}
	}};


	void updateVisibility() {
		showItem(ID_ACC,mActivity.isEnabledByPrefs(TagSensor.ACCELEROMETER));
	}

	private void showItem(int id, boolean visible) {
		View hdr = table.getChildAt(id*2 + ID_OFFSET);
		View txt = table.getChildAt(id*2 + ID_OFFSET + 1);
		int vc = visible ? View.VISIBLE : View.GONE;
		hdr.setVisibility(vc);    
		txt.setVisibility(vc);    
	}

	void setStatus(String txt) {
		mStatus.setText(txt);
		mStatus.setTextAppearance(mActivity, R.style.statusStyle_Success);
	}

	void setError(String txt) {
		mStatus.setText(txt);
		mStatus.setTextAppearance(mActivity, R.style.statusStyle_Failure);
	}

	void setBusy(boolean f) {
		if (f)
			mStatus.setTextAppearance(mActivity, R.style.statusStyle_Busy);
		else
			mStatus.setTextAppearance(mActivity, R.style.statusStyle);  		
	}

	@Override
	public void onAccuracyChanged(android.hardware.Sensor arg0, int arg1) {

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float x=event.values[0];
		float y=event.values[1];
		float z=event.values[2];

		if(data_x.size() >= 10){
			data_x.add(x); // add new x,y,z data
			data_y.add(y);
			data_z.add(z);
			data_x.remove(0); // remove oldest x,y,z data
			data_y.remove(0);
			data_z.remove(0);

			xCoor.setText("X: "+data_x.get(9)); // output newest data as text
			yCoor.setText("Y: "+data_y.get(9));
			zCoor.setText("Z: "+data_z.get(9));

			// reset GraphViewData with newest values
			plotx.resetData(new GraphViewData[] { 
					new GraphViewData(0,data_x.get(0)),
					new GraphViewData(1,data_x.get(1)),
					new GraphViewData(2,data_x.get(2)),
					new GraphViewData(3,data_x.get(3)),
					new GraphViewData(4,data_x.get(4)),
					new GraphViewData(5,data_x.get(5)),
					new GraphViewData(6,data_x.get(6)),
					new GraphViewData(7,data_x.get(7)),
					new GraphViewData(8,data_x.get(8)),
					new GraphViewData(9,data_x.get(9))
			});
			ploty.resetData(new GraphViewData[] { 
					new GraphViewData(0,data_y.get(0)),
					new GraphViewData(1,data_y.get(1)),
					new GraphViewData(2,data_y.get(2)),
					new GraphViewData(3,data_y.get(3)),
					new GraphViewData(4,data_y.get(4)),
					new GraphViewData(5,data_y.get(5)),
					new GraphViewData(6,data_y.get(6)),
					new GraphViewData(7,data_y.get(7)),
					new GraphViewData(8,data_y.get(8)),
					new GraphViewData(9,data_y.get(9))
			});
			plotz.resetData(new GraphViewData[] { 
					new GraphViewData(0,data_z.get(0)),
					new GraphViewData(1,data_z.get(1)),
					new GraphViewData(2,data_z.get(2)),
					new GraphViewData(3,data_z.get(3)),
					new GraphViewData(4,data_z.get(4)),
					new GraphViewData(5,data_z.get(5)),
					new GraphViewData(6,data_z.get(6)),
					new GraphViewData(7,data_z.get(7)),
					new GraphViewData(8,data_z.get(8)),
					new GraphViewData(9,data_z.get(9))
			});
		}
		else{
			data_x.add(x);
			data_y.add(y);
			data_z.add(z);
		}
		if (state == 1){
			try {
				String str = x+"\t"+y+"\t"+z+"\t"+dt+"\n";
				outputStream = new FileOutputStream(newFile,true);
				outputStream.write(str.getBytes());
				outputStream.close();
			}
			catch (final Exception ex) {
				Log.e("JAVA_DEBUGGING", "Exception while creating save file!"); ex.printStackTrace();
			}
		}
	}

	public void onClick(View v){
		switch (v.getId()){
		case R.id.button1:
			Random num = new Random(); // create Random object
			v.setBackgroundColor(num.nextInt()); // set button background color to next random integer
			Vibrator vib = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE); // create Vibrator object
			// Vibrate for 50 milliseconds
			beep.start();
			vib.vibrate(50); // vibrate for 50 ms
			stepCount=0;
			mCalcTest.setText("0 steps recorded");
			break;
		case R.id.button2:
			if(state == 0){
				state = 1;
				v.setBackgroundColor(Color.GREEN);
			}
			else {
				state = 0;
				v.setBackgroundColor(Color.RED);
			}
			break;
		case R.id.dvbutton:
			if(dtable.getVisibility()==View.GONE){
				dtable.setVisibility(View.VISIBLE);
				dataview.setText("Hide Summary");
			}else if(dtable.getVisibility()==View.VISIBLE){
				dtable.setVisibility(View.GONE);
				dataview.setText("Show Summary");
			}
			break;
		}

	}

}

/*
http://www.byteworks.us/Byte_Works/Blog/Entries/2012/10/31_Accessing_the_Bluetooth_low_energy_Accelerometer_on_the_TI_SensorTag.html
http://developer.android.com/guide/topics/data/data-storage.html#filesInternal
http://stackoverflow.com/questions/16268191/android-accelerometer-store-data-in-file
http://chrisrisner.com/31-Days-of-Android--Day-23%E2%80%93Writing-and-Reading-Files
http://uwudamith.wordpress.com/2012/05/19/how-to-use-onclicklistener-in-android/
 */