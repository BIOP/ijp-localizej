/*
 * To the extent possible under law, the Fiji developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;

/**
 * Demo Plugin showing an example of GenericDialog and Parallel Processing 
 *
 * @author Romain Guiet (from minimal plugin fiji crew! AND A LOT of help from Olivier Burri)
 * @author Olivier Burri
 */

public class IJ_Demo_Plugin implements PlugIn {  

	public void run(String arg) {

		final ImagePlus imp 	= IJ.getImage();									//	get the active image
		ImagePlus i2 = imp.duplicate();
		
		double noise = 50;
		GenericDialog gd = new GenericDialog("Parameters");								//	Create a generic dialog
		gd.addNumericField("Noise Level", 20, 1);
		gd.showDialog();																//	(the pixel value should correspond to the position in the comma separated list above.)
		if (gd.wasCanceled())  return ; 												//	to handle cancellation

		noise = gd.getNextNumber();

		final double noise_level = noise;														//	make the final variables from the temporary ones

		// Split the thread in columns on the image
		final int columns = imp.getWidth();
		final int rows = imp.getHeight();

		final	AtomicInteger 	ai 		= new AtomicInteger(0);								// for parallel processing, we need the ai
		final	Thread[] 		threads = newThreadArray();  								// create the thread array
		final Random rand = new Random();

		for (int ithread = 0; ithread < threads.length; ithread++) {  						// 

			// Concurrently run in as many threads as CPUs  

			threads[ithread] = new Thread() {  

				{ setPriority(Thread.NORM_PRIORITY); }  

				public void run() {  

					// Each thread processes a few items in the total list  
					// Each loop iteration within the run method  
					// has a unique 'i' number to work with  
					// and to use as index in the results array:  


					for (int i = ai.getAndIncrement(); i < columns; i = ai.getAndIncrement()) {
						for (int j = 0; j < rows; j++) {
							imp.getProcessor().set(i, j, (int) (imp.getProcessor().getPixel(i, j)+noise_level));
						}
					}
				}};  
		} 

		long start	= System.currentTimeMillis();						// to measure the time required

		startAndJoin(threads);				// DO THE MAGIC ! 

		long end=System.currentTimeMillis(); 
		IJ.log("Parallel Execution time in ms: "+(end-start) );
		imp.updateAndDraw();
		
		
		
		// Now do it without parallel processing
		long start2=System.currentTimeMillis(); 

		for (int i = 0; i < columns; i++) {
			for (int j = 0; j < rows; j++) {
				i2.getProcessor().set(i, j, (int) (i2.getProcessor().getPixel(i, j)+noise_level));
			}
		}
		long end2=System.currentTimeMillis(); 
		IJ.log("Normal Execution time in ms: "+(end2-start2) );
		i2.show();
		
		
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


	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads an
	 * image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = IJ_Demo_Plugin.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();
		// open the blobs sample
		ImagePlus image = IJ.openImage("http://imagej.net/images/blobs.gif");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");

	}
}
