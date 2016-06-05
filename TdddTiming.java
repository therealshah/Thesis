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
public class TdddTiming{

	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 
	private static ArrayList<String> folderList = new ArrayList<String>();
	//private String directory = "html1/";
	//private static String directory = "files/";
	//private String directory = "javabook/";
	//private static String directory = "emacs/"; // this is the versioned set for emacs
	//private String directory = "htmltar/";
	//private String directory = "sublime/";
	//private String directory = "sample/"; // this is used to test the validiy of my code
	//private String directory = "ny/";
	//private static String directory = "gcc/";
	private static String directory = "../thesis/gcc/";

	private static int window;// window size will be fixed around 12
	private static int numOfPieces=0;  // used to calculate block size





	public static void main(String [] args) throws IOException, Exception{
		readFile(directory);
		driverRun(); // driver for taking in inputs and running the 2min method
	}


	private static void driverRun() throws IOException, Exception{
		window = 12;
		Long divisor1;
		Long divisor2; // second mod value we will be using
		Long divisor3;
		Long remainder = new Long(7); // this is the remainder that we will be comparing with
		Long minBoundary;
		Long maxBoundary;
		System.out.println("gcc");
		double factor = 1.5;
		for (int i = 100;i<=1000; i+= 50 )
		{
			//System.out.print("Enter localBoundry:");
			minBoundary  = new Long(i); // we will set the mod value as the minimum boundary
			maxBoundary = new Long(4*i); // we will set this as the maximum boundary
			divisor1 = new Long(i); // this will be used to mod the results
			divisor2 = new Long(i/2); // the backup divisor is half the original divisor
			divisor3 = new Long(i/4);
			System.out.print( divisor1+" " + divisor2 + " " + " " + divisor3 + " ");
			runBytes(window,divisor1,divisor2,divisor3,remainder,minBoundary,maxBoundary);
			numOfPieces = 0; // reset the num of pieces
		}
	}

	/*
		- This method reads the file and basically sets up everything for TTTD
		- @params:
			window - rolling window size 
			divisor1 - the first divisor value we will be using to find the remainder
			divisor2/3 - the second/third divisor value we will be using to find the remainder
			minBoundary/maxBoundary - min/ max boundaries for the chunks
	
	*/
	private static void readBytes(int window, Long divisor1, Long divisor2,Long divisor3, Long remainder,Long minBoundary,Long maxBoundary) throws IOException,Exception{

		boolean first = true; // this will be used to ck if it's the first file or not
		ArrayList<Long> md5Hashes = new ArrayList<Long>(); // used to hold the md5Hashes
		fileName = fileList.get(0); // only get the first file
		//System.out.println("Reading file: " + fileName);
		Path p = Paths.get(directory + fileName); // get the full path of the file that we will be reading
		byte [] array = Files.readAllBytes(p); // read the whole file in byte form							
		int start = 0; // start of the sliding window
		int end = start + window - 1; // end of the sliding window used to compute the hash values
		hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array

		// Start the timing here	
		long startTime = System.nanoTime();	
		determineCutPoints(array,md5Hashes,divisor1,divisor2,divisor3,remainder,minBoundary,maxBoundary);	
		// End the timing here			
		long endTime = System.nanoTime();
		long duration = (endTime - startTime); // this how long this method took
		int totalSize = array.length(); // get the size
		double blockSize = (double)totalSize/(double)numOfPieces;
		System.out.println(blockSize + " " + duration); // printing the avgBlockSize along with the timing					
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
			3. Divisor1/Divisor2/divisor3... - main and back up divisors
			5. The remainder we are looking for
			6/7. min/max boundaries

		-- We are simply choping up the first file
	-------------------------------------------------------------------------------------------------------- */
	private static void determineCutPoints(byte[] array, ArrayList<Long> md5Hashes, Long divisor1, Long divisor2,Long divisor3,Long remainder
		,Long minBoundary,Long maxBoundary){

		boolean match = false; // used to ck if we encountered a match
		int documentStart = 0; // used to keep track of where the boundaries are
		int backUpBreakPoint = -1; // used to store the backup breakpoint
		int secondBackUpBreakPoint = -1; // this is the second backup point with the divisor3
		ArrayList<Long> cutpoints = new ArrayList<Long>(); // used to hold just the cutpoints
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
				cutpoints.add(i); // add this as the cutpoint for the document
				documentStart = i + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				secondBackUpBreakPoint = -1; // second backup point reset it!
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

				cutpoints.add(point); // add this to the boundary
				documentStart = point + 1;// set this as the beginning of the new boundary
				backUpBreakPoint = -1; // reset this
				secondBackUpBreakPoint = -1; // reset second backup break point
				i = point ; // we start i from here again
			}
								
		} // end of the for loop

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












