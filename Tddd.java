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

	private static HashMap<String,Integer> matches = new HashMap<String,Integer>();

	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 
	private static ArrayList<String> folderList = new ArrayList<String>();

	//private static String directory = "../thesis/gcc/";
	//private static String directory = "../thesis-datasets/gcc/";
	private static String directory = "../thesis-datasets/periodic_10/";

	private static int window=12;// window size will be fixed around 12

	// get the ratio of the coverage over the total size
	private static double totalSize=0;
	private static double coverage=0;
	private static int numOfPieces=0;  // used to calculate block size

	// variables for the boundary size
	private static int startBoundary = 10; // start running the algo using this as the starting param
	private static int endBoundary = 100; // go all the way upto here
	private static int increment = 10; // increment in these intervals

	private static ArrayList< byte [] > fileArray = new ArrayList<byte[]>(); // holds both the file arrays
	private static ArrayList<ArrayList<Long>> hashed_File_List = new ArrayList<ArrayList<Long>>(); // used to hold the hashed file





	public static void main(String [] args) throws IOException, Exception{
		System.out.println("Running TDDD " + directory);
		ReadFile.readFile(directory,fileList); // read the two files
		System.out.println(fileList.get(0) + " " + fileList.get(1));
		preliminaryStep(directory);
	 	startCDC();
		//runArchiveSet();
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
		System.out.println("Choping the document\n");
		ArrayList<Long> md5Hashes = new ArrayList<Long>(); // store md5Hases
		HashMap<Integer,Integer> blockFreq = new HashMap<Integer,Integer>(); // this stores the block in the map along there frequencies
		Path p = Paths.get(directory + fileList.get(0)); // get the path of the file, there is only one file
		byte [] array = Files.readAllBytes(p); // read the file into a byte array
		int start = 0; // start of the sliding window
		window = 12;
		int end = start + window - 1; // ending boundary
		int i = 1000;
		Long divisor1 =  new Long(i);
		Long divisor2 = new Long(i/2);
		Long remainder = new Long(7);
		Long minBoundary = new Long(i);
		Long maxBoundary = new Long(4*i);
		HashDocument.hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
		int totalBlocks = chopDocument(array,md5Hashes,divisor1,divisor2,remainder,minBoundary,maxBoundary,blockFreq);
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
			3. Divisor1/Divisor2 - main and back up divisor
			5. The remainder we are looking for
			6/7. min/max boundaries
			8. blockFreq - store the block sizes and how many time's they occur

		-- We are simply choping up the first file
	-------------------------------------------------------------------------------------------------------- */
	private static int chopDocument(byte[] array, ArrayList<Long> md5Hashes, Long divisor1, Long divisor2,Long remainder
		,Long minBoundary,Long maxBoundary,HashMap<Integer,Integer> blockFreq){

		int documentStart = 0; // used to keep track of where the boundaries are
		int backUpBreakPoint = -1; // used to store the backup breakpoint
		int counter = 0;
		// loop through all the values in the document
		for (int i = 0; i < md5Hashes.size();++i)
		{ 	
			if ((i - documentStart + 1) < minBoundary ) //  if the size of this boundary is less than the min, continue looping
				continue;
			/*-----------------------------------------------------------------
				- If the mod of this equals the modvalue we defined, then 
				- this is a boundary
			------------------------------------------------------------------*/ 

			if (md5Hashes.get(i)%divisor1 == remainder) // ck if this equals the mod value
			{

				int size = i - documentStart + 1; // get the size
				if (blockFreq.get(size) == null) // if not in there, then simply store it
						blockFreq.put(size,1); 
				else // increment it's integer count
					blockFreq.put(size,blockFreq.get(size)+1); // increment the count
				counter++; // increment the block count
				documentStart = i + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				
			}		
			else if (md5Hashes.get(i)%divisor2 == remainder){ //  check if this is the backup point
				backUpBreakPoint = i; // this is the backup breakpoint
			}
			if ((i - documentStart + 1) >= maxBoundary ) { // we have reached the maximum
				// ck if we have a backUpbreakpoint, if so set that as the point, otherwise the current value of i
				int point = (backUpBreakPoint != -1)?backUpBreakPoint:i;
				int size = point - documentStart + 1; //we start from the point
				if (blockFreq.get(size) == null) // if not in there, then simply store it
					blockFreq.put(size,1); 
				else // increment it's integer count
					blockFreq.put(size,blockFreq.get(size)+1); // increment the count
				counter++; // increment the block count
				documentStart = point + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				i = point + 1; // we start i from here again
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



	/*
		- This method is used has a helper method to run the algo for the archive dataset
		- Note the archive set has multiple directories ( one for each url )
		- So Read all of the directories in first and for each directory run the code
	*/
	private static void runArchiveSet() throws Exception{

		System.out.println("Running TDDD archive");
		directory = "../thesis-datasets/datasets/";
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

		int current = 0;
		int last_week = 2;
		int last_month = 1;
		int last_year = 3;
		// loop through and run the cdc for each directory
		for (String dir : directory_list){
			// We have 4 files in each directory
			// current, last_week, last_month, last_year
			// read all the files in the directory
			//System.out.println(dir);

			ReadFile.readFile(directory+ dir,fileList); // read all the files in this directory
			preliminaryStep(directory+ dir + "/"); // call the preliminaryStep on all the files
			
			totalRuns++;
			totalSize = fileArray.get(current).length; // get the length of the file we will be running it against!
			
			// run it against last week
			startCDC(block_size_list_last_week,ratio_size_list_last_week,fileArray.get(current),fileArray.get(last_week),hashed_File_List.get(current),hashed_File_List.get(last_week));
			
			// run it against last month
			startCDC(block_size_list_last_month,ratio_size_list_last_month,fileArray.get(current),fileArray.get(last_month),hashed_File_List.get(current),hashed_File_List.get(last_month));

			// run it against last year
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



	private static void startCDC() throws IOException, Exception{
		long remainder = 7;
		for (int i = startBoundary;i<=endBoundary; i+=increment)
		{
			long minBoundary  = i; // we will set the mod value as the minimum boundary
			long maxBoundary = 4*i; // we will set this as the maximum boundary
			long divisor1 = i; // this will be used to mod the results
			long divisor2 = i/2+1; // the backup divisor is half the original divisor
			long divisor3 = i/4+1;
			// minBoundary  = new Long(i); // we will set the mod value as the minimum boundary
			// maxBoundary = new Long(4*i); // we will set this as the maximum boundary
			// divisor1 = new Long(i); // this will be used to mod the results
			// divisor2 = new Long(i/2); // the backup divisor is half the original divisor
			// divisor3 = new Long(i/4);
			System.out.print( divisor1+" " + divisor2 + " " + " " + divisor3 + " ");
			runBytes(window,divisor1,divisor2,divisor3,remainder,minBoundary,maxBoundary); // run the karb rabin algorithm
			// this is the block size per boundary
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
		- current_ -- are the lists that contain the most recent file version
		- previous_ -- are the listrs that contain the previous versions
	*/
	private static void startCDC(double [] block_size_list, double [] ratio_size_list,byte[] current_array,byte[] previous_array,
	 ArrayList<Long> current_md5Hashes,ArrayList<Long> previous_md5Hashes ) throws Exception{
		long remainder = 7; // this is the remainder that we will be comparing with
		int index = 0; // used to traverse the two lists
		for (int i = startBoundary;i<=endBoundary;i+=increment)
		{			
			long minBoundary  = i; // we will set the mod value as the minimum boundary
			long maxBoundary = 4*i; // we will set this as the maximum boundary
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
	private static void runBytes(int window, Long divisor1, Long divisor2,Long divisor3, Long remainder,
		Long minBoundary,Long maxBoundary) throws Exception{

		totalSize = fileArray.get(1).length; // note we only care about the size of the second file since that's the file we are measuring
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
	private static void storeChunks(byte[] array, ArrayList<Long> md5Hashes, Long divisor1, Long divisor2,Long divisor3, Long remainder,
		Long minBoundary,Long maxBoundary){

		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		int backUpBreakPoint = -1; // used to store the backup breakpoint
		int secondBackUpBreakPoint = -1; // this is the second backup point with the divisor3
		StringBuilder builder = new StringBuilder();
		// loop through all the values in the document
		for (int i = 0; i < md5Hashes.size();++i)
		{ 	
			if ((i - documentStart + 1) < minBoundary ) //  if the size of this boundary is less than the min, continue looping
				continue;
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
				String hash = MD5Hash.hashString(builder.toString(),"MD5");	// hash this boundary
				matches.put(hash,1); // simply storing the first document
				documentStart = i + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				secondBackUpBreakPoint = -1; // second backup point reset it!
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
				String hash = MD5Hash.hashString(builder.toString(),"MD5");	// hash this boundary
				matches.put(hash,1); // simply storing the first document
				documentStart = point + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				secondBackUpBreakPoint = -1; // reset second backup break point
				i = point ; // we start i from here again
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

		String hash = MD5Hash.hashString(builder.toString(),"MD5");
		matches.put(hash,1);
		
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
	private static void runTddd(byte[] array, ArrayList<Long> md5Hashes, Long divisor1, Long divisor2,Long divisor3, Long remainder,
		Long minBoundary,Long maxBoundary){
		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		int backUpBreakPoint = -1; // used to store the backup breakpoint
		int secondBackUpBreakPoint = -1; // used with the divisor3
		StringBuilder builder = new StringBuilder();
		// loop through all the values in the document
		for (int i = 0; i < md5Hashes.size();++i)
		{ 	
			if ((i - documentStart + 1) < minBoundary) //  if the size of this boundary is less than the min, continue looping
				continue;
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
				String hash = MD5Hash.hashString(builder.toString(),"MD5");	// hash this boundary
				if (matches.get(hash) != null) // ck if this boundary exists in the hash table
					coverage+= i - documentStart + 1; // this is the amount of bytes we saved
				documentStart = i + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				secondBackUpBreakPoint = -1;
				numOfPieces++; // increment the num of pieces
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
				String hash = MD5Hash.hashString(builder.toString(),"MD5");	// hash this boundary
				if (matches.get(hash) != null) // ck if this boundary exists in the hash table
					coverage+= point - documentStart + 1; // this is the amount of bytes we saved
				numOfPieces++; // increment the num of pieces
				documentStart = point + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				secondBackUpBreakPoint = -1; // reset the secondBackUp point
				i = point ; // we start i from here again
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
		String hash = MD5Hash.hashString(builder.toString(),"MD5");
		if (matches.get(hash) != null) // ck if this boundary exists in the hash table
			coverage+= array.length - documentStart ; // this is the amount of bytes we saved
		numOfPieces++; // increment the num of pieces
		
	} // end of the method

}












