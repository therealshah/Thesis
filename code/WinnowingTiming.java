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


public class WinnowingTiming{

	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 
	
	private static String directory = "../thesis-datasets/gcc/";
	//private static String directory = "../thesis-datasets/emacs/";

	private static int window = 12;// window is size 12
	private static int numOfPieces=0;

	private static ArrayList<Long> md5Hashes = new ArrayList<Long>(); // used to hold the md5Hashes
	private static int totalSize;
	private static int startBoundary = 100;
	private static int endBoundary = 1000;
	private static int increment = 50;
	private static int arraySize = (endBoundary/increment) - 1; // number of elements
	private static long []  timeArray = new long[arraySize]; // default values are 0
	private static double [] blockArray  = new double[arraySize]; // default is 0
	private static int index = 0; // used to store the values in the time array 
	private static int runs = 100; // number of time to run the code



	// made a different method so i can call the timing for all the CDC algos with a single java helper class method

	public static void main(String [] args) throws Exception{
 		
 		preliminaryStep();
		System.out.println("========== Running WinnowingTiming " + " " + runs + " " + fileList.get(0));

 		
		for (int i = 0; i < runs; ++i){
		//	System.out.println("======================== Run " + i + " " + fileList.get(0));
			index = 0;
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
		totalSize = array.length;
		HashDocument.hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
	}


	/*
		- This is basically sets up everything and calls the actual contentDependant methods
	*/
	private static void startCDC() throws Exception{

		for (int i = startBoundary;i<=endBoundary;i+=increment)
		{
			int localBoundary = i;
			//System.out.print( localBoundary+" ");
			// run the winnowing algorithm
			readBytes(localBoundary);
			numOfPieces = 0;
			index++;
					
		}
	}


	/*
		- This method reads the file as a byte stream
		- Then it calls the content dependant paritioning method to get the chunk points
		- Also get the time for the methods
	*/
	private static void readBytes(int localBoundary) throws Exception{
		// this is where we start the timing 
		long startTime = System.nanoTime();
		determineCutPoints(md5Hashes,localBoundary);
		// This is where we end the timing	
		long endTime = System.nanoTime();
		long duration = (endTime - startTime); // this how long this method took
		double blockSize = (double)totalSize/(double)numOfPieces;

		// store the results in this array
		timeArray[index]+= duration;
		blockArray[index] = blockSize;
	} // end of the function


	private static int findMin(int start,int end,ArrayList<Long> md5Hashes){
		int min = start++; // set the min to the first element of the array and increment start
		while (start <= end){
			// if the new boundary is not greater than the current min (aka its the new min) set it to the new min
			if (!(md5Hashes.get(start).compareTo(md5Hashes.get(min)) > 0))
				min = start;
			start++;
		}
		return min;

	}


	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. localboundary - how big the neighborhood is

		-- We are simply finding the boundaries of the file using winnowing and simply storing them. Nothing more!
	-------------------------------------------------------------------------------------------------------- */
	private static void determineCutPoints(ArrayList<Long> md5Hashes, int localBoundary){
		int start = 0; // starting point
		int current = localBoundary - 1;// compare all the values at and before this one
		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match

		ArrayList<Long> cutpoints = new ArrayList<Long>(); // used to store the cutpoints for the arrayList
		int prevBoundary = -1; // used to keep track of the previous boundary
		// loop through until this current equals the end
		while (current<md5Hashes.size())
		{ 
			// if the prevBoundary is null or if it slided out, we will find the minimum within the range [start,current] and 
			// set that as the boundary
			if (prevBoundary == -1 || prevBoundary < start ){
				prevBoundary = findMin(start,current,md5Hashes); // get the min within this window
				match = true;
			}
			// else we have a valid prev boundary and compare that value with the new one we slided in (aka current)
			// if the new one is less than OR equal (AKA its not greater than the prevBoundary) to the prevBoundary, this new one will become the previous boundary
			else if (!(md5Hashes.get(current).compareTo(md5Hashes.get(prevBoundary)) > 0)){
				prevBoundary = current;
				match = true;
			}
			// we have found a boundary, so just hash it 
			if (match){
				cutpoints.add(md5Hashes.get(prevBoundary)); // simply add the boundary
				match = false;
				numOfPieces++;
				//break; // break out of the for loop
				// if the prev boundary is null or 
			}
			start++;
			current++;						
		} // end of the while loop
			
	} // end of the method
}












