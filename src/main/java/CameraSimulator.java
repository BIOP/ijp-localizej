
import java.util.Random;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import imagescience.random.BinomialGenerator;
import imagescience.random.GammaGenerator;
import imagescience.random.GaussianGenerator;
import imagescience.random.PoissonGenerator;
import imagescience.random.RandomGenerator;


public abstract class CameraSimulator {

	final static String []type={"CCD camera","sCMOS","EM CCD"};
	private double pixelSize;						//pixel size of the camera chip in um
	private double exp;					//exposure time of the camera
	private double quant;				//Quantum efficiency of the camera chip
	private double dark;
	private double cic;
	private double read;
	private int readoffset=000;
	
	CameraDialog cd;
										
	private Calibration cal;
	protected ImageProcessor cam_ip;
	protected ImagePlus cam_imp;
	private ImageStack cam_stack;
	private boolean readNoise=false;
	private boolean PoissonNoise=false;
	private boolean DetectionNoise=false;
	private boolean gainEM=false;
	private String title;
	private boolean stackInput=false;
	
	/*===================================================================================================
	 * Constructors
	 ==================================================================================================*/
	
	protected CameraSimulator(double pixelSize,double exposureTime,double quantumEff,ImageProcessor ip){
		this.pixelSize=pixelSize;
		this.exp=exposureTime;
		this.quant=quantumEff;
		this.cam_ip=new ShortProcessor(ip.getWidth(),ip.getHeight());
		this.initCalibration();
	}
	protected CameraSimulator(ImagePlus imp,double quant, double dark, double cic, double read, double exp){
		this.quant=quant;
		this.dark=dark;
		this.cic=cic;
		this.read=read;
		this.exp=exp;
		
		this.cal=imp.getCalibration();
		if (this.cal==null) initCalibrationDialog(); 
		
		this.pixelSize=cal.pixelHeight;
		this.exp=cal.frameInterval;
		this.initCalibration();
		this.cam_imp=imp.duplicate();
		this.cam_ip=this.cam_imp.getProcessor();
		
	}
	
	protected CameraSimulator(ImageProcessor ip,double quant, double dark, double cic, double read, double exp,  Calibration cal){
		this.quant=quant;
		this.dark=dark;
		this.cic=cic;
		this.read=read;
		this.exp=exp;
		
		this.cal=cal;
		if (this.cal==null) initCalibrationDialog(); 
		this.cam_ip=ip;
		this.pixelSize=cal.pixelHeight;
		this.exp=cal.frameInterval;
		
		this.initCalibration();
		this.cam_imp=new ImagePlus ("Camera Simulation",ip);
		
		
	}
	protected CameraSimulator(ImagePlus imp,double quantumEff){
		this.cal=imp.getCalibration();
		if (this.cal==null) initCalibrationDialog(); 
		this.cam_ip=new ShortProcessor(imp.getWidth(),imp.getHeight());
		
		this.pixelSize=cal.pixelHeight;
		this.exp=cal.frameInterval;
		this.quant=quantumEff;
		this.initCalibration();
		this.cam_imp=imp.duplicate();
		this.cam_ip=this.cam_imp.getProcessor();
		
	}
	
	protected CameraSimulator(ImageProcessor ip, double quantumEff, Calibration cal){
		
		if (cal==null) initCalibrationDialog();
		else this.cal=cal; 
		
		this.cam_ip=new ShortProcessor(ip.getWidth(),ip.getHeight());
		this.cam_ip=ip.duplicate();
		
		this.cam_imp=new ImagePlus("",this.cam_ip);
		this.pixelSize=cal.pixelHeight;
		this.exp=cal.frameInterval;
		this.quant=quantumEff;
		this.initCalibration();
		
		
	}
	/*===============================================================================================*
	 * Initialization routines
	 *===============================================================================================*/

	private void initCalibrationDialog(){
		GenericDialog gd=new GenericDialog("Camera Parameters");
		gd.addNumericField("Pixel size in um  :", 6.5, 1);
		gd.addNumericField("Exposure time in ms  :",30,1);
		gd.showDialog();
		
		this.cal.pixelHeight=gd.getNextNumber();
		this.cal.frameInterval=gd.getNextNumber();
		
		
	}
	private void initCalibration(){
		if (this.exp==0) this.exp=100;
		this.cal.frameInterval=this.exp;
		if (this.pixelSize==0) this.pixelSize=6.45;
		this.cal.pixelHeight=this.pixelSize;
		this.cal.pixelWidth=this.pixelSize;
	}
	protected void reset(){
		this.readNoise=false;
		this.PoissonNoise=false;
		this.DetectionNoise=false;
	}
	/*===============================================================================================
	 * get() and set() Methods
	 *================================================================================================*/
	
	public Calibration getCalibration(){
		return this.cal;
	}
	
	public double getQuantumEff(){
		return this.quant;
	}
	
	public int getWidth(){
		return this.cam_imp.getWidth();
	}
	
	public int getHeight(){
		return this.cam_imp.getHeight();
	}
	
	public ImagePlus getImagePlus(){
		
//		cam_imp.setProcessor(cam_ip);
		return this.cam_imp;
	}
	
	public double getPixelSize(){
		return this.pixelSize;
	}
	
	ImageProcessor getProcessor(){
		if (this.cam_ip==null) IJ.log("Image Processor is empty");
		return this.cam_ip;
	}
	
	public ImageStack getStack() {
		return cam_stack;
	}
	
	public String getTitle(){
		if (title==null) title="";
		if (this.PoissonNoise) title+="+Poisson Noise ";
		if (this.DetectionNoise) title+="+Detection Noise ";
		if (this.readNoise) title+="+Read Noise ";
		
		return title;
	}
	private void setImagePlus(){
		this.cam_imp.setProcessor(cam_ip);
		this.cam_imp.setTitle(getTitle());
	}
	
	public void setImagePlus(ImageStack stack){
		this.cam_imp.setStack(stack);
	}
	
	public void setImagePlus(ImagePlus imp){
		this.cam_imp=imp;
	}
	
	public void setImageProcessor(ImageProcessor ip){
		this.cam_ip=ip;
	}
	
	public void setTitle(String title){
		this.title=title;
		this.cam_imp.setTitle(title);
	}
	
	
	/*============================================================================================================================
	 *
	 * Image Processors for Noise calculation
	 *
	 ==============================================================================================================================*/
	
	public ImageProcessor PoissonNoise(){
		
		ImageProcessor ip=this.cam_ip.duplicate();
		
		if (!this.PoissonNoise){
			this.PoissonNoise=true;
			PoissonGenerator pg=new PoissonGenerator();
			final int width=ip.getWidth();
			final int height=ip.getHeight();
			final int bitDepth=ip.getBitDepth();
			ip.convertToFloat();
			double seed=0;
			for (int i=0;i<width;i++){
				for (int j=0;j<height;j++){
					switch (bitDepth){
						case 8:	seed=ip.getPixel(i,j);
							break;
						case 16:seed=ip.getPixel(i,j);
							break;
						case 32:seed=Float.intBitsToFloat(ip.getPixel(i, j));
							break;
					}
				
					seed=pg.next(seed);
					ip.setf(i, j, (float)seed);
				}
			}
		}
		else IJ.log("Poisson noise already added");
		
		return ip;
		
	}

	public ImageProcessor Bimodal() {
		ImageProcessor ip=this.cam_ip.duplicate();
		if (!this.DetectionNoise){
			this.DetectionNoise=true;
			final int w=ip.getWidth();
			final int h=ip.getHeight();
			int phot;
			double value;
			
			
			BinomialGenerator bmg=new BinomialGenerator();
			
			for (int i=0; i<w*h;i++){
				IJ.showProgress(i, w*h);
				
					if (ip.getBitDepth()<32) phot=ip.get(i);
					else phot=(int)Float.intBitsToFloat(ip.get(i));
					
					//bmg=new BinomialGenerator(phot,quantumEff);
					
					value=bmg.next(phot,quant);
					
					ip.setf(i, (int)(value));
					
					
				}
			
		}
		else IJ.log("Detection noise already added");
		
		return ip;
	}
	
	public ImageProcessor ReadNoise() {
		ImageProcessor ip=this.cam_ip.duplicate();
		if (!this.readNoise){
			
			this.readNoise=true;
			final int w=ip.getWidth();
			final int h=ip.getHeight();
			int sig,i=0;
			double value;
			
			int timeNow = Math.abs((int)System.currentTimeMillis());
			//IJ.log(""+timeNow);
			Random rnd=new Random();
			int seed=(int) (timeNow+rnd.nextInt((int)timeNow));
			GaussianGenerator gg=new GaussianGenerator(this.read,this.read,seed);
			
			for (i=0; i<w*h;i++){
				
				IJ.showProgress(i, w*h);
					
					if (ip.getBitDepth()<32) {
						sig=ip.get(i);
						
					}
					else {
						sig=(int)Float.intBitsToFloat(ip.get(i));
						
					}
					value=gg.next()+sig;
					if (value<0) value=0;
					ip.setf(i, (int)(value)+this.readoffset);
					
				}
		}

		return ip;
	}
	
	
	
	public ImageProcessor EMGain(double gainfactor){
		ImageProcessor ip=this.cam_ip.duplicate();
		ip=ip.convertToFloat();
		if (!this.gainEM){
			
			this.gainEM=true;
			
			final int w=ip.getWidth();
			final int h=ip.getHeight();
			int sig,i=0;
			double value;
			
			
			GammaGenerator gg=new GammaGenerator();
			
			for (i=0; i<w*h;i++){
				
				IJ.showProgress(i, w*h);
					
					if (ip.getBitDepth()<32) {
						sig=ip.get(i);
						
					}
					else {
						sig=(int)Float.intBitsToFloat(ip.get(i));
						
					}
					value=gg.next()+sig*gainfactor;
					if (value<0) value=0;
					ip.setf(i, (float)value);
					
				}
		}
		ip=ip.convertToShort(true);
		return ip;
	}
}
