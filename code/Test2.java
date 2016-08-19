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

public class Test2{
			

	//private static HashMap<String,Integer> matches = new HashMap<String,Integer>();
	private static HashSet<String> hash1 = new HashSet<String> ();
	private static HashSet<String> hash2 = new HashSet<String> ();

	private static ArrayList<Integer> c1 = new ArrayList<Integer>();
	private static ArrayList<Integer> c2 = new ArrayList<Integer>();
	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 
	//private static String directory = "../thesis-datasets/gcc/";
  	private static String directory = "../../thesis-datasets/gcc/";

	//private static String directory = "../../thesis-datasets/morph/morph_.95_.10/";	
	// get the ratio of the coverage over the total size
	private static double totalSize=0;
	private static double coverage=0;
	private static int numOfPieces=0;
	private static int window = 12;

	// variables for the boundary size
	private static int startBoundary = 100; // start running the algo using this as the starting param
	private static int endBoundary = 1000; // go all the way upto here
	private static int increment = 50; // increment in these intervals

	private static int document_date_selection = 2; // 1 - last week, 2 - for last month, 3 - for last year

	private static ArrayList< byte [] > fileArray = new ArrayList<byte[]>(); // holds both the file arrays
	private static ArrayList<ArrayList<Long>> hashed_File_List = new ArrayList<ArrayList<Long>>(); // used to hold the hashed file

	private static boolean test = false;
	
	public static void main(String [] args) throws Exception
 	{

		//runPeriodic();
		//runArchiveSet();
		runOtherDataSets();
		//runMorphDataSet();
	}

	/*
		-- This is a helper method run datasets such as emacs, gcc etc
	
	*/
	private static void runOtherDataSets() throws Exception{

		System.out.println("Running LocalMinima " + directory);
		ReadFile.readFile(directory,fileList); // read the two files
		System.out.println(fileList.get(0) + " " + fileList.get(1));
		preliminaryStep(directory);
	 	startCDC();
	 	if (c1.size() != c2.size())
	 		System.out.println("WTH" + " " + c1.size() + " " + c2.size());
	 	else{
	 		int counter = 0;
	 		for (int i = 0;i < c1.size(); ++i)
	 			if (!(c1.get(i).equals(c2.get(i))))
	 				counter++;
	 		System.out.println(counter);
	 	}

	 		
	 	
	}



	/*
		- This reads the file and hashses the document, which are then stored in our arrayLisrs
		- we do this before, so we dont have to hash again later ( which is time consuming)
	*/
	private static void preliminaryStep(String dir) throws Exception{
		int start = 0; // start of the sliding window
		int end = start + window - 1; // ending boundary

		// prepoccessing step to hash the document, since we dont need to hash the document again
		for (int i = 0; i < 1; ++i){
			//System.out.println("preliminaryStep " + fileList.get(i));
			Path p = Paths.get(dir+fileList.get(i)); // read this file
			byte [] array = Files.readAllBytes(p); // read the file in bytes
			//System.out.println(array.length);

			ArrayList<Long> md5Hashes = new ArrayList<Long>(); // make a new arrayList for this document
		//	System.out.println("Before hashing\n");
			HashDocument.hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
			//System.out.println("After Hashing\n");
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
			int localBoundary = i;
			System.out.print( localBoundary+" ");
			readBytes(localBoundary);
			// matches.clear();
			coverage = 0;
			numOfPieces = 0;
			break;
		}	
	}





	/*
		Read in all the files and loop through all the files
		We already have the hashed version of the documents 
		First, we cut up the first document into chunks (using the CDC algorhtim) and store it
		Then we cut up the second document (usually a different version of the same document) and see how many chunks match
	*/
	private static void readBytes(int localBoundary) throws Exception{
		// there are only 2 files
		storeChunks(hashed_File_List.get(0),localBoundary); // cut up the first file and store it
		determineCutPoints_way2(hashed_File_List.get(0),localBoundary); // cut up the first file and store it
	} // end of the function


	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. localboundary - used to keep track of how the 2min chooses it boundaries

		-- We are simply finding the boundaries of the file using 2min and simply storing them. Nothing more!
	-------------------------------------------------------------------------------------------------------- */
	private static void storeChunks(ArrayList<Long> md5Hashes, int localBoundary){
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
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) // less than or equal to
					break; // we will break if the value at the current index is not a local minima
				/*-----------------------------------------------------------------------------
					We have reached the end. Meaning all the values within the range 
					(documentStart,Current) is a boundary
				--------------------------------------------------------------------------------*/
				if (i == end)
				{
			
					// String original = builder.toString();
					// String hash = MD5Hash.hashString(original,"MD5");	// hash this boundary. USe MD5 to reduce the probability of collision
					c1.add(current);
					//matches.put(hash,1); // simply insert the chunks in the hashtable
					documentStart = current + 1;// set this as the beginning of the new boundary
					current = end+ 1; // this is where we start finding the new local minima
					start = documentStart; // we will start comparing from here!, since everything before this is a boundary
					end = current + localBoundary; // this is the new end of the hash boundary
					builder.setLength(0); // reset the stringbuilder for the next round
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

		// // loop through the end of our array and hash the final boundaries
		// for (int j = documentStart; j < array.length;++j ){
		// 	builder.append(array[j]); 
		// }
		// String original = builder.toString();
		// String hash = MD5Hash.hashString(original,"MD5");	// hash this boundary. USe MD5 to reduce the probability of collision
		// hash1.add(hash);
	} // end of the method



//======================================================================================================== TEST CODE

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
		StringBuilder builder = new StringBuilder();

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
				// for (int j = documentStart; j <= current;++j){
				// 	builder.append(array[j]); 
				// }
				// String original = builder.toString();
				// String hash = MD5Hash.hashString(original,"MD5");	// hash this boundary. USe MD5 to reduce the probability of collision
				c2.add(current);
				documentStart = current + 1;// set this as the beginning of the new boundary
				start = current + 1;// set this as the beginning of the new boundary
				current = start + localBoundary; // this is where we start finding the new local minima
				end = current + localBoundary; // this is the new end of the hash boundary
				match = true; // so we don't increment our window values
				// l_min = r_min; // the right min is now the new l_min
				// r_min = findMin(current+1,end,md5Hashes);// find new r_min
				builder.setLength(0);
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


		// loop through the end of our array and hash the final boundaries
		// for (int j = documentStart; j < array.length;++j ){
		// 	builder.append(array[j]); 
		// }
		// String original = builder.toString();
		// String hash = MD5Hash.hashString(original,"MD5");	// hash this boundary. USe MD5 to reduce the probability of collision
		// hash2.add(hash);
	} // end of the method


}
