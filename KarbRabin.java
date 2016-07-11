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

public class KarbRabin{

	private static HashMap<String,Integer> matches = new HashMap<String,Integer>();

	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>();
	//private static String directory = "../thesis/emacs/";
	private static String directory = "../thesis/datasets/";
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


	private static ArrayList< byte [] > fileArray = new ArrayList<byte[]>(); // holds both the file arrays
	private static ArrayList<ArrayList<Long>> hashed_File_List = new ArrayList<ArrayList<Long>>(); // used to hold the hashed file




	public static void main(String [] args) throws Exception{

		System.out.println("Running KarbRabin " + directory);
		//ReadFile.readFile(directory,fileList); // read the two files
		// System.out.println(fileList.get(0) + " " + fileList.get(1));
		// preliminaryStep(directory);
	 // 	startCDC();
		runArchiveSet();
		//getBlockFrequency();
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

		//System.out.println(Arrays.toString(directory_list));
		int sets = 0;
		// make the arrays to hold the respecitve info for the different verions\
		// run it simulateounsly to speed the from the program!
		double [] block_size_list_last_month = new double [total_iter_count];
		double [] ratio_size_list_last_month = new double [total_iter_count];	

		double [] block_size_list_last_week = new double [total_iter_count];
		double [] ratio_size_list_last_week = new double [total_iter_count];

		double [] block_size_list_last_year = new double [total_iter_count];
		double [] ratio_size_list_last_year = new double [total_iter_count];	


		//0 - Last_month
		//1- current
		//2-last_year
		//3 - last_week	
		int current = 1;
		int last_month = 0;
		int last_week = 3;
		int last_year = 2;
		// loop through and run the cdc for each directory
		for (String dir : directory_list){
			// We have 4 files in each directory
			// current, last_week, last_month, last_year
			// read all the files in the directory
			//System.out.println(dir);

			ReadFile.readFile(directory+ dir,fileList); // read all the files in this directory
			preliminaryStep(directory+ dir + "/"); // call the preliminaryStep on all the files
			
			totalRuns++;

			
			// run it against last week
			totalSize = fileArray.get(last_week).length; // get the length of the file we will be running it against!
			startCDC(block_size_list_last_week,ratio_size_list_last_week,fileArray.get(current),fileArray.get(last_week),hashed_File_List.get(current),hashed_File_List.get(last_week));
			
			// run it against last month
			totalSize = fileArray.get(last_month).length; // get the length of the file we will be running it against!
			startCDC(block_size_list_last_month,ratio_size_list_last_month,fileArray.get(current),fileArray.get(last_month),hashed_File_List.get(current),hashed_File_List.get(last_month));

			// run it against last year
			totalSize = fileArray.get(last_year).length; // get the length of the file we will be running it against!
			startCDC(block_size_list_last_year,ratio_size_list_last_year,fileArray.get(current),fileArray.get(last_year),hashed_File_List.get(current),hashed_File_List.get(last_year));

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
		System.out.println("Printing last weeks");
		int index = 0;
		for (int i = startBoundary;i<=endBoundary;i+=increment){
			// avg out the outputs
			double blockSize = block_size_list_last_week[index]/(double)totalRuns;
			double ratio = ratio_size_list_last_week[index]/(double)totalRuns;
			System.out.println(i + " " + blockSize + " " + ratio);
			index++;
		}
		System.out.println("Printing last month");
		index = 0;
		for (int i = startBoundary;i<=endBoundary;i+=increment){
			double blockSize = block_size_list_last_month[index]/(double)totalRuns;
			double ratio = ratio_size_list_last_month[index]/(double)totalRuns;
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
	}


	// this method basically will chop up the blocks and get their frequencies
	private static void getBlockFrequency() throws Exception{
		ArrayList<Long> md5Hashes = new ArrayList<Long>(); // store md5Hases
		HashMap<Integer,Integer> blockFreq = new HashMap<Integer,Integer>(); // this stores the block in the map along there frequencies
		Path p = Paths.get(directory + fileList.get(0)); // get the path of the file, there is only one file
		byte [] array = Files.readAllBytes(p); // read the file into a byte array
		int start = 0; // start of the sliding window
		int end = start + window - 1; // ending boundary
		long remainder = 7;
		HashDocument.hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
		
		long [] divisorArray = {500,1000}; // run the frequency code for these divisor values (AKA expected block Size)
		for (long divisor: divisorArray ){
			System.out.println("Running Likelihood for " + divisor);
			int totalBlocks = chopDocument(array,md5Hashes,blockFreq,divisor,remainder);
			// now output the block sizes, along with there frequencies and probilities
			for (Map.Entry<Integer,Integer> tuple: blockFreq.entrySet()){
				// output the block freq
				double prob = (double)tuple.getValue() / (double)totalBlocks;
				System.out.println(tuple.getKey() + " " + tuple.getValue() + " " + prob);
		
			}
			md5Hashes.clear();
			blockFreq.clear();
		}
	}



	/*
		- This is basically sets up everything and calls the actual contentDependant methods
	*/
	private static void startCDC() throws Exception{
		long remainder = 7; // this is the remainder that we will be comparing with
		for (int i = startBoundary;i<=endBoundary;i+=increment)
		{			
			long divisor = i;
			System.out.print( i+" ");
			runBytes(divisor,remainder); // run the karb rabin algorithm
			// this is the block size per boundary
			totalSize = fileArray.get(1).length; // note we only care about the size of the second file since that's the file we are measuring
			double blockSize = (double)totalSize/(double)numOfPieces;
			double ratio = (double)coverage/(double)totalSize;
			System.out.println( blockSize+ " "+ratio);
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
	*/
	private static void startCDC(double [] block_size_list, double [] ratio_size_list,byte[] array1,byte[] array2,
	 ArrayList<Long> md5Hashes1,ArrayList<Long> md5Hashes2 ) throws Exception{
		long remainder = 7; // this is the remainder that we will be comparing with
		int index = 0; // used to traverse the two lists
		for (int i = startBoundary;i<=endBoundary;i+=increment)
		{			
			long divisor = i;
			// System.out.print( i+" ");
			storeChunks(array1,md5Hashes1,divisor,remainder); // cut up the first file and store it
			runKarbRabin(array2,md5Hashes2,divisor,remainder); // call the method again, but on the second file only
			// this is the block size per boundary
			double blockSize = (double)totalSize/(double)numOfPieces;
			double ratio = (double)coverage/(double)totalSize;

			// System.out.println(Arrays.toString(block_size_list));

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
		Read in all the files and loop through all the files
		We already have the hashed version of the documents 
		First, we cut up the first document into chunks (using the CDC algorhtim) and store it
		Then we cut up the second document (usually a different version of the same document) and see how many chunks match
	*/
	private static void runBytes(long divisor,long remainder) throws Exception{
		totalSize = fileArray.get(1).length; // note we only care about the size of the second file since that's the file we are measuring
		storeChunks(fileArray.get(0),hashed_File_List.get(0),divisor,remainder); // cut up the first file and store it
		runKarbRabin(fileArray.get(1),hashed_File_List.get(1),divisor,remainder); // call the method again, but on the second file only
	} // end of the function


	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document

		-- We are simply finding the boundaries of the file using karbRabin and simply storing them. Nothing more!
	-------------------------------------------------------------------------------------------------------- */
	private static void storeChunks(byte[] array, ArrayList<Long> md5Hashes,long divisor,long remainder){

		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		StringBuilder builder = new StringBuilder();
		// loop through all the values in the document
		for (int i = 0; i < md5Hashes.size();++i)
		{ 	
			/*-----------------------------------------------------------------
				- If the mod of this equals the modvalue we defined, then 
				- this is a boundary
			------------------------------------------------------------------*/ 
			if (md5Hashes.get(i)%divisor == remainder) // ck if this equals the mod value
			{
				// Hash all the values in the range (documentStart,current(i))
				// Remember we only want to hash the original VALUES from the array that contains the original
				// content of the file. Not the hash values in the md5Hash Array
				for (int j = documentStart; j <= i;++j){
					builder.append(array[j]); // store everything upto the current value
				}
				String hash = MD5Hash.hashString(builder.toString(),"MD5");	// hash this boundary
				matches.put(hash,1); // simply storing the first document
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
		// only compute hash and insert into our hashtable only if the string buffer isn't empty
		if (builder.length() > 0){
			String hash = MD5Hash.hashString(builder.toString(),"MD5");
			matches.put(hash,1);
		}
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
	private static void runKarbRabin(byte[] array, ArrayList<Long> md5Hashes,long divisor, long remainder){
		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		StringBuilder builder = new StringBuilder(); // used to hold the bytes
		for (int i = 0; i < md5Hashes.size();++i) // loop through each of the values
		{ 	
			/*-----------------------------------------------------------------
				- If the mod of this equals the modvalue we defined, then 
				- this is a boundary
			------------------------------------------------------------------*/ 
			if (md5Hashes.get(i)%divisor == remainder) // ck if this equals the mod value
			{
				// Hash all the values in the range (documentStart,current(i))
				// Remember we only want to hash the original VALUES from the array that contains the original
				// content of the file. Not the hash values in the md5Hash Array
				for (int j = documentStart; j <= i;++j){
					builder.append(array[j]); 
				}
				// check if this hash value exists, if not then add it
				String hash = MD5Hash.hashString(builder.toString(),"MD5"); // compute the hash of this boundary
				if (matches.get(hash) != null) // ck if this boundary exists in the hash table
					coverage+= i - documentStart + 1; // this is the amount of bytes we saved
				
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
		if (builder.length() > 0){
			String hash = MD5Hash.hashString(builder.toString(),"MD5");
			if (matches.get(hash)!=null)
				coverage+=array.length - documentStart; // no need to add one because end is already one past the end
			numOfPieces++; // incremenet the num of pieces
		}
	} // end of the method

}












