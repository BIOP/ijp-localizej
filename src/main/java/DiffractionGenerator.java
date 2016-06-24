
import ij.*;
import ij.gui.*;
import ij.measure.Calibration;
import ij.process.*;
import ij.plugin.*;

import java.lang.*;
import java.awt.*;
import java.awt.image.*;
import java.text.DecimalFormat;

import ij.plugin.filter.PlugInFilter;
import imagescience.random.BinomialGenerator;
import imagescience.random.PoissonGenerator;

import java.util.*;

public class DiffractionGenerator extends Thread {
	
	// ===============used for multithreading only ========================== 
	//start and stop for the individual threads
	private int start=0;
	private int stop=0;
	private int multiStackSize=0;
	//number of processors
	private final static int n_cpus=Runtime.getRuntime().availableProcessors();
	//=======================================================================
	private boolean isTimeGenerator=false;
	private boolean isStack=false;
	
	private ImageStack timeStack;
	private TimeGenerator tg;
	private static ImageProcessor ip_time;
	private static ImageProcessor ip_psf; 
	private ImageProcessor [] res_ip;
	
	private int nFrames;
	private int width;
	private int height;
	
	private DiffractionDialog dd;
	//================Simple PSF parameters=================================
	private int pixel_size_time=5;
	private int lambda=488;
	private double na=1.4;
	private int scaleFactor=16;
	
	//===============Camera Parameters===no longer used===================================
	private double darkNoise=400;
	private double darkStdv=63.0;
	private double quantumEff=0.7;
	
	private double photons=7000/(2.506*2.506);
	
	//=====================Constructors===================================================
	
	public DiffractionGenerator(ImagePlus imp){
		
		BesselWindow bw=new BesselWindow(imp.getWidth(),imp.getHeight(),pixel_size_time,lambda,na);
		ip_psf=bw.calc().getProcessor();
		ip_time=imp.getProcessor();
	}
	
	public DiffractionGenerator(TimeGenerator tg, double photons, DiffractionDialog dd){
		isTimeGenerator=true;
		this.dd=dd;
		this.tg=tg;
		this.nFrames=tg.getStackSize(); 
		this.photons=photons;
		this.scaleFactor=dd.scale;
		
		if (!tg.isCondensed()) return;
		BesselWindow bw=new BesselWindow(tg.getWidth(),tg.getHeight(),dd.pixel,dd.wave,dd.na);
		ip_psf=bw.calc().getProcessor();
		
	}
	
	public DiffractionGenerator(TimeGenerator tg,int start,int stop, DiffractionDialog dd){
		isTimeGenerator=true;
		this.dd=dd;
		this.scaleFactor=dd.scale;
		this.tg=tg;
		this.width=tg.getWidth();
		this.height=tg.getHeight();
		this.nFrames=tg.getStackSize(); 
		this.start=start;
		this.stop=stop;
		this.multiStackSize=stop-start+1;
		if (!tg.isCondensed()) return;
		BesselWindow bw=new BesselWindow(tg.getWidth(),tg.getHeight(),pixel_size_time,lambda,na);
		ip_psf=bw.calc().getProcessor();
		
	}
	
	public DiffractionGenerator(ImageStack stack){
		if (stack.getSize()<2) return;
		this.isStack=true;
		nFrames=stack.getSize();
		timeStack=stack;
		width=stack.getWidth();
		height=stack.getHeight();
		
		BesselWindow bw=new BesselWindow(stack.getWidth(),stack.getHeight(),pixel_size_time,lambda,na);
		ip_psf=bw.calc().getProcessor();
//		ImagePlus imp_psf=new ImagePlus("",ip_psf);
//		imp_psf.show();
	}
	//==================set() and get()===================================================
	ImagePlus getSingleImage(){
		ImageProcessor ip=convolve().duplicate();
		width=ip.getWidth();
		height=ip.getHeight();
		
		ip.setInterpolate(true);
		ip.setInterpolationMethod(ImageProcessor.NONE);
		ip=ip.resize(width/scaleFactor, height/scaleFactor);
		ip=ip.convertToShort(false);
		return new ImagePlus("",ip);
	}
	
	public void setPhotons(double photons){
		this.photons=photons;
	}
	public int getPhotons(){
		return (int)this.photons;
	}
//====================== ImageProcessors =============================
	ImageProcessor PoissonNoise(ImageProcessor ip){
		PoissonGenerator pg=new PoissonGenerator();
		final int width=ip.getWidth();
		final int height=ip.getHeight();
		double seed;
		for (int i=0;i<width;i++){
			for (int j=0;j<height;j++){
				seed=Float.intBitsToFloat(ip.getPixel(i, j));
				seed=pg.next(seed);
				ip.putPixelValue(i, j, seed);
			}
		}
		return ip;
		
	}
	public ImageProcessor Bimodal(ImageProcessor ip){
		
		
		
		final int w=ip.getWidth();
		final int h=ip.getHeight();
		int phot;
		final ImageProcessor out=new ShortProcessor(w,h);
		final BinomialGenerator bmg=new BinomialGenerator(20,quantumEff);
		
		for (int i=0; i<w;i++){
			IJ.showProgress(i, w);
			for (int j=0;j<h;j++){
				phot=ip.get(i,j);
				
				out.set(i+w*j, (int)((bmg.next()*phot)/20));
				
				
			}
		}
		
		
		return out;
	}
	
	ImagePlus multiThreadCalculate(){
		
		long start=System.currentTimeMillis();
		
		this.width=tg.getWidth();
		this.height=tg.getHeight();
		
		final DiffractionGenerator [] calculate = new DiffractionGenerator(this.tg,this.getPhotons(),this.dd).getArray(); 
		final ImageStack resultStack=new ImageStack(this.width/scaleFactor,this.height/scaleFactor);  
		
		startAndJoin(calculate);
		
		long end=System.currentTimeMillis();    
		IJ.log("Processing time convolution in sec: "+(end-start)/1000);

//	    for (int i=0;i<nFrames;i++){
//	        	resultStack.addSlice(res_ip[i]);
//	    }  
	    

	    
	                	
	    
	    return new ImagePlus("Conv",DiffractionGenerator.getResultStack(calculate));
	}
	public void run(){
		int w=0;
	    int h=0;
	    
		if (isStack){
	    	w=timeStack.getWidth();
	    	h=timeStack.getHeight();
	    }
	    if (isTimeGenerator){
	    	w=tg.getWidth();
	    	h=tg.getHeight();
	    }
		ImageProcessor calc=new ShortProcessor(w,h);
		this.res_ip=new ImageProcessor[this.multiStackSize];
		int count=0;
	    for (int i=start;i<=stop;i++){
	    	if (i%(nFrames/20)==0) IJ.showStatus("Complex conjugate multiply "+i+"/ "+nFrames);
	                		
	        if (isStack) ip_time=timeStack.getProcessor(i+1);
	        if (isTimeGenerator) ip_time=tg.getSlice(i);
	        
	        
	        //new ImagePlus("frame "+i,ip_time).show();
	        
	        calc=new ImagePlus("Test", convolve()).getProcessor();
	        calc.multiply(photons);
	                			                		
	        calc.setInterpolate(false);
	        calc.setInterpolationMethod(ImageProcessor.NONE);
	                		
	        this.res_ip[count]=calc.resize(width/scaleFactor, height/scaleFactor);
	                		
	                		
	        this.res_ip[count].setMinAndMax(0, 65532);
	        this.res_ip[count]=res_ip[count].convertToShort(true);
	        count++;
	    }    
//	    IJ.log("Core processing finished");    
	    
	     
			
	}
	ImagePlus calculate(){
		if (isStack){
			this.width=timeStack.getWidth()/scaleFactor;
			this.height=timeStack.getHeight()/scaleFactor;
			
			
		}
		if (isTimeGenerator){
			this.width=tg.getWidth()/scaleFactor;
			this.height=tg.getHeight()/scaleFactor;
		}
		
		final ImageStack resultStack=new ImageStack(this.width,this.height);  
		final long start=System.currentTimeMillis(); 
		final ImageProcessor [] res_ip=new ImageProcessor[nFrames];
//		final ImageProcessor calc=new ShortProcessor(this.width,this.height);
	                	
	    for (int i=0;i<nFrames;i++){
	    	if (i%(nFrames/20)==0) IJ.showStatus("Complex conjugate multiply "+i+"/ "+nFrames);
	                		
	        if (isStack) ip_time=timeStack.getProcessor(i+1);
	        if (isTimeGenerator) ip_time=tg.getSlice(i);
	                		
	        final ImageProcessor calc=new ImagePlus("Test", convolve()).getProcessor();
	        calc.multiply(photons);
	                			                		
	        calc.setInterpolate(false);
	        calc.setInterpolationMethod(ImageProcessor.NONE);
	                		
	        res_ip[i]=calc.resize(this.width, this.height);
	                		
	                		
	        res_ip[i].setMinAndMax(0, 65532);
	        res_ip[i]=res_ip[i].convertToShort(true);
	    }    
	        
	    long end=System.currentTimeMillis();    
		IJ.log("duration: "+(end-start));
	    for (int i=0;i<nFrames;i++){
	        	resultStack.addSlice(res_ip[i]);
	    }
	    return new ImagePlus("Conv",resultStack); 
			
	}
	/**
	
	
	*/
	
	private static FHT convolve(){
	
		FHT h1, h2=null;
	
		h1 = new FHT(ip_time);
		h2 = new FHT(ip_psf);
			
		if (!h1.powerOf2Size()) {
			IJ.error("FFT Math", "Images must be a power of 2 size (256x256, 512x512, etc.)");
			return null;
		}
/*		if (stack.getWidth()!=filter.getWidth()) {
			IJ.error("FFT Math", "Images must be the same size");
			return null;
		}
*/
		
		h1.transform();
		h2.transform();
		
		FHT result=null;
		
		result = h1.multiply(h2); 
		result.inverseTransform();
		result.swapQuadrants();
		result.resetMinAndMax();
		return result;
	}
	
	
	 /** Create a Thread[] array as large as the number of processors available. 
	    * From Stephan Preibisch's Multithreading.java class. See: 
	    * http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD 
	    */  
	    private Thread[] newThreadArray() {  
	        int n_cpus = Runtime.getRuntime().availableProcessors();  
	        return new Thread[n_cpus];  
	    }  
	  
	    /** Start all given threads and wait on each of them until all are done. 
	    * From Stephan Preibisch's Multithreading.java class. See: 
	    * http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD 
	    */  
	    public static void startAndJoin(DiffractionGenerator [] diffArray)  
	    {  
	        for (int ithread = 0; ithread < n_cpus; ++ithread)  
	        {  
	            diffArray[ithread].setPriority(Thread.NORM_PRIORITY);  
	            diffArray[ithread].start();  
	        }  
	  
	        try  
	        {     
	            for (int ithread = 0; ithread < n_cpus; ++ithread)  
	                diffArray[ithread].join();  
	        } catch (InterruptedException ie)  
	        {  
	            throw new RuntimeException(ie);  
	        }  
	    }
	    public static ImageStack getResultStack(DiffractionGenerator [] diffArray){
	    	int w=diffArray[0].width/diffArray[0].scaleFactor;
	    	int h=diffArray[0].height/diffArray[0].scaleFactor;
	    	ImageStack stack=new ImageStack(w,h);
	    	for (int i=0;i<diffArray.length;i++){
	    		ImageProcessor [] ip=diffArray[i].getResultProcessor();
	    		for (int j=0;j<ip.length;j++){
	    			if (ip[j]!=null) stack.addSlice(ip[j]);
	    		}
	    	}
	    	return stack;
	    }
	    public DiffractionGenerator [] getArray() {
			DiffractionGenerator array []=new DiffractionGenerator[n_cpus];
			
			int stackSize=tg.getStackSize();
			int [] start=new int [n_cpus];
			int [] stop=new int[n_cpus];
			
			int mod=stackSize % n_cpus;
			int delta=stackSize/n_cpus;
			
//			if (stackSize%n_cpus==0) delta=stackSize/n_cpus;
//			else delta=(int)Math.round(0.5+((double)stackSize/n_cpus));
			
			
			
			for (int i=0;i<n_cpus;i++){
				start[i]=delta*(i);
				stop[i]=delta*(i+1)-1;
				if (i>=n_cpus-mod) {
					start[i]=stop[i-1]+1;
					stop[i]=start[i]+delta;
				}
//				IJ.log("i="+i+"start="+start[i]+"   stop"+stop[i]);
				array[i]=new DiffractionGenerator(this.tg,start[i],stop[i],this.dd);
				array[i].setPhotons(this.getPhotons());
			}
			return array;
	}
	public ImageProcessor [] getResultProcessor(){
		return this.res_ip;
	}
}
		
		
    	
  
       