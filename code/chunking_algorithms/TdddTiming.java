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
public class TdddTiming{

	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 
	//private static String directory = "../../thesis-datasets/gcc/";
	//private static String directory = "../thesis-datasets/emacs/";
	private static String directory = "../../thesis-datasets/morph_file_100Mb/";
	private static int window = 12;// window size will be fixed around 12
	private static int numOfPieces=0;  // used to calculate block size

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
	public static void main(String [] args) throws Exception{
		
		preliminaryStep();
		System.out.println("========== Running TDDD " + " " + runs + " " + fileList.get(0));

		for (int i = 0; i < runs; ++i){
		//	System.out.println("======================== Run " + i + " " + fileList.get(0));
			index = 0;
			startCDC(); // driver for taking in inputs and running the 2min method
		}
		//now output the average
		index = 0;
		for (int i = startBoundary; i <= endBoundary; i+=increment){
			System.out.println(i + " " + (i/2+1) + " " + (i/4+1)+ " " + blockArray[index] + " " + timeArray[index]/(long)runs);
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




	private static void startCDC() throws IOException, Exception{
		long remainder = 7;

		for (int i = startBoundary;i<=endBoundary; i+= increment )
		{
			//System.out.print("Enter localBoundry:");
			long minBoundary  = i;// we will set the mod value as the minimum boundary
			long maxBoundary = 4*i; // we will set this as the maximum boundary
			long divisor1 = i;// this will be used to mod the results
			long divisor2 = i/2+1; // the backup divisor is half the original divisor
			long divisor3 = i/4+1;
			//System.out.println( divisor1+" " + divisor2 + " " + " " + divisor3 + " ");
			readBytes(window,divisor1,divisor2,divisor3,remainder,minBoundary,maxBoundary);
			numOfPieces = 0; // reset the num of pieces
			index++;
		}
	}

	/*
		- This method reads the file and basically sets up everything for TTTD
		- @params:
			window - rolling window size 
			divisor1 - the first divisor value we will be using to find the remainder
			divisor2/3 - the second/third divisor value we will be using to find the remainder
			minBoundary/maxBoundary - min/ max boundaries for the chunks
	
	*/
	private static void readBytes(int window, long divisor1, long divisor2,long divisor3, long remainder,long minBoundary,long maxBoundary) throws IOException,Exception{

		// Start the timing here	
		long startTime = System.nanoTime();	
		determineCutPoints(md5Hashes,divisor1,divisor2,divisor3,remainder,minBoundary,maxBoundary);	
		// End the timing here			
		long endTime = System.nanoTime();
		long duration = (endTime - startTime); // this how long this method took
		double blockSize = (double)totalSize/(double)numOfPieces;
		//System.out.println(blockSize + " " + duration); // printing the avgBlockSize along with the timing	
		// store the results in this array
		timeArray[index]+= duration;
		blockArray[index] = blockSize;				
	} // end of the function



	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. Divisor1/Divisor2/divisor3... - main and back up divisors
			5. The remainder we are looking for
			6/7. min/max boundaries

		-- We are simply choping up the first file
	-------------------------------------------------------------------------------------------------------- */
	private static void determineCutPoints(ArrayList<Long> md5Hashes, long divisor1, long divisor2,long divisor3,long remainder
		,long minBoundary,long maxBoundary){

		boolean match = false; // used to ck if we encountered a match
		int documentStart = 0; // used to keep track of where the boundaries are
		int backUpBreakPoint = -1; // used to store the backup breakpoint
		int secondBackUpBreakPoint = -1; // this is the second backup point with the divisor3
		ArrayList<Long> cutpoints = new ArrayList<Long>(); // used to hold just the cutpoints
		int i = documentStart + (int)minBoundary - 1; // so we start at the minimum
		// loop through all the values in the document
		for (; i < md5Hashes.size();++i)
		{ 	
			/*-----------------------------------------------------------------
				- If the mod of this equals the modvalue we defined, then 
				- this is a boundary
			------------------------------------------------------------------*/ 

			if (md5Hashes.get(i)%divisor1 == remainder) // ck if this equals the mod value
			{
				cutpoints.add(md5Hashes.get(i)); // add this as the cutpoint for the document
				numOfPieces++;
				documentStart = i + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				secondBackUpBreakPoint = -1; // second backup point reset it!
				i = i + (int)minBoundary-1; // skip all the way here
			}		
			else if (md5Hashes.get(i)%divisor2 == remainder){ //  check if this is the backup point
				backUpBreakPoint = i; // this is the backup breakpoint
			}
			else if (md5Hashes.get(i)%divisor3 == remainder){
				secondBackUpBreakPoint = i; // we found a second backup point with divisor3
			}
			if ((i - documentStart + 1) >= maxBoundary ) { // we have reached the maximum
				// ck if we have a backUpbreakpoint
				int point;
				if (backUpBreakPoint != -1)// if we do, set this as the boundary
			    	point = backUpBreakPoint;
			    else if (secondBackUpBreakPoint != -1)
			    	point = secondBackUpBreakPoint; // if we don't have a first backup, ck if we have a second
			    else
			    	point = i; // else this current value of i is the breakpoint

				cutpoints.add(md5Hashes.get(point)); // add this to the boundary
				numOfPieces++;
				documentStart = point + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				secondBackUpBreakPoint = -1; // reset second backup break point
				i = point + (int)minBoundary-1; // skip all the way here
			}
								
		} // end of the for loop

	} // end of the method
}












