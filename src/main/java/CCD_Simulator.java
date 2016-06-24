
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;


public class CCD_Simulator extends CameraSimulator {
	private boolean knownCamera=false;
	
	public static int COOLSNAPHQ=1;
	
	public CCD_Simulator(double pixelSize, int exposureTime, double quantumEff, ImageProcessor ip){
		super(pixelSize,exposureTime,quantumEff, ip);
		
	}
	public CCD_Simulator(ImagePlus imp){
		super(imp,0.7,0.0005,0.001,6,10);
	}
	
	public CCD_Simulator(ImagePlus imp, double quant, double dark, double cic, double read, double exp){
		super(imp,quant,dark,cic,read,exp);
	}
	
	
	public CCD_Simulator(ImagePlus imp,CameraDialog cd){
		super(imp,cd.quant,cd.dark,cd.cic,cd.read,cd.exp);
		this.cd=cd;
		
	}
	
	public CCD_Simulator(ImageProcessor ip, CameraDialog cd,Calibration cal){
		super(ip,cd.quant,cd.dark,cd.cic,cd.read,cd.exp, cal);
		this.cd=cd;
	}
	public ImagePlus run(){
		int slice=super.getImagePlus().getStackSize();
		super.setTitle("CCD Camera");
		if (slice==1){
			this.setImageProcessor(this.PoissonNoise());
			this.setImageProcessor(this.Bimodal());
			this.setImageProcessor(this.ReadNoise());
			this.cam_imp.setProcessor(this.cam_ip);
		}
		else {
			
			ImageStack stack=this.getImagePlus().createEmptyStack();

			for (int i=1;i<=slice;i++){
				IJ.showProgress(i/slice);
				
				this.getImagePlus().setSliceWithoutUpdate(i);
				CCD_Simulator ccd=new CCD_Simulator(this.getProcessor().duplicate(),this.cd,this.getCalibration());

					ccd.run();
					stack.addSlice(ccd.getProcessor());
//					ImagePlus test=new ImagePlus("test",stack);
//					test.show();

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
