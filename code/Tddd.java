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


	Tddd: This algorithm is similar to the karb Rabin content Dependent partioning algo. The karb rabin algorithim declares 
	 a hash value a cutpoint if the hash value mod d = q.
	Where d is the divisor and used to get the expected chunk lengths and q is the remainder that the value equals. TDDD has mulitple d's. 
	The mainDivisor works the same way as that for Karb Rabin. The secondDivisor is used only if the mainDivisor fails but the hash satisfying 
	this condition isn't immediately declared a cut-point. It is simiply stored. We could also have thirdDivisor, fourthDivisor etc

	Now we also have two more params. To prevent too small chunks, we have a minimum boundary size and can only start declaring chunks
	only after we have a chunk size greater than this value. Similarly, to prevent too big chunks, we have a max boudary size. Once we hit that, we check
	if we have a backup divisor (second, third etc) and use that to declare the cutpoint. If not, then we declare the current hash as the cut point 
*/


public class Tddd{
	private static HashMap<String,ArrayList<String>> table = new HashMap<String,ArrayList<String>>(); // store the actual strings

	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 
	private static ArrayList<String> folderList = new ArrayList<String>();
	private static String directory = "../../thesis-datasets/gcc/";	

	private static int window=12;// window size will be fixed around 12

	// get the ratio of the coverage over the total size
	private static double totalSize=0;
	private static double coverage=0;
	private static int numOfPieces=0;  // used to calculate block size

	// variables for the boundary size
	private static int startBoundary = 10; // start running the algo using this as the starting param
	private static int endBoundary = 100; // go all the way upto here
	private static int increment = 5; // increment in these intervals
	private static int min_multiplier = 2;
	private static int max_multiplier = 8; // two multipliers for min and max boundaries

	private static ArrayList< byte [] > fileArray = new ArrayList<byte[]>(); // holds both the file arrays
	private static ArrayList<ArrayList<Long>> hashed_File_List = new ArrayList<ArrayList<Long>>(); // used to hold the hashed file





	public static void main(String [] args) throws Exception
 	{

		//runPeriodic();
 		int [] min_arr = {2};
 		int [] max_arr = {8};
 		// run it for different min/max values
 		for (int i : min_arr){
 			for (int j:max_arr){
 				if (i < j){
 					//System.out.println("Min = " + i + " Max = " + j);
 					min_multiplier=i;
 					max_multiplier = j;
 					//getBlockFrequency();
 					//runOtherDataSets();
 					//runArchiveSet();
 					runPeriodic();
 					//runMorphDataSet();

 				}
 			}
 		}
 		// runPeriodic();
 		//runOtherDataSets();
	}

	/*
		-- This is a helper method to run the periodic dataset basically

	*/
	private static void runPeriodic() throws Exception {
		System.out.println("Running TDDD Periodic");
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
		
	}//end of methid
	/*
		-- This is a helper method run datasets such as emacs, gcc etc
	
	*/
	private static void runOtherDataSets() throws Exception{
		System.out.println("Running tddd " + directory);
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
	    	System.out.println("Running TDDD " + directory);
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
		- This method is used has a helper method to run the algo for the archive dataset
		- Note the archive set has multiple directories ( one for each url )
		- So Read all of the directories in first and for each directory run the code
	*/
	private static void runArchiveSet() throws Exception{

		System.out.println("Running TDDD archive");
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
			System.out.println(i + " " +i/2+1 + " " + i/4 +1 + " " + blockSize + " " + ratio);
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

	// this method basically will chop up the blocks and get their frequencies
	private static void getBlockFrequency() throws Exception{
		directory= "../../thesis-datasets/morph_file_100MB/";
		ReadFile.readFile(directory,fileList); // read the two files
		HashMap<Integer,Integer> blockFreq = new HashMap<Integer,Integer>(); // this stores the block in the map along there frequencies
		int start = 0; // start of the sliding window
		int end = start + window - 1; // ending boundary
		preliminaryStep(directory);
		//System.out.println("Choping the document TDDD " + fileList.get(0));
		long [] divisorArray = {1000}; // run the frequency code for these divisor values (AKA expected block Size)
		for (long i: divisorArray ){

			long divisor1 = i;
			long divisor2 =i/2;
			long divisor3 = i/4;
			long remainder =7;
			long minBoundary = min_multiplier*i;
			long maxBoundary = max_multiplier*i;
			//System.out.println("Running Likelihood for " + i + " " + divisor2 + " " + divisor3);
			int totalBlocks = chopDocument(fileArray.get(0),hashed_File_List.get(0),divisor1,divisor2,divisor3,remainder,minBoundary,maxBoundary,blockFreq);
			// now output the block sizes, along with there frequencies and probilities
			for (Map.Entry<Integer,Integer> tuple: blockFreq.entrySet()){
				// output the block freq
				double prob = (double)tuple.getValue() / (double)totalBlocks;
				System.out.println(tuple.getKey() + " " + tuple.getValue() + " " + prob);
			}

			blockFreq.clear();
		}
	}

	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. Divisor1/Divisor2 - main and back up divisor
			5. The remainder we are looking for
			6/7. min/max boundaries
			8. blockFreq - store the block sizes and how many time's they occur

		-- We are simply choping up the first file
	-------------------------------------------------------------------------------------------------------- */
	private static int chopDocument(byte[] array, ArrayList<Long> md5Hashes, long divisor1, long divisor2,long divisor3,long remainder
		,long minBoundary,long maxBoundary,HashMap<Integer,Integer> blockFreq){

	
		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		int backUpBreakPoint = -1; // used to store the backup breakpoint
		int secondBackUpBreakPoint = -1; // this is the second backup point with the divisor3
		StringBuilder builder = new StringBuilder();
		int counter = 0;
		int i = documentStart + (int)minBoundary-1 ; // so we start at the minimum
		// loop through all the values in the document
		for (; i < md5Hashes.size();++i)
		{ 	
			

			if (md5Hashes.get(i)%divisor1 == remainder) // ck if this equals the mod value
			{
				int size = i - documentStart + 1; // we only care about the size
				if (blockFreq.get(size) == null) // if not in there, then simply store it
						blockFreq.put(size,1); 
				else // increment it's integer count
					blockFreq.put(size,blockFreq.get(size)+1); // increment the count
				counter++; // increment the block count
				documentStart = i + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				secondBackUpBreakPoint = -1; // second backup point reset it!
				i = i + (int)minBoundary-1; // skip all the way here
			}		
			else if (md5Hashes.get(i)%divisor2 == remainder){ //  check if this is the backup point
				backUpBreakPoint = i; // this is the backup breakpoint
			}
			else if (md5Hashes.get(i)%divisor3 == remainder){
				secondBackUpBreakPoint = i; // we found a second backup point with divisor3
			}
			if ((i - documentStart + 1) >= maxBoundary ) { // we have reached the maximum
				// ck if we have a backUpbreakpoint
				int point;
				if (backUpBreakPoint != -1)// if we do, set this as the boundary
			    	point = backUpBreakPoint;
			    else if (secondBackUpBreakPoint != -1)
			    	point = secondBackUpBreakPoint; // if we don't have a first backup, ck if we have a second
			    else
			    	point = i; // else this current value of i is the breakpoint

			    // Hash all the values in the range (documentStart,point
				// Remember we only want to hash the original VALUES from the array that contains the original
				// content of the file. Not the hash values in the md5Hash Array
				int size = point - documentStart + 1; // we only care about the size
				if (blockFreq.get(size) == null) // if not in there, then simply store it
						blockFreq.put(size,1); 
				else // increment it's integer count
					blockFreq.put(size,blockFreq.get(size)+1); // increment the count
				counter++; // increment the block count
				documentStart = point + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				secondBackUpBreakPoint = -1; // reset second backup break point
				i = point + (int)minBoundary-1; // so we start at the minimum
			}
								
		} // end of the for loop

		// -------------------------------------------------------------------------------------------
		//  we are missing the last boundary, so hash that last value
		//	We will also check against our values of the strings we already have, and if we encountered this 
		//	already, then we will simply increment the counter, otherwise we will insert it in the hashtable
		//	and increase our miss counter
		//----------------------------------------------------------------------------------------------
		int size = array.length - documentStart; //we start from the point
		if (blockFreq.get(size) == null) // if not in there, then simply store it
			blockFreq.put(size,1); 
		else // increment it's integer count
			blockFreq.put(size,blockFreq.get(size)+1); // increment the count
		return ++counter; // increment the block count
	} // end of the method





	private static void startCDC() throws IOException, Exception{
		long remainder = 7;
		for (int i = startBoundary;i<=endBoundary; i+=increment)
		{
			long minBoundary  = min_multiplier*i; // we will set the mod value as the minimum boundary
			long maxBoundary = max_multiplier*i; // we will set this as the maximum boundary
			long divisor1 = i; // this will be used to mod the results
			long divisor2 = i/2+1; // the backup divisor is half the original divisor
			long divisor3 = i/4+1;
			totalSize = fileArray.get(1).length; // note we only care about the size of the second file since that's the file we are measuring
			System.out.print( divisor1+" " + divisor2 + " " + divisor3 + " ");
			runBytes(window,divisor1,divisor2,divisor3,remainder,minBoundary,maxBoundary); // run the karb rabin algorithm
			// this is the block size per boundary
			double blockSize = (double)totalSize/(double)numOfPieces;
			double ratio = (double)coverage/(double)totalSize;
			System.out.println( blockSize+ " "+ratio);

			// clear the hashTable, and counters so we can reset the values for the next round of boundaries
			coverage = 0;
			numOfPieces = 0; 
			table.clear();	
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
		int index = 0; // used to traverse the two lists
		for (int i = startBoundary;i<=endBoundary;i+=increment)
		{			
			long minBoundary  = min_multiplier* i; // we will set the mod value as the minimum boundary
			long maxBoundary = max_multiplier*i; // we will set this as the maximum boundary
			long divisor1 = i; // this will be used to mod the results
			long divisor2 = i/2+1; // the backup divisor is half the original divisor
			long divisor3 = i/4+1;
			// System.out.print( i+" ");
			storeChunks(previous_array,previous_md5Hashes,divisor1,divisor2,divisor3,remainder,minBoundary,maxBoundary); // cut up the first file and store it
			runTddd(current_array,current_md5Hashes,divisor1,divisor2,divisor3,remainder,minBoundary,maxBoundary); // call the method again, but on the second file only
			// this is the block size per boundary
			double blockSize = (double)totalSize/(double)numOfPieces;
			double ratio = (double)coverage/(double)totalSize;

			// extra step, add the data back into the list
			block_size_list[index] += blockSize;
			ratio_size_list[index] += ratio;
			++index;
			// clear the hashTable, and counters so we can reset the values for the next round of boundaries
			coverage = 0;
			numOfPieces = 0; 
			table.clear();
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
	private static void runBytes(int window, long divisor1, long divisor2,long divisor3, long remainder,
		Long minBoundary,Long maxBoundary) throws Exception{

		storeChunks(fileArray.get(0),hashed_File_List.get(0),divisor1,divisor2,divisor3,remainder,minBoundary,maxBoundary);// here we run 2min, ck how similar the documents are to the one already in the system
		runTddd(fileArray.get(1),hashed_File_List.get(1),divisor1,divisor2,divisor3,remainder,minBoundary,maxBoundary);// here we run 2min, ck how similar the documents are to the one already in the system
	} // end of the function

	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. Divisor1/Divisor2/divisor3... - main and back up divisors
			5. The remainder we are looking for
			6/7. min/max boundaries

		-- We are simply choping up the first file
	-------------------------------------------------------------------------------------------------------- */
	private static void storeChunks(byte[] array, ArrayList<Long> md5Hashes, long divisor1, long divisor2,long divisor3, long remainder,
		long minBoundary,long maxBoundary){

		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		int backUpBreakPoint = -1; // used to store the backup breakpoint
		int secondBackUpBreakPoint = -1; // this is the second backup point with the divisor3
		StringBuilder builder = new StringBuilder();
		int i = documentStart + (int)minBoundary-1 ; // so we start at the minimum
		// loop through all the values in the document
		for (; i < md5Hashes.size();++i)
		{ 	
			// if ((i - documentStart + 1) < minBoundary ) //  if the size of this boundary is less than the min, continue looping
			// 	continue;
			/*-----------------------------------------------------------------
				- If the mod of this equals the modvalue we defined, then 
				- this is a boundary
			------------------------------------------------------------------*/ 

			if (md5Hashes.get(i)%divisor1 == remainder) // ck if this equals the mod value
			{
				// Hash all the values in the range (documentStart,current(i))
				// Remember we only want to hash the original VALUES from the array that contains the original
				// content of the file. Not the hash values in the md5Hash Array
				for (int j = documentStart; j <= i;++j){
					builder.append(array[j]); // store everything upto the current value
				}
				String original = builder.toString();
				HashClass.put_hash(original,table);
				documentStart = i + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				secondBackUpBreakPoint = -1; // second backup point reset it!
				i = i + (int)minBoundary-1; // skip all the way here
				builder.setLength(0); // reset the stringBuilder for the next round
			}		
			else if (md5Hashes.get(i)%divisor2 == remainder){ //  check if this is the backup point
				backUpBreakPoint = i; // this is the backup breakpoint
			}
			else if (md5Hashes.get(i)%divisor3 == remainder){
				secondBackUpBreakPoint = i; // we found a second backup point with divisor3
			}
			if ((i - documentStart + 1) >= maxBoundary ) { // we have reached the maximum
				// ck if we have a backUpbreakpoint
				int point;
				if (backUpBreakPoint != -1)// if we do, set this as the boundary
			    	point = backUpBreakPoint;
			    else if (secondBackUpBreakPoint != -1)
			    	point = secondBackUpBreakPoint; // if we don't have a first backup, ck if we have a second
			    else
			    	point = i; // else this current value of i is the breakpoint

			    // Hash all the values in the range (documentStart,point
				// Remember we only want to hash the original VALUES from the array that contains the original
				// content of the file. Not the hash values in the md5Hash Array
				for (int j = documentStart; j <= point;++j){
					builder.append(array[j]); // store everything upto the current value
				}
				String original = builder.toString();
				HashClass.put_hash(original,table); // iinsert the hash in the table
				documentStart = point + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				secondBackUpBreakPoint = -1; // reset second backup break point
				i = point + (int)minBoundary-1; // so we start at the minimum
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
		// only compute hash and insert into our hashtable only if the string buffer isn't empty

		String original = builder.toString();
		HashClass.put_hash(original,table); // iinsert the hash in the table
		
	} // end of the method



	/* -------------------------------------------------------------------------------------------------------
	This method:

		This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. Divisor1/Divisor2/divisor3... - main and back up divisors
			5. The remainder we are looking for
			6/7. min/max boundaries

		-- We will start running the karb rabin algorithm
		-- We will find the boundaries using mod values and once they equal the mod value we have stored
		-- we also have the divsor2/3 .. which are backup divisors. If we don't find a boundary by the divisor1 once we hit the maxBoundary
		-- we will see if we have one with divisor2, if not, then we will see if we have one with divisor3 and so on
		-- We will hash everything in that hash boundary and store it
	-------------------------------------------------------------------------------------------------------- */
	private static void runTddd(byte[] array, ArrayList<Long> md5Hashes, long divisor1, long divisor2,long divisor3, long remainder,
		long minBoundary,long maxBoundary){
		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		int backUpBreakPoint = -1; // used to store the backup breakpoint
		int secondBackUpBreakPoint = -1; // used with the divisor3
		StringBuilder builder = new StringBuilder();
		int i =  documentStart + (int)minBoundary-1 ; // so we start at the minimum
		// loop through all the values in the document
		for (; i < md5Hashes.size();++i)
		{ 	
			// if ((i - documentStart + 1) < minBoundary) //  if the size of this boundary is less than the min, continue looping
			// 	continue;
			/*-----------------------------------------------------------------
				- If the mod of this equals the modvalue we defined, then 
				- this is a boundary
			------------------------------------------------------------------*/ 

			if (md5Hashes.get(i)%divisor1 == remainder) // ck if this equals the mod value
			{
				// Hash all the values in the range (documentStart,current(i))
				// Remember we only want to hash the original VALUES from the array that contains the original
				// content of the file. Not the hash values in the md5Hash Array
				for (int j = documentStart; j <= i;++j){
					builder.append(array[j]); // store everything upto the current value
				}
				String original = builder.toString();
				// if the string is a perfect match ( hash and original string)
				if (HashClass.is_string_match(original,table)) // iinsert the hash in the table)
					coverage+= i - documentStart + 1; // this is the amount of bytes we saved
				documentStart = i + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				secondBackUpBreakPoint = -1;
				numOfPieces++; // increment the num of pieces
				i = i + (int)minBoundary-1; // skip all the way here
				builder.setLength(0); // reset the stringBuilder for the next round
			}		
			else if (md5Hashes.get(i)%divisor2 == remainder){ //  check if this is the backup point
				backUpBreakPoint = i; // this is the backup breakpoint
			}
			else if (md5Hashes.get(i)%divisor2 == remainder){
				secondBackUpBreakPoint = i; // set the second backup point
			}
			if ((i - documentStart + 1) >= maxBoundary ) { // we have reached the maximum
				// ck if we have a backUpbreakpoint
				int point;
				if (backUpBreakPoint != -1)// if we do, set this as the boundary
			    	point = backUpBreakPoint;
			    else if (secondBackUpBreakPoint != -1){
			    	point = secondBackUpBreakPoint; // if we don't have a first break point, find the second
			    }
			    else
			    	point = i; // else this current value of i is the breakpoint

			    // Hash all the values in the range (documentStart,current(i))
				// Remember we only want to hash the original VALUES from the array that contains the original
				// content of the file. Not the hash values in the md5Hash Array
				for (int j = documentStart; j <= point;++j){
					builder.append(array[j]); // store everything upto the current value
				}
				String original = builder.toString();
				if (HashClass.is_string_match(original,table))
					coverage+= point - documentStart + 1; // this is the amount of bytes we saved
				numOfPieces++; // increment the num of pieces
				documentStart = point + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				secondBackUpBreakPoint = -1; // reset the secondBackUp point
				i = point +(int)minBoundary-1; // skip all the way here ; 
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
		// only compute hash and insert into our hashtable only if the string buffer isn't empty
		String original = builder.toString();
		if (HashClass.is_string_match(original,table))
			coverage+= array.length - documentStart ; // this is the amount of bytes we saved
		numOfPieces++; // increment the num of pieces
		
	} // end of the method

}












