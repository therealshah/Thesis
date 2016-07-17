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
	private static String directory = "../thesis/gcc/";
	//private static String directory = "../thesis-datasets/periodic_50/";

	//private static String directory = "../thesis/datasets/1389blog.com/";
	//private static String directory = "../thesis-datasets/datasets/id.mind.ne/";

	//private static String directory = "../thesis/periodic/";
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
	}

	/*
		-- This is a helper method to run the periodic dataset basically

	*/
	private static void runPeriodic() throws Exception {
		System.out.println("Running LocalMinima Periodic");
		// this is alll the directories we will be running 
		String [] periodic_directory = {"../thesis-datasets/periodic_100/","../thesis-datasets/periodic_300/","../thesis-datasets/periodic_500/","../thesis-datasets/periodic_700/","../thesis-datasets/periodic_1000/"};
		
		// run the dataset
		for (String dir : periodic_directory){
			directory = dir;
			ReadFile.readFile(directory,fileList); // read the two files
			System.out.println(fileList.get(0) + " " + fileList.get(1));
			preliminaryStep(directory);
		 	startCDC();
		 	fileList.clear(); // clear the list
		 	fileArray.clear(); // clear the array of files we have read in
		 	hashed_File_List.clear(); // clear all the hashed files we have
		}
		
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
			int localBoundary = i;
			System.out.print( localBoundary+" ");
			readBytes(localBoundary);
			// this is the block size per boundary
			double blockSize = (double)totalSize/(double)numOfPieces;
			double ratio = (double)coverage/(double)totalSize;
			System.out.println(blockSize + " " + ratio);
			// clear the hashTable, and counters so we can reset the values for the next round of boundaries
			matches.clear();
			coverage = 0;
			numOfPieces = 0;
		}	
	}


	/*
		-- Find the starting point where we will start the documentStart 
		-- reduce slack from the left

		@params:
			-- index - the point where we start
	*/
	private static int lowerSlack(ArrayList<Long> md5Hashes,int index){

		/* simply function, we will set this as the 
			-- start of a potential file by having n odd, then even
			-- make a chunk start if that point is divisble by a certain point
		*/
		int prev = index;
		int localBoundary = 2;
		int start = index;
		int current = index + localBoundary;
		int end = current + localBoundary;
		boolean matche = false;
		// run 2min using a super small boundary to determine the start of the chunk
		// more likely to reduce the slack from the left ( AKA both documents will most likely start from this same position)
		while (end < md5Hashes.size()){

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
					return current; // this will be the starting point
			}						
			// go to the next window only if we didnt find a match
			// because if we did find a boundary, we would automatically go to the next window
			start++;
			current++;
			end++;
			
		}
		return prev; // otherwise return original 
	}

	/*
		- Overloaded method just for the internet archive dataset
		- The first two params hold the block size and ratioSize respectively (for all the runnings)
		- The last set of params are the actual file in byte and the hashed versions of the file we will be running the code against
		- current_ -- are the lists that contain the most recent file version
		- previous_ -- are the listrs that contain the previous versions
	*/
	private static void startCDC(double [] block_size_list, double [] ratio_size_list,byte[] current_array,byte[] previous_array,
	 ArrayList<Long> current_md5Hashes,ArrayList<Long> previous_md5Hashes ) throws Exception{
		int index = 0; // used to traverse the two lists
		for (int i = startBoundary;i<=endBoundary;i+=increment)
		{			
			int localBoundary = i;
			// System.out.print( i+" ");
			storeChunks(previous_array,previous_md5Hashes,localBoundary); // cut up the first file and store it
			run2min(current_array,current_md5Hashes,localBoundary); // call the method again, but on the second file only
			// this is the block size per boundary
			double blockSize = (double)totalSize/(double)numOfPieces;
			double ratio = (double)coverage/(double)totalSize;
			// if (Double.isNaN(ratio)){
			// 	System.out.println(ratio + " " + coverage + " " + totalSize);

			// }
			//System.out.println(ratio);
			// extra step, add the data back into the list
			block_size_list[index] += blockSize;
			ratio_size_list[index] += ratio;
			//System.out.println(Arrays.toString(ratio_size_list));

			++index;
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
	private static void readBytes(int localBoundary) throws Exception{
		// there are only 2 files
		totalSize = fileArray.get(1).length; // note we only care about the size of the second file since that's the file we are measuring
		storeChunks(fileArray.get(0),hashed_File_List.get(0),localBoundary); // cut up the first file and store it
		run2min(fileArray.get(1),hashed_File_List.get(1),localBoundary); // call the method again, but on the second file only

		// determineCutPoints_way2(fileArray.get(0),hashed_File_List.get(0),localBoundary); // cut up the first file and store it
		// run2(fileArray.get(1),hashed_File_List.get(1),localBoundary); // call the method again, but on the second file only
	} // end of the function


	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. localboundary - used to keep track of how the 2min chooses it boundaries

		-- We are simply finding the boundaries of the file using 2min and simply storing them. Nothing more!
	-------------------------------------------------------------------------------------------------------- */
	private static void storeChunks(byte [] array, ArrayList<Long> md5Hashes, int localBoundary){
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
					// Hash all the values in the range (documentStart,current)
					// Remember we only want to hash the original VALUES from the array that contains the original
					// content of the file. Not the hash values in the md5Hash Array
					for (int j = documentStart; j <= current;++j){
						builder.append(array[j]); 
					}
					String hash = MD5Hash.hashString(builder.toString(),"MD5"); // hash this boundary
					matches.put(hash,1); // simply insert the chunks in the hashtable
					//documentStart = current + 1;// set this as the beginning of the new boundary
					documentStart = lowerSlack(md5Hashes,current+1);
					start = documentStart; // we will start comparing from here!, since everything before this is a boundary
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary; // this is the new boundary
					builder.setLength(0); // reset the stringbuilder for the next round
					match = true; // so we don't increment our window values
					// Start the documentStart at a precondition

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

		// loop through the end of our array and hash the final boundaries
		for (int j = documentStart; j < array.length;++j ){
			builder.append(array[j]); 
		}

		String hash = MD5Hash.hashString(builder.toString(),"MD5");
		matches.put(hash,1); // simply insert the chunks in the document
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
	private static void run2min(byte [] array, ArrayList<Long> md5Hashes, int localBoundary) throws Exception{
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
		while (end<md5Hashes.size())
		{ 
			for (int i = start; i <= end; ++i)
			{							
				if (i == current){ // we are looking for strictly less than, so we don't want to compare with ourselve
					++i; // we want continute and go to next value. Not we dont just increment i because if it was at the end - 1 value, incrementing it will put it at end and crash
				}		

				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) 
					break; // we will break if the value at the current index is not a local minima
				
				/*-----------------------------------------------------------------------------
					We have reached the end. Meaning all the values within the range 
					(documentStart,Current) is a boundary
				--------------------------------------------------------------------------------*/
				 if (i == end)
				{
					// Hash all the values in the range (documentStart,current)
					// Remember we only want to hash the original VALUES from the array that contains the original
					// content of the file. Not the hash values in the md5Hash Array
					for (int j = documentStart; j <= current;++j){
						builder.append(array[j]); 
					}
					String hash = MD5Hash.hashString(builder.toString(),"MD5"); // hash this boundary

					// Check if this value exists in the hash table
					// If it does, we will increment the coverage count
					if (matches.get(hash) != null)
						coverage+= current-documentStart+1; // this is how much we saved
									
					//documentStart = current + 1;// set this as the beginning of the new boundary
					documentStart = lowerSlack(md5Hashes,current+1);
					start = documentStart; // we will start comparing from here!, since everything before this is a boundary
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary; // this is the new boundary
					builder.setLength(0); // reset the stringbuilder to get the next window
					match = true; //  so we don't increment our window again
					numOfPieces++; // increment the number of pieces we got
					break; // we break because WE ARE CHANGING THE END 
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

		for (int j = documentStart; j < array.length;++j ){
			builder.append(array[j]); 
		}

		String hash = MD5Hash.hashString(builder.toString(),"MD5"); // hash our value
		if (matches.get(hash)!=null)
			coverage+=array.length - documentStart; // this is how much we saved. Dont need to add 1 cuz end it one past end anyway
		numOfPieces++; // we just got another boundary piece

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
	private static void determineCutPoints_way2(byte [] array, ArrayList<Long> md5Hashes, int localBoundary){
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
				for (int j = documentStart; j <= current;++j){
					builder.append(array[j]); 
				}
				String hash = MD5Hash.hashString(builder.toString(),"MD5"); // hash this boundary
				matches.put(hash,1); // simply insert the chunks in the hashtable
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
		for (int j = documentStart; j < array.length;++j ){
			builder.append(array[j]); 
		}
		String hash = MD5Hash.hashString(builder.toString(),"MD5");
		matches.put(hash,1); // simply insert the chunks in the document
	} // end of the method


	private static void run2(byte [] array, ArrayList<Long> md5Hashes, int localBoundary){
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
			if (md5Hashes.get(current).compareTo(md5Hashes.get(Math.min(r_min,l_min))) < 0)
			{
				for (int j = documentStart; j <= current;++j){
					builder.append(array[j]); 
				}
				String hash = MD5Hash.hashString(builder.toString(),"MD5"); // hash this boundary

				if (matches.get(hash) != null)
						coverage+= current-documentStart+1; // this is how much we saved\

				documentStart = current + 1;// set this as the beginning of the new boundary
				numOfPieces++;
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
		for (int j = documentStart; j < array.length;++j ){
			builder.append(array[j]); 
		}
		String hash = MD5Hash.hashString(builder.toString(),"MD5");
		if (matches.get(hash) != null)
			coverage+= current-documentStart+1; // this is how much we saved
	} // end of the method

}
