import java.util.*;
import java.io.*;
import java.math.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
	private String directory = "emacs/"; // this is the versioned set for emacs
	private int window;// window is size 3
	private int localBoundry; // size of how many elements this hash must be greater than/less than to be considered a boundary

	// get the ratio of the coverage over the total size
	private int totalSize=0;
	private int coverage=0;
	private int numOfPieces=0;
	private int totalWindowPieces=0;

	public static void main(String [] args) throws IOException, Exception
 	{
		ContentPartitioning program = new ContentPartitioning();
		program.driverRun(); // driver for taking in inputs and running the 2min method
			//System.out.println("TESTIBG")
	
	}

	private void driverRun() throws IOException, Exception
	{
		readDir(); // directories dont change
		Scanner in = new Scanner(System.in);
		for (int i = 20;i<=200;i+=10)
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
			System.out.println(blockSize+ " "+ratio);

			// clear the hashTable, and counters so we can reset the values for the next round of boundaries
			matches.clear();
			coverage = 0;
			totalSize = 0;
			numOfPieces = 0;
			totalWindowPieces = 0;
		
		}
		//in.close();
		
	}



	/*
		- This method reads the file using bytes
		- This is where we run the 2min content dependent partitioning
	*/
	private void runBytes() throws IOException,Exception
	{

		/*----------------------------------------------------------------------------
			-- Run the algorithm for every folder in the directory. The folders each have alot
			-- Of html files

			-- The emacs one doesn't have any sub directories, just two files inside
		--------------------------------------------------------------------------------*/
	
			// for (String folder: folderList) // only needed for the HTML directory, which has sub directories
			// {
			// 	//System.out.println(folder);
				//folder =directory; // this is the folder that has the emacs
				//readFile(folder);

				readFile(directory);
				File file = null;
				InputStream is;
				for (String fileName: fileList)
				{
					// Get the full path to the file and parse using JSOUP since its a HTML file
					//file = new File(directory+folder+ "/" + fileName);

					//  Read the fileinto a byte array ( FOR THE HTML DIR)
					//Path p = Paths.get(directory+folder+ "/" + fileName);

					// THIS IS FOR THE EMACS DIR
					//Path p = Paths.get(directory + fileName);
					//byte [] array = Files.readAllBytes(p);	
					is = new FileInputStream(directory + fileName);
					is = new GZIPInputStream(is);
					file = new File(directory + fileName);
					int len = (int)file.length();
					byte [] array = new byte[len];
					int noRead = 0;

					//read the whole file until we hit the end
					while ((noRead = is.read(array)) != -1){
						
											
/* -------------------------------------------------------------------------------------------------------

	-- We are hashing the while document here
	-- We hash the document using a sliding window
	-- Since this is a byte Array, we will sum up the bytes using an int
-------------------------------------------------------------------------------------------------------- */
						int start = 0; // start of the sliding window
						int end = start + window - 1;
						ArrayList<String> md5Hashes = new ArrayList<String>(); // used to hold the md5Hashes
						int byteSum = 0;

						while (end < array.length)
						{
							for (int i = start; i <= end;++i)
								byteSum += array[i];
							md5Hashes.add(hashString(Integer.toString(byteSum),"MD5"));
							totalSize+=byteSum;//get the size ( sum all the bytes? for the total size??) // ARE THE BYTES UNIQUE??/
							byteSum = 0;
							start++;
							end++; // sliding window
						}
						totalWindowPieces = md5Hashes.size(); // total window pieces
/* -------------------------------------------------------------------------------------------------------

-- We will start running the 2 min algorithim here
-- We have a sliding window and find the local minima or local maxima within the document
-- We have a hashTable where we store the values of the boundaries and compare to see if we have
-- already seen this
-- we also keep track of a counter and misscounter, which we use to compute the ratio
-------------------------------------------------------------------------------------------------------- */
						// finding the local minima
						StringBuilder slider = new StringBuilder(); // used to slide through the hash array
						start = 0; // starting point
						int current = localBoundry - 1;// has to be atlead here to be the local minima
						end  = localBoundry *2 - 1; 

						// this is used to scroll through the document boundry
						int documentStart = 0;
						int thresHold = localBoundry*3; // will use local maxima at this point
						int missCounter = 0; // used to keep track if we are close to the threshold
						boolean useMaxima = false;

						// new way 
						// find a boundary using either a local maxima or local minima
						boolean missMinima = false;
						boolean missMaxima = false;
						boolean match = false; // used to ck if we encountered a match

						while (end<md5Hashes.size())
						{ 
							for (int i = start; i <= end; ++i)
							{									
								// if we have found a value bigger than the current one, then we know this
								// not the boundary since we are looling for local minima
								//System.out.println("Current:" + current + " End:" + end + "Array size:" + md5Hashes.size());
								if (md5Hashes.get(current).compareTo(md5Hashes.get(i)) > 0)
								{
									
									// meaning we have already missed a minima and have missed a maxima. We break since this cant
									// be the boundary
									// if (missMinima && missMaxima)
									// 	break;
									// // we have missed a minima, so this cant be a boundary
									// else if (!missMinima)
									// 	missMinima = true;
									break;
								 }
								// // we are using a local maxima and have found a smaller value
								// else if(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)
								// {
								// 	// if we have missed a minima and missed a maxima, we are done and break
								// 	if (missMinima && missMaxima)
								// 		break;
								// 	// we have missed a maxima, so this cant be a boundary
								// 	else if (!missMaxima)
								// 		missMaxima = true;							
								// }
								// if we have reached the end, meaning this is a successful 
								// but we must also be a valid maxima boundary or minima boundary. SO check for that
								// if (i == end && (!missMaxima || !missMinima))
								// {

								// 	//useMaxima = false; // just assign the maxima value to false
								// 	//missCounter = 0;

								// 	// Hash all the values from the start of the boundary( where we left off), all the way to
								// 	// the current boundary
								// 	for (int j = documentStart; j <= current;++j)
								// 		slider.append(array[j]);

								// 	// check if this hash value exists, if not then add it
								// 	// if it does exist, then incremenet the miss counter (used to compute the ratio) 
								// 	String hash = hashString(slider.toString(),"MD5");
								// 	if (matches.get(hash)==null)
								// 		matches.put(hash,1);  // this has not occured to insert it
								// 	else
								// 	{
								// 		matches.put(hash,matches.get(hash) + 1);
								// 		// we have saved some coverage. Incremenet the coverage
								// 		coverage += slider.length();
								// 	}
								// 	//System.out.println(slider.toString());
								// 	documentStart = current + 1;// set this as the beginning of the new boundary
								// 	current = end+ 1; // this is where we start finding the new local minima
								// 	start = documentStart; // we will start comparing from here!, since everything before this is a boundary
									
								// 	// check if it isnt out of bounds
								// 	// change the end as well!
								// 	if (end + localBoundry < md5Hashes.size()) 
								// 		end = end + localBoundry;
								// 	else
								// 		end = md5Hashes.size();
								// 	//documentStart = current;
								// 	//System.out.println()
								// 	slider.setLength(0);	
								// 	match = true;
								// 	// this is a piece, so increment the number of pieces
								// 	numOfPieces++;
								// 	break;

								// }
	/*
		This the original 2min algo where we dont check for both the minima and maxima


	*/
								// We have found a boundary, so incremenet num of boundaries found
								 if (i == end )
								{

									// Hash all the values from the start of the boundary( where we left off), all the way to
									// the current boundary
									for (int j = documentStart; j <= current;++j)
										slider.append(array[j]);

									// check if this hash value exists, if not then add it
									// if it does exist, then incremenet the miss counter (used to compute the ratio) 
									String hash = hashString(slider.toString(),"MD5");
									if (matches.get(hash)==null)
										matches.put(hash,1);  // this has not occured to insert it
									else
									{
										matches.put(hash,matches.get(hash) + 1);
										// we have saved some coverage. Incremenet the coverage
										coverage += slider.length();
									}
									//System.out.println(slider.toString());
									documentStart = current + 1;// set this as the beginning of the new boundary
									current = end+ 1; // this is where we start finding the new local minima
									start = documentStart; // we will start comparing from here!, since everything before this is a boundary
									
									// check if it isnt out of bounds
									// change the end as well!
									if (end + localBoundry < md5Hashes.size()) 
										end = end + localBoundry;
									else
										end = md5Hashes.size();
									//documentStart = current;
									//System.out.println()
									slider.setLength(0);	
									match = true;
									// this is a piece, so increment the number of pieces ( meaning how many boundaries we have)
									numOfPieces++;
									break;

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

							for (int i = documentStart; i < array.length;++i )
							{
								slider.append(array[i]);
							}
							numOfPieces++; // increment numOfPieces
							String hash = hashString(slider.toString(),"MD5");
							if (matches.get(hash)==null)
								matches.put(hash,1); 							
							else
							{
								matches.put(hash,matches.get(hash) + 1);
								// we have made more coverage 
								coverage+= slider.length();
							}
						} // end of thw while loop that reads the file
							
					} // end of the for ( that reads the files) loop
				
			// } // end of the dir for loop
			
		} // end of the function


	// 2 min method
	private void run() throws IOException,Exception
	{

		/*----------------------------------------------------------------------------
			-- Run the algorithm for every folder in the directory. The folders each have alot
			-- Of html files
		--------------------------------------------------------------------------------*/
	
			for (String folder: folderList)
			{
				//System.out.println(folder);
				readFile(folder);
				File file = null;
				for (String fileName: fileList)
				{
					// Get the full path to the file and parse using JSOUP since its a HTML file
					file = new File(directory+folder+ "/" + fileName);

					// here we are parsing the html code. But we will read the file in as bytes 
					Document doc = Jsoup.parse(file,"UTF-8");
					if (doc.body()!=null)
					{
						String [] array = doc.body().getElementsByTag("b").text().replaceAll("[^a-zA-Z0-9 ]","").split(" ");						
/* -------------------------------------------------------------------------------------------------------

	-- We are hashing the while document here
	-- We hash the document using a sliding window
-------------------------------------------------------------------------------------------------------- */
						int start = 0; // start of the sliding window
						int end = start + window - 1;
						ArrayList<String> md5Hashes = new ArrayList<String>(); // used to hold the md5Hashes
						StringBuilder slider = new StringBuilder(); // used to slide through the document
						while (end < array.length)
						{
							for (int i = start; i <= end;++i)
								slider.append(array[i]);
							md5Hashes.add(hashString(slider.toString(),"MD5"));
							totalSize+=slider.length();//get the size
							slider.setLength(0);
							start++;
							end++; // sliding window
						}
						totalWindowPieces = md5Hashes.size(); // total window pieces
/* -------------------------------------------------------------------------------------------------------

	-- We will start running the 2 min algorithim here
	-- We have a sliding window and find the local minima or local maxima within the document
	-- We have a hashTable where we store the values of the boundaries and compare to see if we have
	-- already seen this
	-- we also keep track of a counter and misscounter, which we use to compute the ratio
-------------------------------------------------------------------------------------------------------- */
						// finding the local minima
						start = 0; // starting point
						int current = localBoundry - 1;// has to be atlead here to be the local minima
						end  = localBoundry *2 - 1; 

						// this is used to scroll through the document boundry
						int documentStart = 0;
						int thresHold = localBoundry*3; // will use local maxima at this point
						int missCounter = 0; // used to keep track if we are close to the threshold
						boolean useMaxima = false;

						// new way 
						// find a boundary using either a local maxima or local minima
						boolean missMinima = false;
						boolean missMaxima = false;
						boolean match = false;

						while (end<md5Hashes.size())
						{ 
							for (int i = start; i <= end; ++i)
							{									
								// if we have found a value bigger than the current one, then we know this
								// not the boundary since we are looling for local minima
								//System.out.println("Current:" + current + " End:" + end + "Array size:" + md5Hashes.size());
								if (md5Hashes.get(current).compareTo(md5Hashes.get(i)) > 0)
								{
									
									// meaning we have already missed a minima and have missed a maxima. We break since this cant
									// be the boundary
									if (missMinima && missMaxima)
										break;
									// we have missed a minima, so this cant be a boundary
									else if (!missMinima)
										missMinima = true;
									//break;
								 }
								// // we are using a local maxima and have found a smaller value
								else if(md5Hashes.get(current).compareTo(md5Hashes.get(i)) < 0)
								{
									// if we have missed a minima and missed a maxima, we are done and break
									if (missMinima && missMaxima)
										break;
									// we have missed a maxima, so this cant be a boundary
									else if (!missMaxima)
										missMaxima = true;							
								}
								// if we have reached the end, meaning this is a successful 
								// but we must also be a valid maxima boundary or minima boundary. SO check for that
								if (i == end && (!missMaxima || !missMinima))
								{

									//useMaxima = false; // just assign the maxima value to false
									//missCounter = 0;

									// Hash all the values from the start of the boundary( where we left off), all the way to
									// the current boundary
									for (int j = documentStart; j <= current;++j)
										slider.append(array[j]);

									// check if this hash value exists, if not then add it
									// if it does exist, then incremenet the miss counter (used to compute the ratio) 
									String hash = hashString(slider.toString(),"MD5");
									if (matches.get(hash)==null)
										matches.put(hash,1);  // this has not occured to insert it
									else
									{
										matches.put(hash,matches.get(hash) + 1);
										// we have saved some coverage. Incremenet the coverage
										coverage += slider.length();
									}
									//System.out.println(slider.toString());
									documentStart = current + 1;// set this as the beginning of the new boundary
									current = end+ 1; // this is where we start finding the new local minima
									start = documentStart; // we will start comparing from here!, since everything before this is a boundary
									
									// check if it isnt out of bounds
									// change the end as well!
									if (end + localBoundry < md5Hashes.size()) 
										end = end + localBoundry;
									else
										end = md5Hashes.size();
									//documentStart = current;
									//System.out.println()
									slider.setLength(0);	
									match = true;
									// this is a piece, so increment the number of pieces
									numOfPieces++;
									break;

								}
							}			
							// go to the next window only if we didnt find a match
							if (!match)
							{
								start++;
								current++;
								end++;
							}
							match = false;
							missMaxima = false;
							missMaxima = false;
							
						}
// -------------------------------------------------------------------------------------------

//  we are missing the last boundary, so hash that last value
//	We will also check against our values of the strings we already have, and if we encountered this 
//	already, then we will simply increment the counter, otherwise we will insert it in the hashtable
//	and increase our miss counter

//----------------------------------------------------------------------------------------------

						for (int i = documentStart; i < array.length;++i )
						{
							slider.append(array[i]);
						}
						numOfPieces++; // increment numOfPieces
						String hash = hashString(slider.toString(),"MD5");
						if (matches.get(hash)==null)
							matches.put(hash,1); 							
						else
						{
							matches.put(hash,matches.get(hash) + 1);
							// we have made more coverage 
							coverage+= slider.length();
						}
					} // end of the if statement
					

				} // end of the for loop
				
			} // end of the dir for loop
			
		} // end of the run function

/*
* reads all the files within this folder
* @param folderName - This is the foldername that we will read all the files from
*/
	private void readFile(String folderName)
	{
		//File folder = new File(directory + folderName); only needed for HTML directories
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












