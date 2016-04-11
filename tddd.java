import java.util.*;
import java.io.*;
import java.math.*;
import java.security.MessageDigest;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.zip.*;

/*
	- What Im doing atm, is read the file by bytes. Not just scrape the html
*/

public class tddd{

	private static HashMap<String,Integer> matches = new HashMap<String,Integer>();

	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 
	private static ArrayList<String> folderList = new ArrayList<String>();
	//private String directory = "html1/";
	//private static String directory = "files/";
	//private String directory = "javabook/";
	private static String directory = "emacs/"; // this is the versioned set for emacs
	//private String directory = "htmltar/";
	//private String directory = "sublime/";
	//private String directory = "sample/"; // this is used to test the validiy of my code
	//private String directory = "ny/";
	//private static String directory = "gcc/";
	private static int window;// window size will be fixed around 12

	// get the ratio of the coverage over the total size
	private static double totalSize=0;
	private static double coverage=0;
	private static int numOfPieces=0;  // used to calculate block size





	public static void main(String [] args) throws IOException, Exception{
		tddd program = new tddd();
		program.driverRun(); // driver for taking in inputs and running the 2min method
			//System.out.println("TESTIBG")
	}

	private static void driverRun() throws IOException, Exception{
		//readDir(); // directories dont change
		readFile(directory);

		Scanner in = new Scanner(System.in);
		window = 12;
		Long divisor1;
		Long divisor2; // second mod value we will be using
		Long remainder = new Long(7); // this is the remainder that we will be comparing with
		for (int i = 10;i<=1000;i+=50)
		{
			//System.out.print("Enter localBoundry:");
			Long minBoundary  = new Long(2*i); // we will set the mod value as the minimum boundary
			Long maxBoundary = new Long(8*i); // we will set this as the maximum boundary
			divisor1 = new Long(i); // this will be used to mod the results
			divisor2 = new Long(i/2); // the backup divisor is half the original divisor
			System.out.print( i+" " + i/2 + " ");
			runBytes(window,divisor1,divisor2,remainder,minBoundary,maxBoundary); // run the karb rabin algorithm
			// this is the block size per boundary
			double blockSize = (double)totalSize/(double)numOfPieces;
			double ratio = (double)coverage/(double)totalSize;
			//System.out.print("Coverage " + coverage + " Totalsize " + totalSize);
			System.out.println( blockSize+ " "+ratio);

			// clear the hashTable, and counters so we can reset the values for the next round of boundaries
			matches.clear();
			coverage = 0;
			totalSize = 0;
			numOfPieces = 0; 		
		}
		//in.close();		
	}

	/*
		- This method reads the file and basically sets up everything for TTTD
		- @params:
			window - rolling window size 
			divisor1 - the first divisor value we will be using to find the remainder
			divisor2 - the second divisor value we will be using to find the remainder
			minBoundary/maxBoundary - min/ max boundaries for the chunks
	
	*/
	private static void runBytes(int window, Long divisor1, Long divisor2, Long remainder,Long minBoundary,Long maxBoundary) throws IOException,Exception{

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
					//System.out.println("Reading file: " + fileName);
					Path p = Paths.get(directory + fileName); // get the full path of the file that we will be reading
					byte [] array = Files.readAllBytes(p); // read the whole file in byte form							
					int start = 0; // start of the sliding window
					int end = start + window - 1; // end of the sliding window used to compute the hash values
					hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
					// if this is the first document, we will simply get the boundary chunks and store them
					if (first){
						storeChunks(array,md5Hashes,divisor1,divisor2,remainder,minBoundary,maxBoundary);
						first = !first;
						totalSize = 0;
					}
					else{
						totalSize = array.length; // get the length for this document
						runTddd(array,md5Hashes,divisor1,divisor2,remainder,minBoundary,maxBoundary);// here we run 2min, ck how similar the documents are to the one already in the system
					}
					md5Hashes.clear(); // clear our md5hashes array									
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
		-- Since this is a byte Array, we will sum up the bytes using an int
	-------------------------------------------------------------------------------------------------------- */
	private static void hashDocument(byte [] array, ArrayList<Long> md5Hashes, int start, int end ){

		StringBuilder builder = new StringBuilder(); // used to hash the document
		while (end < array.length)
		{
			for (int i = start; i <= end;++i){
				builder.append(array[i]);  // add the byte to the string builder
			}
			String hash = hashString(builder.toString(),"MD5"); // hash this value
			long val = Long.parseLong(hash.substring(24),16); // compute the int value of the lower 32 bits
			md5Hashes.add(val); // store the lower 32 bits only
			//md5Hashes.add(md5Hash); // store the lower 32 bits only
			start++;
			end++; // increment the sliding window
			builder.setLength(0);
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

		-- We are simply choping up the first file
	-------------------------------------------------------------------------------------------------------- */
	private static void storeChunks(byte[] array, ArrayList<Long> md5Hashes, Long divisor1, Long divisor2,Long remainder
		,Long minBoundary,Long maxBoundary){

		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		int backUpBreakPoint = -1; // used to store the backup breakpoint
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
				String hash = hashString(builder.toString(),"MD5");	// hash this boundary
				matches.put(hash,1); // simply storing the first document
				documentStart = i + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				builder.setLength(0); // reset the stringBuilder for the next round
			}		
			else if (md5Hashes.get(i)%divisor2 == remainder){ //  check if this is the backup point
				backUpBreakPoint = i; // this is the backup breakpoint
			}
			if ((i - documentStart + 1) >= maxBoundary ) { // we have reached the maximum
				// ck if we have a backUpbreakpoint
				int point;
				if (backUpBreakPoint != -1)// if we do, set this as the boundary
			    	point = backUpBreakPoint;
			    else
			    	point = i; // else this current value of i is the breakpoint

			    // Hash all the values in the range (documentStart,point
				// Remember we only want to hash the original VALUES from the array that contains the original
				// content of the file. Not the hash values in the md5Hash Array
				for (int j = documentStart; j <= point;++j){
					builder.append(array[j]); // store everything upto the current value
				}
				String hash = hashString(builder.toString(),"MD5");	// hash this boundary
				matches.put(hash,1); // simply storing the first document
				documentStart = point + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				i = point + 1; // we start i from here again
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
			String hash = hashString(builder.toString(),"MD5");
			matches.put(hash,1);
		}
	} // end of the method



	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. localboundary - used to keep track of how the 2min chooses it boundaries

		-- We will start running the karb rabin algorithm
		-- We will find the boundaries using mod values and once they equal the mod value we have stored
		-- We will hash everything in that hash boundary and store it
	-------------------------------------------------------------------------------------------------------- */
	private static void runTddd(byte[] array, ArrayList<Long> md5Hashes, Long divisor1, Long divisor2,Long remainder
		,Long minBoundary,Long maxBoundary){
		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		int backUpBreakPoint = -1; // used to store the backup breakpoint
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
				String hash = hashString(builder.toString(),"MD5");	// hash this boundary
				if (matches.get(hash) != null) // ck if this boundary exists in the hash table
					coverage+= i - documentStart + 1; // this is the amount of bytes we saved
				documentStart = i + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				numOfPieces++; // increment the num of pieces
				builder.setLength(0); // reset the stringBuilder for the next round
			}		
			else if (md5Hashes.get(i)%divisor2 == remainder){ //  check if this is the backup point
				backUpBreakPoint = i; // this is the backup breakpoint
			}
			if ((i - documentStart + 1) >= maxBoundary ) { // we have reached the maximum
				// ck if we have a backUpbreakpoint
				int point;
				if (backUpBreakPoint != -1)// if we do, set this as the boundary
			    	point = backUpBreakPoint;
			    else
			    	point = i; // else this current value of i is the breakpoint

			    // Hash all the values in the range (documentStart,current(i))
				// Remember we only want to hash the original VALUES from the array that contains the original
				// content of the file. Not the hash values in the md5Hash Array
				for (int j = documentStart; j <= point;++j){
					builder.append(array[j]); // store everything upto the current value
				}
				String hash = hashString(builder.toString(),"MD5");	// hash this boundary
				if (matches.get(hash) != null) // ck if this boundary exists in the hash table
					coverage+= i - documentStart + 1; // this is the amount of bytes we saved
				numOfPieces++; // increment the num of pieces
				documentStart = point + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				i = point + 1; // we start i from here again
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
			String hash = hashString(builder.toString(),"MD5");
			if (matches.get(hash) != null) // ck if this boundary exists in the hash table
				coverage+= array.length - documentStart ; // this is the amount of bytes we saved
			numOfPieces++; // increment the num of pieces
		}
	} // end of the method

	/*
	* reads all the files within this folder
	* @param folderName - This is the foldername that we will read all the files from
	*/
	private static void readFile(String folderName){
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
	private void readDir(){
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
	private static String hashString(String message, String algorithm){
 
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

	private static String convertByteArrayToHexString(byte[] arrayBytes) {
	    StringBuffer stringBuffer = new StringBuffer();
	    for (int i = 0; i < arrayBytes.length; i++) {
	        stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16)
	                .substring(1));
	    }
	    return stringBuffer.toString();
	}	


}












