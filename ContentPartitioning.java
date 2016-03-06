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

public class ContentPartitioning{

	private HashMap<String,Integer> matches = new HashMap<String,Integer>();

	// used to store the files in the list
	private ArrayList<String> fileList = new ArrayList<String>(); 
	private ArrayList<String> folderList = new ArrayList<String>();
	//private String directory = "html1/";
	//private String directory = "emacs/"; // this is the versioned set for emacs
	//private String directory = "sample/"; // this is used to test the validiy of my code
	//private String directory = "jdk/";
	//private String directory = "ny/";
	private String directory = "files/";
	//private String directory = "gcc/";
	private int window;// window is size 3
	private int localBoundry; // size of how many elements this hash must be greater than/less than to be considered a boundary

	// get the ratio of the coverage over the total size
	private double totalSize=0;
	private double coverage=0;
	private int numOfPieces=0;
	private int totalWindowPieces=0;

	// used for debugging
	//PrintWriter writer;

	public static void main(String [] args) throws IOException, Exception
 	{
		ContentPartitioning program = new ContentPartitioning();
		program.driverRun(); // driver for taking in inputs and running the 2min method
			//System.out.println("TESTIBG")
	
	}

	private void test() throws IOException,Exception{
		String file1 = fileList.get(0);
		String file2 = fileList.get(1);

		Path p = Paths.get(directory+file1);
		byte [] arr1 = Files.readAllBytes(p);
		p = Paths.get(directory+file2);
		byte [] arr2 = Files.readAllBytes(p);

		int counter = 0; // ck how much they are similar
		for (int i = 0; i < arr2.length; ++i)
			if (arr1[i] == arr2[i])
				counter++;
		System.out.println("Matches = " + counter + " out of " + arr2.length);
	}

	private void driverRun() throws IOException, Exception{
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
			// if (localBoundry == -1)
			// 	break;

			window = 12;
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
		//	break;

			// clear out the fileList
		
		}
		//in.close();		
	}


	/*
		- This method reads the file using bytes
		- This is where we run the 2min content dependent partitioning
	*/
	private void runBytes() throws IOException,Exception
	{
			/*---------------------------------------------------------------------------------
				Read in all the files and loop through all the files
				We will first cut the first document into chuncks and store it
				Then we will hash the next document and see how much coverage we get (how many matches we get)
			--------------------------------------------------------------------------------------*/
				File file = null;
				boolean first = true; // this will be used to ck if it's the first file or not
				ArrayList<String> md5Hashes = new ArrayList<String>(); // used to hold the md5Hashes
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
						//System.out.println("Is first negated? " + first);
						totalSize = 0;
						//writer.close();
					}
					else{

						//writer = new PrintWriter("file2 " + fileName);
						
						//writer.println("\n\n");
						//writer.println("================= Writing boundaries\n\n\n");
						totalSize = array.length; // get the total size of the file
						run2min(array,md5Hashes,localBoundry);// here we run 2min, ck how similar the documents are to the one already in the system
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
	-- Since this is a byte Array, we will sum up the bytes using an int
-------------------------------------------------------------------------------------------------------- */
	private void hashDocument(byte [] array, ArrayList<String> md5Hashes, int start, int end ){

		StringBuilder builder = new StringBuilder(); // used as a sliding window and compute the hash value of each window
		while (end < array.length)
		{
			for (int i = start; i <= end;++i){
				// we use a commma to diffentiaite between: 6 59 & 65 9 because these two are different and w/o comma they would have identical stuff
				builder.append(array[i] + ","); // we won't we adding the bytes, since i tk that leads to error. So checking
			}
			
			String hash = hashString(builder.toString(),"MD5");
			md5Hashes.add(hash.substring(24)); // hash this array
			start++;
			end++; // sliding window
			// reset the bytesum
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
	private void storeChunks(byte [] array, ArrayList<String> md5Hashes, int localBoundry){
					// finding the local minima
					//StringBuilder slider = new StringBuilder(); // used to slide through the hash array
					int start = 0; // starting point
					int current = localBoundry;// has to be atlead here to be the local minima
					int end  = localBoundry *2; 
					int documentStart = 0; // used to keep track of where the boundaries are
					boolean match = false; // used to ck if we encountered a match
					StringBuilder builder = new StringBuilder(); // this is used to store the original document content

					// loop through all the values in the document
					while (end<md5Hashes.size())
					{ 
						for (int i = start; i <= end; ++i)
						{							
							if (i == current)
								++i; // we don't wanna compare withourselves		
							// compare this current with all the values that are (current postition (+/-)localboundaries)
							// CompareTo returns
								// >1 if greater
								// <-1 if less than
								// 0 if equal
								// break if this isnt the smallest one
							if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) 
								break; // we will break if the value at the current index is not a local minima
							
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
									// we will mask the bits bc we dont want negative numbers
									// This wont affect the positive values bc it will just return the same value back

									builder.append(array[j]+","); // simply add it to the string builder
									// DEBUGGING. Writing the boundaries to the file
									//writer.print(array[j] + ",");
								}
								//writer.println("");
								//writer.println("\n\n======NEXT BOUNDARY=====\n\n");
									
								// check if this hash value exists, if not then add it
								// if it does exist, then incremenet the miss counter (used to compute the ratio) 
								String hash = hashString(builder.toString(),"MD5");
								matches.put(hash.substring(24),1); // simply insert the chunks in the document
			
								documentStart = current + 1;// set this as the beginning of the new boundary
								current = end+ 1; // this is where we start finding the new local minima
								start = documentStart; // we will start comparing from here!, since everything before this is a boundary
								
								// check if it isnt out of bounds
								// change the end as well!
								if (current + localBoundry < md5Hashes.size()) 
									end = current + localBoundry;
								else
									end = md5Hashes.size();
								builder.setLength(0); // reset the stringbuilder for the next round
								match = true;
								break; // break out of the for loop
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
											
					} // end of the while loop

					// -------------------------------------------------------------------------------------------
					//  we are missing the last boundary, so hash that last value
					//	We will also check against our values of the strings we already have, and if we encountered this 
					//	already, then we will simply increment the counter, otherwise we will insert it in the hashtable
					//	and increase our miss counter
					//----------------------------------------------------------------------------------------------

					for (int i = documentStart; i < array.length;++i ){
						// we will mask the bits bc we dont want negative numbers
						// This wont affect the positive values bc it will just return the same value back
						builder.append(array[i] + ",");
						//writer.print(array[i] + ",");
					}
					//writer.println("");

					String hash = hashString(builder.toString(),"MD5");
	 				matches.put(hash.substring(24),1); // simply insert the chunks in the document

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
	private void run2min(byte [] array, ArrayList<String> md5Hashes, int localBoundry){
					// finding the local minima
					//StringBuilder slider = new StringBuilder(); // used to slide through the hash array
					int start = 0; // starting point
					int current = localBoundry;// has to be atlead here to be the local minima
					int end  = localBoundry *2; 
					int documentStart = 0; // used to keep track of where the boundaries are
					boolean match = false; // used to ck if we encountered a match
					StringBuilder builder = new StringBuilder(); // used to create the boundaries from the original file

					// loop through all the values in the document
					while (end<md5Hashes.size())
					{ 
						for (int i = start; i <= end; ++i)
						{							
							if (i==current)
								++i;		
							// compare this current with all the values that are (current postition (+/-)localboundaries)
							if (!(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)) 
								break; // we will break if the value at the current index is not a local minima
							
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
									builder.append(array[j]+","); // add the string to the stringbuilder
									//writer.print(array[j] + ",");
								}
								//writer.println("");
								//writer.println("\n\n====Next Boundary===\n\n");
								// check if this hash value exists, if not then add it
								String hash = hashString(builder.toString(),"MD5");
								

								// we are not inserting anything in the matches here. We are simply checking for how similar the documents are to one another
								if (matches.get(hash.substring(24)) != null)
									coverage+= current-documentStart+1; // this is how much we saved					
			
								documentStart = current + 1;// set this as the beginning of the new boundary
								current = end+ 1; // this is where we start finding the new local minima
								start = documentStart; // we will start comparing from here!, since everything before this is a boundary
								
								// check if it isnt out of bounds
								// change the end as well!
								if (current + localBoundry < md5Hashes.size()) 
									end = current + localBoundry;
								else
									end = md5Hashes.size();
								builder.setLength(0); // reset the stringbuilder to get the next window
								//System.out.println("builder length " + builder.length());
								match = true;
								numOfPieces++; // increment the number of pieces we got
								break; // break out of the for loop
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
											
					} // end of the while loop

					// -------------------------------------------------------------------------------------------
					//  we are missing the last boundary, so hash that last value
					//	We will also check against our values of the strings we already have, and if we encountered this 
					//	already, then we will simply increment the counter, otherwise we will insert it in the hashtable
					//	and increase our miss counter
					//----------------------------------------------------------------------------------------------

					for (int i = documentStart; i < array.length;++i ){
						// we will mask the bits bc we dont want negative numbers
						// This wont affect the positive values bc it will just return the same value back
						builder.append(array[i] + ",");
						//writer.print(array[i]+",");
					}
					//writer.println("\n\n");

					String hash = hashString(builder.toString(),"MD5");
	 				if (matches.get(hash.substring(24))!=null)
	 					coverage+=array.length - documentStart; // this is how much we saved. Dont need to add 1 cuz end it one past end anyway
	 				numOfPieces++; // we just got another boundary piece

	} // end of the method




/*-------------------------------------------------------------------------------------------------------------------------*/
	// THIS IS THE FILE INPUT/OUTPUT. Also the md5 hashing method



	/*-------------------------------------------------------------------
		-- This function basically reads the file ( which is stored in the scanner) and reads it into the list
		-- All the white spaces are ommitted 

	-----------------------------------------------------------------------*/
	private void readFile(Scanner in, ArrayList<String> list){

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
	private void readFile(String folderName)
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
	private void readDir()
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












