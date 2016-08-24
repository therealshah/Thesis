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


	Karb-Rabin: This algorithm is a very simply content dependant chunking algorithm. A hash value is declared a cutpoint if the hash value mod d = q.
	Where d is the divisor and used to get the expected chunk lengths and q is the remainder that the value equals. Note we are comparing two files
*/

public class NewRabin{

	private static HashSet<String> table = new HashSet<String>(); // store the actual strings
	private static int duplicate_counter = 0; // count # of duplicates for the hash ( testing purposes)
	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>();
	//private static String directory = "../thesis/emacs/";
	private static String directory = "../../thesis-datasets/gcc/";	
	//private static String directory = "../thesis/periodic/";

 	//private static String directory = "../thesis/nytimes/";

	private static int window = 12;// window size will be fixed around 12
	private static int localBoundry; // size of how many elements this hash must be greater than/less than to be considered a boundary

	// get the ratio of the coverage over the total size
	private static double totalSize=0;
	private static double coverage=0;
	private static int numOfPieces=0;  // used to calculate block size

	// variables for the boundary size
	private static int startBoundary = 100; // start running the algo using this as the starting param
	private static int endBoundary = 1000; // go all the way upto here
	private static int increment = 50; // increment in these intervals
	private static long divisor2;

	private static ArrayList< byte [] > fileArray = new ArrayList<byte[]>(); // holds both the file arrays
	private static ArrayList<ArrayList<Long>> hashed_File_List = new ArrayList<ArrayList<Long>>(); // used to hold the hashed file




	public static void main(String [] args) throws Exception
 	{

		//runPeriodic();
		//runArchiveSet();
		runOtherDataSets();
		//runMorphDataSet();
		//getBlockFrequency();
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
	}

	/*

	/*
		-- This is a helper method run datasets such as emacs, gcc etc
	
	*/
	private static void runOtherDataSets() throws Exception{
		System.out.println("Running KR " + directory);
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
	    	System.out.println("Running KR " + directory);
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
			HashDocument.hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
			
			// add the fileArray and hashedFile to our lists so we can use them later to run the algorithms
			// note we hash and read file before, so we don't have to do it again
			fileArray.add(array);
			hashed_File_List.add(md5Hashes);


		}
	}


	/*
		- This method is used has a helper method to run the algo for the archive dataset
		- Note the archive set has multiple directories ( one for each url )
		- So Read all of the directories in first and for each directory run the code
	*/
	private static void runArchiveSet() throws Exception{

		System.out.println("Running KarbRabin archive");
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


	// this method basically will chop up the blocks and get their frequencies
	private static void getBlockFrequency() throws Exception{
		directory = "../../thesis-datasets/morph_file_100MB/";
		ReadFile.readFile(directory,fileList); // read the two files
		preliminaryStep(directory);
		//System.out.println("Choping the document KR " + fileList.get(0));
		HashMap<Integer,Integer> blockFreq = new HashMap<Integer,Integer>(); // this stores the block in the map along there frequencies
		int start = 0; // start of the sliding window
		int end = start + window - 1; // ending boundary
		long remainder = 7;
		
		long [] divisorArray = {1000}; // run the frequency code for these divisor values (AKA expected block Size)
		for (long divisor: divisorArray ){
			//System.out.println("Running Likelihood for " + divisor);
			int totalBlocks = chopDocument(fileArray.get(0),hashed_File_List.get(0),blockFreq,divisor,remainder);
			// now output the block sizes, along with there frequencies and probilities
			for (Map.Entry<Integer,Integer> tuple: blockFreq.entrySet()){
				// output the block freq
				double prob = (double)tuple.getValue() / (double)totalBlocks; // calculating the prob
				System.out.println(tuple.getKey() + " " + tuple.getValue() + " " + prob);
		
			}
			//md5Hashes.clear();
			blockFreq.clear();
		}
	}



	/*-----------------------------------------------------------------------------------------------
	This method:
		--	@param:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. blockFreq - Store the block sizes, along with there frequencies
			4. Divisor - the value we will be dividing by 
			5. This determines if this is a boundary point for the docu
			@return: - returns the total number of block chunks

		-- We are simply finding how the document is chopped up using this winnowing
	-------------------------------------------------------------------------------------------------------- */
	private static int chopDocument(byte [] array, ArrayList<Long> md5Hashes,HashMap<Integer,Integer> blockFreq, Long divisor, Long remainder ){
		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		int counter = 0;
		// loop through all the values in the document
		for (int i = 0; i < md5Hashes.size();++i)
		{ 	
			/*-----------------------------------------------------------------
				- If the mod of this equals the modvalue we defined, then 
				- this is a boundary
			------------------------------------------------------------------*/ 
			if (md5Hashes.get(i)%divisor == remainder) // ck if this equals the mod value
			{
				int size = i - documentStart + 1; // we only care about the size
				if (blockFreq.get(size) == null) // if not in there, then simply store it
						blockFreq.put(size,1); 
				else // increment it's integer count
					blockFreq.put(size,blockFreq.get(size)+1); // increment the count
				counter++; // increment the block count
				documentStart = i + 1;// set this as the beginning of the new boundary
			}		
								
		} // end of the for loop

		// get the last block size
		int size = array.length - documentStart;
		if (blockFreq.get(size) == null) // if not in there, then simply store it
			blockFreq.put(size,1); 
		else // increment it's integer count
			blockFreq.put(size,blockFreq.get(size)+1); // increment the count
		return ++counter;

	} // end of the method


	/*
		- This is basically sets up everything and calls the actual contentDependant methods
	*/
	private static void startCDC() throws Exception{
		long remainder = 7; // this is the remainder that we will be comparing with
		long remainder2 = 0;
		for (int i = startBoundary;i<=endBoundary;i+=increment)
		{			
			long divisor = i;
			divisor2 = 2*i + 3;
			System.out.print( i+" ");
			runBytes(divisor,remainder,remainder2); // run the karb rabin algorithm
			// this is the block size per boundary
			totalSize = fileArray.get(1).length; // note we only care about the size of the second file since that's the file we are measuring
			double blockSize = (double)totalSize/(double)numOfPieces;
			double ratio = (double)coverage/(double)totalSize;
			System.out.println( blockSize+ " "+ratio);
			// clear the hashTable, and counters so we can reset the values for the next round of boundaries
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
		long remainder = 7; // this is the remainder that we will be comparing with
		long remainder2 = 2;
		int index = 0; // used to traverse the two lists
		for (int i = startBoundary;i<=endBoundary;i+=increment)
		{			
			long divisor = i;
			// System.out.print( i+" ");
			storeChunks(previous_array,previous_md5Hashes,divisor,remainder,remainder2); // cut up the first file and store it
			runKarbRabin(current_array,current_md5Hashes,divisor,remainder,remainder2); // call the method again, but on the second file only
			// this is the block size per boundary
			double blockSize = (double)totalSize/(double)numOfPieces;
			double ratio = (double)coverage/(double)totalSize;

			// System.out.println(Arrays.toString(block_size_list));

			// extra step, add the data back into the list
			block_size_list[index] += blockSize;
			ratio_size_list[index] += ratio;
			++index;
			// clear the hashTable, and counters so we can reset the values for the next round of boundaries
			table.clear(); // clear the table
			coverage = 0;
			numOfPieces = 0; 
			HashClass.duplicate_counter = 0; // reset the duplicate counter
			HashClass.max_list_length = 0;			
		}
	}




	/*
		Read in all the files and loop through all the files
		We already have the hashed version of the documents 
		First, we cut up the first document into chunks (using the CDC algorhtim) and store it
		Then we cut up the second document (usually a different version of the same document) and see how many chunks match
	*/
	private static void runBytes(long divisor,long remainder,long remainder2) throws Exception{
		storeChunks(fileArray.get(0),hashed_File_List.get(0),divisor,remainder,remainder2); // cut up the first file and store it
		runKarbRabin(fileArray.get(1),hashed_File_List.get(1),divisor,remainder,remainder2); // call the method again, but on the second file only
	} // end of the function


	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document

		-- We are simply finding the boundaries of the file using karbRabin and simply storing them. Nothing more!
	-------------------------------------------------------------------------------------------------------- */
	private static void storeChunks(byte[] array, ArrayList<Long> md5Hashes,long divisor,long remainder,long remainder2){

		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		StringBuilder builder = new StringBuilder();
		// loop through all the values in the document
		for (int i = 1; i < md5Hashes.size() - 1;++i)
		{ 	
			/*-----------------------------------------------------------------
				- If the mod of this equals the modvalue we defined, then 
				- this is a boundary
			------------------------------------------------------------------*/ 
			if ((md5Hashes.get(i-1)+md5Hashes.get(i) + md5Hashes.get(i+1))%divisor == remainder) // ck if this equals the mod value
			{
				// Hash all the values in the range (documentStart,current(i))
				// Remember we only want to hash the original VALUES from the array that contains the original
				// content of the file. Not the hash values in the md5Hash Array
				for (int j = documentStart; j <= i;++j){
					builder.append(array[j]); // store everything upto the current value
				}
				String original = builder.toString();
				HashClass.put_hash(original,table);
				//matches.put(hash,1); // simply storing the first document
				documentStart = i + 1;// set this as the beginning of the new boundary
				builder.setLength(0); // reset the stringBuilder for the next round
			}		
								
		} // end of the for loop

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
		HashClass.put_hash(original,table);
	} // end of the method



	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. divisor - the value we will be dividing by
			4. remainder - value we will be comparing the mod value to

		-- We will start running the karb rabin algorithm
		-- We will find the boundaries using mod values and once they equal the mod value we have stored
		-- We will hash everything in that hash boundary and store it
	-------------------------------------------------------------------------------------------------------- */
	private static void runKarbRabin(byte[] array, ArrayList<Long> md5Hashes,long divisor, long remainder,long remainder2){
		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		StringBuilder builder = new StringBuilder(); // used to hold the bytes
		for (int i = 1; i < md5Hashes.size()-1;++i) // loop through each of the values
		{ 	
			/*-----------------------------------------------------------------
				- If the mod of this equals the modvalue we defined, then 
				- this is a boundary
			------------------------------------------------------------------*/ 
			if ((md5Hashes.get(i-1)+md5Hashes.get(i)+md5Hashes.get(i+1))%divisor == remainder) // ck if this equals the mod value
			{
				// Hash all the values in the range (documentStart,current(i))
				// Remember we only want to hash the original VALUES from the array that contains the original
				// content of the file. Not the hash values in the md5Hash Array
				for (int j = documentStart; j <= i;++j){
					builder.append(array[j]); 
				}
				// check if this hash value exists, if not then add it
				String original = builder.toString();
				// if the string is a perfect match ( hash and original string)
				if (HashClass.is_string_match(original,table)) // iinsert the hash in the table)
					coverage+= i - documentStart + 1; // this is the amount of bytes we saved
				// if (matches.get(hash) != null) // ck if this boundary exists in the hash table
				// 	coverage+= i - documentStart + 1; // this is the amount of bytes we saved
				documentStart = i + 1;// set this as the beginning of the new boundary
				numOfPieces++; // increment the num of pieces
				builder.setLength(0); // reset the stringbuilder so we could re use it 
			}		
								
		} // end of the for loop

		// -------------------------------------------------------------------------------------------
		//  we are missing the last boundary, so hash that last value
		//	We will also check against our values of the strings we already have, and if we encountered this 
		//	already, then we will simply increment the counter, otherwise we will insert it in the hashtable
		//	and increase our miss counter
		//----------------------------------------------------------------------------------------------
		for (int j = documentStart; j < array.length;++j){ // hash the last bit of boundary that we may have left
			builder.append(array[j]); 
		}
		String original = builder.toString();
		// if the string is a perfect match ( hash and original string)
		if (HashClass.is_string_match(original,table)) // iinsert the hash in the table)
			coverage+= array.length - documentStart; // this is the amount of bytes we saved
		numOfPieces++; // increment the num of pieces

	} // end of the method

}












