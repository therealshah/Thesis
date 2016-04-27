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

public class decrement2min{

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
	//private static String directory = "javabook/";
	private static String directory = "gcc/";
	//private static String directory = "morph.998/";
	//private static String directory = "htmltar/";
	//private static String directory = "sublime/";
	
	// get the ratio of the coverage over the total size
	private static double totalSize=0;
	private static double coverage=0;
	private static int numOfPieces=0;
	private static int totalWindowPieces=0;
	private static int window;// window is size 3
	//private static int localBoundry; // size of how many elements this hash must be greater than/less than to be considered a boundary


	private static int numHashBoundariesAtEnd = 0; // used to keep track of how many times we went to the end
	private static int numHashBoundariesAtEndSecondTime = 0;
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

		for (int i = 10;i<=1000;i+=50)
		{

			maxBoundary = 4*i; // mas boudnary
			int localBoundary = i;
			window = 12; // set value
		/*--------------------------------------------------------------------------------------------
					-- Run the 2 min algorithm for all the way upto the value the user enters
					-- We will use the local boundary for all the way up to the value the user entered
		-------------------------------------------------------------------------------------------------*/
			System.out.print( localBoundary+" ");
			runBytes(localBoundary);
			// this is the block size per boundary
			double blockSize = (double)totalSize/(double)numOfPieces;
			double ratio = (double)coverage/(double)totalSize;
			System.out.println(blockSize + " " + ratio);
			// clear the hashTable, and counters so we can reset the values for the next round of boundaries
			matches.clear();
			coverage = 0;
			totalSize = 0;
			numOfPieces = 0;
		}// end of the for loop

		//write.close();	// close the file
	}


	/*
		- This method reads the file using bytes
		- This is where we run the 2min content dependent partitioning
	*/
	private static void runBytes(int localBoundary) throws IOException,Exception{
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
					//System.out.println(array.length);
					//System.out.println(fileName + "  " + array.length);
					int start = 0; // start of the sliding window
					int end = start + window - 1; // ending boundary
					hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
					// if this is the first document, we will simply get the boundary chunks and store them
					if (first){
						storeChunks(array,md5Hashes,localBoundary);
						first = !first;
						totalSize = 0;
					}
					else{

						totalSize = array.length; // get the total size of the file
						run2min(array,md5Hashes,localBoundary);// here we run 2min, ck how similar the documents are to the one already in the system
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
	private static void storeChunks(byte [] array, ArrayList<Long> md5Hashes, int localBoundary){
		int start = 0; // starting point
		int current = localBoundary;// has to be atlead here to be the local minima
		int end  = localBoundary *2;  // this is the end of the boundary
		int documentStart = 0; // used to keep track of where the boundaries start from
		StringBuilder builder = new StringBuilder(); // this is used to store the original document content
		boolean match = false;
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
					// ck is this the 

					// Hash all the values in the range (documentStart,current)
					// Remember we only want to hash the original VALUES from the array that contains the original
					// content of the file. Not the hash values in the md5Hash Array
					for (int j = documentStart; j <= current;++j){
						builder.append(array[j]); 
					}
					String hash = hashString(builder.toString(),"MD5"); // hash this boundary
					//System.out.println(current-documentStart + 1);
					matches.put(hash,1); // simply insert the chunks in the hashtable
					documentStart = current + 1;// set this as the beginning of the new boundary
					start = current + 1;
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary; // this is the new end of the hash boundary
					builder.setLength(0); // reset the stringbuilder for the next round
					match = true; // so we don't increment our window values
					break; // break out of the for loop
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
				//System.out.println("exiting while loop");
				// now we made it, with point being boundary point
				for (int j = documentStart; j <= point;++j)
					builder.append(array[j]); 
				
				String hash = hashString(builder.toString(),"MD5"); // hash this boundary
				//System.out.println(current-documentStart + 1);
				matches.put(hash,1); // simply insert the chunks in the hashtable
				documentStart = point + 1;// set this as the beginning of the new boundary
				start = point + 1;
				current = start + localBoundary; // this is where we start finding the new local minima
				end = current + localBoundary; // this is the new end of the hash boundary
				builder.setLength(0); // reset the stringbuilder for the next round
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

		// -------------------------------------------------------------------------------------------
		//  we are missing the last boundary, so hash that last value
		//	We will also check against our values of the strings we already have, and if we encountered this 
		//	already, then we will simply increment the counter, otherwise we will insert it in the hashtable
		//	and increase our miss counter
		//----------------------------------------------------------------------------------------------

		// loop through the end of our array and hash the final boundaries
		for (int j = documentStart; j < array.length;++j ){
			builder.append(array[j]); 
		}
		if (builder.length()> 0 ){
			String hash = hashString(builder.toString(),"MD5");
			//numHashBoundariesAtEnd+=builder.length();
			matches.put(hash,1); // simply insert the chunks in the document
		}
		else{
			System.out.println("Yolo");
		}

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

	/* -------------------------------------------------------------------------------------------------------
	This method:
		--	Takes in three paramters:
			1. array - this is the byte array that actually holds the document contents
			2. md5Hases - holds the entire hash values of the document
			3. localboundary - used to keep track of how the 2min chooses it boundaries

		-- We will start running the 2 min algorithim here
		-- We have a sliding window and find the local minima or local maxima within the document
		-- We have a hashTable where we store the values of the boundaries and compare to see if we have
		-- already seen this
		-- we also keep track of a counter and misscounter, which we use to compute the ratio
	-------------------------------------------------------------------------------------------------------- */
	private static void run2min(byte [] array, ArrayList<Long> md5Hashes, int localBoundary) throws Exception{
		//System.out.println("Inside run2min");
		int start = 0; // starting point
		int current = localBoundary;// has to be atlead here to be the local minima
		int end  = localBoundary *2;  // this is the end of the window
		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		StringBuilder builder = new StringBuilder(); // used to create the boundaries from the original file
		/* --------------------------------------------
			-- Loop throught and compare each value in the boundary 
			-- and find the boundaries
		----------------------------------------------*/
		while (end<md5Hashes.size()){ 
	
			for (int i = start; i <= end; ++i)
			{							
				if (i==current) // we don't want to compare with ourselves
					++i;	

		
				if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) {
						break; // we will break if the value at the current index is not a local minima
				}
				
				/*-----------------------------------------------------------------------------
					We have reached the end. Meaning all the values within the range 
					(documentStart,Current) is a boundary
				--------------------------------------------------------------------------------*/
				if (i == end)
				{

					// Hash all the values in the range (documentStart,current)
					// Remember we only want to hash the original VALUES from the array that contains the original
					// content of the file. Not the hash values in the md5Hash Array
					for (int j = documentStart; j <= current;++j){
						builder.append(array[j]); 
					}
					String hash = hashString(builder.toString(),"MD5"); // hash this boundary

					// Check if this value exists in the hash table
					// If it does, we will increment the coverage count
					if (matches.get(hash) != null){
						// byte [] arr = builder.toString().getBytes("UTF-8");
						// System.out.println(arr);
						coverage+= current-documentStart+1; // this is how much we saved
					}					
					documentStart = current + 1;// set this as the beginning of the new boundary
					start = current + 1;
					current = start + localBoundary; // this is where we start finding the new local minima
					end = current + localBoundary;
					builder.setLength(0); // reset the stringbuilder to get the next window
					match = true; //  so we don't increment our window again
					numOfPieces++; // we just got another boundary piece
					break; // break out of the for loop
				}
			} // end of for

				// if we have reached our maximum threshold
			// we will run the decrement 2min
			if ((current-documentStart + 1) >= maxBoundary){
				// start is document start
				// end is current-documentStart + 1
				//System.out.println("inside threshold");
				int dStart = documentStart; // this is the beginning
				int dEnd = dStart + (current-documentStart+1); // this is the end
				int decrementSize = 1; // initially decrement by one
				int point = runDecrement2min(dStart,dEnd,md5Hashes,localBoundary,decrementSize);// will return if a boundary is found, if not then -1
				while (point == -1){

					// if we didn't find a boundary, then decrement theSize by one and run again
					point = runDecrement2min(dStart,dEnd,md5Hashes,localBoundary,++decrementSize);// will return if a boundary is found, if not then -1

				}
				//System.out.println("making boundary");
				// now we made it, with point being boundary point
				for (int j = documentStart; j <= point;++j)
					builder.append(array[j]); 
				
				String hash = hashString(builder.toString(),"MD5"); // hash this boundary
				//System.out.println(current-documentStart + 1);
				if (matches.get(hash) != null)
					coverage+= point-documentStart+1; // this is how much we saved
				documentStart = point + 1;// set this as the beginning of the new boundary
				start = point + 1;
				current = start + localBoundary; // this is where we start finding the new local minima
				end = current + localBoundary; // this is the new end of the hash boundary
				builder.setLength(0); // reset the stringbuilder for the next round
				match = true; // so we don't increment our window values
				numOfPieces++;
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
			String hash = hashString(builder.toString(),"MD5"); // hash our value
			if (matches.get(hash)!=null)
				coverage+=array.length - documentStart; // this is how much we saved. Dont need to add 1 cuz end it one past end anyway
			numOfPieces++; // we just got another boundary piece
		}
		else
			System.out.println("In here yoloing");

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












