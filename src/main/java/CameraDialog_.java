
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;


public class CameraDialog_ implements PlugIn{

		
	public void run(String arg) {
		ImagePlus imp=IJ.openImage();
		
		CameraDialog cd=new CameraDialog();
		cd.showDialog();
		
		ImageProcessor ip=imp.getProcessor().duplicate();
		double max=ip.maxValue();
		ip=ip.convertToShort(false);
			
		ip.multiply(cd.photon/max);
		imp.setProcessor(ip);
				
		switch (selectCameraType(cd.choice)){
		case 0: CCD_Simulator ccd;
				if (cd.preset) ccd=new CCD_Simulator(imp);
				else ccd=new CCD_Simulator(imp,cd.quant,cd.dark,cd.cic,cd.read,cd.exp);
				imp=ccd.run();
				break;
		case 1: IJ.showMessage("...non valid choice");
				break;
		case 2: EMCCD_Simulator emccd;
				if (cd.preset) emccd=new EMCCD_Simulator(imp,cd.emGain);
				else emccd=new EMCCD_Simulator(imp,cd.quant,cd.dark,cd.cic,cd.read,cd.exp,cd.emGain);
				imp=emccd.run();
		}
	imp.show();	
	}
int selectCameraType(String choice){
	int i=0;
	for (i=0;i<CameraSimulator.type.length;i++){
		if (choice.equals(CameraSimulator.type[i])) break;
	}
	return i;
}
}
