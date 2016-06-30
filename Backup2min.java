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


	Custom2min: This algorithm is similiar to the LocalMinima method, but has minor tweaks. Similar to the localMinima algorithm, this one 
	also has a BoundarySize associated with it called B. Similar to the LocalMinima, this algorithm declares a hash a cutpoint only if the hash
	is strictly less than the B hases before it and B hashes after it. However, if this current hash isn't the minima, then we check too see
	if the hash is the second smallest. If so, we will store it. Note only the rightmost secondSmallest is stored which is reset if a boundary
	is made. The second parameter associated with this algorithm is a max chunk size. Once we hit the maximum chunk size, we will declare the 
	cutpoint either with smallest point (original) or if we have a backup point (most recent second smallest seen)


*/
public class Backup2min{

	private static HashMap<String,Integer> matches = new HashMap<String,Integer>();

	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 
	private static ArrayList<String> folderList = new ArrayList<String>();
	private static String directory = "../thesis/gcc/";

	
	// get the ratio of the coverage over the total size
	private static double totalSize=0;
	private static double coverage=0;
	private static int numOfPieces=0;
	private static int window = 12;
	private static int maxBoundary;

	// variables for the boundary size
	private static int startBoundary = 100; // start running the algo using this as the starting param
	private static int endBoundary = 1000; // go all the way upto here
	private static int increment = 50; // increment in these intervals
	private static ArrayList< byte [] > fileArray = new ArrayList<byte[]>(); // holds both the file arrays
	private static ArrayList<ArrayList<Long>> hashed_File_List = new ArrayList<ArrayList<Long>>(); // used to hold the hashed file


	public static void main(String [] args) throws IOException, Exception
 	{
 		String [] dir = {"morph.998001/","morph.99805/","morph.999001/"};
 		for (String s: dir){
 			directory = s;
 			System.out.println(directory);
 			ReadFile.readFile(directory,fileList);
 			driverRun();
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
		minBoundary = 2*localBoundary;
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

	-- We are simply finding the boundaries of the file using 2min and simply storing them. Nothing more!
	-------------------------------------------------------------------------------------------------------- */
	private static int chopDocument(byte [] array, ArrayList<Long> md5Hashes, int localBoundary,HashMap<Integer,Integer> blockFreq){
		int start = 0; // starting point
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
			if ((current - documentStart + 1) >= minBoundary){
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
						break;
					}
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
		- This reads the file and hashses the document, which are then stored in our arrayLisrs
		- we do this before, so we dont have to hash again later ( which is time consuming)
	*/
	private static void preliminaryStep(String dir) throws Exception{
		int start = 0; // start of the sliding window
		int end = start + window - 1; // ending boundary
		// prepoccessing step to hash the document, since we dont need to hash the document again
		for (int i = 0; i < fileList.size(); ++i){
			System.out.println("preliminaryStep " + fileList.get(i));
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
		totalSize = fileArray.get(1).length; // note we only care about the size of the second file since that's the file we are measuring
	}

	/*
		- This method is used has a helper method to run the algo for the archive dataset
		- Note the archive set has multiple directories ( one for each url )
		- So Read all of the directories in first and for each directory run the code
	*/
	private static void runArchiveSet() throws Exception{

		directory = "../thesis/datasets/";
		File file = new File(directory);
		String[] directory_list = file.list(new FilenameFilter() {
		  @Override
		  public boolean accept(File current, String name) {
		    return new File(current, name).isDirectory(); // make sure its a directory
		  }
		});

		int totalRuns = 0; // used to avg the runs in the end
		int total_iter_count = 0; // this is used check how many times we will iterate through the data so we can make an array of that size
		for (int i = startBoundary;i<=endBoundary;i+=increment)
			total_iter_count++;

		System.out.println(Arrays.toString(directory_list));
		double [] block_size_list = new double [total_iter_count];
		double [] ratio_size_list = new double [total_iter_count];
	
		// loop through and run the cdc for each directory
		for (String dir : directory_list){
			// We have 4 files in each directory
			// current, last_week, last_month, last_year
			// read all the files in the directory
			System.out.println(dir);
			ReadFile.readFile(directory+"/" + dir,fileList); // read all the files in this directory
			preliminaryStep(directory+ dir + "/"); // call the preliminaryStep on all the files

			// now loop through and call each pair of files with the current one (index 0)
			for (int i = 1; i < fileArray.size(); ++i){
				totalRuns++;
				//System.out.println("Running it against " + fileList.get(0) + " " + fileList.get(i));
				totalSize = fileArray.get(i).length; // get the length of the file we will be running it against!
				startCDC(block_size_list,ratio_size_list,fileArray.get(0),fileArray.get(i),hashed_File_List.get(0),hashed_File_List.get(i));
			}
			// clear the fileList and hashed_file_list array
			fileArray.clear();
			hashed_File_List.clear();
			fileList.clear();
		} // end of directory list for loop


		// now output the avged value for all the runs
		int index = 0;
		for (int i = startBoundary;i<=endBoundary;i+=increment){
			double blockSize = block_size_list[index]/(double)totalRuns;
			double ratio = ratio_size_list[index]/(double)totalRuns;
			System.out.println(i + " " + blockSize + " " + ratio);
			index++;
		}
	}

	private static void startCDC() throws IOException, Exception{
		for (int i = startBoundary;i<=endBoundary;i+=increment)
		{
			maxBoundary = 4*i; // if we miss this many times
			int localBoundary = i;
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
	}

	/*
		- Overloaded method just for the internet archive dataset
		- The first two params hold the block size and ratioSize respectively (for all the runnings)
		- The last set of params are the actual file in byte and the hashed versions of the file we will be running the code against
	*/
	private static void startCDC(double [] block_size_list, double [] ratio_size_list,byte[] array1,byte[] array2,
	 ArrayList<Long> md5Hashes1,ArrayList<Long> md5Hashes2 ) throws Exception{
		int index = 0; // used to traverse the two lists
		for (int i = startBoundary;i<=endBoundary;i+=increment)
		{		
			maxBoundary = 4*i;
			int localBoundary = i;
			// System.out.print( i+" ");
			storeChunks(array1,md5Hashes1,localBoundary); // cut up the first file and store it
			run2min(array2,md5Hashes2,localBoundary); // call the method again, but on the second file only
			// this is the block size per boundary
			double blockSize = (double)totalSize/(double)numOfPieces;
			double ratio = (double)coverage/(double)totalSize;

			// extra step, add the data back into the list
			block_size_list[index] += blockSize;
			ratio_size_list[index] += ratio;
			++index;
			// clear the hashTable, and counters so we can reset the values for the next round of boundaries
			matches.clear();
			coverage = 0;
			numOfPieces = 0; 		
		}
	}



	/*
		- This method reads the file using bytes
		- This is where we run the 2min content dependent partitioning
	*/
	private static void runBytes(int localBoundary) throws IOException,Exception{
		storeChunks(fileArray.get(0),hashed_File_List.get(0),localBoundary); // cut up the first file and store it
		run2min(fileArray.get(1),hashed_File_List.get(1),localBoundary); // call the method again, but on the second file only		
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
		int missCounter = 0; // missCounter. Used for finding the second smallest
		int secondSmallest = -1; // this is the second smallest
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
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)){
					if (++missCounter >1) // remember we are allowed to miss once ( AKA second smallest)
						break;
				}
				/*-----------------------------------------------------------------------------
					We have reached the end. Meaning all the values within the range 
					(documentStart,Current) is either a second smallest or first smallest
				--------------------------------------------------------------------------------*/
				if (i == end && missCounter > 0)
					secondSmallest = current; // this is the second smallest
				if (i == end && missCounter == 0) // we have reached the end
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
					missCounter = 0; // reset the misCounter
					secondSmallest = -1; //reset the second smallest
					break;
				}
			} // end of for

			// if we have reached our maximum threshold
			// we will see if we have a second boundary, if yes, then make that the boundary
			// otherwise now we will make either the first minima or the second minima the boundary
			if ((current - documentStart + 1) >= maxBoundary){
				if (secondSmallest != -1){
					for (int j = documentStart; j <= secondSmallest;++j){
						builder.append(array[j]); 
					}
					String hash = MD5Hash.hashString(builder.toString(),"MD5"); // hash this boundary
					matches.put(hash,1); // simply insert the chunks in the hashtable
					documentStart = secondSmallest + 1;// set this as the beginning of the new boundary
					start = secondSmallest + 1;
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary; // this is the new end of the hash boundary
					builder.setLength(0); // reset the stringbuilder for the next round
					match = true; // so we don't increment our window values
					missCounter = 0; // reset the misCounter
					secondSmallest = -1; // reset the second smallest
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
		int missCounter = 0;
		int secondSmallest = -1; // this is the second smallest
		/* --------------------------------------------
			-- Loop throught and compare each value in the boundary 
			-- and find the boundaries

		----------------------------------------------*/
		while (end<md5Hashes.size())
		{ 
			for (int i = start; i <= end; ++i)
			{							
				if (i==current) // we don't want to compare with ourselves
					++i;	
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) {
					if (++missCounter > 1) // inrease the missCounter
						break; // we will break if the value at the current index is not a local minima
				}
				/*-----------------------------------------------------------------------------
					We have reached the end. Meaning all the values within the range 
					(documentStart,Current) is a boundary
				--------------------------------------------------------------------------------*/
				if (i == end && missCounter >0)
					secondSmallest = current;
				if (i == end && missCounter == 0)
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
					documentStart = current + 1;// set this as the beginning of the new boundary
					start = current + 1;
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary;
					builder.setLength(0); // reset the stringbuilder to get the next window
					match = true; //  so we don't increment our window again
					secondSmallest = -1;
					numOfPieces++; // we just got another boundary piece
					break;
				}
			} // end of for
			// if we have reached our maximum threshold
			// we will see if we have a second boundary, if yes, then make that the boundary
			// otherwise now we will make either the first minima or the second minima the boundary
			if (((current - documentStart + 1)) >= maxBoundary){
				if (secondSmallest != -1){
					for (int j = documentStart; j <= secondSmallest;++j){
						builder.append(array[j]); 
					}
					String hash = MD5Hash.hashString(builder.toString(),"MD5"); // hash this boundary
					if (matches.get(hash) != null){
						coverage+= secondSmallest-documentStart+1; // this is how much we saved
					}				
					documentStart = secondSmallest + 1;// set this as the beginning of the new boundary
					start = secondSmallest + 1;
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary; // this is the new end of the hash boundary
					builder.setLength(0); // reset the stringbuilder for the next round
					match = true; // so we don't increment our window values
					secondSmallest = -1; // reset the second smallest
					numOfPieces++; // we just got another boundary piece
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
			missCounter = 0;								
		} // end of the while loop
		// -------------------------------------------------------------------------------------------
		//  we are missing the last boundary, so hash that last value
		//	We will also check against our values of the strings we already have, and if we encountered this 
		//	already, then we will simply increment the counter, otherwise we will insert it in the hashtable
		//	and increase our miss counter
		//----------------------------------------------------------------------------------------------
		for (int j = documentStart; j < array.length;++j )
			builder.append(array[j]); 
		String hash = MD5Hash.hashString(builder.toString(),"MD5"); // hash our value
		if (matches.get(hash)!=null)
			coverage+=array.length - documentStart; // this is how much we saved. Dont need to add 1 cuz end it one past end anyway
		numOfPieces++; // we just got another boundary piece
	} // end of the method
}












