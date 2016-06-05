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


	TwoMax: This algorithm is similiar to the LocalMinima method, but has minor tweaks. Similar to the localMinima algorithm, this one 
	also has a BoundarySize associated with it called B. Similar to the LocalMinima, this algorithm declares a hash a cutpoint only if the hash
	is strictly less than the B hases before it and B hashes after it. The original localMaxima breaks as soon as it fails. TwoMax however, will continue
	until it fails to be both the minimum in its vicinity and the maximum in its vicinity. If the hash is the maximum, then that is stored as a 
	backup point. 
		The second parameter associated with this algorithm is a max chunk size. Once we hit the maximum chunk size, we will 
	use the maximum hash value that is stored, if there is one. If not, then we will find a hash that is either the minimum or maximum as the boundary point

*/


public class TwoMax{

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
	private static String directory = "gcc/";
	//private static String directory = "htmltar/";
	//private static String directory = "sublime/";
	
	private static int numOfPieces=0;
	private static int window;// window is size 3
	//private static int localBoundry; // size of how many elements this hash must be greater than/less than to be considered a boundary

	private static int maxBoundary;

	public static void main(String [] args) throws IOException, Exception
 	{
 		//String [] dir = {"morph.99590/","morph.99595/","morph.99870/","morph.99875/","morph.99880/","morph.99885/","morph.99890/","morph.99970/","morph.99975/","morph.99980/","morph.99985/","morph.99990/"};
 		// String [] dir = {"morph.998001/","morph.998005/"};

 		// for (String s: dir){
 		// 	directory = s;
 		// 	System.out.println(directory);
 		// 	readFile(directory);
 		// 	driverRun();
 		// }
 		directory = "morph.999001/";
 		readFile(directory);
		driverRun();
	}

	private static void driverRun() throws IOException, Exception{
		System.out.println(directory);
		for (String s : fileList)
			System.out.println(s);
		System.out.println("4*i");
		double factor = 1.5;
		for (int i = 100;i<=1000; i+=50)
		{
			//System.out.print("Enter localBoundry:");
			
			// we will run the code from boundary from 2-window size
			// it will also run the code for window sizes upto the one inputted
			//localBoundry = in.nextInt();
			//minBoundary = new Long(3*i);
			maxBoundary = 4*i;
			int localBoundary = i;
			window = 12; // set value
		/*--------------------------------------------------------------------------------------------
					-- Run the 2 min algorithm for all the way upto the value the user enters
					-- We will use the local boundary for all the way up to the value the user entered
		-------------------------------------------------------------------------------------------------*/
			System.out.print( localBoundary+" ");
			// run the 2min algorithm
			readBytes(localBoundary);
			numOfPieces = 0;

	
		}	
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
			3. localboundary - how to big the neighborhood is for finding the minimum for hash value

		-- We are simply finding the boundaries of the file using TwoMax and simply storing them. Nothing more!
	-------------------------------------------------------------------------------------------------------- */
	private static void determineCutPoints(byte [] array, ArrayList<Long> md5Hashes, int localBoundary){
		int start = 0; // starting point
		int current = localBoundary;// has to be atlead here to be the local minima
		int end  = localBoundary *2;  // this is the end of the boundary
		int documentStart = 0; // used to keep track of where the boundaries start from
		boolean match = false; // used to ck if we encountered a match and is used to determine whether to increment the hash window
		int maxPoint = -1; // used as the secondary point
		boolean notMax = false;
		boolean notMin = false; // keep track if this is a max/min

		ArrayList<Long> cutpoints = new ArrayList<Long>(); // used to hold the cutpoints
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

				// not a min
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) 
					notMin = true; // this is not a min, so just set the variable to min
				// not max
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) > 0)) 
					notMax = true; // this is not a max, so just set the variable to min
				// if it's either a max/min then break
				if (notMax && notMin)
					break; // only break if it's not a max and min
				/*-----------------------------------------------------------------------------
					We have reached the end. Meaning all the values within the range 
					(documentStart,Current) is a boundary
					Only make this boundary if this is a min
				--------------------------------------------------------------------------------*/
				if (i == end && !notMax)
					maxPoint = current; // store this as the maxPoint
				else if (i == end && !notMin)
				{
					cutpoints.add(md5Hashes.get(current));
					numOfPieces++;
					
					documentStart = current + 1;// set this as the beginning of the new boundary
					current = end+ 1; // this is where we start finding the new local minima
					start = documentStart; // we will start comparing from here!, since everything before this is a boundary
					end = current + localBoundary; // this is the new end of the hash boundary
					match = true; // so we don't increment our window values
					maxPoint = -1; // reset the maxPoint
				}
			}

			// if we have reached our maximum threshold
			// we will see if we have a secondary boundary (AKA has a max boundary), if yes, then make that the boundary
			// otherwise now we will make either the first minima or the second minima the boundary
			if ((current-documentStart + 1) >= maxBoundary && !notMax){
				

				cutpoints.add(md5Hashes.get(maxPoint));
				numOfPieces++;
				
				documentStart = maxPoint + 1;// set this as the beginning of the new boundary
				start = maxPoint + 1;
				current = start + localBoundary; // this is where we start finding the new local minima
				end = current + localBoundary; // this is the new end of the hash boundary
				match = true; // so we don't increment our window values	
				maxPoint = -1; // reset the max point		
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
			notMax = false;
			notMin = false;
								
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












