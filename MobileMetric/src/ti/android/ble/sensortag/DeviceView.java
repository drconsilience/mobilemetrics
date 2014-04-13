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
import java.util.Calendar;

import ti.android.util.Point3D;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.LineGraphView;
import com.mobilemetric.main.Complex;
import com.mobilemetric.main.FFT;

import android.view.View.OnClickListener;

// Fragment for Device View
public class DeviceView extends Fragment implements SensorEventListener,OnClickListener,SeekBar.OnSeekBarChangeListener{
	private SensorManager sensorManager;
	View view;
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
	int sensorpoints=128;
	Complex[] compx = new Complex[sensorpoints];
	Complex[] compy = new Complex[sensorpoints];
	Complex[] compz = new Complex[sensorpoints];
	Complex[] fftx = new Complex[sensorpoints];
	Complex[] ffty = new Complex[sensorpoints];
	Complex[] fftz = new Complex[sensorpoints];
	double meanx=0;
	double meany=0;
	double meanz=0;
	double samplefreq=10;
	double beepwait=1000;
	double hzratio=sensorpoints/samplefreq;
	int lowbandl=(int) Math.ceil(0.5*hzratio);
	int lowbandh=(int) Math.floor(3*hzratio);
	int highbandl=(int) Math.ceil(3*hzratio);
	int highbandh=(int) Math.floor(5*hzratio);
	boolean beeping=false;
	double maxtriggerratio=2;
	double triggerratio=maxtriggerratio;
	boolean firstbeep=false;
	String algorithm1="Fourier Method";
	String algorithm2="Step Detection Method";
	int currentAlgorithm=1;
	int devmodestate=1;
	double bandratio;

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

	//Andrews Variables

	int i = 0; // declare iteration counter and samples to wait variable
	int sec = 10;
	int n_and = 2;

	long tone; // declare time variables
	long ttwo;
	long dt_and;

	int state = 0;
	int movstate = 0;
	int movcond = 0;
	int evnt = 0;
	int intn = 0;

	// GUI
	private TableLayout table;
	private TableLayout devlayout;
	private LinearLayout patlayout;
	private LinearLayout testlayout;
	private TableLayout dtable;
	private TextView mAccValue;
	private TextView mStatus;
	private TextView mCalcTest;
	private TextView showRatio;

	// House-keeping
	private DecimalFormat decimal = new DecimalFormat("+0.00;-0.00");
	private DeviceActivity mActivity;
	public MediaPlayer beep;
	long t1;
	long t2;
	long dt;
	long tstep1;
	long tstep2;
	//int state = 0;
	int stepCount=0;
	FileOutputStream outputStream;
	File newFile;
	String path;
	String nam;
	File dir;
	Button dataview;
	Button devmode;
	Button btn1;
	Button btn2;
	String[] freqs;
	SeekBar slideRatio;
	Calendar cal;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");
		mInstance = this;
		mActivity = (DeviceActivity) getActivity();
		beep=MediaPlayer.create(getActivity(), R.raw.beep);
		// The last two arguments ensure LayoutParams are inflated properly.
		view = inflater.inflate(R.layout.services_browser, container, false);

		// Hide all Sensors initially (but show the last line for status)
		table = (TableLayout) view.findViewById(R.id.services_browser_layout);
		devlayout= (TableLayout) view.findViewById(R.id.developer_layout);
		patlayout= (LinearLayout) view.findViewById(R.id.patient_layout);
		testlayout= (LinearLayout) view.findViewById(R.id.testlayout);

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
		dataview = (Button)view.findViewById(R.id.dvbutton);
		dataview.setOnClickListener(this);
		devmode = (Button)view.findViewById(R.id.devMode);
		devmode.setOnClickListener(this);
		slideRatio=(SeekBar)view.findViewById(R.id.slideRatio);
		showRatio=(TextView)view.findViewById(R.id.showRatio);
		slideRatio.setOnSeekBarChangeListener(this);
		dataview.setText("Switch from "+algorithm1+" to "+algorithm2);
		mCalcTest.setText("Hi/Lo ratio not yet calculated");
		patlayout.setVisibility(View.GONE);


		sensorManager=(SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);
		// add listener. The listener will be HelloAndroid (this) class
		sensorManager.registerListener(this, 
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);

		// Notify activity that UI has been inflated
		mActivity.onViewInflated(view);

		for(int n=0;n<sensorpoints;n++){
			compx[n]=new Complex((double) 0,(double) 0);
			compy[n]=new Complex((double) 0,(double) 0);
			compz[n]=new Complex((double) 0,(double) 0);
		}

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

		String[] freqs=new String[] {"0",".5","1","1.5","2","2.5","3","3.5","4","4.5","5"};

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
				"Fourier Transform" // heading  
				);
		graphViews.addSeries(plotxs); // data  
		graphViews.addSeries(plotys);
		graphViews.addSeries(plotzs);

		//Andrew new stuff
		int[] buttons = {R.id.button1, R.id.button2, R.id.button3, R.id.button4, 
				R.id.button5, R.id.button6, R.id.button7, R.id.button8, 
				R.id.button9, R.id.button10, R.id.button11, R.id.button12, 
				R.id.button13, R.id.button14, R.id.button15, R.id.button16};
		for(int i=0;i<buttons.length;i++){
			Button b = (Button)view.findViewById(buttons[i]);
			b.setOnClickListener(this);
		}
		view.findViewById(R.id.button1).setBackgroundColor(Color.RED);

		SeekBar bar = (SeekBar)view.findViewById(R.id.seekBar1);
		bar.setOnSeekBarChangeListener(this);

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
		graphViews.setManualYAxisBounds(50,0);
		graphViews.getGraphViewStyle().setNumHorizontalLabels(11);
		graphViews.setHorizontalLabels(freqs);
		graphViews.getGraphViewStyle().setVerticalLabelsColor(Color.BLACK);
		graphViews.getGraphViewStyle().setHorizontalLabelsColor(Color.BLACK);
		graphViews.getGraphViewStyle().setGridColor(Color.BLACK);
		graphViews.setShowLegend(true);
		graphViews.setLegendAlign(LegendAlign.TOP);

		btn1 = (Button)view.findViewById(R.id.button1);
		btn1.setOnClickListener(this);

		btn2 = (Button)view.findViewById(R.id.button2);
		btn2.setOnClickListener(this);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateVisibility();
	}

	@Override
	public void onPause() {
		beep.release();
		beep = null;
		super.onPause();
	}

	/**
	 * Handle changes in sensor values
	 * */

	public void onCharacteristicChanged(String uuidStr, byte[] rawValue) {

		if (uuidStr.equals(UUID_ACC_DATA.toString())) {
			Point3D v;
			String msg;
			t2=SystemClock.uptimeMillis();
			dt=t2-t1;
			dtime.setText("dt: "+dt);
			t1=t2;
			v = TagSensor.ACCELEROMETER.convert(rawValue);
			double x = v.x*9.81; double y = v.y*9.81; double z = v.z*9.81;
			msg = decimal.format(x) + "\n" + decimal.format(y) + "\n" + decimal.format(z) + "\n";
			mAccValue.setText(msg);

			//Implement algorithm here!!!!!!!
			accx.add(x);
			accy.add(y);
			accz.add(z);			
			if(accx.size()>=sensorpoints){
				accx.remove(0);
				accy.remove(0);
				accz.remove(0);
			}
			meanx*=accx.size()-1;
			meanx+=x;
			meanx/=accx.size();
			meany*=accy.size()-1;
			meany+=y;
			meany/=accy.size();
			meanz*=accz.size()-1;
			meanz+=z;
			meanz/=accz.size();
			if (currentAlgorithm==1){

				for(int n=0;n<accx.size();n++){
					compx[n]=(new Complex(accx.get(n)-meanx,(double)0));
					compy[n]=(new Complex(accy.get(n)-meany,(double)0));
					compz[n]=(new Complex(accz.get(n)-meanz,(double)0));
				}
				fftx=FFT.fft(compx);
				ffty=FFT.fft(compy);
				fftz=FFT.fft(compz);

				double lowbandmean=Complex.mean(Complex.subset(ffty, lowbandl, lowbandh), lowbandh-lowbandl);
				double highbandmean=Complex.mean(Complex.subset(ffty, highbandl, highbandh),highbandh-highbandl);
				bandratio=highbandmean/lowbandmean;
				mCalcTest.setText("Hi/Lo Ratio= "+decimal.format(bandratio));
				if (bandratio>triggerratio){
					tstep2=SystemClock.uptimeMillis();
					if(tstep2-tstep1>beepwait){	
						beep.start();
						tstep1=tstep2;
					}
				}
				// reset GraphViewData with newest values
				GraphViewData[] newdataxs = new GraphViewData[accx.size()/2];
				GraphViewData[] newdatays = new GraphViewData[accx.size()/2];
				GraphViewData[] newdatazs = new GraphViewData[accx.size()/2];
				for(int m=0;m<accx.size()/2;m++){
					newdataxs[m]=new GraphViewData(m, Complex.abs(fftx[m]));
					newdatays[m]=new GraphViewData(m, Complex.abs(ffty[m]));
					newdatazs[m]=new GraphViewData(m, Complex.abs(fftz[m]));
				}
				plotxs.resetData(newdataxs);
				plotys.resetData(newdatays);
				plotzs.resetData(newdatazs);

				//Paige code here
			}else if (currentAlgorithm==2){
				if(Math.sqrt(x*x+z*z)>5){
					tstep2=SystemClock.uptimeMillis();
					if(tstep2-tstep1>300){	
						beep.start(); 
						tstep1=tstep2;
						stepCount++;
						mCalcTest.setText(stepCount+" steps recorded.");
					}
				}
			}

			//Andrew stuff
			if(state == 1){
				try
				{
					cal = Calendar.getInstance();
					int hr = cal.get(Calendar.HOUR_OF_DAY);
					int min = cal.get(Calendar.MINUTE);
					int sec = cal.get(Calendar.SECOND);
					int msec = cal.get(Calendar.MILLISECOND);
					String str = x+"\t"+y+"\t"+z+"\t"+dt_and+"\t"+movstate+"\t"+movcond+"\t"+evnt+"\t"+intn+"\t"+bandratio+"\t"+triggerratio+"\t"+hr+"\t"+min+"\t"+sec+"\t"+msec+"\n";
					// x,y,z,dt,movstate,movcond,evnt,intn,bandratio,triggerratio,hr,min,sec,msec
					outputStream = new FileOutputStream(newFile,true);
					outputStream.write(str.getBytes());
					outputStream.close();
				}
				catch (final Exception ex) { 
					Log.e("JAVA_DEBUGGING", "Exception while creating save file!"); ex.printStackTrace(); 
				}
			}

		}
	}

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
	}

	public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
		showRatio.setText("Trigger ratio is "+(double)progress/100*maxtriggerratio);
		triggerratio=(double)progress/100*maxtriggerratio;
	}

	public void onClick(View v){
		switch (v.getId()){
		case R.id.dvbutton:
			if(currentAlgorithm==1){
				dataview.setText("Switch from "+algorithm2+" to "+algorithm1);
				currentAlgorithm=2;;
				mCalcTest.setText("0 steps recorded");
				stepCount=0;
				slideRatio.setVisibility(View.GONE);
				showRatio.setVisibility(View.GONE);
			}else {
				dataview.setText("Switch from "+algorithm1+" to "+algorithm2);
				currentAlgorithm=1;
				mCalcTest.setText("Hi/Lo ratio not yet calculated");
				slideRatio.setVisibility(View.VISIBLE);
				showRatio.setVisibility(View.VISIBLE);
			}break;
		case R.id.devMode:
			if (devmodestate==1){
				devlayout.setVisibility(View.GONE);
				testlayout.setVisibility(View.GONE);
				patlayout.setVisibility(View.VISIBLE);
				devmode.setText("Turn Developer Mode ON");
				devmodestate=0;
			}else{
				devlayout.setVisibility(View.VISIBLE);
				testlayout.setVisibility(View.VISIBLE);
				patlayout.setVisibility(View.GONE);
				devmode.setText("Turn Developer Mode OFF");
				devmodestate=1;
			}break;
			//Andrew
		case R.id.button1:
			Context thisContext = getActivity();
			path = thisContext.getExternalFilesDir(null).getAbsolutePath();
			dir = new File(path);
			int n = dir.list().length+1;
			nam = "/data" + n + ".txt";
			newFile = new File(path + nam);
			if(state == 0){
				state = 1;
				v.setBackgroundColor(Color.GREEN);
			}
			else {
				state = 0;
				v.setBackgroundColor(Color.RED);
			}
			break;
		case R.id.button2:
			if(movstate != 1){
				movstate = 1;
				movcond = 0;
			}
			v.setBackgroundColor(Color.GREEN);
			view.findViewById(R.id.button3).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button4).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button5).setVisibility(View.VISIBLE);
			view.findViewById(R.id.button6).setVisibility(View.GONE);
			view.findViewById(R.id.button7).setVisibility(View.GONE);
			view.findViewById(R.id.button8).setVisibility(View.VISIBLE);
			view.findViewById(R.id.button9).setVisibility(View.GONE);
			view.findViewById(R.id.button10).setVisibility(View.GONE);
			view.findViewById(R.id.button11).setVisibility(View.VISIBLE);
			view.findViewById(R.id.button12).setVisibility(View.GONE);
			view.findViewById(R.id.button13).setVisibility(View.GONE);
			view.findViewById(R.id.button14).setVisibility(View.VISIBLE);
			view.findViewById(R.id.buttonp1).setVisibility(View.GONE);
			view.findViewById(R.id.buttonp2).setVisibility(View.GONE);
			break;
		case R.id.button3:
			if(movstate != 2){
				movstate = 2;
				movcond = 0;
			}
			v.setBackgroundColor(Color.GREEN);
			view.findViewById(R.id.button2).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button4).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button5).setVisibility(View.GONE);
			view.findViewById(R.id.button6).setVisibility(View.VISIBLE);
			view.findViewById(R.id.button7).setVisibility(View.GONE);
			view.findViewById(R.id.button8).setVisibility(View.GONE);
			view.findViewById(R.id.button9).setVisibility(View.VISIBLE);
			view.findViewById(R.id.button10).setVisibility(View.GONE);
			view.findViewById(R.id.button11).setVisibility(View.GONE);
			view.findViewById(R.id.button12).setVisibility(View.VISIBLE);
			view.findViewById(R.id.button13).setVisibility(View.GONE);
			view.findViewById(R.id.button14).setVisibility(View.GONE);
			view.findViewById(R.id.buttonp1).setVisibility(View.VISIBLE);
			view.findViewById(R.id.buttonp2).setVisibility(View.GONE);
			break;
		case R.id.button4:
			if(movstate != 3){
				movstate = 3;
				movcond = 0;
			}
			v.setBackgroundColor(Color.GREEN);
			view.findViewById(R.id.button2).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button3).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button5).setVisibility(View.GONE);
			view.findViewById(R.id.button6).setVisibility(View.GONE);
			view.findViewById(R.id.button7).setVisibility(View.VISIBLE);
			view.findViewById(R.id.button8).setVisibility(View.GONE);
			view.findViewById(R.id.button9).setVisibility(View.GONE);
			view.findViewById(R.id.button10).setVisibility(View.VISIBLE);
			view.findViewById(R.id.button11).setVisibility(View.GONE);
			view.findViewById(R.id.button12).setVisibility(View.GONE);
			view.findViewById(R.id.button13).setVisibility(View.VISIBLE);
			view.findViewById(R.id.button14).setVisibility(View.GONE);
			view.findViewById(R.id.buttonp1).setVisibility(View.GONE);
			view.findViewById(R.id.buttonp2).setVisibility(View.VISIBLE);
			break;
		case R.id.button5:
			movcond = 1;
			v.setBackgroundColor(Color.GREEN);
			view.findViewById(R.id.button6).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button7).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button8).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button9).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button10).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button11).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button12).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button13).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button14).setBackgroundColor(Color.LTGRAY);
			break;
		case R.id.button6:
			movcond = 5;
			v.setBackgroundColor(Color.GREEN);
			view.findViewById(R.id.button5).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button7).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button8).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button9).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button10).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button11).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button12).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button13).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button14).setBackgroundColor(Color.LTGRAY);
			break;
		case R.id.button7:
			movcond = 8;
			v.setBackgroundColor(Color.GREEN);
			view.findViewById(R.id.button5).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button6).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button8).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button9).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button10).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button11).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button12).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button13).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button14).setBackgroundColor(Color.LTGRAY);
			break;
		case R.id.button8:
			movcond = 2;
			v.setBackgroundColor(Color.GREEN);
			view.findViewById(R.id.button5).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button6).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button7).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button9).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button10).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button11).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button12).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button13).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button14).setBackgroundColor(Color.LTGRAY);
			break;
		case R.id.button9:
			movcond = 6;
			v.setBackgroundColor(Color.GREEN);
			view.findViewById(R.id.button5).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button6).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button7).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button8).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button10).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button11).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button12).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button13).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button14).setBackgroundColor(Color.LTGRAY);
			break;
		case R.id.button10:
			movcond = 9;
			v.setBackgroundColor(Color.GREEN);
			view.findViewById(R.id.button5).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button6).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button7).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button8).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button9).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button11).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button12).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button13).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button14).setBackgroundColor(Color.LTGRAY);
			break;
		case R.id.button11:
			movcond = 3;
			v.setBackgroundColor(Color.GREEN);
			view.findViewById(R.id.button5).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button6).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button7).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button8).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button9).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button10).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button12).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button13).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button14).setBackgroundColor(Color.LTGRAY);
			break;
		case R.id.button12:
			movcond = 7;
			v.setBackgroundColor(Color.GREEN);
			view.findViewById(R.id.button5).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button6).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button7).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button8).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button9).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button10).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button11).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button13).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button14).setBackgroundColor(Color.LTGRAY);
			break;
		case R.id.button13:
			movcond = 10;
			v.setBackgroundColor(Color.GREEN);
			view.findViewById(R.id.button5).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button6).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button7).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button8).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button9).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button10).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button11).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button12).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button14).setBackgroundColor(Color.LTGRAY);
			break;
		case R.id.button14:
			movcond = 4;
			v.setBackgroundColor(Color.GREEN);
			view.findViewById(R.id.button5).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button6).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button7).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button8).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button9).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button10).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button11).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button12).setBackgroundColor(Color.LTGRAY);
			view.findViewById(R.id.button13).setBackgroundColor(Color.LTGRAY);
			break;
		case R.id.button15:
			if(evnt == 0){
				evnt = 1;
			}
			else {
				evnt = 0;
			}
			break;
		case R.id.button16:
			if(intn == 0){
				intn = 1;
			}
			else {
				intn = 0;
			}
			break;
		}

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

}

/*
http://www.byteworks.us/Byte_Works/Blog/Entries/2012/10/31_Accessing_the_Bluetooth_low_energy_Accelerometer_on_the_TI_SensorTag.html
http://developer.android.com/guide/topics/data/data-storage.html#filesInternal
http://stackoverflow.com/questions/16268191/android-accelerometer-store-data-in-file
http://chrisrisner.com/31-Days-of-Android--Day-23%E2%80%93Writing-and-Reading-Files
http://uwudamith.wordpress.com/2012/05/19/how-to-use-onclicklistener-in-android/
 */