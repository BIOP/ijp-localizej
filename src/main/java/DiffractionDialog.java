
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;


public class DiffractionDialog {
	
	GenericDialog gd;
	int wave=488;
	double na=1.4;
	int pixel=5;
	int camerapixel=80;
	int scale=camerapixel/pixel;
	
	DiffractionDialog(){
		
		gd=new GenericDialog("Diffraction Parameters");
		gd.addNumericField("Wavelength/nm", wave,0);
		gd.addNumericField("NA objective", na,2);
		gd.addNumericField("Pixelsize_PSF [nm]", pixel, 0);
		gd.addNumericField("Pixelsize_camera [nm]", camerapixel, 0);
		
	}
	
	public void showDialog() {
		
		gd.showDialog();
		if (gd.wasCanceled()) return;
		
		wave=(int)gd.getNextNumber();
		na=gd.getNextNumber();
		pixel=(int)gd.getNextNumber();
		camerapixel=(int)gd.getNextNumber();
		
		scale=camerapixel/pixel;	
	}

}
