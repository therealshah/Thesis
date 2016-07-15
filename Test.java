import java.util.*;
import java.io.*;
import java.math.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.zip.*;
import java.lang.*;

/*
	Author: Shahzaib Javed
	Purpose: Research for NYU Tandon University



	Abstract: LocalMinima is a content dependant chunking method. It determines the boundaries for the document using the local minima. 
	All content dependant algorithms first hash the document using a sliding window of length w, which we will call the hash array. (12 for all these experiments). This step is true for all content dependant chunking algorithms.
	Next the cut points for the document are determined from the hash array, using a content dependant method, (Local Minima in this case).
	The original document is divided into chunks using the cut points as boundaries between the chunks. Different versions of the documents are
	using where the first chunks of the document are stored, whereas the second version is simply used to see of that portion of the document
	occurred.


	LocalMinima: This algorithm has a parameter, which we will call B or boundarySize associated with it. The algorithm declares a hash a cutpoint
	only if the hash is strictly less than the B hashes before it and B hashes after it. Continue if the current hash fails the conditions.


*/

public class Test{

	private static HashMap<String,Integer> matches = new HashMap<String,Integer>();

	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 
	//private static String directory = "../thesis-datasets/gcc/";
	private static String directory = "../thesis-datasets/test/";

	//private static String directory = "../thesis/datasets/1389blog.com/";
	//private static String directory = "../thesis-datasets/datasets/id.mind.ne/";

	//private static String directory = "../thesis/periodic/";
	// get the ratio of the coverage over the total size
	private static double totalSize=0;
	private static double coverage=0;
	private static int numOfPieces=0;
	private static int window = 3;

	// variables for the boundary size
	private static int startBoundary = 3; // start running the algo using this as the starting param
	private static int endBoundary = 3; // go all the way upto here
	private static int increment = 10; // increment in these intervals

	private static int document_date_selection = 2; // 1 - last week, 2 - for last month, 3 - for last year

	private static ArrayList< byte [] > fileArray = new ArrayList<byte[]>(); // holds both the file arrays
	private static ArrayList<ArrayList<Long>> hashed_File_List = new ArrayList<ArrayList<Long>>(); // used to hold the hashed file
	private static String s = "Adorunrunadorunrun";
	private static ArrayList<Integer> md5Hashes = new ArrayList<Integer>();
	private static boolean test = false;
	private static int localBoundary;
	
	public static void main(String [] args) throws Exception
 	{

		System.out.println("Running LocalMinima " + directory);
		md5Hashes.add(5);
		md5Hashes.add(9);
		md5Hashes.add(5);
		md5Hashes.add(2);
		md5Hashes.add(10);
		md5Hashes.add(7);
		md5Hashes.add(9);
		md5Hashes.add(1);
		md5Hashes.add(6);
		md5Hashes.add(80);
		md5Hashes.add(30);
		md5Hashes.add(90);
		md5Hashes.add(5);
		md5Hashes.add(45);
		md5Hashes.add(60);
		// ReadFile.readFile(directory,fileList); // read the two files
		// System.out.println(fileList.get(0) + " " + fileList.get(1));
		// preliminaryStep(directory);
	 	startCDC();
		//runArchiveSet();
	}



	/*
		- This reads the file and hashses the document, which are then stored in our arrayLisrs
		- we do this before, so we dont have to hash again later ( which is time consuming)
	*/
	private static void preliminaryStep(String dir) throws Exception{
		int start = 0; // start of the sliding window
		int end = start + window - 1; // ending boundary
		// prepoccessing step to hash the document, since we dont need to hash the document again
		for (int i = 0; i < fileList.size(); ++i){
			//System.out.println("preliminaryStep " + fileList.get(i));
			Path p = Paths.get(dir+fileList.get(i)); // read this file
			byte [] array = Files.readAllBytes(p); // read the file in bytes
			//System.out.println(array.length);

			ArrayList<Long> md5Hashes = new ArrayList<Long>(); // make a new arrayList for this document
			HashDocument.hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
			
			// add the fileArray and hashedFile to our lists so we can use them later to run the algorithms
			// note we hash and read file before, so we don't have to do it again
			fileArray.add(array);
			hashed_File_List.add(md5Hashes);
		}
	}


	/*
		- This is basically sets up everything and calls the actual contentDependant methods
	*/
	private static void startCDC() throws Exception{
		for (int i = startBoundary;i<=endBoundary;i+=increment)
		{			
			localBoundary = i;
			//System.out.print( localBoundary+" ");
			readBytes();
			// this is the block size per boundary
			// double blockSize = (double)totalSize/(double)numOfPieces;
			// double ratio = (double)coverage/(double)totalSize;
			// System.out.println(blockSize + " " + ratio);
			// clear the hashTable, and counters so we can reset the values for the next round of boundaries
			matches.clear();
			coverage = 0;
			numOfPieces = 0;
		}	
	}


	/*
		Read in all the files and loop through all the files
		We already have the hashed version of the documents 
		First, we cut up the first document into chunks (using the CDC algorhtim) and store it
		Then we cut up the second document (usually a different version of the same document) and see how many chunks match
	*/
	private static void readBytes() throws Exception{
		// there are only 2 files
		storeChunks(); // cut up the first file and store it
		//run2min(); // call the method again, but on the second file only

		System.out.println("=========== Way 2");
		determineCutPoints_way2(); // cut up the first file and store it
		//run2(); // call the method again, but on the second file only
		System.out.println("===== END");
	} // end of the function




	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. localboundary - used to keep track of how the 2min chooses it boundaries

		-- We are simply finding the boundaries of the file using 2min and simply storing them. Nothing more!
	-------------------------------------------------------------------------------------------------------- */
	private static void storeChunks(){
		int start = 0; // starting point
		int current = localBoundary;// has to be atlead here to be the local minima
		int end  = localBoundary *2;  // this is the end of the boundary
		int documentStart = 0; // used to keep track of where the boundaries start from
		boolean match = false; // used to ck if we encountered a match and is used to determine whether to increment the hash window
		StringBuilder builder = new StringBuilder(); // this is used to store the original document content
		/*--------------------------------------------------
			-- Now we run the window over and compute the value
			-- in each window and store in hash table
		----------------------------------------------------*/
		while (end<md5Hashes.size()) // loop through till we hit the end of the array
		{ 
			//System.out.println("Checking " + end + " " + md5Hashes.size());
			for (int i = start; i <= end; ++i) // loop through each of the values in this boundary
			{							
				if (i == current){ // we are looking for strictly less than, so we don't want to compare with ourselve
					i++;
				}		
				// CompareTo returns
					// >0 if greater
					// <0 if less than
					// 0 if equal
				// 	// break if this isnt the smallest one
				//System.out.println(current + " " + i + " " + md5Hashes.size());
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) 
					break; // we will break if the value at the current index is not a local minima
				/*-----------------------------------------------------------------------------
					We have reached the end. Meaning all the values within the range 
					(documentStart,Current) is a boundary
				--------------------------------------------------------------------------------*/
				if (i == end)
				{
					System.out.println("Current " + md5Hashes.get(current));
					documentStart = current + 1;// set this as the beginning of the new boundary
					current = end+ 1; // this is where we start finding the new local minima
					start = documentStart; // we will start comparing from here!, since everything before this is a boundary
					end = current + localBoundary; // this is the new end of the hash boundary
					match = true; // so we don't increment our window values
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

		// -------------------------------------------------------------------------------------------
		//  we are missing the last boundary, so hash that last value
		//	We will also check against our values of the strings we already have, and if we encountered this 
		//	already, then we will simply increment the counter, otherwise we will insert it in the hashtable
		//	and increase our miss counter
		//----------------------------------------------------------------------------------------------
	} // end of the method

	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. localboundary - used to keep track of how the 2min chooses it boundaries

		-- We will start running the 2 min algorithim here
		-- We have a sliding window and find the local minima or local maxima within the document
		-- We have a hashTable where we store the values of the boundaries and compare to see if we have
		-- already seen this
		-- we also keep track of a counter and misscounter, which we use to compute the ratio
	-------------------------------------------------------------------------------------------------------- */
	private static void run2min() throws Exception{
		int start = 0; // starting point
		int current = localBoundary;// has to be atlead here to be the local minima
		int end  = localBoundary *2;  // this is the end of the window
		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		StringBuilder builder = new StringBuilder(); // used to create the boundaries from the original file
		/* --------------------------------------------
			-- Loop throught and compare each value in the boundary 
			-- and find the boundaries

		----------------------------------------------*/
		while (end<md5Hashes.size()) // loop through till we hit the end of the array
		{ 
			//System.out.println("Checking " + end + " " + md5Hashes.size());
			for (int i = start; i <= end; ++i) // loop through each of the values in this boundary
			{							
				if (i == current){ // we are looking for strictly less than, so we don't want to compare with ourselve
					i++;
				}		
				// CompareTo returns
					// >0 if greater
					// <0 if less than
					// 0 if equal
				// 	// break if this isnt the smallest one
				//System.out.println(current + " " + i + " " + md5Hashes.size());
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) 
					break; // we will break if the value at the current index is not a local minima
				/*-----------------------------------------------------------------------------
					We have reached the end. Meaning all the values within the range 
					(documentStart,Current) is a boundary
				--------------------------------------------------------------------------------*/
				if (i == end)
				{
					System.out.println("Current " + md5Hashes.get(current));
					documentStart = current + 1;// set this as the beginning of the new boundary
					current = end+ 1; // this is where we start finding the new local minima
					start = documentStart; // we will start comparing from here!, since everything before this is a boundary
					end = current + localBoundary; // this is the new end of the hash boundary
					match = true; // so we don't increment our window values
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


	//======================================================================================================== TEST CODE

	/*
		- Finds the hash value with the lowest value within the specified range
	*/
	private static int findMin(int start,int end,ArrayList<Integer> md5Hashes){
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
	private static void determineCutPoints_way2(){
		int start = 0; // starting point
		int current = localBoundary;// has to be atlead here to be the local minima
		int end  = localBoundary *2;  // this is the end of the boundary
		int documentStart = 0; // used to keep track of where the boundaries start from
		boolean match = false; // used to ck if we encountered a match and is used to determine whether to increment the hash window
		StringBuilder builder = new StringBuilder();

		int l_min = findMin(start,current-1,md5Hashes); //find min from left side
		int r_min = findMin(current + 1,end,md5Hashes); // find min from right side
		/*--------------------------------------------------
			-- Now we run the window over and compute the value
			-- in each window and store in hash table
		----------------------------------------------------*/
		while (end<md5Hashes.size()) // loop through till we hit the end of the array
		{ 
								
			// ck of l_min and r_min are valid ( as in are within the boundary range)
			if (!(l_min >= start))
				l_min = findMin(start,current-1,md5Hashes); // find new min
			if (!(r_min > current))
				r_min = findMin(current+1,end,md5Hashes);

			// now check the new value that was just slides in ( we incremented current so we compare the value that was just slided in, as in current -1)
			if (!(md5Hashes.get(l_min).compareTo(md5Hashes.get(current-1)) < 0))
				l_min = current-1; // this is the new l_min
			// compare r_min to the new value that was just slided in , as in the end value
			if (!(md5Hashes.get(r_min).compareTo(md5Hashes.get(end)) < 0))
				r_min = end; // this is the new l_min

			/*-----------------------------------------------------------------------------
				 if current is the minimum, we have a boundary
			--------------------------------------------------------------------------------*/
			if (md5Hashes.get(current).compareTo(md5Hashes.get(Math.min(r_min,l_min))) < 0){
				
				System.out.println("Current " + md5Hashes.get(current));
				documentStart = current + 1;// set this as the beginning of the new boundary
				start = current + 1;// set this as the beginning of the new boundary
				current = start + localBoundary; // this is where we start finding the new local minima
				end = current + localBoundary; // this is the new end of the hash boundary
				match = true; // so we don't increment our window values
				// l_min = r_min; // the right min is now the new l_min
				// r_min = findMin(current+1,end,md5Hashes);// find new r_min
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


	private static void run2(){
		int start = 0; // starting point
		int current = localBoundary;// has to be atlead here to be the local minima
		int end  = localBoundary *2;  // this is the end of the boundary
		int documentStart = 0; // used to keep track of where the boundaries start from
		boolean match = false; // used to ck if we encountered a match and is used to determine whether to increment the hash window
		StringBuilder builder = new StringBuilder();

		int l_min = findMin(start,current-1,md5Hashes); //find min from left side
		int r_min = findMin(current + 1,end,md5Hashes); // find min from right side
		/*--------------------------------------------------
			-- Now we run the window over and compute the value
			-- in each window and store in hash table
		----------------------------------------------------*/
		while (end<md5Hashes.size()) // loop through till we hit the end of the array
		{ 
								
			// ck of l_min and r_min are valid ( as in are within the boundary range)
			if (!(l_min >= start))
				l_min = findMin(start,current-1,md5Hashes); // find new min
			if (!(r_min > current))
				r_min = findMin(current+1,end,md5Hashes);

			// now check the new value that was just slides in ( we incremented current so we compare the value that was just slided in, as in current -1)
			if (!(md5Hashes.get(l_min).compareTo(md5Hashes.get(current-1)) < 0))
				l_min = current-1; // this is the new l_min
			// compare r_min to the new value that was just slided in , as in the end value
			if (!(md5Hashes.get(r_min).compareTo(md5Hashes.get(end)) < 0))
				r_min = end; // this is the new l_min

			/*-----------------------------------------------------------------------------
				 if current is the minimum, we have a boundary
			--------------------------------------------------------------------------------*/
			if (md5Hashes.get(current).compareTo(md5Hashes.get(Math.min(r_min,l_min))) < 0){
				
				System.out.println("Current " + md5Hashes.get(current));
				documentStart = current + 1;// set this as the beginning of the new boundary
				start = current + 1;// set this as the beginning of the new boundary
				current = start + localBoundary; // this is where we start finding the new local minima
				end = current + localBoundary; // this is the new end of the hash boundary
				match = true; // so we don't increment our window values
				// l_min = r_min; // the right min is now the new l_min
				// r_min = findMin(current+1,end,md5Hashes);// find new r_min
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
