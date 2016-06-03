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
	//private String directory = "html1/";
	//private static String directory = "emacs/"; // this is the versioned set for emacs
	//private String directory = "sample/"; // this is used to test the validiy of my code
	//private String directory = "jdk/";
	//private String directory = "ny/";
	//private static String directory = "files/";
	//private static String directory = "javabook/";
	private static String directory = "gcc/";
	//private static String directory = "htmltar/";
	//private static String directory = "sublime/";
	
	// get the ratio of the coverage over the total size
	private static double totalSize=0;
	private static double coverage=0;
	private static int numOfPieces=0;
	private static int totalWindowPieces=0;
	private static int window;// window is size 3
	//private static int localBoundry; // size of how many elements this hash must be greater than/less than to be considered a boundary


	private static int numHashBoundariesAtEnd = 0; // used to keep track of how many times we went to the end
	private static int numHashBoundariesAtEndSecondTime = 0;
	private static int maxBoundary;
	private static int minBoundary;
	private static int boundaryDivisor = 4; // sets the minimum boundary divisor
	private static int smoothBoundary; // used to determine when we should smooth
	private static double smoothParam = .7; // smoothing param

	public static void main(String [] args) throws IOException, Exception
 	{
 		//directory = "morph.999001/";
 		String [] dir = {"morph.998001/","morph.99805/","morph.999001/"};
 		for (String s: dir){
 			directory = s;
 			System.out.println(directory);
 			readFile(directory);
 			driverRun();
 		}
 	// 	readFile(directory);
		// driverRun();

	
		//getBlockFrequency();
			//System.out.println("TESTIBG")
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
		hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
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
							//System.out.println("in here");
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


	private static void driverRun() throws IOException, Exception{
		//readDir(); // directories dont change
		// readFile(directory);
		//test();// test the code
		//PrintWriter write = new PrintWriter("output" + smoothParam + ".txt"); // write results ot the output
		System.out.println("i/2 misses " + directory + " " + "Choose second largest");
		double factor = 1.5;
		for (int i = 100;i<=1000;i*=1.2)
		{
			//System.out.print("Enter localBoundry:");
			
			// we will run the code from boundary from 2-window size
			// it will also run the code for window sizes upto the one inputted
			//localBoundry = in.nextInt();
			//smoothBoundary = i/2; // we will smooth the boundary when we reach here
			//minBoundary = 2*i;
			maxBoundary = 4*i; // if we miss this many times
			int localBoundary = i;
			window = 12; // set value
		/*--------------------------------------------------------------------------------------------
					-- Run the 2 min algorithm for all the way upto the value the user enters
					-- We will use the local boundary for all the way up to the value the user entered
		-------------------------------------------------------------------------------------------------*/
			System.out.print( localBoundary+" ");
			//write.print(localBoundary+" ");
			// run the 2min algorithm
			runBytes(localBoundary);
			// this is the block size per boundary
			double blockSize = (double)totalSize/(double)numOfPieces;
			double ratio = (double)coverage/(double)totalSize;
			//System.out.print("Coverage " + coverage + " Totalsize " + totalSize);
			//System.out.println( " block size: " + blockSize+ " ratio: "+ratio);
			//write.println(blockSize + " " + ratio); // write to the file
			System.out.println(blockSize + " " + ratio);
			//System.out.println(numHashBoundariesAtEnd + " " + numHashBoundariesAtEndSecondTime);

			// clear the hashTable, and counters so we can reset the values for the next round of boundaries
			matches.clear();
			coverage = 0;
			totalSize = 0;
			numOfPieces = 0;
			numHashBoundariesAtEnd = 0;
			numHashBoundariesAtEndSecondTime = 0;
			// if (i > 500)
			// 	i += 30;
			// else 
			// 	i *= factor;			
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
			//System.out.println(fileName);
			Path p = Paths.get(directory+fileName);

			// read the file
			byte [] array = Files.readAllBytes(p); // read the file in bytes
			//System.out.println(array.length);
			//System.out.println(fileName + "  " + array.length);
			int start = 0; // start of the sliding window
			int end = start + window - 1; // ending boundary
			hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
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
		-- Takes in four params: 
				1. array - this is the byte array that actually holds the document contents
				2. md5Hashes - will store the hash values of the entire document hashed
				3. Start - starting point of the hash window (most likely 0)
				4. End - ending point of the hash window 
		-- We are hashing the while document here
		-- We hash the document using a sliding window
		-- We will compute the md5Hash and only store the lower 32 bits (4bytes each)
	-------------------------------------------------------------------------------------------------------- */
	private static void hashDocument(byte [] array, ArrayList<Long> md5Hashes, int start, int end ){

		StringBuilder builder = new StringBuilder(); // used as a sliding window and compute the hash value of each window
		// only store the lower 32 bits of the md5Hash
		while (end < array.length)
		{
			for (int i = start; i <= end;++i){
				builder.append(array[i]);  // store the byte in a stringbuilder which we will use to compute hashvalue
			}		
			String hash = hashString(builder.toString(),"MD5"); // compute the hash value
			long val = Long.parseLong(hash.substring(24),16); // compute the int value of the lower 32 bits
			md5Hashes.add(val); // put the hash value
			start++; // increment the starting of the sliding window
			end++; // increment the ending of the sliding window
			builder.setLength(0); // to store the sum of the next window
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
	private static void storeChunks(byte [] array, ArrayList<Long> md5Hashes, int localBoundary){
		int start = 0; // starting point
		//System.out.println("TESTING" + localBoundary);
		//int tempBoundary = localBoundary; // this one will be modified for the smoothing
		int current = localBoundary;// has to be atlead here to be the local minima
		//System.out.println(localBoundary + " " + tempBoundary);
		int end  = localBoundary *2;  // this is the end of the boundary
		int documentStart = 0; // used to keep track of where the boundaries start from
		StringBuilder builder = new StringBuilder(); // this is used to store the original document content
		int missCounter = 0; // missCounter. Used for finding the second smallest
		int boundaryMisses = 0; // keep track of boundary misses
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
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) {
					boundaryMisses++; // missed a boundary
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
					// ck is this the 

					// Hash all the values in the range (documentStart,current)
					// Remember we only want to hash the original VALUES from the array that contains the original
					// content of the file. Not the hash values in the md5Hash Array
					for (int j = documentStart; j <= current;++j){
						builder.append(array[j]); 
					}
					String hash = hashString(builder.toString(),"MD5"); // hash this boundary
					//System.out.println(current-documentStart + 1);
					matches.put(hash,1); // simply insert the chunks in the hashtable
					documentStart = current + 1;// set this as the beginning of the new boundary
					start = current + 1;
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary; // this is the new end of the hash boundary
					builder.setLength(0); // reset the stringbuilder for the next round
					match = true; // so we don't increment our window values
					missCounter = 0; // reset the misCounter
					secondSmallest = -1; //reset the second smallest
					boundaryMisses = 0; // reset boundary miss
					break; // break out of the for loop
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
					String hash = hashString(builder.toString(),"MD5"); // hash this boundary
					//System.out.println(current-documentStart + 1);
					matches.put(hash,1); // simply insert the chunks in the hashtable
					documentStart = secondSmallest + 1;// set this as the beginning of the new boundary
					start = secondSmallest + 1;
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary; // this is the new end of the hash boundary
					builder.setLength(0); // reset the stringbuilder for the next round
					match = true; // so we don't increment our window values
					missCounter = 0; // reset the misCounter
					secondSmallest = -1; // reset the second smallest
					boundaryMisses = 0; // reset boundary misses
					//break; // break out of the for loop

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
			missCounter = 0; // reset the miss counter as well
								
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
		if (builder.length()> 0 ){
			String hash = hashString(builder.toString(),"MD5");
			//numHashBoundariesAtEnd+=builder.length();
			matches.put(hash,1); // simply insert the chunks in the document
			}
		else{
			System.out.println("Yolo");
		}

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
		int boundaryMisses = 0;

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
					boundaryMisses++;
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
					String hash = hashString(builder.toString(),"MD5"); // hash this boundary

					// Check if this value exists in the hash table
					// If it does, we will increment the coverage count
					if (matches.get(hash) != null){
						// byte [] arr = builder.toString().getBytes("UTF-8");
						// System.out.println(arr);
						coverage+= current-documentStart+1; // this is how much we saved
					}					
					documentStart = current + 1;// set this as the beginning of the new boundary
					start = current + 1;
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary;
					builder.setLength(0); // reset the stringbuilder to get the next window
					match = true; //  so we don't increment our window again
					missCounter = 0;
					secondSmallest = -1;
					numOfPieces++; // we just got another boundary piece
					boundaryMisses = 0;
					break; // break out of the for loop
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
					String hash = hashString(builder.toString(),"MD5"); // hash this boundary
					//System.out.println(current-documentStart + 1);
					if (matches.get(hash) != null){
						coverage+= secondSmallest-documentStart+1; // this is how much we saved
					}				
					documentStart = secondSmallest + 1;// set this as the beginning of the new boundary
					start = secondSmallest + 1;
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary; // this is the new end of the hash boundary
					builder.setLength(0); // reset the stringbuilder for the next round
					match = true; // so we don't increment our window values
					missCounter = 0; // reset the misCounter
					secondSmallest = -1; // reset the second smallest
					numOfPieces++; // we just got another boundary piece
					boundaryMisses = 0;

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

		for (int j = documentStart; j < array.length;++j ){
			builder.append(array[j]); 
		}
		if (builder.length() > 0){
			String hash = hashString(builder.toString(),"MD5"); // hash our value
			if (matches.get(hash)!=null)
				coverage+=array.length - documentStart; // this is how much we saved. Dont need to add 1 cuz end it one past end anyway
			numOfPieces++; // we just got another boundary piece
			numHashBoundariesAtEndSecondTime+= builder.length();
		}
		else
			System.out.println("In here yoloing");

	} // end of the method




/*-------------------------------------------------------------------------------------------------------------------------*/
// Everything below is the code for reading the file and hashing the string



	/*-------------------------------------------------------------------
		-- This function basically reads the file ( which is stored in the scanner) and reads it into the list
		-- All the white spaces are ommitted 

	-----------------------------------------------------------------------*/
	private static void readFile(Scanner in, ArrayList<String> list){

		while (in.hasNext()){
			String [] arr = in.nextLine().replaceAll("\\s+"," ").split(" "); // basically read the string, replace all whitespaces and split by each word
			for (String s: arr)
				if (!s.isEmpty())
					list.add(s); // only add it to the list if it's not empty
		}

		// testing purposes
		// for (String s: list)
		// 	System.out.println(s);
	}


	/*
	* reads all the files within this folder
	* @param folderName - This is the foldername that we will read all the files from
	*/
	private static void readFile(String folderName)
	{
		//File folder = new File(directory + folderName); //only needed for HTML directories
		File folder = new File(directory);
		File [] listOfFiles = folder.listFiles();

		// clear the fileList for the new files to be added in
		fileList.clear();

		for (File file : listOfFiles)
		{
			if (file.isFile())
			{
				fileList.add(file.getName());
				//System.out.println(file.getName());
			}
		}
	}

	/*
	* Finds all the directories that are in the folder ( these folders contain the actual html documents)
	*/
	private static void readDir()
	{
		File folder = new File(directory);
		File [] listOfFiles = folder.listFiles();
		folderList.clear(); // clear the list of directories

		for (File file:listOfFiles)
		{
			if (file.isDirectory())
			{
				folderList.add(file.getName());
				//System.out.println(file.getName());
			}
				
		}

	}




		// computes the md5
	// originally takes a string
	// we will just pass in the bytearray
	private static String hashString(String message, String algorithm) 
    {
 
	    try 
	    {
	   
	        MessageDigest digest = MessageDigest.getInstance(algorithm);
	        byte[] hashedBytes = digest.digest(message.getBytes("UTF-8"));
	
	 
	        return convertByteArrayToHexString(hashedBytes);
	    } 
	    catch (Exception ex) 
	    {
	        return null;
		}
	}

	// computes the md5
	// originally takes a string
	// we will just pass in the bytearray
	private static String hashString(byte [] message, String algorithm, int start, int end) 
    {
 
	    try 
	    {
	    	byte [] arr = new byte [end - start+1];
	    	int i = 0;
	    	while (start <= end)
	    		arr[i++] = message[start++];
	    	//System.out.println(arr.length + " " + end + " " + start);

	        MessageDigest digest = MessageDigest.getInstance(algorithm);
	        //byte[] hashedBytes = digest.digest(message.getBytes("UTF-8"));
	        byte [] hashedBytes = digest.digest(arr);
	 
	        return convertByteArrayToHexString(hashedBytes);
	    } 
	    catch (Exception ex) 
	    {
	        return null;
		}
	}

	private static String convertByteArrayToHexString(byte[] arrayBytes) 
	{
	    StringBuffer stringBuffer = new StringBuffer();
	    for (int i = 0; i < arrayBytes.length; i++) {
	        stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16)
	                .substring(1));
	    }
	    return stringBuffer.toString();
	}	


}












