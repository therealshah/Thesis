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

public class KarbRabinTiming{

	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 
	private static ArrayList<String> folderList = new ArrayList<String>();
	private static String directory = "../thesis/gcc/";
	//private String directory = "html1/";
	//private static String directory = "files/";
	//private String directory = "javabook/";
	//private String directory = "emacs/"; // this is the versioned set for emacs
	//private String directory = "htmltar/";
	//private String directory = "sublime/";
	//private String directory = "sample/"; // this is used to test the validiy of my code
	//private String directory = "ny/";
	//private String directory = "gcc/";
	
	private static int window;// window size will be fixed around 12
	private static int numOfPieces=0;  // used to calculate block size



	public static void main(String [] args) throws IOException, Exception{
		
		for (int i = 0; i < 3; ++i){
			System.out.println("======================== Run " + i);
			readFile(directory);
			driverRun(); // driver for taking in inputs and running the 2min method
		}
	}


	private static void driverRun() throws IOException, Exception{
		window = 12;
		//modValue = new BigInteger("7",10); // This is the remainder that we will be comparing with
		Long remainder = new Long(7); // this is the remainder that we will be comparing with
		//Long divisor;
		for (int i = 100;i<=1000;i+=50)
		{
			/*--------------------------------------------------------------------------------------------
			-- Run the karb rabin algorithm for the set mod values
			-- We will use the local boundary for all the way up to the value the user entered
			-------------------------------------------------------------------------------------------------*/
			long divisor = i;
			System.out.print( i+" ");
			readBytes(divisor,remainder); // run the karb rabin algorithm
			numOfPieces = 0; // reset this
		}
		//in.close();		
	}

	/*
		- This method reads the file as a byte stream
		- Then it calls the content dependant paritioning method to get the chunk points
		- Also get the time for the methods
	*/
	private static void readBytes(long divisor, long remainder) throws IOException,Exception{
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
		long startTime = System.nanoTime();
		determineCutPoints(array,md5Hashes,divisor,remainder);
		// This is where we end the timing	
		long endTime = System.nanoTime();
		long duration = (endTime - startTime); // this how long this method took
		int totalSize = array.length; // get the size
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

		-- We are simply finding the boundaries of the file using karbRabin and simply storing them. Nothing more!
	-------------------------------------------------------------------------------------------------------- */
	private static void determineCutPoints(byte[] array, ArrayList<Long> md5Hashes,long divisor,long remainder){

		ArrayList<Long> cutpoints = new ArrayList<Long>(); // used to hold all the cutpoints of the document
		// loop through all the values in the document
		for (int i = 0; i < md5Hashes.size();++i)
		{ 	
			/*-----------------------------------------------------------------
				- If the mod of this equals the modvalue we defined, then 
				- this is a boundary
			------------------------------------------------------------------*/ 
			if (md5Hashes.get(i)%divisor == remainder){ // ck if this equals the mod value
				cutpoints.add(md5Hashes.get(i));// store this index as the cutpoint for the boundary
				numOfPieces++; // to compute the avg blockSize
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












