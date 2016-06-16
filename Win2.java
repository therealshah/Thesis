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



	Abstract: LocalMinima is a content dependant chunking method. It determines the boundaries for the document using the local minima. 
	All content dependant algorithms first hash the document using a sliding window of length w, which we will call the hash array. (12 for all these experiments). This step is true for all content dependant chunking algorithms.
	Next the cut points for the document are determined from the hash array, using a content dependant method, (Local Minima in this case).
	The original document is divided into chunks using the cut points as boundaries between the chunks. Different versions of the documents are
	using where the first chunks of the document are stored, whereas the second version is simply used to see of that portion of the document
	occurred.


	2Win: This algorithm is similiar to the LocalMinima method, but has minor tweaks. Similar to the localMinima algorithm, this one 
	also has a BoundarySize associated with it called B. Similar to the LocalMinima, this algorithm declares a hash a cutpoint only if the hash
	is strictly less than the B hases before it and B hashes after it. The second parameter associated with this algorithm is a max chunk size. Once we hit the maximum chunk size, we will declare the 
	cutpoint by just running winnowing on the block. (take minimum)


*/

public class Win2{

	private static HashMap<String,Integer> matches = new HashMap<String,Integer>();

	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 
	private static String directory = "../thesis/gcc/";
	// get the ratio of the coverage over the total size
	private static double totalSize=0;
	private static double coverage=0;
	private static int numOfPieces=0;
	private static int window;// window is size 3
	private static int maxBoundary;
	private static int multiplier;

	public static void main(String [] args) throws IOException, Exception
 	{

		int [] arr = {2,4,6}; // these are the multipliers we will use
 		for (int m : arr){
 			multiplier = m;
			ReadFile.readFile(directory,fileList);
			System.out.println(fileList.get(0) + " " + " " + fileList.get(1) + " " +multiplier);
			driverRun(); // driver for taking in inputs and running the 2min method
			
		}
	}

	// this method basically will chop up the blocks and get their frequencies
	private static void getBlockFrequency() throws Exception{
		ArrayList<Long> md5Hashes = new ArrayList<Long>(); // store md5Hases
		HashMap<Integer,Integer> blockFreq = new HashMap<Integer,Integer>(); // this stores the block in the map along there frequencies
		System.out.println(fileList.get(0));

		Path p = Paths.get(directory + fileList.get(0)); // get the path of the file, there is only one file
		byte [] array = Files.readAllBytes(p); // read the file into a byte array
		int start = 0; // start of the sliding window
		window = 12;
		int end = start + window - 1; // ending boundary
		int localBoundary = 500;
		HashDocument.hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
		int totalBlocks = chopDocument(array,md5Hashes,localBoundary,blockFreq);
		// now output the block sizes, along with there frequencies and probilities
		for (Map.Entry<Integer,Integer> tuple: blockFreq.entrySet()){
			// output the block freq
			double prob = (double)tuple.getValue() / (double)totalBlocks;
			System.out.println(tuple.getKey() + " " + tuple.getValue() + " " + prob);
		}
	}

	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
		1. array - this is the byte array that actually holds the document contents
		2. md5Hases - holds the entire hash values of the document
		3. localboundary - used to keep track of how the 2min chooses it boundaries
		4. blockFreq - used to store the block sizes and the frequency of how many times that block size appears

		-- We are simply storing how many times each blockSize appears
	-------------------------------------------------------------------------------------------------------- */
	private static int chopDocument(byte [] array, ArrayList<Long> md5Hashes, int localBoundary,HashMap<Integer,Integer> blockFreq){
		int start = 0; // starting point
		//System.out.println("TESTING" + localBoundary);
		int counter = 0; // count the number of blocks we have
		int current = localBoundary;// has to be atlead here to be the local minima
		int end  = localBoundary *2;  // this is the end of the boundary
		int documentStart = 0; // used to keep track of where the boundaries start from
		boolean match = false; // used to ck if we encountered a match and is used to determine whether to increment the hash window
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
					int size = current - documentStart + 1; // this is the size of this block freq
					//System.out.println(size);
					if (blockFreq.get(size) == null){ // if not in there, then simply store it}
						blockFreq.put(size,1); // simply insert the chunks in the document
					}
					else // increment it's integer count
						blockFreq.put(size,blockFreq.get(size)+1); // increment the count
					counter++; // increment the block count
					documentStart = current + 1;// set this as the beginning of the new boundary
					current = end+ 1; // this is where we start finding the new local minima
					start = documentStart; // we will start comparing from here!, since everything before this is a boundary
					end = current + localBoundary; // this is the new end of the hash boundary
					match = true; // so we don't increment our window values
					break; // break out of the for loop
				}
			}
				
			// go to the next window only if we didnt find a match
			// because if we did find a boundary, we would automatically go to the next window
			if (!match){
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
		int size = array.length - documentStart;
		if (blockFreq.get(size) == null) // if not in there, then simply store it
			blockFreq.put(size,1); // simply insert the chunks in the document
		else // increment it's integer count
			blockFreq.put(size,blockFreq.get(size)+1); // increment the count
		return ++counter;
	} // end of the method


	/*
		- This is basically sets up everything and calls the actual contentDependant methods
	*/
	private static void driverRun() throws IOException, Exception{
		
		for (int i = 100;i<=1000;i+=50)
		{
			maxBoundary = multiplier*i;
			int localBoundary = i;
			window = 12; // set value
		/*--------------------------------------------------------------------------------------------
					-- Run the 2 min algorithm for all the way upto the value the user enters
					-- We will use the local boundary for all the way up to the value the user entered
		-------------------------------------------------------------------------------------------------*/
			System.out.print( localBoundary+" ");
			readBytes(localBoundary);
			// this is the block size per boundary
			double blockSize = (double)totalSize/(double)numOfPieces;
			double ratio = (double)coverage/(double)totalSize;
			System.out.println(blockSize + " " + ratio);

			// clear the hashTable, and counters so we can reset the values for the next round of boundaries
			matches.clear();
			coverage = 0;
			totalSize = 0;
			numOfPieces = 0;
		}// end of the for loop
	}


	/*
		- This method 
			--reads the file using bytes
			-- calls the chunkingMethod
			-- and runs and finds the boundary points
	*/
	private static void readBytes(int localBoundary) throws IOException,Exception{
		/*---------------------------------------------------------------------------------
			Read in all the files and loop through all the files
			We will first cut the first document into chuncks and store it
			Then we will hash the next document and see how much coverage we get (how many matches we get)
		--------------------------------------------------------------------------------------*/
			File file = null;
			boolean first = true; // this will be used to ck if it's the first file or not
			ArrayList<Long> md5Hashes = new ArrayList<Long>(); // used to hold the md5Hashes
			for (String fileName: fileList)
			{
				//System.out.println(fileName);
				Path p = Paths.get(directory+fileName);

				// read the file
				byte [] array = Files.readAllBytes(p); // read the file in bytes
				int start = 0; // start of the sliding window
				int end = start + window - 1; // ending boundary
				HashDocument.hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
				// if this is the first document, we will simply get the boundary chunks and store them
				if (first){
					storeChunks(array,md5Hashes,localBoundary);
					first = !first;
					totalSize = 0;
				}
				else{

					totalSize = array.length; // get the total size of the file
					run2Win(array,md5Hashes,localBoundary);// here we run 2min, ck how similar the documents are to the one already in the system
				}
				// empty out the md5 Hashes for reuse
				md5Hashes.clear();
								
			} // end of the for ( that reads the files) loop
						
	} // end of the function


	/*
		- This method basically finds the minimum within the range
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
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. localboundary - used to keep track of how the 2min chooses it boundaries

		-- We are simply finding the boundaries of the file using tweaked version of the localMinima which i call 2Win and simply storing them. Nothing more!
	-------------------------------------------------------------------------------------------------------- */
	private static void storeChunks(byte [] array, ArrayList<Long> md5Hashes, int localBoundary){
		int start = 0; // starting point
		int current = localBoundary;// has to be atlead here to be the local minima
		int end  = localBoundary *2;  // this is the end of the boundary
		int documentStart = 0; // used to keep track of where the boundaries start from
		StringBuilder builder = new StringBuilder(); // this is used to store the original document content
		boolean match = false;
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
						break;
				if (i == end) // we have reached the end
				{
					// Hash all the values in the range (documentStart,current)
					// Remember we only want to hash the original VALUES from the array that contains the original
					// content of the file. Not the hash values in the md5Hash Array
					for (int j = documentStart; j <= current;++j){
						builder.append(array[j]); 
					}
					String hash = MD5Hash.hashString(builder.toString(),"MD5"); // hash this boundary
					matches.put(hash,1); // simply insert the chunks in the hashtable
					documentStart = current + 1;// set this as the beginning of the new boundary
					start = current + 1;
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary; // this is the new end of the hash boundary
					builder.setLength(0); // reset the stringbuilder for the next round
					match = true; // so we don't increment our window values
				}
			} // end of for

			// if we have reached our maximum threshold
			// we will see if we have a second boundary, if yes, then make that the boundary
			// otherwise now we will make either the first minima or the second minima the boundary
			if ((current-documentStart + 1) >= maxBoundary){
				
				// find smallest in this range ( most right occurance)
				int point = findMin(documentStart,current,md5Hashes);
				for (int j = documentStart; j <= point;++j){
					builder.append(array[j]); 
				}
				String hash = MD5Hash.hashString(builder.toString(),"MD5"); // hash this boundary
				matches.put(hash,1); // simply insert the chunks in the hashtable
				documentStart = point + 1;// set this as the beginning of the new boundary
				start = point + 1;
				current = start + localBoundary; // this is where we start finding the new local minima
				end = current + localBoundary; // this is the new end of the hash boundary
				builder.setLength(0); // reset the stringbuilder for the next round
				match = true; // so we don't increment our window values
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
			3. localboundary - How big the neighborhood is

		-- We will start running the modified version of the localMinima algorithm with tweaking so that it takes
		-- the smallest hash value between the range
	-------------------------------------------------------------------------------------------------------- */
	private static void run2Win(byte [] array, ArrayList<Long> md5Hashes, int localBoundary) throws Exception{
		int start = 0; // starting point
		int current = localBoundary;// has to be atlead here to be the local minima
		int end  = localBoundary *2;  // this is the end of the window
		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		StringBuilder builder = new StringBuilder(); // used to create the boundaries from the original file

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
					break;
				if (i == end) // we have reached the end
				{
					// Hash all the values in the range (documentStart,current)
					// Remember we only want to hash the original VALUES from the array that contains the original
					// content of the file. Not the hash values in the md5Hash Array
					for (int j = documentStart; j <= current;++j){
						builder.append(array[j]); 
					}
					String hash = MD5Hash.hashString(builder.toString(),"MD5"); // hash this boundary
					if (matches.get(hash) != null)
						coverage += current - documentStart + 1; // increment the coverage
					documentStart = current + 1;// set this as the beginning of the new boundary
					start = current + 1;
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary; // this is the new end of the hash boundary
					builder.setLength(0); // reset the stringbuilder for the next round
					match = true; // so we don't increment our window values
					numOfPieces++;
				}
			} // end of for

			// if we have reached our maximum threshold
			// we will see if we have a second boundary, if yes, then make that the boundary
			// otherwise now we will make either the first minima or the second minima the boundary
			if ((current-documentStart + 1) >= maxBoundary){
				
				// find smallest in this range ( most right occurance)
				int point = findMin(documentStart,current,md5Hashes);
				for (int j = documentStart; j <= point;++j){
					builder.append(array[j]); 
				}
				String hash = MD5Hash.hashString(builder.toString(),"MD5"); // hash this boundary
				if (matches.get(hash) != null)
					coverage += point - documentStart + 1; // increment the coverage
				documentStart = point + 1;// set this as the beginning of the new boundary
				start = point + 1;
				current = start + localBoundary; // this is where we start finding the new local minima
				end = current + localBoundary; // this is the new end of the hash boundary
				builder.setLength(0); // reset the stringbuilder for the next round
				match = true; // so we don't increment our window values	
				numOfPieces++;		
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
		}

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
}












