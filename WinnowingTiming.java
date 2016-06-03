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



	This code is used to simulate the timing anaylsis for the localMinima method. The timing is determined:
		--  by first hashing the whole document
		--	Starting the timer
		-- calling the method to determine the cutpoints
		-- Stopping the timer once we have our smaller array of cutpoints.

*/


public class WinnowingTiming{

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
	private static String directory = "gcc/";
	//private static String directory = "sublime/";
	private static int window;// window is size 3
	//private static int localBoundry; // size of how many elements this hash must be greater than/less than to be considered a boundary

	// get the ratio of the coverage over the total size
	private static double totalSize=0;
	private static double coverage=0;
	private static int numOfPieces=0;
	private static int totalWindowPieces=0;

	// used for debugging
	//PrintWriter writer;

	public static void main(String [] args) throws IOException, Exception{
 	// 	readFile(directory); // read the file
		// //driverRun();
		// getBlockFrequency(); // this generates all the block sizes for winnowing along with there frequencies
			//System.out.println("TESTIBG")
		// String [] dir = {"morph.998/","morph.99/","morph.98/"};
		// for (String s: dir){
		// 	directory = s;
		// 	System.out.println(directory);
		// 	readFile(directory);
		// 	driverRun();
		// }
		readFile(directory);
		driverRun();

	
	}



	private static void driverRun() throws IOException, Exception{

		for (int i = 1000;i<=10000;i+=500)
		{
			//System.out.print("Enter localBoundry:");
			
			// we will run the code from boundary from 2-window size
			// it will also run the code for window sizes upto the one inputted
			//localBoundry = in.nextInt();
			int localBoundary = i;
			window = 12; // set value
		/*--------------------------------------------------------------------------------------------
					-- Run the 2 min algorithm for all the way upto the value the user enters
					-- We will use the local boundary for all the way up to the value the user entered
		-------------------------------------------------------------------------------------------------*/
			System.out.print( localBoundary+" ");
			// run the 2min algorithm
			readBytes(localBoundary);
			// this is the block size per boundary
			double blockSize = (double)totalSize/(double)numOfPieces;
			double ratio = (double)coverage/(double)totalSize;
			//System.out.print("Coverage " + coverage + " Totalsize " + totalSize);
			//System.out.println( " block size: " + blockSize+ " ratio: "+ratio);
			System.out.println(blockSize + " " + ratio);

			// clear the hashTable, and counters so we can reset the values for the next round of boundaries
			matches.clear();
			coverage = 0;
			totalSize = 0;
			numOfPieces = 0;		
		}
		//in.close();		
	}


	/*
		- This method reads the file as a byte stream
		- Then it calls the content dependant paritioning method to get the chunk points
		- Also get the time for the methods
	*/
	private static void readBytes(int localBoundary) throws IOException,Exception{
		File file = null;
		boolean first = true; // this will be used to ck if it's the first file or not
		ArrayList<Long> md5Hashes = new ArrayList<Long>(); // used to hold the md5Hashes
		String fileName = fileList.get(0); // we will only use the first file
		//System.out.println(fileName);
		Path p = Paths.get(directory+fileName);
		byte [] array = Files.readAllBytes(p); // read the file in bytes

		int start = 0; // start of the sliding window
		int end = start + window - 1; // ending boundary
		hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array

		// this is where we start the timing 
		determineCutPoints(array,md5Hashes,localBoundary);
		// This is where we end the timing						
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
	private static void determineCutPoints(byte [] array, ArrayList<Long> md5Hashes, int localBoundary){
		int start = 0; // starting point
		int current = localBoundary - 1;// compare all the values at and before this one
		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match

		ArrayList<Long> cutpoints = new ArrayList<Long>(); // used to store the cutpoints for the arrayList
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
				cutpoints.add(prevBoundary); // simply add the boundary
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
		String hash = hashString(builder.toString(),"MD5");
		matches.put(hash,1); // simply insert the chunks in the document
	
	} // end of the method








	/*-------------------------------------------------------------------------------------------------------------------------*/
	// THIS IS THE FILE INPUT/OUTPUT. Also the md5 hashing method



	/*-------------------------------------------------------------------
		-- This function basically reads the file ( which is stored in the scanner) and reads it into the list
		-- All the white spaces are ommitted 

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
	private static void readDir(){
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
	private static String hashString(String message, String algorithm) {
 
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












