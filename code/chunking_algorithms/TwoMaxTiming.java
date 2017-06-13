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


public class TwoMaxTiming{

	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 

	private static String directory = "../thesis/gcc/";

	
	private static int numOfPieces=0;
	private static int window = 12;// window is size 12
	private static int multiplier;
	private static int maxBoundary;

	private static ArrayList<Long> md5Hashes = new ArrayList<Long>(); // used to hold the md5Hashes
	private static int totalSize;


	private static int startBoundary = 100;
	private static int endBoundary = 1000;
	private static int increment = 50;
	private static int arraySize = (endBoundary/increment) - 1; // number of elements
	private static long timeArray [] = new long[arraySize]; // default values are 0
	private static double blockArray [] = new double[arraySize]; // default is 0
	private static int index = 0; // used to store the values in the time array 
	private static int runs = 100; // number of time to run the code



	// made a different method so i can call the timing for all the CDC algos with a single java helper class method
	public static void main(String [] args) throws Exception {
		
		preliminaryStep();
		System.out.println("========== Running Backup2minTiming " + runs + " " + fileList.get(0));
		int [] arr = {2,4,6}; // these are the multipliers we will use


		for (int m : arr){ // we want to run it for mulitple boundaries
			multiplier = m;
			System.out.println("======================== multiplier " +  multiplier);
			for (int i = 0; i < runs; ++i){
				index = 0; // set the index to 0 after each iteration
				driverRun(); // driver for taking in inputs and running the 2min method
			}
			index = 0;
			for (int i = startBoundary; i <= endBoundary; i+=increment){
				System.out.println(i + " " + blockArray[index] + " " + timeArray[index]/(long)runs);
				// clear the array for the next round
				blockArray[index] = 0;
				timeArray[index] = 0;
				index++;
			}
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


	private static void driverRun() throws IOException, Exception{
		
		for (int i = 100;i<=1000; i+=50)
		{
			maxBoundary = multiplier*i;
			int localBoundary = i;
		/*--------------------------------------------------------------------------------------------
					-- Run the 2 min algorithm for all the way upto the value the user enters
					-- We will use the local boundary for all the way up to the value the user entered
		-------------------------------------------------------------------------------------------------*/
			System.out.print( localBoundary+" ");
			// run the 2min algorithm
			readBytes(localBoundary);
			numOfPieces = 0;
			index++;
		}	
	}


	/*
		- This method reads the file using bytes
		- This is where we run the 2min content dependent partitioning
	*/
	private static void readBytes(int localBoundary) throws IOException,Exception{
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


	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. localboundary - how to big the neighborhood is for finding the minimum for hash value

		-- We are simply finding the boundaries of the file using TwoMax and simply storing them. Nothing more!
	-------------------------------------------------------------------------------------------------------- */
	private static void determineCutPoints(ArrayList<Long> md5Hashes, int localBoundary){
		int start = 0; // starting point
		int current = localBoundary;// has to be atlead here to be the local minima
		int end  = localBoundary *2;  // this is the end of the boundary
		int documentStart = 0; // used to keep track of where the boundaries start from
		boolean match = false; // used to ck if we encountered a match and is used to determine whether to increment the hash window
		int maxPoint = -1; // used as the secondary point
		boolean notMax = false;
		boolean notMin = false; // keep track if this is a max/min

		ArrayList<Long> cutpoints = new ArrayList<Long>(); // used to hold the cutpoints
		/*--------------------------------------------------
			-- Now we run the window over and compute the value
			-- in each window and store in hash table
		----------------------------------------------------*/
		while (end<md5Hashes.size()) // loop through till we hit the end of the array
		{ 
		
			for (int i = start; i <= end; ++i) // loop through each of the values in this boundary
			{							
				if (i == current) // we are looking for strictly less than, so we don't want to compare with ourselve
					++i; // we don't wanna compare withourselves		

				// not a min
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) 
					notMin = true; // this is not a min, so just set the variable to min
				// not max
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) > 0)) 
					notMax = true; // this is not a max, so just set the variable to min
				// if it's either a max/min then break
				if (notMax && notMin)
					break; // only break if it's not a max and min
				/*-----------------------------------------------------------------------------
					We have reached the end. Meaning all the values within the range 
					(documentStart,Current) is a boundary
					Only make this boundary if this is a min
				--------------------------------------------------------------------------------*/
				if (i == end && !notMax)
					maxPoint = current; // store this as the maxPoint
				else if (i == end && !notMin)
				{
					cutpoints.add(md5Hashes.get(current));
					numOfPieces++;
					
					documentStart = current + 1;// set this as the beginning of the new boundary
					current = end+ 1; // this is where we start finding the new local minima
					start = documentStart; // we will start comparing from here!, since everything before this is a boundary
					end = current + localBoundary; // this is the new end of the hash boundary
					match = true; // so we don't increment our window values
					maxPoint = -1; // reset the maxPoint\
					break;
				}
			}

			// if we have reached our maximum threshold
			// we will see if we have a secondary boundary (AKA has a max boundary), if yes, then make that the boundary
			// otherwise now we will make either the first minima or the second minima the boundary
			if ((current-documentStart + 1) >= maxBoundary && !notMax){
				// check if we have a secondary boundary
				if (maxPoint != -1){
					cutpoints.add(md5Hashes.get(maxPoint));
					numOfPieces++;
					documentStart = maxPoint + 1;// set this as the beginning of the new boundary
					start = maxPoint + 1;
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary; // this is the new end of the hash boundary
					match = true; // so we don't increment our window values	
					maxPoint = -1; // reset the max point	
				}
				
			}				
			
			// go to the next window only if we didnt find a match
			// because if we did find a boundary, we would automatically go to the next window
			if (!match)
			{
				start++;
				current++;
				end++;
			}
			match = false; // reset this match
			notMax = false;
			notMin = false;
								
		} // end of the while loop
	} // end of the method
}












