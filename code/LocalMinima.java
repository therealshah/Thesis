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

public class LocalMinima{
			

	//private static HashMap<String,Integer> matches = new HashMap<String,Integer>();
	private static HashMap<String,ArrayList<String>> table = new HashMap<String,ArrayList<String>>(); // store the actual strings

	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 
	//private static String directory = "../thesis-datasets/gcc/";
	//private static String directory = "../thesis-datasets/gcc/";

	private static String directory = "../../thesis-datasets/morph/morph_.95_.10/";	
	// get the ratio of the coverage over the total size
	private static double totalSize=0;
	private static double coverage=0;
	private static int numOfPieces=0;
	private static int window = 12;

	// variables for the boundary size
	private static int startBoundary = 10; // start running the algo using this as the starting param
	private static int endBoundary = 200; // go all the way upto here
	private static int increment = 10; // increment in these intervals

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
		-- This is a helper method to run the periodic dataset basically

	*/
	private static void runPeriodic() throws Exception {
		System.out.println("Running 2min Periodic");
		// this is alll the directories we will be running 
		int arr []  = {10,15,20,25,30}; // this is the input number we will be running on
		// this is the base of the two files
		// these two are directories, we will concanate with the numbers to get the full dir name
		String base_old_file = "../../thesis-datasets/input_";
		String base_new_file = "../../thesis-datasets/periodic_";	

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
				//break;

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
		-- This is a helper methid to run the morph files
	*/
	private static void runMorphDataSet() throws Exception{

		String morph_directory = "../../thesis-datasets/morph/"; // directory where all the morph code is stored
		File d = new File(morph_directory);
	    // get all the files from a directory
	    File[] fList = d.listFiles();
	    List<String> dir_list = new ArrayList<String>();
	    for (File file : fList) {
	        if (file.isDirectory()) {
	            dir_list.add(file.getName());
	        }
	    }
	    for (String dir : dir_list){
	    	directory = morph_directory + dir + "/";
	    	System.out.println("Running 2min " + directory);
			ReadFile.readFile(directory,fileList); // read the two files
			System.out.println(fileList.get(0) + " " + fileList.get(1));
			preliminaryStep(directory);
		 	startCDC();
		 	fileList.clear();
		 	fileArray.clear();
		 	hashed_File_List.clear();
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
		for (int i = 0; i < fileList.size(); ++i){
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

	// this method basically will chop up the blocks and get their frequencies
	private static void getBlockFrequency() throws Exception{
		ArrayList<Long> md5Hashes = new ArrayList<Long>(); // store md5Hases
		HashMap<Integer,Integer> blockFreq = new HashMap<Integer,Integer>(); // this stores the block in the map along there frequencies
		System.out.println(fileList.get(0));
		Path p = Paths.get(directory + fileList.get(0)); // get the path of the file, there is only one file
		byte [] array = Files.readAllBytes(p); // read the file into a byte array
		int start = 0; // start of the sliding window
		int end = start + window - 1; // ending boundary
		HashDocument.hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
		
		int localBoundary = 500;
		//System.out.println("Running Likelihood for " + localBoundary);
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
			for (int i = start; i <= end; ++i) // loop through each of the values in this boundary
			{							
				if (i == current) // we are looking for strictly less than, so we don't want to compare with ourselve
					++i; // we don't wanna compare withourselves		
				// CompareTo returns
					// >0 if greater
					// <0 if less than
					// 0 if equal
				// 	// break if this isnt the smallest one
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) // less than or equal to
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
		- This method is used has a helper method to run the algo for the archive dataset
		- Note the archive set has multiple directories ( one for each url )
		- So Read all of the directories in first and for each directory run the code
	*/
	private static void runArchiveSet() throws Exception{

		System.out.println("Running LocalMinima archive");
		directory = "../../thesis-datasets/datasets2/";
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

		//System.out.println(Arrays.toString(directory_list));
		int sets = 0;
		// make the arrays to hold the respecitve info for the different verions\
		// run it simulateounsly to speed the from the program!
		double [] block_size_list_last_year = new double [total_iter_count];
		double [] ratio_size_list_last_year = new double [total_iter_count];	

		double [] block_size_list_six_month = new double [total_iter_count];
		double [] ratio_size_list__six_month = new double [total_iter_count];

		double [] block_size_list_two_year = new double [total_iter_count];
		double [] ratio_size_list_two_year = new double [total_iter_count];	

		int current = 0;
		int six_month = 2;
		int last_year = 1;
		int two_year = 3;
		// loop through and run the cdc for each directory
		for (String dir : directory_list){

			ReadFile.readFile(directory+ dir,fileList); // read all the files in this directory
			preliminaryStep(directory+ dir + "/"); // call the preliminaryStep on all the files
			
			totalRuns++;

			
			totalSize = fileArray.get(current).length; // get the length of the file we will be running it against!
			
			// run it against six month
			startCDC(block_size_list_six_month,ratio_size_list__six_month,fileArray.get(current),fileArray.get(six_month),hashed_File_List.get(current),hashed_File_List.get(six_month));
			
			// run it against last year
			startCDC(block_size_list_last_year,ratio_size_list_last_year,fileArray.get(current),fileArray.get(last_year),hashed_File_List.get(current),hashed_File_List.get(last_year));

			// run it against 2
			startCDC(block_size_list_two_year,ratio_size_list_two_year,fileArray.get(current),fileArray.get(two_year),hashed_File_List.get(current),hashed_File_List.get(two_year));

			// // clear the fileList and hashed_file_list array
			fileArray.clear();
			hashed_File_List.clear();
			fileList.clear();

			// if (Double.isNaN(ratio_size_list[0])){
			// 	System.out.println(sets+" "+Arrays.toString(ratio_size_list));
			// 	test = true;
			// 	break;
			// }
			if (sets % 200 == 0)
				System.out.println(sets);
			++sets;
		} // end of directory list for loop


		// now output the avged value for all the runs
		//System.out.println(Arrays.toString(ratio_size_list));
		System.out.println("Printing six_month");
		int index = 0;
		for (int i = startBoundary;i<=endBoundary;i+=increment){
			// avg out the outputs
			double blockSize = block_size_list_six_month[index]/(double)totalRuns;
			double ratio = ratio_size_list__six_month[index]/(double)totalRuns;
			System.out.println(i + " " + blockSize + " " + ratio);
			index++;
		}
		System.out.println("Printing last year");
		index = 0;
		for (int i = startBoundary;i<=endBoundary;i+=increment){
			double blockSize = block_size_list_last_year[index]/(double)totalRuns;
			double ratio = ratio_size_list_last_year[index]/(double)totalRuns;
			System.out.println(i + " " + blockSize + " " + ratio);
			index++;
		}

		System.out.println("Printing two year");
		index = 0;
		for (int i = startBoundary;i<=endBoundary;i+=increment){
			double blockSize = block_size_list_two_year[index]/(double)totalRuns;
			double ratio = ratio_size_list_two_year[index]/(double)totalRuns;
			System.out.println(i + " " + blockSize + " " + ratio);
			index++;
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
			totalSize = fileArray.get(1).length; // note we only care about the size of the second file since that's the file we are measuring

			// this is the block size per boundary
			double blockSize = (double)totalSize/(double)numOfPieces;
			double ratio = (double)coverage/(double)totalSize;
			System.out.println(blockSize + " " + ratio );
			// clear the hashTable, and counters so we can reset the values for the next round of boundaries
			// matches.clear();
			table.clear();
			coverage = 0;
			numOfPieces = 0;
			HashClass.duplicate_counter = 0;
			HashClass.max_list_length = 0;
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
			run2min(current_array,current_md5Hashes,localBoundary); // call the method again, but on the second file only
			// this is the block size per boundary
			double blockSize = (double)totalSize/(double)numOfPieces;
			double ratio = (double)coverage/(double)totalSize;
			if (ratio == 1){
				System.out.println("We have found same file!!!!!!!");
			}
			//System.out.println(ratio);
			// extra step, add the data back into the list
			block_size_list[index] += blockSize;
			ratio_size_list[index] += ratio;
			//System.out.println(Arrays.toString(ratio_size_list));

			++index;
			// clear the hashTable, and counters so we can reset the values for the next round of boundaries
			// matches.clear();
			table.clear();
			coverage = 0;
			numOfPieces = 0; 		
			HashClass.duplicate_counter = 0;
			HashClass.max_list_length = 0;
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
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) // less than or equal to
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
					String original = builder.toString();
					HashClass.put_hash(original,table); // iinsert the hash in the table
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

		// loop through the end of our array and hash the final boundaries
		for (int j = documentStart; j < array.length;++j ){
			builder.append(array[j]); 
		}
		String original = builder.toString();
		HashClass.put_hash(original,table); // iinsert the hash in the table
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
	private static void run2min(byte [] array, ArrayList<Long> md5Hashes, int localBoundry) throws Exception{
		int start = 0; // starting point
		int current = localBoundry;// has to be atlead here to be the local minima
		int end  = localBoundry *2;  // this is the end of the window
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

				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) // less than or equal to
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
					// String hash = MD5Hash.hashString(builder.toString(),"MD5"); // hash this boundary
					String original = builder.toString();
					// Check if this value exists in the hash table
					// If it does, we will increment the coverage count
					// if the string is a perfect match ( hash and original string)
					if (HashClass.is_string_match(original,table))
						coverage+= current - documentStart + 1; // this is the amount of bytes we saved		
					documentStart = current + 1;// set this as the beginning of the new boundary
					current = end+ 1; // this is where we start finding the new local minima
					start = documentStart; // we will start comparing from here!, since everything before this is a boundary
					end = current + localBoundry; // this is the new boundary
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
		String original = builder.toString();
		// if the string is a perfect match ( hash and original string)
		if (HashClass.is_string_match(original,table))
			coverage+= array.length - documentStart; // this is the amount of bytes we saved
		numOfPieces++; // increment the num of pieces
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
		long l_val = md5Hashes.get(l_min);
		long r_val = md5Hashes.get(r_min);
		/*--------------------------------------------------
			-- Now we run the window over and compute the value
			-- in each window and store in hash table
		----------------------------------------------------*/
		while (end<md5Hashes.size()) // loop through till we hit the end of the array
		{ 
			// now check the new value that was just slides in ( we incremented current so we compare the value that was just slided in, as in current -1)
			if (!(md5Hashes.get(l_min).compareTo(md5Hashes.get(current-1)) < 0)){
				l_min = current-1; // this is the new l_min
				l_val = md5Hashes.get(l_min);
			}
				// ck of l_min and r_min are valid ( as in are within the boundary range)
			else if (!(l_min >= start && l_min < current)){
				l_min = findMin(start,current-1,md5Hashes); // find new min
				l_val = md5Hashes.get(l_min);
			}
			// compare r_min to the new value that was just slided in , as in the end value
			if (!(md5Hashes.get(r_min).compareTo(md5Hashes.get(end)) < 0)){
				r_min = end; // this is the new l_min
				r_val = md5Hashes.get(r_min);
			}
			else if (!(r_min > current)){
				r_min = findMin(current+1,end,md5Hashes);
				r_val = md5Hashes.get(r_min);
			}		
					
								
		
			/*-----------------------------------------------------------------------------
				 if current is the minimum, we have a boundary
			--------------------------------------------------------------------------------*/
			if (md5Hashes.get(current).compareTo(Math.min(r_val,l_val)) < 0)
			{
				for (int j = documentStart; j <= current;++j){
					builder.append(array[j]); 
				}
				String original = builder.toString();
				HashClass.put_hash(original,table); // iinsert the hash in the table
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
		String original = builder.toString();
		HashClass.put_hash(original,table); // iinsert the hash in the tableent
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

		long l_val = md5Hashes.get(l_min);
		long r_val = md5Hashes.get(r_min);
		/*--------------------------------------------------
			-- Now we run the window over and compute the value
			-- in each window and store in hash table
		----------------------------------------------------*/
		while (end<md5Hashes.size()) // loop through till we hit the end of the array
		{ 
			// now check the new value that was just slides in ( we incremented current so we compare the value that was just slided in, as in current -1)
			if (!(md5Hashes.get(l_min).compareTo(md5Hashes.get(current-1)) < 0)){
				l_min = current-1; // this is the new l_min
				l_val = md5Hashes.get(l_min);
			}
				// ck of l_min and r_min are valid ( as in are within the boundary range)
			else if (!(l_min >= start && l_min < current)){
				l_min = findMin(start,current-1,md5Hashes); // find new min
				l_val = md5Hashes.get(l_min);
			}
			// compare r_min to the new value that was just slided in , as in the end value
			if (!(md5Hashes.get(r_min).compareTo(md5Hashes.get(end)) < 0)){
				r_min = end; // this is the new l_min
				r_val = md5Hashes.get(r_min);
			}
			else if (!(r_min > current)){
				r_min = findMin(current+1,end,md5Hashes);
				r_val = md5Hashes.get(r_min);
			}		
					

			if (md5Hashes.get(current).compareTo(Math.min(r_val,l_val)) < 0)
			{
				for (int j = documentStart; j <= current;++j){
					builder.append(array[j]); 
				}
				String original = builder.toString();

				if (HashClass.is_string_match(original,table))
					coverage+= current - documentStart + 1; // this is the amount of bytes we saved

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
		String original = builder.toString();
		if (HashClass.is_string_match(original,table))
			coverage+= array.length-documentStart; // this is how much we saved
	} // end of the method

}
