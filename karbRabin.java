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

public class karbRabin{

	private HashMap<String,Integer> matches = new HashMap<String,Integer>();

	// used to store the files in the list
	private ArrayList<String> fileList = new ArrayList<String>(); 
	private ArrayList<String> folderList = new ArrayList<String>();
	//private String directory = "html1/";
	//private String directory = "files/";
	//private String directory = "emacs/"; // this is the versioned set for emacs
	//private String directory = "sample/"; // this is used to test the validiy of my code
	//private String directory = "ny/";
	private String directory = "gcc/";
	private int window;// window size will be fixed around 12
	private int localBoundry; // size of how many elements this hash must be greater than/less than to be considered a boundary

	// get the ratio of the coverage over the total size
	private double totalSize=0;
	private double coverage=0;
	private int numOfPieces=0;  // used to calculate block size


	// These will be the mod values
	BigInteger mod;
	BigInteger modValue;



	public static void main(String [] args) throws IOException, Exception
 	{
		karbRabin program = new karbRabin();
		program.driverRun(); // driver for taking in inputs and running the 2min method
			//System.out.println("TESTIBG")
	
	}

	private void driverRun() throws IOException, Exception
	{
		//readDir(); // directories dont change
		readFile(directory);

		Scanner in = new Scanner(System.in);
		window = 12;
		modValue = new BigInteger("7",10); // This is the remainder that we will be comparing with
		for (int i = 10;i<=200;i+=10)
		{
			//System.out.print("Enter localBoundry:");
			
			
				/*--------------------------------------------------------------------------------------------
				-- Run the karb rabin algorithm for the set mod values
				-- We will use the local boundary for all the way up to the value the user entered
				-------------------------------------------------------------------------------------------------*/
			mod = new BigInteger(Integer.toString(i),10); // this will be used to mod the results
			System.out.print( i+" ");
			// run the 2min algorithm
			runBytes();


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
				InputStream is;
				boolean first = true; // this will be used to ck if it's the first file or not
				ArrayList<String> md5Hashes = new ArrayList<String>(); // used to hold the md5Hashes
				for (String fileName: fileList)
				{
					//System.out.println("Reading file: " + fileName);
					Path p = Paths.get(directory + fileName);
					byte [] array = Files.readAllBytes(p);								
					int start = 0; // start of the sliding window
					int end = start + window - 1; // end of the sliding window used to compute the hash values
					hashDocument(array,md5Hashes,start,end); // this hashes the entire document using the window and stores itto md5hashes array
					// if this is the first document, we will simply get the boundary chunks and store them
					if (first){
						storeChunks(array,md5Hashes);
						first = !first;
						totalSize = 0;
					}
					else{
						//System.out.println(fileName);
						// first calculate the total size of the document
						totalSize = array.length; // get the length for this document
						runKarbRabin(array,md5Hashes);// here we run 2min, ck how similar the documents are to the one already in the system

					}

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

		StringBuilder builder = new StringBuilder(); // used to hash the document

		while (end < array.length)
		{
			for (int i = start; i <= end;++i){
				// we will mask the bits bc we dont want negative numbers
				// This wont affect the positive values bc it will just return the same value back
				builder.append(array[i] + ",");
			}
			
			String md5Hash = hashString(builder.toString(),"MD5");
			md5Hashes.add(md5Hash.substring(24)); // store the lower 32 bits only
			start++;
			end++; // sliding window
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
	private void storeChunks(byte[] array, ArrayList<String> md5Hashes){

		int documentStart = 0; // used to keep track of where the boundaries are
		boolean match = false; // used to ck if we encountered a match
		BigInteger val; // used to store the mod values of each hash value
		StringBuilder builder = new StringBuilder();
		// loop through all the values in the document
		for (int i = 0; i < md5Hashes.size();++i)
		{ 	
			val = new BigInteger(md5Hashes.get(i),16); // the md5hash is in hex
			/*-----------------------------------------------------------------
				- If the mod of this equals the modvalue we defined, then 
				- this is a boundary
			------------------------------------------------------------------*/ 
			if (val.mod(mod).equals(modValue))
			{

				// Hash all the values in the range (documentStart,current(i))
				// Remember we only want to hash the original VALUES from the array that contains the original
				// content of the file. Not the hash values in the md5Hash Array
				for (int j = documentStart; j <= i;++j){
					builder.append(array[j] + ","); // make the boundary	
				}
					
				// check if this hash value exists, if not then add it
				// if it does exist, then incremenet the miss counter (used to compute the ratio) 
				String hash = hashString(builder.toString(),"MD5");	
				matches.put(hash.substring(24),1); // simply storing the first document
				documentStart = i + 1;// set this as the beginning of the new boundary
				builder.setLength(0); // set the length
			}		
								
		} // end of the for loop

		// -------------------------------------------------------------------------------------------
		//  we are missing the last boundary, so hash that last value
		//	We will also check against our values of the strings we already have, and if we encountered this 
		//	already, then we will simply increment the counter, otherwise we will insert it in the hashtable
		//	and increase our miss counter
		//----------------------------------------------------------------------------------------------

		for (int i = documentStart; i < array.length;++i ){
			builder.append(array[i]+",");
		}
		String hash = hashString(builder.toString(),"MD5");
		matches.put(hash.substring(24),1);
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
	private void runKarbRabin(byte[] array, ArrayList<String> md5Hashes){
					// finding the local minima
					//StringBuilder slider = new StringBuilder(); // used to slide through the hash array
					int documentStart = 0; // used to keep track of where the boundaries are
					boolean match = false; // used to ck if we encountered a match
					StringBuilder builder = new StringBuilder();
					BigInteger val; // used to store the mod values of each hash value
					// loop through all the values in the document
					for (int i = 0; i < md5Hashes.size();++i)
					{ 	
						val = new BigInteger(md5Hashes.get(i),16); // the md5hash is in hex
						/*-----------------------------------------------------------------
							- If the mod of this equals the modvalue we defined, then 
							- this is a boundary
						------------------------------------------------------------------*/ 
						if (val.mod(mod).equals(modValue))
						{

							// Hash all the values in the range (documentStart,current(i))
							// Remember we only want to hash the original VALUES from the array that contains the original
							// content of the file. Not the hash values in the md5Hash Array
							for (int j = documentStart; j <= i;++j){
								builder.append(array[j]+",");
							}
								
							// check if this hash value exists, if not then add it
							// if it does exist, then incremenet the miss counter (used to compute the ratio) 
							String hash = hashString(builder.toString(),"MD5");	
							// we are not inserting anything in the matches here. We are simply checking for how similar the documents are to one another
							if (matches.get(hash.substring(24)) != null)
								coverage+= i - documentStart + 1; // this is the amount of bytes we saved
							
							documentStart = i + 1;// set this as the beginning of the new boundary
							numOfPieces++; // increment the num of pieces
							builder.setLength(0); // reset the stringbuilder so we could re use it 
						}		
											
					} // end of the for loop

					// -------------------------------------------------------------------------------------------
					//  we are missing the last boundary, so hash that last value
					//	We will also check against our values of the strings we already have, and if we encountered this 
					//	already, then we will simply increment the counter, otherwise we will insert it in the hashtable
					//	and increase our miss counter
					//----------------------------------------------------------------------------------------------

					for (int i = documentStart; i < array.length;++i ){
						builder.append(array[i] + ",");
					}

					String hash = hashString(builder.toString(),"MD5");
	 				if (matches.get(hash.substring(24))!=null)
	 					coverage+=array.length - documentStart; // no need to add one because end is already one past the end
	 				numOfPieces++; // incremenet the num of pieces

	} // end of the method

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












