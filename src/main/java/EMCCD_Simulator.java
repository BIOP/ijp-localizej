
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;


public class EMCCD_Simulator extends CameraSimulator {
	private boolean knownCamera=false;
	
	public double gainfactor=1000;
	
	public EMCCD_Simulator(double pixelSize, int exposureTime, double quantumEff, ImageProcessor ip){
		super(pixelSize,exposureTime,quantumEff/2, ip);
		
	}
		
	public EMCCD_Simulator(ImagePlus imp, double gain){
		super(imp,0.46,0.001,0.0018,0.105*gain,10);
		this.gainfactor=gain;
	}
	public EMCCD_Simulator(ImagePlus imp, double quant, double dark, double cic, double read, double exp, double gain){
		super(imp,quant/2,dark,cic,read*gain,exp);
		this.gainfactor=gain;
	}
	
	public EMCCD_Simulator(ImagePlus imp, CameraDialog cd){
		super(imp,cd.quant/2,cd.dark,cd.cic,cd.read*cd.emGain,cd.exp);
		this.gainfactor=cd.emGain;
	}
	
	public EMCCD_Simulator(ImageProcessor ip, double quantumEff,Calibration cal){
			super(ip,quantumEff,cal);
	}
	public ImagePlus run(){
		int slice=super.getImagePlus().getStackSize();
		super.setTitle("EM CCD Camera");
		if (slice==1){
			this.setImageProcessor(this.PoissonNoise());
			this.setImageProcessor(this.Bimodal());
			this.setImageProcessor(this.EMGain(gainfactor));
			this.setImageProcessor(this.ReadNoise());
			this.cam_imp.setProcessor(this.cam_ip);
		}
		else {
			
			ImageStack stack=this.getImagePlus().createEmptyStack();
			
			for (int i=1;i<=slice;i++){
				IJ.showProgress(i/slice);
				
				this.getImagePlus().setSliceWithoutUpdate(i);
				EMCCD_Simulator ccd=new EMCCD_Simulator(this.getProcessor().duplicate(),this.getQuantumEff(),this.getCalibration());

					ccd.run();
					stack.addSlice(ccd.getProcessor());

			}
			this.setImagePlus(stack);
		}
		return this.getImagePlus();
	}
	
	ImagePlus resultStack(){
		 
		 ImageStack stack=new ImageStack(super.getWidth(),super.getHeight());
		 stack.addSlice(this.getImagePlus().getProcessor().duplicate().convertToFloat());
		 stack.addSlice(this.PoissonNoise().duplicate().convertToFloat());
		 stack.addSlice(this.Bimodal().duplicate());
		 
		 return new ImagePlus("Noise stack",stack);
	 }
	
}
