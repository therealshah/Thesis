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
	Next the cut points for the document are determined from the hash array, using a content dependant method, (decrement2min in this case).
	The original document is divided into chunks using the cut points as boundaries between the chunks. Different versions of the documents are
	using where the first chunks of the document are stored, whereas the second version is simply used to see of that portion of the document
	occurred.


	Decrement2min: This algorithm is similiar to the LocalMinima method, but has minor tweaks. Similar to the localMinima algorithm, this one 
	also has a BoundarySize associated with it called B. Similar to the LocalMinima, this algorithm declares a hash a cutpoint only if the hash
	is strictly less than the B hases before it and B hashes after it. The second parameter associated with this algorithm is a max chunk size. Once we hit the maximum chunk size, we will 
	slowly decrease the BoundarySize by a constant factor until we hit a boundary


*/

public class Decrement2min{

	private static HashMap<String,Integer> matches = new HashMap<String,Integer>();

	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 
	private static String directory = "../thesis/gcc/";

	// get the ratio of the coverage over the total size
	private static double totalSize=0;
	private static double coverage=0;
	public static int multiplier;
	private static int window = 12;
	private static int maxBoundary;
	private static int numOfPieces = 0;



	public static void main(String [] args) throws IOException, Exception
 	{
 		int [] arr = {2,4,6}; // these are the multipliers we will use
 		for (int m : arr){
 			multiplier = m;
			ReadFile.readFile(directory,fileList);
			System.out.println(fileList.get(0) + " " + " " + fileList.get(1) + " " +multiplier);
			driverRun(); // driver for taking in inputs and running the 2min method
			
		}
		fileList.clear();//clear this
		md5Hashes.clear(); // clear this
		
	}


	private static void driverRun() throws IOException, Exception{

		for (int i = 100;i<=1000;i+=50)
		{

			maxBoundary = multiplier*i; // mas boudnary
			int localBoundary = i;
		/*--------------------------------------------------------------------------------------------
					-- Run the 2 min algorithm for all the way upto the value the user enters
					-- We will use the local boundary for all the way up to the value the user entered
		-------------------------------------------------------------------------------------------------*/
			System.out.print( localBoundary+" ");
			runBytes(localBoundary);
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

		//write.close();	// close the file
	}


	/*
		- This method reads the file using bytes
		- This is where we run the 2min content dependent partitioning
	*/
	private static void runBytes(int localBoundary) throws IOException,Exception{
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
						run2min(array,md5Hashes,localBoundary);// here we run 2min, ck how similar the documents are to the one already in the system
					}
					// empty out the md5 Hashes for reuse
					md5Hashes.clear();	
				} // end of the for ( that reads the files) loop
						
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
				/*-----------------------------------------------------------------------------
					We have reached the end. Meaning all the values within the range 
					(documentStart,Current) is either a second smallest or first smallest
				--------------------------------------------------------------------------------*/
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
			// we will run the decrement 2min
			if ((current-documentStart + 1) >= maxBoundary){
				// start is document start
				// end is current-documentStart + 1
				int dStart = documentStart; // this is the beginning
				int dEnd = current; // this is the end (note add dStart!)
				int decrementSize = 1; // initially decrement by one
				int point = runDecrement2min(dStart,dEnd,md5Hashes,localBoundary,decrementSize);// will return if a boundary is found, if not then -1
				while (point == -1){
					// if we didn't find a boundary, then decrement theSize by one and run again
					point = runDecrement2min(dStart,dEnd,md5Hashes,localBoundary,++decrementSize);// will return if a boundary is found, if not then -1
				}
				// now we made it, with point being boundary point
				for (int j = documentStart; j <= point;++j)
					builder.append(array[j]); 
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

		// loop through the end of our array and hash the final boundaries
		for (int j = documentStart; j < array.length;++j ){
			builder.append(array[j]); 
		}
		String hash = MD5Hash.hashString(builder.toString(),"MD5");
		matches.put(hash,1); // simply insert the chunks in the document
		

	} // end of the method

	/* -------------------------------------------------------------------------------------------------------
	This method:
			-- takes in 5 params
			-- dStart/dEnd - start/end
			-- decrementSize -- how much we decrement the boundarySize
				

			-- We are determing if we can find a boundary with the local boundary decremented by decrementSize
	-------------------------------------------------------------------------------------------------------- */
	private static int runDecrement2min(int dStart,int dEnd, ArrayList<Long> md5Hashes, int localBoundary,int decrementSize){
		localBoundary = localBoundary - decrementSize; // decrement the localBoundary by the decrement factor
		int start = dStart; // starting point
		int current = start + localBoundary;// has to be atlead here to be the local minima
		int end  = current + localBoundary;  // this is the end of the boundary
		/*--------------------------------------------------
			-- Now we run the window over and compute the value
			-- in each window and store in hash table
		----------------------------------------------------*/

		// if it's zero, that means we havent found a boundary
		if (localBoundary == 0)
			return dStart; // we'll make the start the boundary
		while (end<=dEnd) // loop through till we our end ( which was the max boundary)
		{ 
			for (int i = start; i <= end; ++i) // loop through each of the values in this boundary
			{							
				if (i == current) // we are looking for strictly less than, so we don't want to compare with ourselves
					++i; // we don't wanna compare withourselves		
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) 
						break;
				/*-----------------------------------------------------------------------------
					We have reached the end. Meaning all the values within the range 
					(documentStart,Current) is either a second smallest or first smallest
				--------------------------------------------------------------------------------*/
				if (i == end) // we have reached the end
					return current; // we have found a boundary and return it 
				
			} // end of for
			// go to the next window because we still havent found the boundary
			start++;
			current++;
			end++;	
		} // end of the while loop
		return -1; // return -1 because we still didn't find a boundary
	} // end of the method

	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. localboundary - used to keep track of how the 2min chooses it boundaries

		-- We will start running the decrement2min algorithim here
		-- We start off with the regular 2min algo and once we hit the max boundary size, we run decrement2min
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
		while (end<md5Hashes.size()){ 
	
			for (int i = start; i <= end; ++i)
			{							
				if (i==current) // we don't want to compare with ourselves
					++i;	
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) {
						break; // we will break if the value at the current index is not a local minima
				}
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
					if (matches.get(hash) != null){
						coverage+= current-documentStart+1; // this is how much we saved
					}					
					documentStart = current + 1;// set this as the beginning of the new boundary
					start = current + 1;
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary;
					builder.setLength(0); // reset the stringbuilder to get the next window
					match = true; //  so we don't increment our window again
					numOfPieces++; // we just got another boundary piece
				}
			} // end of for

			// if we have reached our maximum threshold
			// we will run the decrement 2min
			if ((current-documentStart + 1) >= maxBoundary){
				int dStart = documentStart; // this is the beginning
				int dEnd = current; // this is the end
				int decrementSize = 1; // initially decrement by one
				int point = runDecrement2min(dStart,dEnd,md5Hashes,localBoundary,decrementSize);// will return if a boundary is found, if not then -1
				while (point == -1){

					// if we didn't find a boundary, then decrement theSize by one and run again
					point = runDecrement2min(dStart,dEnd,md5Hashes,localBoundary,++decrementSize);// will return if a boundary is found, if not then -1

				}
				// now we made it, with point being boundary point
				for (int j = documentStart; j <= point;++j)
					builder.append(array[j]); 
				
				String hash = MD5Hash.hashString(builder.toString(),"MD5"); // hash this boundary
				if (matches.get(hash) != null)
					coverage+= point-documentStart+1; // this is how much we saved
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
}












