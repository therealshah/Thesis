import java.util.*;
import java.io.*;
import java.math.*;
import java.security.MessageDigest;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.zip.*;

/*
	- This is the code for Winnowing. It's basically the same code at 2min but only goes in one direction
	- We loop through the array and find the first minimum that is less than or equal to the previous and k hash
	-- values

	// 1st step- Hash every document
	-- 2nd step - get the boundaries of the first document
	-- 3rd step - hash and get boundaries of second document and ck similarities
*/

public class Winnowing{

	private static HashMap<String,Integer> matches = new HashMap<String,Integer>();

	// used to store the files in the list
	private static ArrayList<String> fileList = new ArrayList<String>(); 
	private static ArrayList<String> folderList = new ArrayList<String>();
	//private String directory = "html1/";
	//private String directory = "emacs/"; // this is the versioned set for emacs
	//private String directory = "sample/"; // this is used to test the validiy of my code
	//private String directory = "jdk/";
	//private String directory = "ny/";
	private static String directory = "files/";
	//private static String directory = "gcc/";
	//private static String directory = "sublime/";
	private static int window;// window is size 3
	private static int localBoundry; // size of how many elements this hash must be greater than/less than to be considered a boundary

	// get the ratio of the coverage over the total size
	private static double totalSize=0;
	private static double coverage=0;
	private static int numOfPieces=0;
	private static int totalWindowPieces=0;

	// used for debugging
	//PrintWriter writer;

	public static void main(String [] args) throws IOException, Exception
 	{
		driverRun();
			//System.out.println("TESTIBG")
	
	}

	private static void driverRun() throws IOException, Exception{
		//readDir(); // directories dont change
		readFile(directory);
		//test();// test the code

		Scanner in = new Scanner(System.in);
		for (int i = 10;i<=200;i+=10)
		{
			//System.out.print("Enter localBoundry:");
			
			// we will run the code from boundary from 2-window size
			// it will also run the code for window sizes upto the one inputted
			//localBoundry = in.nextInt();
			localBoundry = i;
			window = 12; // set value
		/*--------------------------------------------------------------------------------------------
					-- Run the 2 min algorithm for all the way upto the value the user enters
					-- We will use the local boundary for all the way up to the value the user entered
		-------------------------------------------------------------------------------------------------*/
			System.out.print( localBoundry+" ");
			// run the 2min algorithm
			runBytes();
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
		- This method reads the file using bytes
		- This is where we run the 2min content dependent partitioning
	*/
	private static void runBytes() throws IOException,Exception
	{
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
					//System.out.println(fileName);
					Path p = Paths.get(directory+fileName);

					// read the file
					byte [] array = Files.readAllBytes(p); // read the file in bytes
					int start = 0; // start of the sliding window
					int end = start + window - 1; // ending boundary
					hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
					// if this is the first document, we will simply get the boundary chunks and store them
					if (first){
						//writer = new PrintWriter("file1 " + fileName);// create the file for writing
			
						//writer.println("\n\n");
						//writer.println("================= Writing boundaries\n\n\n");
						storeChunks(array,md5Hashes,localBoundry);
						first = !first;
						totalSize = 0;
					}
					else{

						//writer = new PrintWriter("file2 " + fileName);
						
						//writer.println("\n\n");
						//writer.println("================= Writing boundaries\n\n\n");
						totalSize = array.length; // get the total size of the file
						winnowing(array,md5Hashes,localBoundry);// here we run 2min, ck how similar the documents are to the one already in the system
						//writer.close();
					}

					// empty out the md5 Hashes for reuse
					md5Hashes.clear();
									
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
	private static void storeChunks(byte [] array, ArrayList<Long> md5Hashes, int localBoundry){
					int start = 0; // starting point
					int current = localBoundry;// compare all the values at and before this one
					int documentStart = 0; // used to keep track of where the boundaries are
					boolean match = false; // used to ck if we encountered a match
					StringBuilder builder = new StringBuilder(); // this is used to store the original document content
					// loop through until this current equals the end
					while (current<md5Hashes.size())
					{ 
						for (int i = start; i <= current; ++i)
						{		
							/*-----------------------------------------------------------------------------
								We have reached the end. Meaning all the values within the range 
								(documentStart,Current) is a boundary
							--------------------------------------------------------------------------------*/
							if (i == current)
							{

								// Hash all the values in the range (documentStart,current)
								// Remember we only want to hash the original VALUES from the array that contains the original
								// content of the file. Not the hash values in the md5Hash Array
								for (int j = documentStart; j <= current;++j){
									builder.append(array[j]);  // append the bytes to a string builder
								}
								String hash = hashString(builder.toString(),"MD5"); // hash this boundary
								matches.put(hash,1); // simply insert the chunks in the document
								documentStart = current + 1;// set this as the beginning of the new boundary
								builder.setLength(0); // reset the stringbuilder for the next round
								break; // break out of the for loop
							}						
							// compare this current with all the values that are (current postition (+/-)localboundaries)
							// CompareTo returns
								// >0 if greater
								// <0 if less than
								// 0 if equal
								// break if this isnt the smallest one
							else if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) 
								break; // we will break if the value at the current index is not a local minima
							
						
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
					if (builder.length() > 0){ // only go here if the builder isn't empty
						String hash = hashString(builder.toString(),"MD5");
		 				matches.put(hash,1); // simply insert the chunks in the document
	 				}	

	} // end of the method



/* -------------------------------------------------------------------------------------------------------
This method:
	--	Takes in three paramters:
		1. array - this is the byte array that actually holds the document contents
		2. md5Hases - holds the entire hash values of the document
		3. localboundary - used to keep track of how the 2min chooses it boundaries

	-- We will start running the winnowing algorithim here
	-- We have a sliding window and find the local minima or local maxima within the document
	-- We have a hashTable where we store the values of the boundaries and compare to see if we have
	-- already seen this
	-- we also keep track of a counter and misscounter, which we use to compute the ratio
-------------------------------------------------------------------------------------------------------- */
	private static void winnowing(byte [] array, ArrayList<Long> md5Hashes, int localBoundry){
					int start = 0; // starting point
					int current = localBoundry;// this is the end of the boundary
					int documentStart = 0; // used to keep track of where the boundaries are
					boolean match = false; // used to ck if we encountered a match
					StringBuilder builder = new StringBuilder(); // used to create the boundaries from the original file
					while (current<md5Hashes.size())
					{ 
						for (int i = start; i <= current; ++i)
						{	
							/*-----------------------------------------------------------------------------
								We have reached the end. Meaning all the values within the range 
								(documentStart,Current) is a boundary
							--------------------------------------------------------------------------------*/
							if (i == current)
							{
								// Hash all the values in the range (documentStart,current)
								// Remember we only want to hash the original VALUES from the array that contains the original
								// content of the file. Not the hash values in the md5Hash Array
								for (int j = documentStart; j <= current;++j){
									builder.append(array[j]);  // put the values into a string builder
								}
								String hash = hashString(builder.toString(),"MD5");
								// we are not inserting anything in the matches here. We are simply checking for how similar the documents are to one another
								if (matches.get(hash) != null)
									coverage+= current-documentStart+1; // this is how much we saved					
								documentStart = current + 1;// set this as the beginning of the new boundary
								start++;
								current++;
								builder.setLength(0); // reset the stringbuilder to get the next window
								match = true;
								numOfPieces++; // increment the number of pieces we got
								break; // break out of the for loop
							}						
								
							// compare this current with all the values that are (current postition (+/-)localboundaries)
							else if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) 
								break; // we will break if the value at the current index is not a local minima						
						}			
						// go to the next window only if we didnt find a match
						// because if we did find a boundary, we would automatically go to the next window
						if (!match)
						{
							start++;
							current++;
						}
						match = false; // reset this match
											
					} // end of the while loop

					// -------------------------------------------------------------------------------------------
					//  we are missing the last boundary, so hash that last value
					//	We will also check against our values of the strings we already have, and if we encountered this 
					//	already, then we will simply increment the counter, otherwise we will insert it in the hashtable
					//	and increase our miss counter
					//----------------------------------------------------------------------------------------------

					for (int j = documentStart; j < array.length;++j ){
						builder.append(array[j]);  
					}
					if (builder.length() > 0){
						String hash = hashString(builder.toString(),"MD5");
		 				if (matches.get(hash)!=null)
		 					coverage+=array.length - documentStart; // this is how much we saved. Dont need to add 1 cuz end it one past end anyway
		 				numOfPieces++; // we just got another boundary piece
		 			}
	} // end of the method




/*-------------------------------------------------------------------------------------------------------------------------*/
	// THIS IS THE FILE INPUT/OUTPUT. Also the md5 hashing method



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












