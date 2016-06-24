
import ij.gui.GenericDialog;


public class CameraDialog {
	
	GenericDialog gd;
	double photon=200;
	String choice=CameraSimulator.type[0];
	double quant=0.7;
	double dark=0.0005;
	double cic=0.001;
	double exp=10;
	double read=5;
	double emGain=1000;
	boolean preset;
		
	public CameraDialog(){
		
		gd=new GenericDialog("Camera Parameters");
		gd.addNumericField("PhotonFlux", photon,0);
		gd.addChoice("Camera Type", CameraSimulator.type, choice);
		gd.addNumericField("QE", quant, 2);
		gd.addNumericField("Dark noise cps:", dark, 4);
		gd.addNumericField("CIC noise cps:", cic, 4);
		gd.addNumericField("Read Noise", read, 0);
		gd.addNumericField("Exposure Time ms:",exp,0);
		gd.addNumericField("EM gain", emGain, 0);
		gd.addCheckbox("Preset camera parameters", true);
	}
	
	public void showDialog() {
		
		gd.showDialog();
		if (gd.wasCanceled()) return;
		
		photon=gd.getNextNumber();
		choice=gd.getNextChoice();
		quant=gd.getNextNumber();
		dark=gd.getNextNumber();
		cic=gd.getNextNumber();
		read=gd.getNextNumber();
		exp=gd.getNextNumber();
		emGain=gd.getNextNumber();
		preset=gd.getNextBoolean();
			
	}

}
