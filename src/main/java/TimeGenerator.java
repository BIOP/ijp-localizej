
import ij.*;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Thresholder;
import ij.process.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TimeGenerator {
	final static String gridPath=Prefs.get("TimeGenerator.gridPath", "path");

	private int numParticles=200;
	private int numFrames=20; 
	private double switchOn=0.20;
	private double switchOff=0;
	private double bleachRate=0.2;
	
	private int framewidth=256;
	private int frameheight=256;
	private int bright=1;
	private Calibration cal;
	private String stacktitle="Time Stack";
	private ImagePlus nimp;
	private int XPos[];
	private int YPos[];
	private int timeTrace[][];
	public Vector <Integer >store=new Vector <Integer>(3);
	
	private boolean isCondensed=false;
	boolean Diffusion2D=true;
	
/*=======================================================================================
 * 		Constructors
 *=========================================================================================*/
	TimeGenerator(){
		this.init();
	}
	TimeGenerator(int w, int h, int numPart){
		this.setFrameSize(w,h);
		this.setParticleNumber(numPart);
		this.init();
	}
	
	TimeGenerator(int w, int h){
		this.setFrameSize(w, h);
		this.init();
	}
	
	TimeGenerator(ImagePlus imp){
		this.init(imp);
	}
/*=======================================================================================
 * 		set() and get() 
 *=========================================================================================*/	
	private void setFrameSize(int width, int height){
		this.framewidth=width;
		this.frameheight=height;
	}
	void setOnRate(double on){
		if (on>1.0) on=1.0;
		this.switchOn=on;
	}
	void setOffRate(double off){
		if (off>1.0) off=1.0;
		this.switchOff=off;
	}
	void setBleachRate(double bleach){
		this.bleachRate=bleach;
	}
	void setFrameNumber(int n){
		this.numFrames=n;
		this.init();
	}
	
	void setParticleNumber(int n){
		this.numParticles=n;
		this.init();
	}
	
	boolean isCondensed(){
		return this.isCondensed;
	}
	ImagePlus getProjectedTimeTrace(){
		return this.projectTimeTrace();
	}
	int getWidth(){
		return framewidth;
	}
	int getHeight(){
		return frameheight;
	}
	int getStackSize(){
		return store.get(0);
	}
	ShortProcessor getSlice(int slice){
		
		int stackSize=store.get(0);
		int width=store.get(1);
		int height=store.get(2);
		int startSlice=3;
		ShortProcessor n_ip=new ShortProcessor(width,height);
		for (int i=0;i<stackSize;i++){
			
			int numPart=store.get(startSlice+1);
			if (i==slice) {
				for (int j=0;j<numPart;j++){
					int pos=store.get(startSlice+2+j);
					n_ip.set(pos, 1);
				}
				i=stackSize;
			}
			startSlice+=numPart+2;
		}
		
		return n_ip;
	}
/*=======================================================================================
 * 		Initialization
 *=========================================================================================*/	
	void init(int number){
		
		XPos=new int[number];
		YPos=new int[number];
		timeTrace=new int[2][number];
	}
	
	void init(){
		
		XPos=new int[this.numParticles];
		YPos=new int[this.numParticles];
		timeTrace=new int[2][this.numParticles];
	}
	void init(ImagePlus imp){
		FloatProcessor ip=(FloatProcessor) imp.getProcessor();
		this.setFrameSize((int)ip.getf(1),(int)ip.getf(2));
	}
	
/*=======================================================================================
 * 		generate Points 
 *=========================================================================================*/	
	public void generatePoints(int edge){
		Random generator = new Random ();
		for (int i=0;i<(numParticles);i++){
			XPos[i]=(edge)+generator.nextInt(framewidth-2*edge);
			YPos[i]=(edge)+generator.nextInt(frameheight-2*edge);
			timeTrace[0][i]=0;
		}
	}
	public void generateGridPoints(){
		
		
		int num=(int)Math.sqrt(numParticles);
		int distX=(int)(0.75*framewidth/(num-1));
		int distY=(int)(0.75*frameheight/(num-1));
		
		this.setParticleNumber(num*num);
		
		int count=0;
		for (int j=0;j<num;j++){
			for (int i=0;i<num;i++){
				XPos[count]=(int)(0.125*framewidth)+i*distX;
				YPos[count]=(int)(0.125*frameheight)+j*distY;
				count++;
			
			}
		}
		
	}
	public void generateMask(String path){
		
		String open=checkPath(path);
		ImagePlus imp=IJ.openImage(open);
		if (imp==null) {
			IJ.log("Please select an image (mask)");
			imp=IJ.openImage();
//			imp.show();
			Prefs.set("TimeGenerator.gridPath", imp.getTitle());
		}
		
		this.framewidth=imp.getWidth();
		this.frameheight=imp.getHeight();
		ImageProcessor ipTest=imp.getProcessor();
		Random generator = new Random ();
		
		long start=System.currentTimeMillis();
		for (int i=0;i<numParticles;i++){
			do {
				XPos[i]=generator.nextInt(framewidth);
				YPos[i]=generator.nextInt(frameheight);
				timeTrace[0][i]=0;
				
			} while (ipTest.getPixel(XPos[i], YPos[i])==0);
		}
		long end=System.currentTimeMillis();    
		IJ.log("Processing time point generation in sec: "+(end-start)/1000);
	}
	public void generateFromMask(ImagePlus imp){
		
		this.framewidth=imp.getWidth();
		this.frameheight=imp.getHeight();
		ImageProcessor ipTest=imp.getProcessor();
		Random generator = new Random ();
		
		for (int i=0;i<numParticles;i++){
			do {
				XPos[i]=generator.nextInt(framewidth);
				YPos[i]=generator.nextInt(frameheight);
				timeTrace[0][i]=0;
				
			} while (ipTest.getPixel(XPos[i], YPos[i])==0);
		}
	}
/*=======================================================================================
 * 		calculate TimeTrace
 *=========================================================================================*/
	public void calcTimeTrace() 
	{
		
		
		Random generator = new Random ();	
        
        for (int i=1; i<numFrames;i++){
			IJ.showProgress(i, numFrames);
			for (int j=0;j<numParticles;j++){
				double changeState=generator.nextDouble();
				if (timeTrace[i-1][j]==1){
					if (changeState<switchOff) timeTrace[i][j]=0;
					else timeTrace[i][j]=1;		
				}
				if (timeTrace[i-1][j]==0){
					if (changeState<switchOn) timeTrace[i][j]=1;
					else timeTrace[i][j]=0;		
				}
			}
			
        }	
	}
	public void calcTimeTrace(int oversample) 
	{
		Random generator = new Random ();	
        double expon=Math.log10(switchOff)/oversample;
		double over_off=Math.pow(10, expon);
		expon=Math.log10(switchOn)/oversample;
		double over_on=Math.pow(10, expon);
		
        for (int i=1; i<numFrames;i++){
			IJ.showProgress(i, numFrames);
			for (int j=0;j<numParticles;j++){
				int state_old=timeTrace[i-1][j];
				int state_new=0;
				
				for (int k=0;k<oversample;k++){
					
					double changeState=generator.nextDouble();
					if (state_old==1){
						if (changeState<over_off) state_new=0;
						else state_new=1;		
					}
					if (state_old==0){
						if (changeState<over_on) state_new=1;
						else state_new=0;		
					}
					state_old=state_new;
				}
			}
			
        }	
	}
	
	
	public void heavyTimeTrace(){
		
		this.isCondensed=true;
		Random generator = new Random ();	
		
		store.addElement(numFrames);
		store.addElement(framewidth);
		store.addElement(frameheight);
		
		int pos=0;
		long start=System.currentTimeMillis();		
        for (int i=0; i<numFrames;i++){
			IJ.showProgress(i, numFrames);
			store.addElement(i);
			int count=store.size();
			store.addElement(0);
//			int numPos=store.size();
//			store.addElement(0);
			int numCount=0;

			for (int j=0;j<numParticles;j++){
			
				double changeState=generator.nextDouble();
				double bleach=generator.nextDouble();
				
				if (timeTrace[0][j]==1){
					if (bleach<bleachRate) timeTrace[1][j]=-1;
					else {
				
						if (changeState<switchOff) timeTrace[1][j]=0;
						else timeTrace[1][j]=1;		
					}
				}
				if (timeTrace[0][j]==0){
					if (changeState<switchOn) timeTrace[1][j]=1;
					else timeTrace[1][j]=0;		
				}
				
				pos=YPos[j]*framewidth+XPos[j];
				if (timeTrace[1][j]>0){
					numCount++;

					store.setElementAt((Integer) store.get(count)+1, count);
//					store.setElementAt(numCount, numPos);
					store.addElement(pos);
					
				}	   	
				timeTrace[0][j]=timeTrace[1][j];
				   
			}
		}
        long end=System.currentTimeMillis();    
		IJ.log("Processing time time-lapse in sec: "+(end-start)/1000);
        
		
	}
public void heavyTimeTrace(final int oversample){
		
		this.isCondensed=true;
		/*double expon=Math.log10(switchOff)/oversample;
		double over_off=Math.pow(10, expon);
		expon=Math.log10(switchOn)/oversample;
		double over_on=Math.pow(10, expon);
		expon=Math.log10(bleachRate)/oversample;
		double over_bleach=Math.pow(10, expon);
*/		
		 
		 
		final double over_off=switchOff/oversample;
		final double over_on=switchOn/oversample;
		final double over_bleach=bleachRate/oversample;
		
		store.addElement(numFrames);
		store.addElement(framewidth);
		store.addElement(frameheight);
		
		
		long start=System.currentTimeMillis();		
		
       	final AtomicInteger numCount=new AtomicInteger(0);
		
		for (int i=1; i<numFrames;i++){
			IJ.showProgress(i, numFrames);
			store.addElement(i);
			final int count=store.size();
			store.addElement(0);
			final int numPos=store.size();
			store.addElement(0);
//			final Thread[] threads = newThreadArray();  

		    
//			for (int ithread = 0; ithread < threads.length; ithread++) {  

		        // Concurrently run in as many threads as CPUs  

//		        threads[ithread] = new Thread() {  
		                      
//		            { setPriority(Thread.NORM_PRIORITY); }  

//		            public void run() {

		            	Random generator = new Random ();
			           	for (int j=0;j<numParticles;j++){
			           		
			           		for (int k=0;k<oversample;k++){
			
			           			double changeState=generator.nextDouble();
								double bleach=generator.nextDouble();
								if (timeTrace[0][j]==1){
									if (bleach<over_bleach) timeTrace[1][j]=-1;
									else {
												
										if (changeState<over_off) timeTrace[1][j]=0;
										else timeTrace[1][j]=1;		
									}
								}
								if (timeTrace[0][j]==0){
									if (changeState<over_on) timeTrace[1][j]=1;
									else timeTrace[1][j]=0;		
								}
								timeTrace[0][j]=timeTrace[1][j];
							}
							int pos=YPos[j]*framewidth+XPos[j];
							
							if (timeTrace[1][j]>0){
								numCount.incrementAndGet();
							
								store.setElementAt((Integer) store.get(count)+1, count);
								store.setElementAt(numCount.get(), numPos);
								store.addElement(pos);
												
							}	   	
							timeTrace[0][j]=timeTrace[1][j];
											   
						}
			
//					}
//		        };  
//	    }
	

//	    startAndJoin(threads);
		}
		long end=System.currentTimeMillis();    
		IJ.log("duration: "+(end-start));
	}
	public void BlinkStatistics(ImagePlus imp,int size,double scale){
		
		IJ.run("Set Measurements...", "area mean standard centroid area_fraction redirect=None decimal=3");
		Roi [] roi= new Roi[numParticles];
		for (int i=0; i<numParticles;i++){
			roi[i]=new Roi((XPos[i]/scale)-size/2,(YPos[i]/scale)-size/2,size,size);
			
			
			
		}
		FloatProcessor res=new FloatProcessor(numParticles,numFrames);
		for (int s=1;s<=numFrames;s++){
			imp.setSlice(s);
			
			ImageProcessor ip=imp.getProcessor();
			
			for (int i=0; i<numParticles;i++){
				roi[i].setPosition(i);
				imp.setRoi(roi[i]);
				ip.setRoi(roi[i]);
				res.putPixelValue(i, s, ip.getStatistics().mean);
				
			}
		}
		ImagePlus calc=new ImagePlus("BlinkStatistic",res);
		IJ.setThreshold(calc, 50, 1000);
		IJ.run(calc, "Measure", "");
//		imp.show();
//		calc.show();
		
		
		
	}
	public ImagePlus reconstructTimeTrace(ImagePlus imp){
		ImageProcessor ip=imp.getProcessor();
		int stackSize=(int)ip.getf(0);
		int width=(int)ip.getf(1);
		int height=(int)ip.getf(2);
		int start=3;
		ImageStack stack=new ImageStack(width, height);
		for (int i=0;i<stackSize;i++){
			ImageProcessor stack_ip=new ShortProcessor(width, height);
			int numPart=(int)ip.getf(start+1);
			for (int j=0;j<numPart;j++){
				int pos=(int)ip.getf(start+2+j);
				stack_ip.set(pos, 1);
			}
			stack.addSlice(stack_ip);
			start+=numPart+2;
		}
		
		return new ImagePlus ("TimeTrace",stack);
	}
	public ResultsTable groundTruthTable(FloatProcessor ip){
		ResultsTable rt = null;
		int stackSize=(int)ip.getf(0);
		int width=(int)ip.getf(1);
		int height=(int)ip.getf(2);
		int start=3;
		ShortProcessor n_ip=new ShortProcessor(width,height);
		for (int i=0;i<stackSize;i++){
			rt.addValue("Position x", i);
			int numPart=(int)ip.getf(start+1);
			for (int j=0;j<numPart;j++){
				int pos=(int)ip.getf(start+2+j);
				n_ip.set(pos,n_ip.get(pos)+ 1);
			}
			
			start+=numPart+2;
		}
		
		return rt;
	}
	public ImagePlus projectTimeTrace(FloatProcessor ip){
		
		int stackSize=(int)ip.getf(0);
		int width=(int)ip.getf(1);
		int height=(int)ip.getf(2);
		int start=3;
		ShortProcessor n_ip=new ShortProcessor(width,height);
		for (int i=0;i<stackSize;i++){
			
			int numPart=(int)ip.getf(start+1);
			for (int j=0;j<numPart;j++){
				int pos=(int)ip.getf(start+2+j);
				n_ip.set(pos,n_ip.get(pos)+ 1);
			}
			
			start+=numPart+2;
		}
		
		return new ImagePlus("Projected Time Trace",n_ip);
	}
	
	private ImagePlus projectTimeTrace(){
		
		int stackSize=store.get(0);
		int width=store.get(1);
		int height=store.get(2);
		int start=3;
		ShortProcessor n_ip=new ShortProcessor(width,height);
		for (int i=0;i<stackSize;i++){
			
			int numPart=store.get(start+1);
			for (int j=0;j<numPart;j++){
				int pos=store.get(start+2+j);
				n_ip.set(pos,n_ip.get(pos)+ 1);
			}
			
			start+=numPart+2;
		}
		
		return new ImagePlus("Projected Time Trace",n_ip);
	}
	
	public FloatProcessor condensedTimeTrace(){
		int size=store.size();
		int width=(int)(Math.sqrt(size))+1;
		FloatProcessor ip=new FloatProcessor(width,width);
		for (int i=0;i<size;i++){
			ip.setf(i, store.elementAt(i));
		}
		return ip;
	}
	
	
	 ImagePlus CreateStack(){
	    	
 		
	    	ImageStack ims = new ImageStack(framewidth, frameheight);
	    	
	          		
	    	
			int dimension = framewidth*frameheight;
			
			int pos=0;
	       		 
			for (int s=0; s<numFrames;s++){
				IJ.showProgress(s, numFrames);
//				IJ.log("===================="+s+". Frame ====================================");
				byte[] sum = new byte [dimension];
				if ((s%1)==0){
				   for (int j=0; j<(numParticles);j++) {
					   pos=YPos[j]*framewidth+XPos[j];
//					   IJ.log(""+timeTrace[s][j]);
					   if (pos>=0 && pos< dimension) {
						   	sum[pos]+=bright*timeTrace[s][j];
						   	
					   }
//					   else IJ.log("Slice"+s+"   Particle"+j+"  not included");
					   
				   }
				   ImageProcessor ip = new ByteProcessor(framewidth, frameheight,sum);
				   
				   ims.addSlice("Time="+s+" s",ip);
				}
			}
			
			nimp = new ImagePlus(checkTitle(stacktitle),ims);
			nimp.setCalibration(cal);
			
			return nimp;			

		}
	   ImagePlus getImagePlus(){
		   if (nimp!=null) return nimp;
		   else return CreateStack();
	   }
	   private String checkPath(String path){
		   if (path.equals(path)|| path==null){
			   path=gridPath;
		   }
		   return path;
	   }
	   private String checkTitle(String input){
		   String output=input;
		   int list []=WindowManager.getIDList();
		   int count =1;
		   if (list != null){
			   	   
			   for (int i=0;i<list.length;i++){
				   if (WindowManager.getImage(list[i]).getTitle().equals(input)){
					   output=checkTitle(input+"-"+count);
					   count++;
				   }
			   }
		   }
		   return output;
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