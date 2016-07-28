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
	Next the cut points for the document are determined from the hash array, using a content dependant method.
	The original document is divided into chunks using the cut points as boundaries between the chunks. Different versions of the documents are
	using where the first chunks of the document are stored, whereas the second version is simply used to see of that portion of the document
	occurred.


	Winnowing: This algorithm is kinda similar to the local minima method. We have a parameter called w which is the window size which we slide. A cutpoint is
	determined by the following way, we find the minimum hash value within the w size and declare that as the boundary. When a new hash comes in,
	we compare with the smallest and if it is the smallest, that is a hash value. Note when the previous boundary slides out of the window, we
	find a new minimum hash value and declare that as the cutpoint.
*/


public class Winnowing{

	private static HashMap<String,Integer> matches = new HashMap<String,Integer>();

	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 

	//private static String directory = "../thesis/gcc/";
	// /private static String directory = "../thesis/emacs/";
	private static String directory = "../thesis-datasets/periodic_50/";	

	//private static String directory = "../thesis/emacs/";

	private static int window = 12;// window is size 3

	// get the ratio of the coverage over the total size
	private static double totalSize;
	private static double coverage=0;
	private static int numOfPieces=0;

	// variables for the boundary size
	private static int startBoundary = 100; // start running the algo using this as the starting param
	private static int endBoundary = 2000; // go all the way upto here
	private static int increment = 50; // increment in these intervals

	private static ArrayList< byte [] > fileArray = new ArrayList<byte[]>(); // holds both the file arrays
	private static ArrayList<ArrayList<Long>> hashed_File_List = new ArrayList<ArrayList<Long>>(); // used to hold the hashed file

	public static void main(String [] args) throws Exception
 	{

		//runPeriodic();
		runArchiveSet();
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
			4. blockFreq - Store the block sizes, along with there frequencies
			5. return type - returns the total number of block chunks

		-- We are simply finding how the document is chopped up using this winnowing
	-------------------------------------------------------------------------------------------------------- */
	private static int chopDocument(byte [] array, ArrayList<Long> md5Hashes, int localBoundary,HashMap<Integer,Integer> blockFreq ){
		int start = 0; // starting point
		int current = localBoundary - 1;// compare all the values at and before this one
		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		int counter = 0; // count the total num of blocks
		int prevBoundary = -1;
		// loop through until this current equals the end
		while (current<md5Hashes.size())
		{ 

			// if the prevBoundary is null or if it slided out, we will find the minimum within the range [start,current] and 
			// set that as the boundary
			if (prevBoundary == -1 || prevBoundary < start ){
				prevBoundary = findMin(start,current,md5Hashes); // get the min within this window
				match = true;
			}
			// else we have a valid prev boundary and compare that value with the new one we slided in (aka current)
			// if the new one is less than OR equal (AKA its not greater than the prevBoundary) to the prevBoundary, this new one will become the previous boundary
			else if (!(md5Hashes.get(current).compareTo(md5Hashes.get(prevBoundary)) > 0)){
				prevBoundary = current;
				match = true;
			}
			// we have found a boundary, so just hash it 
			if (match){
				int size = prevBoundary - documentStart + 1; // this is the size of this block freq
				if (size == 0){
					System.out.println(prevBoundary + " " + documentStart);
				}
				if (blockFreq.get(size) == null){ // if not in there, then simply store it}
					blockFreq.put(size,1); // simply insert the chunks in the document
					//System.out.println("in here");
				}
				else // increment it's integer count
					blockFreq.put(size,blockFreq.get(size)+1); // increment the count
				counter++; // increment the block count
				documentStart = prevBoundary + 1;// set this as the beginning of the new boundary
				match = false;
				//prevBoundary = -1;
			}
			start++;
			current++;						
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
		-- This is a helper method to run the periodic dataset basically

	*/
	private static void runPeriodic() throws Exception {
		System.out.println("Running KR Periodic");
		// this is alll the directories we will be running 
		int arr []  = {10,15,20,25,30}; // this is the input number we will be running on
		// this is the base of the two files
		// these two are directories, we will concanate with the numbers to get the full dir name
		String base_old_file = "../thesis-datasets/input_";
		String base_new_file = "../thesis-datasets/periodic_";	

		int total_iter_count = 0; // this is used check how many times we will iterate through the data so we can make an array of that size
		for (int i = startBoundary;i<=endBoundary;i+=increment)
			total_iter_count++;

		for (int dir_num : arr){
			// set up our directories

			String old_file_dir = base_old_file + dir_num + "/";
			String new_file_dir = base_new_file + dir_num +"/";
			System.out.println(old_file_dir);

			// read all the files in these two directories in sorted order
			ArrayList<String> old_file_list = new ArrayList<String>();
			ArrayList<String> new_file_list = new ArrayList<String>();

			ReadFile.readFile(old_file_dir,old_file_list);
			ReadFile.readFile(new_file_dir,new_file_list);


			// used to store all the runnings for the periodic data
			double [] block_size_list = new double [total_iter_count];
			double [] ratio_size_list = new double [total_iter_count];	
			int totalRuns = 0;

			for (int i = 0; i < old_file_list.size(); ++i){
				//System.out.println(old_file_list.get(i) + " " + new_file_list.get(i));
				String [] s1 = old_file_list.get(i).split("_");
				String [] s2 = new_file_list.get(i).split("_");
				// input file should corrospond to the output file
				if (!s1[1].equals(s2[1]) || !s1[2].equals(s2[2]) )
					System.out.println("We got a huge problem");

				// basically same code as in the prelinaryStep method, but we need to modify it for perdiodic files
				int start = 0; // start of the sliding window
				int end = start + window - 1; // ending boundary
				// we cant call preliminary function, so hash the two files individually 
				//System.out.println("preliminaryStep " + fileList.get(i));
				Path p = Paths.get(old_file_dir+old_file_list.get(i)); // read the old file ( the one which we will be using as the base comparason)
				Path p2 = Paths.get(new_file_dir+new_file_list.get(i)); // read the old file ( the one which we will be using as the base comparason)

				byte [] old_file = Files.readAllBytes(p); // read the file in bytes
				byte [] new_file = Files.readAllBytes(p2);
				//System.out.println(array.length);
				ArrayList<Long> old_file_hashes = new ArrayList<Long>(); // make a new arrayList for this document
				ArrayList<Long> new_file_hashes = new ArrayList<Long>(); // make a new arrayList for this document

				HashDocument.hashDocument(old_file,old_file_hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
				HashDocument.hashDocument(new_file,new_file_hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array

				// now call the startCdc method
				totalSize = new_file.length; // this is the length of the file
				startCDC(block_size_list,ratio_size_list,new_file,old_file,new_file_hashes,old_file_hashes);

				if (totalRuns % 10 == 0)
					System.out.println(totalRuns);
				totalRuns++;

			}

			// now output the results
			System.out.println("File dir = " + dir_num + " totalRuns = " +totalRuns);
			int index = 0;
			for (int i = startBoundary;i<=endBoundary;i+=increment){
				// avg out the outputs
				double blockSize = block_size_list[index]/(double)totalRuns;
				double ratio = ratio_size_list[index]/(double)totalRuns;
				System.out.println(i + " " + blockSize + " " + ratio);
				index++;
		}

			// now each index matches the corrosponding file
		}	
		// // run the dataset
		// for (String dir : periodic_directory){
		// 	directory = dir;
		// 	ReadFile.readFile(directory,fileList); // read the two files
		// 	System.out.println(fileList.get(0) + " " + fileList.get(1));
		// 	preliminaryStep(directory);
		//  	startCDC();
		//  	fileList.clear(); // clear the list
		//  	fileArray.clear(); // clear the array of files we have read in
		//  	hashed_File_List.clear(); // clear all the hashed files we have
		// }
		
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
			totalSize = fileArray.get(1).length; // note we only care about the size of the second file since that's the file we are measuring
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
			winnowing(current_array,current_md5Hashes,localBoundary); // call the method again, but on the second file only
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
		Read in all the files and loop through all the files
		We already have the hashed version of the documents 
		First, we cut up the first document into chunks (using the CDC algorhtim) and store it
		Then we cut up the second document (usually a different version of the same document) and see how many chunks match
	*/
	private static void readBytes(int localBoundary) throws Exception{
		totalSize = fileArray.get(1).length; // note we only care about the size of the second file since that's the file we are measuring
		storeChunks(fileArray.get(0),hashed_File_List.get(0),localBoundary); // cut up the first file and store it
		winnowing(fileArray.get(1),hashed_File_List.get(1),localBoundary); // call the method again, but on the second file only
	} // end of the function



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
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. localboundary - how big the neighborhood is

		-- We are simply finding the boundaries of the file using winnowing and simply storing them. Nothing more!
	-------------------------------------------------------------------------------------------------------- */
	private static void storeChunks(byte [] array, ArrayList<Long> md5Hashes, int localBoundary){
		int start = 0; // starting point
		int current = localBoundary - 1;// compare all the values at and before this one
		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		StringBuilder builder = new StringBuilder(); // this is used to store the original document content
		int prevBoundary = -1; // used to keep track of the previous boundary
		// loop through until this current equals the end
		while (current<md5Hashes.size())
		{ 
			// if the prevBoundary is null or if it slided out, we will find the minimum within the range [start,current] and 
			// set that as the boundary
			if (prevBoundary == -1 || prevBoundary < start ){
				prevBoundary = findMin(start,current,md5Hashes); // get the min within this window
				match = true;
			}
			// else we have a valid prev boundary and compare that value with the new one we slided in (aka current)
			// if the new one is less than OR equal (AKA its not greater than the prevBoundary) to the prevBoundary, this new one will become the previous boundary
			else if (!(md5Hashes.get(current).compareTo(md5Hashes.get(prevBoundary)) > 0)){
				prevBoundary = current;
				match = true;
			}
			// we have found a boundary, so just hash it 
			if (match){
				// Hash all the values in the range (documentStart,current)
				// Remember we only want to hash the original VALUES from the array that contains the original
				// content of the file. Not the hash values in the md5Hash Array
				for (int j = documentStart; j <= prevBoundary;++j){
					builder.append(array[j]);  // append the bytes to a string builder
				}
				String hash = MD5Hash.hashString(builder.toString(),"MD5"); // hash this boundary
				matches.put(hash,1); // simply insert the chunks in the document
				documentStart = prevBoundary + 1;// set this as the beginning of the new boundary
				builder.setLength(0); // reset the stringbuilder for the next round
				match = false;
				//break; // break out of the for loop
				// if the prev boundary is null or 
			}
			start++;
			current++;						
		} // end of the while loop

		// -------------------------------------------------------------------------------------------
		//  we are missing the last boundary, so hash that last value
		//	We will also check against our values of the strings we already have, and if we encountered this 
		//	already, then we will simply increment the counter, otherwise we will insert it in the hashtable
		//	and increase our miss counter
		//----------------------------------------------------------------------------------------------
		for (int j = documentStart; j < array.length;++j ){
			builder.append(array[j]);  // hash the last boundary
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

		-- We will start running the winnowing algorithim here
		-- We have a sliding window and find the local minima or local maxima within the document
		-- We have a hashTable where we store the values of the boundaries and compare to see if we have
		-- already seen this
		-- we also keep track of a counter and misscounter, which we use to compute the ratio
	-------------------------------------------------------------------------------------------------------- */
	private static void winnowing(byte [] array, ArrayList<Long> md5Hashes, int localBoundary){
		int start = 0; // starting point
		int current = localBoundary - 1;// this is the end of the boundary
		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		StringBuilder builder = new StringBuilder(); // used to create the boundaries from the original file
		int prevBoundary = -1; // used to keep track of the previous boundary

		while (current<md5Hashes.size())
		{ 
			// if the prevBoundary is null or if it slided out, we will find the minimum within the range [start,current] and 
			// set that as the boundary
			if (prevBoundary == -1 || prevBoundary < start ){
				prevBoundary = findMin(start,current,md5Hashes); // get the min within this window
				match = true;
			}
			// else we have a valid prev boundary and compare that value with the new one we slided in (aka current)
			// if the new one is less than OR equal (AKA its not greater than the prevBoundary) to the prevBoundary, this new one will become the previous boundary
			else if (!(md5Hashes.get(current).compareTo(md5Hashes.get(prevBoundary)) > 0)){
				prevBoundary = current;
				match = true;
			}
			// we have found a boundary, so just hash it 
			if (match){
				// Hash all the values in the range (documentStart,current)
				// Remember we only want to hash the original VALUES from the array that contains the original
				// content of the file. Not the hash values in the md5Hash Array
				for (int j = documentStart; j <= prevBoundary;++j){
					builder.append(array[j]);  // append the bytes to a string builder
				}
				String hash = MD5Hash.hashString(builder.toString(),"MD5"); // hash this boundary
				if (matches.get(hash)!= null)
					coverage += prevBoundary - documentStart + 1; // we have saved this much of the document
				documentStart = prevBoundary + 1;// set this as the beginning of the new boundary
				builder.setLength(0); // reset the stringbuilder for the next round
				match = false;
				numOfPieces++; //  we got another boundary
			}
			start++;
			current++;																	
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
		String hash = MD5Hash.hashString(builder.toString(),"MD5");
		if (matches.get(hash)!=null)
			coverage+=array.length - documentStart; // this is how much we saved. Dont need to add 1 cuz end it one past end anyway
		numOfPieces++; // we just got another boundary piece
		 			
	} // end of the method
}












