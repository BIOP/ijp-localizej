
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.*;

import java.lang.*;
import java.awt.*;
import java.awt.image.*;
import java.text.DecimalFormat;

import ij.plugin.filter.PlugInFilter;

import java.util.*;


public class MaskBlinkDialog_ implements PlugIn {

	
	
	public void run(String arg) {
		
		IJ.log("Please specify Parameters for fluorophore blinking");
		
		GenericDialog gd=new GenericDialog("Blink Parameters");
		gd.addNumericField("on Rate", 0.01, 6);
		gd.addNumericField("off Rate", 0.95, 6);
		gd.addNumericField("Bleach rate", 0.2, 2);
		gd.addNumericField("Number_of_particles", 100000,0);
		gd.addNumericField("Number_of_Frames",1500,0);
		gd.addNumericField("Number_of_photons", 500, 0);
		gd.addStringField("Path", "path");
		gd.addCheckbox("Save_Blink Stack?", false);
		gd.addStringField("Name_BlinkStack", "saveBlink");
		gd.addCheckbox("Save_Time Trace?", false);
		gd.addStringField("Name_TimeTrace", "saveTime");
		gd.addCheckbox("Projection time stack (unscaled)",false);
		
		gd.showDialog();		
		if (gd.wasCanceled()) return;
		
		final double on=gd.getNextNumber();
		final double off=gd.getNextNumber();
		final double bleach=gd.getNextNumber();
		final int numPar=(int)gd.getNextNumber();
		final int numFrames=(int)gd.getNextNumber();
		final int photons=(int)(gd.getNextNumber()/(2.506*2.506));
		
		String path=gd.getNextString();
//		if (path.equals("path")) path="d:\\Blink\\ContrastGridRim.tif";
		final boolean saveBlink=gd.getNextBoolean();
		String blinkName=gd.getNextString();
//		if (blinkName.equals("saveBlink")) blinkName="d:\\Blink\\BlinkStack.tif";
		
		final boolean saveTime=gd.getNextBoolean();
		String timeName=gd.getNextString();
		if (timeName.equals("saveTime")) timeName="d:\\Blink\\TimeTrace.tif";
		
		final boolean project=gd.getNextBoolean();
		
		IJ.log("Please specify the Camera Parameters");
		CameraDialog cd=new CameraDialog();
		cd.showDialog();
		
		TimeGenerator time=new TimeGenerator(1024,1024);
		time.setParticleNumber(numPar);
		time.setFrameNumber(numFrames);
		time.setOnRate(on);
		time.setOffRate(off);
		time.setBleachRate(bleach); 
		time.generateMask(path);
		
		time.heavyTimeTrace();
		if (saveTime){
			IJ.save(new ImagePlus("TimeTrace condensed",time.condensedTimeTrace()),timeName);
		}
		
		if (project) time.getProjectedTimeTrace().show();
		
		IJ.log("Please specify the diffracton parameters (PSF)");
		DiffractionDialog dd=new DiffractionDialog();
		dd.showDialog();
		
		DiffractionGenerator dg=new DiffractionGenerator(time,photons,dd);
//		dg.multiThreadCalculate().show();
		
		if (cd.choice.equals(CameraSimulator.type[0])){
			CCD_Simulator ccd=new CCD_Simulator(dg.multiThreadCalculate(),cd);
				
			if (saveBlink){
				IJ.save(ccd.run(),blinkName);
			} else  ccd.run().show();
		}
		
		if (cd.choice.equals(CameraSimulator.type[2])){
			EMCCD_Simulator emccd=new EMCCD_Simulator(dg.multiThreadCalculate(),cd);
				
			if (saveBlink){
				IJ.save(emccd.run(),blinkName);
			} else emccd.run().show();
		}
		
	}
}

		
		
		
