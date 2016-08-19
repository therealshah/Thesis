import java.util.*;
import java.io.*;
import java.math.*;
import java.security.MessageDigest;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.zip.*;

/*
	Author: Shahzaib Javed
	Purpose: Research for NYU Tandon University



	This code is used to simulate the timing anaylsis for the localMinima method. The timing is determined:
		--  by first hashing the whole document
		--	Starting the timer
		-- calling the method to determine the cutpoints
		-- Stopping the timer once we have our smaller array of cutpoints.

*/

public class KarbRabinTiming{

	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 
	private static ArrayList<Long> md5Hashes = new ArrayList<Long>(); // used to hold the md5Hashes

	private static String directory = "../../thesis-datasets/morph_file_20Mb/";
	//private static String directory = "../thesis-datasets/emacs/";
	
	private static int window =12 ;// window size will be fixed around 12
	private static int numOfPieces=0;  // used to calculate block size
	private static int totalSize; // hold the document size

	private static int startBoundary = 100;
	private static int endBoundary = 1000;
	private static int increment = 50;
	private static int arraySize = (endBoundary/increment) - 1; // number of elements
	private static long timeArray [] = new long[arraySize]; // default values are 0
	private static double blockArray [] = new double[arraySize]; // default is 0
	private static int index = 0; // used to store the values in the time array 
	private static int runs = 100; // number of time to run the code

	// made a different method so i can call the timing for all the CDC algos with a single java helper class method
	public static void main(String [] args) throws Exception{

		System.out.println("========== Running KR + " + directory + " runs " + runs);
		//runArchiveSet();
		preliminaryStep(); // this is to set everythinh up for the methods
		// run the method for runs amount of time and avg the results
		for (int i = 0; i < runs; ++i){
			index = 0; // set the index to 0 so we can add the correct values in to correct blocksizes
			startCDC(); // driver for taking in inputs and running the 2min method
		}
		// now output the average
		index = 0;
		for (int i = startBoundary; i <= endBoundary; i+=increment){
			System.out.println(i + " " + blockArray[index] + " " + timeArray[index]/(long)runs);
			index++;
		}
	}


	/*
		- This hashes the document and sets the totalSize. We split this method to make the code more modular
		-
	*/
	private static void preliminaryStep() throws Exception{
		// prepoccessing step to hash the document, since we dont need to hash the document again
		ReadFile.readFile(directory,fileList);
		String fileName = fileList.get(0); // we will only use the first file
		Path p = Paths.get(directory+fileName);
		byte [] array = Files.readAllBytes(p); // read the file in bytes
		int start = 0; // start of the sliding window
		int end = start + window - 1; // ending boundary
		totalSize = array.length; // total size is fixed
		HashDocument.hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
	}

	/*
		-- This is the portion of the code that runs the timing anaylsis on the archive set
		-- Only determine to see how long it takes to get the cutpoints for the current version
	*/
	private static void runArchiveSet() throws Exception{

		directory = "../thesis-datasets/datasets/";
		File file = new File(directory);
		String[] directory_list = file.list(new FilenameFilter() {
		  @Override
		  public boolean accept(File current, String name) {
		    return new File(current, name).isDirectory(); // make sure its a directory
		  }
		});

		int totalRuns = 0;
		// loop through and run the cdc for each directory
		for (String dir : directory_list){
			ReadFile.readFile(directory+ dir,fileList); // read all the files in this directory
			//preliminaryStep(directory+ dir + "/"); // call the preliminaryStep on first file		
			totalRuns++;			
			startCDC();
			//clear the fileLisand hashed_file_list array
			//hashed_File_List.clear();
			fileList.clear();
			index = 0; // set index to 0
		} // end of directory list for loop

		index = 0;
		for (int i = startBoundary; i <= endBoundary; i+=increment){
			System.out.println(i + " " + blockArray[index]/totalRuns + " " + timeArray[index]/(long)runs);
			index++;
		}
	}


	/*
		- This is basically sets up everything and calls the actual contentDependant methods
	*/
	private static void startCDC() throws Exception{
		long remainder = 7; // this is the remainder that we will be comparing with
		for (int i = startBoundary;i<=endBoundary;i+=increment)
		{
			long divisor = i;
			readBytes(divisor,remainder); // run the karb rabin algorithm
			numOfPieces = 0; // reset this
			index++; // go to the next index
		}	
	}


	/*
		- This method reads the file as a byte stream
		- Then it calls the content dependant paritioning method to get the chunk points
		- Also get the time for the methods
	*/
	private static void readBytes(long divisor, long remainder) throws IOException,Exception{
		// this is where we start the timing 
		long startTime = System.nanoTime();
		determineCutPoints(md5Hashes,divisor,remainder);
		// This is where we end the timing	
		long endTime = System.nanoTime();
		long duration = (endTime - startTime); // this how long this method took
		double blockSize = (double)totalSize/(double)numOfPieces;

		// store the results in this array
		timeArray[index]+= duration;
		blockArray[index] = blockSize;
	} // end of the function


	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document

		-- We are simply finding the boundaries of the file using karbRabin and simply storing them. Nothing more!
	-------------------------------------------------------------------------------------------------------- */
	private static void determineCutPoints(ArrayList<Long> md5Hashes,long divisor,long remainder){

		ArrayList<Long> cutpoints = new ArrayList<Long>(); // used to hold all the cutpoints of the document
		for (int i = 0; i < md5Hashes.size();++i)
		{ 	
			/*-----------------------------------------------------------------
				- If the mod of this equals the modvalue we defined, then 
				- this is a boundary
			------------------------------------------------------------------*/ 
			if (md5Hashes.get(i)%divisor == remainder){ // ck if this equals the mod value
				cutpoints.add(md5Hashes.get(i));// store this index as the cutpoint for the boundary
				numOfPieces++; // to compute the avg blockSize
			}							
		} // end of the for loop
	} // end of the method
}












