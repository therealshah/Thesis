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
public class Backup2minTiming{


	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 
	private static ArrayList<String> folderList = new ArrayList<String>();
	//private String directory = "html1/";
	//private static String directory = "emacs/"; // this is the versioned set for emacs
	//private String directory = "sample/"; // this is used to test the validiy of my code
	//private String directory = "jdk/";
	//private String directory = "ny/";
	//private static String directory = "files/";
	//private static String directory = "javabook/";
	// private static String directory = "gcc/";
	//private static String directory = "htmltar/";
	//private static String directory = "sublime/";
	private static String directory = "../thesis/gcc/";

	
	private static int window;// window is size 3
	private static int numOfPieces = 0;

	private static int numHashBoundariesAtEnd = 0; // used to keep track of how many times we went to the end
	private static int numHashBoundariesAtEndSecondTime = 0;
	private static int maxBoundary;
	private static int minBoundary;
	private static int boundaryDivisor = 4; // sets the minimum boundary divisor
	private static int smoothBoundary; // used to determine when we should smooth
	private static double smoothParam = .7; // smoothing param

	public static void main(String [] args) throws IOException, Exception
 	{
 		//directory = "morph.999001/";
 		// String [] dir = {"morph.998001/","morph.99805/","morph.999001/"};
 		// for (String s: dir){
 		// 	directory = s;
 		// 	System.out.println(directory);
 		// 	readFile(directory);
 		// 	driverRun();
 		// }
 		readFile(directory);
		driverRun();

	
		//getBlockFrequency();
			//System.out.println("TESTIBG")
	}




	private static void driverRun() throws IOException, Exception{

		System.out.println("i/2 misses " + directory + " " + "Choose second largest");
		double factor = 1.5;
		for (int i = 100;i<=1000;i+= 50)
		{
			//System.out.print("Enter localBoundry:");
			
			// we will run the code from boundary from 2-window size
			// it will also run the code for window sizes upto the one inputted
			//localBoundry = in.nextInt();
			//smoothBoundary = i/2; // we will smooth the boundary when we reach here
			//minBoundary = 2*i;
			maxBoundary = 4*i; // if we miss this many times
			int localBoundary = i;
			window = 12; // set value
		/*--------------------------------------------------------------------------------------------
					-- Run the 2 min algorithm for all the way upto the value the user enters
					-- We will use the local boundary for all the way up to the value the user entered
		-------------------------------------------------------------------------------------------------*/
			System.out.print( localBoundary+" ");
			//write.print(localBoundary+" ");
			// run the 2min algorithm
			readBytes(localBoundary);
			numOfPieces = 0;


			
		}// end of the for loop

		//write.close();	// close the file
	}


	/*
		- This method reads the file using bytes
		- This is where we run the 2min content dependent partitioning
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
		long startTime = System.nanoTime();
		determineCutPoints(array,md5Hashes,localBoundary);
		// This is where we end the timing
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

	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. localboundary - used to keep track of how the 2min chooses it boundaries

		-- We are simply finding the boundaries of the file using 2min and simply storing them. Nothing more!
	-------------------------------------------------------------------------------------------------------- */
	private static void determineCutPoints(byte [] array, ArrayList<Long> md5Hashes, int localBoundary){
		int start = 0; // starting point
		int current = localBoundary;// has to be atlead here to be the local minima
		//System.out.println(localBoundary + " " + tempBoundary);
		int end  = localBoundary *2;  // this is the end of the boundary
		int documentStart = 0; // used to keep track of where the boundaries start from
		int missCounter = 0; // missCounter. Used for finding the second smallest
		int boundaryMisses = 0; // keep track of boundary misses
		int secondSmallest = -1; // this is the second smallest
		boolean match = false;
		ArrayList<Long> cutpoints = new ArrayList<Long>(); // this holds the cutpoints for hashes
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
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) {
					boundaryMisses++; // missed a boundary
					if (++missCounter >1) // remember we are allowed to miss once ( AKA second smallest)
						break;
				}
				
				/*-----------------------------------------------------------------------------
					We have reached the end. Meaning all the values within the range 
					(documentStart,Current) is either a second smallest or first smallest
				--------------------------------------------------------------------------------*/
				if (i == end && missCounter > 0)
					secondSmallest = current; // this is the second smallest
				if (i == end && missCounter == 0) // we have reached the end
				{
					cutpoints.add(md5Hashes.get(current));
					documentStart = current + 1;// set this as the beginning of the new boundary
					start = current + 1;
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary; // this is the new end of the hash boundary
					match = true; // so we don't increment our window values
					missCounter = 0; // reset the misCounter
					secondSmallest = -1; //reset the second smallest
					boundaryMisses = 0; // reset boundary miss
					numOfPieces++;
				}
			} // end of for

			// if we have reached our maximum threshold
			// we will see if we have a second boundary, if yes, then make that the boundary
			// otherwise now we will make either the first minima or the second minima the boundary
			if ((current - documentStart + 1) >= maxBoundary){
				if (secondSmallest != -1){
					cutpoints.add(md5Hashes.get(current));
					numOfPieces++;
					documentStart = secondSmallest + 1;// set this as the beginning of the new boundary
					start = secondSmallest + 1;
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary; // this is the new end of the hash boundary
					builder.setLength(0); // reset the stringbuilder for the next round
					match = true; // so we don't increment our window values
					missCounter = 0; // reset the misCounter
					secondSmallest = -1; // reset the second smallest
					boundaryMisses = 0; // reset boundary misses
					//break; // break out of the for loop
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
			missCounter = 0; // reset the miss counter as well
								
		} // end of the while loop

	} // end of the method



/*-------------------------------------------------------------------------------------------------------------------------*/
// Everything below is the code for reading the file and hashing the string



	/*-------------------------------------------------------------------
		-- This function basically reads the file ( which is stored in the scanner) and reads it into the list
		-- All the white spaces are ommitted 

	-----------------------------------------------------------------------*/
	private static void readFile(Scanner in, ArrayList<String> list){

		while (in.hasNext()){
			String [] arr = in.nextLine().replaceAll("\\s+"," ").split(" "); // basically read the string, replace all whitespaces and split by each word
			for (String s: arr)
				if (!s.isEmpty())
					list.add(s); // only add it to the list if it's not empty
		}

		// testing purposes
		// for (String s: list)
		// 	System.out.println(s);
	}


	/*
	* reads all the files within this folder
	* @param folderName - This is the foldername that we will read all the files from
	*/
	private static void readFile(String folderName)
	{
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
	private static void readDir()
	{
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
	private static String hashString(String message, String algorithm) 
    {
 
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

	// computes the md5
	// originally takes a string
	// we will just pass in the bytearray
	private static String hashString(byte [] message, String algorithm, int start, int end) 
    {
 
	    try 
	    {
	    	byte [] arr = new byte [end - start+1];
	    	int i = 0;
	    	while (start <= end)
	    		arr[i++] = message[start++];
	    	//System.out.println(arr.length + " " + end + " " + start);

	        MessageDigest digest = MessageDigest.getInstance(algorithm);
	        //byte[] hashedBytes = digest.digest(message.getBytes("UTF-8"));
	        byte [] hashedBytes = digest.digest(arr);
	 
	        return convertByteArrayToHexString(hashedBytes);
	    } 
	    catch (Exception ex) 
	    {
	        return null;
		}
	}

	private static String convertByteArrayToHexString(byte[] arrayBytes) 
	{
	    StringBuffer stringBuffer = new StringBuffer();
	    for (int i = 0; i < arrayBytes.length; i++) {
	        stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16)
	                .substring(1));
	    }
	    return stringBuffer.toString();
	}	


}












