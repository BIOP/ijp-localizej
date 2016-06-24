
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

public class BlinkGenerator {
	
	
	
	ImageProcessor ip_time, ip_psf;
	ImageStack timeStack;
	int pixel_size_time=4;
	int lambda=488;
	int nFrames;
	double na=1.4;
	static int scaleFactor=16;
	int width;
	int height;
	double darkNoise=400;
	double darkStdv=63.0;
	double quantumEff=0.7;
	
	double photons=7000/(2.506*2.506);
	
	public BlinkGenerator(ImagePlus imp){
		ImageProcessor test=PoissonNoise(imp.getProcessor());
		ImagePlus noise=new ImagePlus("",test);
		noise.show();
	}
	
	public BlinkGenerator(ImageStack stack){
		if (stack.getSize()<2) return;
		nFrames=stack.getSize();
		timeStack=stack;
		width=stack.getWidth();
		height=stack.getHeight();
		
		BesselWindow bw=new BesselWindow(stack.getWidth(),stack.getHeight(),pixel_size_time,lambda,na);
		ip_psf=bw.calc().getProcessor();
//		ImagePlus imp_psf=new ImagePlus("",ip_psf);
//		imp_psf.show();
	}
	
	void calculate(){
		final ImageStack resultStack = new ImageStack(timeStack.getWidth()/scaleFactor,timeStack.getHeight()/scaleFactor);  
		final Thread[] threads = newThreadArray();  
	    final ImageProcessor [] res_ip=new ImageProcessor[nFrames];
//	    final ImageProcessor calc=new ShortProcessor(timeStack.getWidth(),timeStack.getHeight());
	    
//	        for (int ithread = 0; ithread < threads.length; ithread++) {  
	  
	            // Concurrently run in as many threads as CPUs  
	  
//	            threads[ithread] = new Thread() {  
	                          
//	                { setPriority(Thread.NORM_PRIORITY); }  
	  
//	                public void run() {
	                	ImageProcessor calc=new ShortProcessor(timeStack.getWidth(),timeStack.getHeight());
	                	
	                	for (int i=0;i<nFrames;i++){
	                		IJ.showStatus("Complex conjugate multiply"+i+"/"+nFrames);
	                		ip_time=timeStack.getProcessor(i+1);
	                		calc=new ImagePlus("Test", convolve()).getProcessor();
	                		calc.multiply(photons);
	                		calc.add(0.001);
	                		ImagePlus intermediate=new ImagePlus("",calc);
	                		calc=PoissonNoise(calc);
	                		
	                		calc.setInterpolate(false);
	                		calc.setInterpolationMethod(ImageProcessor.NONE);
	                		calc=calc.resize(width/scaleFactor, height/scaleFactor);
	                		calc=Bimodal(calc);
	                		intermediate.setProcessor(calc);
	                		
	                		intermediate.show();
	                		
	                		ImagePlus impNoise = IJ.createImage("Untitled", "32-bit Black", width/scaleFactor, height/scaleFactor, 1);
	                		IJ.run(impNoise, "Add...", "value="+darkNoise+" stack");
	                		IJ.run(impNoise, "Add Specified Noise...", "stack standard="+darkStdv);
	                		
	                		ImageCalculator ic = new ImageCalculator();
	                		ic.run("Add", intermediate, impNoise);
	                		
	                		calc=intermediate.getProcessor();
	                		calc.setInterpolate(true);
	                		calc.setInterpolationMethod(ImageProcessor.NONE);
	                		res_ip[i]=calc.resize(width/scaleFactor, height/scaleFactor);
	                		
	                		
	                		res_ip[i].setMinAndMax(0, 65532);
	                		res_ip[i]=res_ip[i].convertToShort(true);
	                		
	                		impNoise.close();
	                		intermediate.close();
	                	}
//	                }
//	            };
//	        }
//	        startAndJoin(threads); 
	        
	        for (int i=0;i<nFrames;i++){
	        	resultStack.addSlice(res_ip[i]);
	        }
	        ImagePlus resultImg=new ImagePlus("Conv",resultStack);
			resultImg.show();
	}
	/**
	
	
	*/
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
	FHT convolve(){
	
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
	    public static void startAndJoin(Thread[] threads)  
	    {  
	        for (int ithread = 0; ithread < threads.length; ++ithread)  
	        {  
	            threads[ithread].setPriority(Thread.NORM_PRIORITY);  
	            threads[ithread].start();  
	        }  
	  
	        try  
	        {     
	            for (int ithread = 0; ithread < threads.length; ++ithread)  
	                threads[ithread].join();  
	        } catch (InterruptedException ie)  
	        {  
	            throw new RuntimeException(ie);  
	        }  
	    }
}
		
		
    	
  
       