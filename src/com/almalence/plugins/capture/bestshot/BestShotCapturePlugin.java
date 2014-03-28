/*
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is collection of files collectively known as Open Camera.

The Initial Developer of the Original Code is Almalence Inc.
Portions created by Initial Developer are Copyright (C) 2013 
by Almalence Inc. All Rights Reserved.
*/

package com.almalence.plugins.capture.bestshot;

import java.util.Date;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.Image;
import android.os.CountDownTimer;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.almalence.opencam.CameraController;
import com.almalence.opencam.CameraParameters;
/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginCapture;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.R;
+++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
//-+- -->

import com.almalence.SwapHeap;

/***
Implements burst capture plugin - captures predefined number of images
***/

public class BestShotCapturePlugin extends PluginCapture
{
	private boolean takingAlready=false;
		
    //defaul val. value should come from config
	private int imageAmount = 3;

    private boolean inCapture;
    private int imagesTaken=0;
	
	public BestShotCapturePlugin()
	{
		super("com.almalence.plugins.bestshotcapture",
			  R.xml.preferences_capture_bestshot,
			  0,
			  R.drawable.gui_almalence_mode_bestshot,
			  "Best Shot images");

		refreshPreferences();
	}
	
	@Override
	public void onResume()
	{
		takingAlready = false;
		imagesTaken=0;
		inCapture = false;
		refreshPreferences();
	}
	
	private void refreshPreferences()
	{
		try
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
			imageAmount = Integer.parseInt(prefs.getString("bestshotImagesAmount", "3"));
		}
		catch (Exception e)
		{
			Log.v("Bestshot capture", "Cought exception " + e.getMessage());
		}
		
        switch (imageAmount)
        {
        case 3:
        	quickControlIconID = R.drawable.gui_almalence_mode_burst3;
        	break;
        case 5:
        	quickControlIconID = R.drawable.gui_almalence_mode_burst5;
        	break;
        case 10:
        	quickControlIconID = R.drawable.gui_almalence_mode_burst10;
        	break;
        }       
	}
	
	@Override
	public void onQuickControlClick()
	{        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
        int val = Integer.parseInt(prefs.getString("bestshotImagesAmount", "1"));
        int selected = 0;
        switch (val)
        {
        case 3:
        	selected=0;
        	break;
        case 5:
        	selected=1;
        	break;
        case 10:
        	selected=2;
        	break;
        }
        selected= (selected+1)%3;
        
    	Editor editor = prefs.edit();
    	switch (selected)
        {
        case 0:
        	quickControlIconID = R.drawable.gui_almalence_mode_burst3;
        	editor.putString("bestshotImagesAmount", "3");
        	break;
        case 1:
        	quickControlIconID = R.drawable.gui_almalence_mode_burst5;
        	editor.putString("bestshotImagesAmount", "5");
        	break;
        case 2:
        	quickControlIconID = R.drawable.gui_almalence_mode_burst10;
        	editor.putString("bestshotImagesAmount", "10");
        	break;
        }
    	editor.commit();
	}

	public boolean delayedCaptureSupported(){return true;}
	
	@Override
	public void OnShutterClick()
	{
		if (inCapture == false)
        {
			Date curDate = new Date();
			SessionID = curDate.getTime();
			
			MainScreen.thiz.MuteShutter(true);
			
			int focusMode = CameraController.getInstance().getFocusMode();
			if(takingAlready == false && (CameraController.getFocusState() == CameraController.FOCUS_STATE_IDLE ||
					CameraController.getFocusState() == CameraController.FOCUS_STATE_FOCUSING)
					&& focusMode != -1
					&& !(focusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE ||
	      				 focusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO ||
	    				 focusMode == CameraParameters.AF_MODE_INFINITY ||
	    				 focusMode == CameraParameters.AF_MODE_FIXED ||
	    				 focusMode == CameraParameters.AF_MODE_EDOF)
	        				&& !MainScreen.getAutoFocusLock())
				takingAlready = true;			
			else if(takingAlready == false)
			{
				takePicture();
			}
        }
	}
	
	
	public void takePicture()
	{
		refreshPreferences();
		inCapture = true;
		takingAlready = true;
		new CountDownTimer(50, 50) {
		     public void onTick(long millisUntilFinished) {}
		     public void onFinish() 
		     {
				Message msg = new Message();
				msg.arg1 = PluginManager.MSG_NEXT_FRAME;
				msg.what = PluginManager.MSG_BROADCAST;
				MainScreen.H.sendMessage(msg);					
		     }
		  }.start();
	}


	@Override
	public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera)
	{
		imagesTaken++;
		int frame_len = paramArrayOfByte.length;
		int frame = SwapHeap.SwapToHeap(paramArrayOfByte);
    	
    	if (frame == 0)
    	{
    		Log.i("Bestshot", "Load to heap failed");
    		Message message = new Message();
    		message.obj = String.valueOf(SessionID);
			message.what = PluginManager.MSG_CAPTURE_FINISHED;
			MainScreen.H.sendMessage(message);
			
			imagesTaken=0;
			MainScreen.thiz.MuteShutter(false);
			inCapture = false;
			return;
    	}
    	String frameName = "frame" + imagesTaken;
    	String frameLengthName = "framelen" + imagesTaken;
    	
    	PluginManager.getInstance().addToSharedMem(frameName+String.valueOf(SessionID), String.valueOf(frame));
    	PluginManager.getInstance().addToSharedMem(frameLengthName+String.valueOf(SessionID), String.valueOf(frame_len));
    	PluginManager.getInstance().addToSharedMem("frameorientation" + imagesTaken + String.valueOf(SessionID), String.valueOf(MainScreen.guiManager.getDisplayOrientation()));
    	PluginManager.getInstance().addToSharedMem("framemirrored" + imagesTaken + String.valueOf(SessionID), String.valueOf(MainScreen.getCameraMirrored()));
    	
    	if(imagesTaken == 1)
    		PluginManager.getInstance().addToSharedMem_ExifTagsFromJPEG(paramArrayOfByte, SessionID);
		
		try
		{
			paramCamera.startPreview();
		}
		catch (RuntimeException e)
		{
			Log.i("Bestshot", "StartPreview fail");
			Message message = new Message();
			message.obj = String.valueOf(SessionID);
			message.what = PluginManager.MSG_CAPTURE_FINISHED;
			MainScreen.H.sendMessage(message);
			
			imagesTaken=0;
			MainScreen.thiz.MuteShutter(false);
			inCapture = false;
			return;
		}
		if (imagesTaken < imageAmount)
			MainScreen.H.sendEmptyMessage(PluginManager.MSG_TAKE_PICTURE);
		else
		{
			PluginManager.getInstance().addToSharedMem("amountofcapturedframes"+String.valueOf(SessionID), String.valueOf(imagesTaken));
			
			Message message = new Message();
			message.obj = String.valueOf(SessionID);
			message.what = PluginManager.MSG_CAPTURE_FINISHED;
			MainScreen.H.sendMessage(message);
			
			imagesTaken=0;
			inCapture = false;
		}
		takingAlready = false;
	}
	
	@Override
	public void onImageAvailable(Image im)
	{
		
	}
	
	@Override
	public void onAutoFocus(boolean paramBoolean)
	{
		if(takingAlready == true)
			takePicture();
	}

	@Override
	public boolean onBroadcast(int arg1, int arg2)
	{
		if (arg1 == PluginManager.MSG_NEXT_FRAME)
		{
			// play tick sound
			MainScreen.guiManager.showCaptureIndication();
    		MainScreen.thiz.PlayShutter();
    		
    		try 
    		{
    			CameraController.captureImage(1, ImageFormat.JPEG);
			}
    		catch (Exception e)
			{
				e.printStackTrace();
				Log.e("Bestshot takePicture() failed", "takePicture: " + e.getMessage());
				inCapture = false;
				takingAlready = false;
				Message msg = new Message();
    			msg.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
    			msg.what = PluginManager.MSG_BROADCAST;
    			MainScreen.H.sendMessage(msg);	    			
    			MainScreen.guiManager.lockControls = false;
			}				
    		return true;
		}
		return false;
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera paramCamera){}	
}
