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

public class LocalMinimaTiming{


	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 
	private static ArrayList<Long> md5Hashes = new ArrayList<Long>(); // used to hold the md5Hashes

	private static String directory = "../../thesis-datasets/morph_file_100Mb/";
	//private static String directory = "../../thesis-datasets/gcc/";

	private static int window = 12;// window is size 12
	private static int numOfPieces = 0;
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

	public static void main(String [] dir) throws Exception{
 		preliminaryStep();
		System.out.println("========== Running LocalMinimaTiming " + " " + runs + " " + fileList.get(0));

 		
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
		System.out.println(totalSize);
		HashDocument.hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
	}



	private static void startCDC() throws IOException, Exception{

		for (int i = startBoundary;i<=endBoundary;i+= increment)
		{			
			int localBoundary = i;
		/*--------------------------------------------------------------------------------------------
					-- Run the 2 min algorithm for all the way upto the value the user enters
					-- We will use the local boundary for all the way up to the value the user entered
		-------------------------------------------------------------------------------------------------*/
			//System.out.print( localBoundary+" ");
			readBytes(localBoundary);
			numOfPieces = 0; // reset this
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
		//determineCutPoints(md5Hashes,localBoundary);
		determineCutPoints_way2(md5Hashes,localBoundary);
		// This is where we end the timing	
		long endTime = System.nanoTime();
		long duration = (endTime - startTime); // this how long this method took
		double blockSize = (double)totalSize/(double)numOfPieces;

		// store the results in this array
		timeArray[index]+= duration;
		blockArray[index] = blockSize;// same for all times
	} // end of the function


	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. localboundary - used to keep track of how the 2min chooses it boundaries

		-- We are simply finding the boundaries of the file using 2min and simply storing them. Nothing more!
	-------------------------------------------------------------------------------------------------------- */
	private static void determineCutPoints(ArrayList<Long> md5Hashes, int localBoundary){
		int start = 0; // starting point
		int current = localBoundary;// has to be atlead here to be the local minima
		int end  = localBoundary *2;  // this is the end of the boundary
		int documentStart = 0; // used to keep track of where the boundaries start from
		boolean match = false; // used to ck if we encountered a match and is used to determine whether to increment the hash window
		ArrayList<Long> cutpoints = new ArrayList<Long>(); // this arraylist is used to hold the cutpoints
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
				// CompareTo returns
					// >0 if greater
					// <0 if less than
					// 0 if equal
				// 	// break if this isnt the smallest one
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) 
					break; // we will break if the value at the current index is not a local minima
				/*-----------------------------------------------------------------------------
					We have reached the end. Meaning all the values within the range 
					(documentStart,Current) is a boundary
				--------------------------------------------------------------------------------*/
				if (i == end)
				{
					cutpoints.add(md5Hashes.get(current)); // simply add the boundary point to the array
					start = current + 1;// set this as the beginning of the new boundary
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary; // this is the new end of the hash boundary
					match = true; // so we don't increment our window values
					numOfPieces++; // we have added a cutpoint
					break;
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
								
		} // end of the while loop
	} // end of the method



	/*
		- Finds the hash value with the lowest value within the specified range
	*/
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
		 This method is same as the one above, but is modified in the way it determines cutpoints


		 [start - current-1 ] - left interval
		 [current + 1 - end] - right interval
	-------------------------------------------------------------------------------------------------------- */
	private static void determineCutPoints_way2(ArrayList<Long> md5Hashes, int localBoundary){
		int start = 0; // starting point
		int current = localBoundary;// has to be atlead here to be the local minima
		int end  = localBoundary *2;  // this is the end of the boundary
		int documentStart = 0; // used to keep track of where the boundaries start from
		boolean match = false; // used to ck if we encountered a match and is used to determine whether to increment the hash window
		ArrayList<Long> cutpoints = new ArrayList<Long>(); // this arraylist is used to hold the cutpoints

		int l_min = findMin(start,current-1,md5Hashes); //find min from left side
		int r_min = findMin(current + 1,end,md5Hashes); // find min from right side
		long l_val = md5Hashes.get(l_min);
		long r_val = md5Hashes.get(r_min);
		/*--------------------------------------------------
			-- Now we run the window over and compute the value
			-- in each window and store in hash table
		----------------------------------------------------*/
		while (end<md5Hashes.size()) // loop through till we hit the end of the array
		{ 


			// ck of l_min and r_min are valid ( as in are within the boundary range)
			if (!(l_min >= start && l_min < current)){
				l_min = findMin(start,current-1,md5Hashes); // find new min
				l_val = md5Hashes.get(l_min);
			}
			// now check the new value that was just slides in ( we incremented current so we compare the value that was just slided in, as in current -1)
			if (!(md5Hashes.get(l_min).compareTo(md5Hashes.get(current-1)) < 0)){
				l_min = current-1; // this is the new l_min
				l_val = md5Hashes.get(l_min);
			}

			if (!(r_min > current)){
				r_min = findMin(current+1,end,md5Hashes);
				r_val = md5Hashes.get(r_min);
			}		
					
			// compare r_min to the new value that was just slided in , as in the end value
			if (!(md5Hashes.get(r_min).compareTo(md5Hashes.get(end)) < 0)){
				r_min = end; // this is the new l_min
				r_val = md5Hashes.get(r_min);
			}
		


			/*-----------------------------------------------------------------------------
				 if current is the minimum, we have a boundary
			--------------------------------------------------------------------------------*/
			if (md5Hashes.get(current).compareTo(Math.min(r_val,l_val)) < 0)
			{
				cutpoints.add(md5Hashes.get(current)); // simply add the boundary point to the array
				start = current + 1;// set this as the beginning of the new boundary
				current = start + localBoundary; // this is where we start finding the new local minima
				end = current + localBoundary; // this is the new end of the hash boundary
				match = true; // so we don't increment our window values
				numOfPieces++; // we have added a cutpoint
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
		} // end of the while loop
	} // end of the method
}












