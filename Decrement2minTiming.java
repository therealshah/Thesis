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

public class Decrement2min{


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
	//private static String directory = "gcc/";
	//private static String directory = "morph.998/";
	//private static String directory = "htmltar/";
	//private static String directory = "sublime/";
	private static String directory = "../thesis/gcc/";

	
	private static int numOfPieces=0;
	private static int window;// window is size 3


	private static int maxBoundary;
	private static int minBoundary;
	private static int boundaryDivisor = 4; // sets the minimum boundary divisor
	private static int smoothBoundary; // used to determine when we should smooth
	private static double smoothParam = .7; // smoothing param
	// used for debugging
	//PrintWriter writer;

	public static void main(String [] args) throws IOException, Exception
 	{
 		readFile(directory);
 		// int x = 6;
 		// x = (int) (x*smoothParam);
 		// System.out.println(x);
 	// 	System.out.println("Gcc");
		// System.out.println("Smooth Param: " + smoothParam);
		driverRun();
	}



	

	private static void driverRun() throws IOException, Exception{

		for (int i = 100;i<=1000;i+=50)
		{

			maxBoundary = 4*i; // mas boudnary
			int localBoundary = i;
			window = 12; // set value
		/*--------------------------------------------------------------------------------------------
					-- Run the 2 min algorithm for all the way upto the value the user enters
					-- We will use the local boundary for all the way up to the value the user entered
		-------------------------------------------------------------------------------------------------*/
			System.out.print( localBoundary+" ");
			readBytes(localBoundary);
			numOfPieces = 0;
		}// end of the for loop
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
		int end  = localBoundary *2;  // this is the end of the boundary
		int documentStart = 0; // used to keep track of where the boundaries start from
		boolean match = false;
		ArrayList<Long> cutpoints = new ArrayList<Long>(); // new arraylist
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
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) 
						break;
				
				
				/*-----------------------------------------------------------------------------
					We have reached the end. Meaning all the values within the range 
					(documentStart,Current) is either a second smallest or first smallest
				--------------------------------------------------------------------------------*/
				if (i == end) // we have reached the end
				{
					cutpoints.add(md5Hashes.get(current)); // add point to arrayList
					numOfPieces++; // increment num of pieces
					documentStart = current + 1;// set this as the beginning of the new boundary
					start = current + 1;
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary; // this is the new end of the hash boundary
					match = true; // so we don't increment our window values
				}
			} // end of for

			// if we have reached our maximum threshold
			// we will run the decrement 2min
			if ((current-documentStart + 1) >= maxBoundary){
				// start is document start
				// end is current-documentStart + 1
				int dStart = documentStart; // this is the beginning
				//System.out.println("in threshold");

				int dEnd = dStart + (current-documentStart+1); // this is the end (note add dStart!)
				int decrementSize = 1; // initially decrement by one
				int point = runDecrement2min(dStart,dEnd,md5Hashes,localBoundary,decrementSize);// will return if a boundary is found, if not then -1
				while (point == -1){
					// if we didn't find a boundary, then decrement theSize by one and run again
					//System.out.println("in while loop for decrement");
					point = runDecrement2min(dStart,dEnd,md5Hashes,localBoundary,++decrementSize);// will return if a boundary is found, if not then -1

				}
				cutpoints.add(md5Hashes.get(point)); // add the point to the array
				numOfPieces++; // increment pieces we got
				
				documentStart = point + 1;// set this as the beginning of the new boundary
				start = point + 1;
				current = start + localBoundary; // this is where we start finding the new local minima
				end = current + localBoundary; // this is the new end of the hash boundary
				match = true; // so we don't increment our window values
			}	
			// go to the next window only if we didnt find a match
			// because if we did find a boundary, we would automatically go to the next window
			if (!match){
				start++;
				current++;
				end++;
			}
			//System.out.println(start + " " + current + " " + end);
			match = false; // reset this match								
		} // end of the while loop

	} // end of the method

	/* -------------------------------------------------------------------------------------------------------
	This method:
			-- takes in 5 params
			-- dStart/dEnd - start/end
			-- decrementSize -- how much we decrement the boundarySize
				

			-- We are determing if we can find a boundary with the local boundary decremented by decrementSize
	-------------------------------------------------------------------------------------------------------- */
	private static int runDecrement2min(int dStart,int dEnd, ArrayList<Long> md5Hashes, int localBoundary,int decrementSize){
		//System.out.println("Entering runDecrement");
		localBoundary = localBoundary - decrementSize; // decrement the localBoundary by the decrement factor
		int start = dStart; // starting point
		int current = start + localBoundary;// has to be atlead here to be the local minima
		int end  = current + localBoundary;  // this is the end of the boundary
		/*--------------------------------------------------
			-- Now we run the window over and compute the value
			-- in each window and store in hash table
		----------------------------------------------------*/
		if (localBoundary == 0){
				// if it's zero, that means we havent found a boundary
			//System.out.println("In here " + dStart + " " + dEnd + " " + decrementSize);
			return dStart; // we'll make the start the boundary
		}

		while (end<dEnd) // loop through till we our end ( which was the max boundary)
		{ 
			for (int i = start; i <= end; ++i) // loop through each of the values in this boundary
			{							
				if (i == current) // we are looking for strictly less than, so we don't want to compare with ourselves
					++i; // we don't wanna compare withourselves		

				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) 
						break;
				/*-----------------------------------------------------------------------------
					We have reached the end. Meaning all the values within the range 
					(documentStart,Current) is either a second smallest or first smallest
				--------------------------------------------------------------------------------*/
				if (i == end) // we have reached the end
					return current; // we have found a boundary and return it 
				
			} // end of for
			// go to the next window because we still havent found the boundary
			start++;
			current++;
			end++;	
			//System.out.println(start + " " + current+" " + end);					
		} // end of the while loop
		return -1; // return -1 because we still didn't find a boundary
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












