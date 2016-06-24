
import cern.jet.math.Bessel;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.FloatProcessor;



public class BesselWindow{ 


	int pos=0;
	double a=0;
	int pixelsize=5;
	int lambda=488;
	double na=0.8;
	int width=256;
	int height=256;
	
	String title;
	Calibration cal =new Calibration();
	
	BesselWindow(){
	}
	
	BesselWindow(int width, int height, int pixelsize, int lambda, double na){
		this.width=width;
		this.height=height;
		this.pixelsize=pixelsize;
		this.lambda=lambda;
		this.na=na;
	}
	public ImagePlus calc(){
		
		title="lambda="+String.valueOf(lambda)+"  NA="+String.valueOf(na);
		
		double [] pixels=new double[width*height];
		int xc=width/2;
		int yc=height/2;	
		
		cal.pixelHeight=pixelsize/1000.0;
	    cal.pixelWidth=pixelsize/1000.0;
	    cal.setUnit("um");
	    double dist=0;
	    
		for (int i=0;i<width;i++){
			for (int j=0;j<height;j++){
				a=2*Math.PI*na/lambda;
				dist=Math.sqrt(Math.pow(0.5+j-xc,2)+Math.pow(i-yc+0.5, 2));
				pixels[pos]=Math.pow(2*Bessel.j1(a*dist*pixelsize)/(dist*pixelsize),2);
								
				pos+=1;
			}
			
		}
		FloatProcessor ip= new FloatProcessor(width,height,pixels);
		double max=ip.getMax();
		ip.multiply(1/max);
		ImagePlus imp=new ImagePlus();
		imp.setProcessor(ip);
		imp.setCalibration(cal);
		imp.setTitle(title);
		return imp;

	}

}
