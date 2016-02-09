import java.util.*;
import java.io.*;
import java.math.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.security.MessageDigest;



public class Winnowing{

	private HashMap<String,Integer> matches = new HashMap<String,Integer>();
	private int matchCounter = 0; // keep track of all the matches that occurred in the document
	// best way to parse the data

	// used to store the files in the list

	private ArrayList<String> fileList = new ArrayList<String>(); 
	private ArrayList<String> folderList = new ArrayList<String>();
	private String directory = "html1/";
	private int window;// window is size 3
	private int localBoundry;
	private int counter=0;

	public static void main(String [] args) throws IOException, Exception
 	{
		Winnowing program = new Winnowing();
		program.driverRun(); // driver for taking in inputs and running the 2min method
			//System.out.println("TESTIBG")
	
	}

	private void driverRun() throws IOException, Exception
	{
		readDir(); // directories dont change
		System.out.print("Enter window size:");
		Scanner in = new Scanner(System.in);
		// we will run the code from boundary from 2-window size
		// it will also run the code for window sizes upto the one inputted
		int maxWindow = in.nextInt();

		//System.out.println("RUNNNNNNNNNING");
		
		while (true)
		{
			// this is the window size
			for (int i = 3; i <= maxWindow; ++i)
			{
				// this is the boundary size
				window = i;
				for (int j = 2; j <= maxWindow; ++j)
				{
					localBoundry = maxWindow;
					//System.out.print("\nwindow size: " + window + " boundarysize: " + localBoundry);
					run();
					// clear the hash table!!!
					//System.out.println(" matches: "+matchCounter + "/" + counter +" size: " + matches.size() + " ratio: " + (double)matchCounter/(double)counter);
					System.out.println((double)matchCounter/(double)counter);

					matches.clear();
					matchCounter = 0; // reset the counter!
					counter = 0;
				}
			}
			break;			
		}
		in.close();
	}

	// 2 min method
	private void run() throws IOException,Exception
	{
	
			for (String folder: folderList)
			{
				//System.out.println(folder);
				readFile(folder);
				File file = null;
				for (String fileName: fileList)
				{

					file = new File(directory+folder+ "/" + fileName);
					//System.out.println("==============" + file.getName());
					InputStream in = new FileInputStream(file);

					Document doc = Jsoup.parse(file,"UTF-8");
					if (doc.body()!=null)
					{
						String [] array = doc.body().getElementsByTag("b").text().replaceAll("[^a-zA-Z ]","").split(" ");
						
						int start = 0; // start of the string
						int end = start + window - 1;
						ArrayList<String> md5Hashes = new ArrayList<String>();
						// compute the hash values for the whole document
						StringBuilder slider = new StringBuilder(); // used to slide through the document
						while (end < array.length)
						{
							//md5Hashes.add(hashString(body.substring(start,end),"MD5"));
							for (int i = start; i <= end;++i)
								slider.append(array[i]);
							//System.out.println(slider.toString());
							md5Hashes.add(hashString(slider.toString(),"MD5"));
							slider.setLength(0);
							start++;
							end++; // sliding window
						}
						// finding the local minima
						start = 0; // starting point
						int current = localBoundry - 1;// has to be atlead here to be the local minima
						end  = localBoundry *2 -1; 

						// this is used to scroll through the document boundry
						int documentStart = 0;

						// compute the local minimas and add them to the list
						//ArrayList<String> localMinimas = new ArrayList<String>();

						int thresHold = localBoundry*2; // will use local maxima at this point
						int missCounter = 0; // used to keep track if we are close to the threshold
						boolean match = false;
						while (current<md5Hashes.size())
						{ 

							// winnowing will compare everything before in the current window
							for (int i = start; i <= current; ++i)
							{									
								// not a local minimum, meaning it is greater than some other value
								// System.out.println("Comparing " + md5Hashes.get(current) + " and " + md5Hashes.get(i) +" = " +
								// 	md5Hashes.get(current).compareTo(md5Hashes.get(i)));
								if (md5Hashes.get(current).compareTo(md5Hashes.get(i)) > 0)
								{
									//System.out.println("Breaking");
									//missCounter++;
									break;
								}
								if (i == current)
								{
									//useMaxima = false; // just assign the maxima value to false
									// If we reach here, this means that this is a locoal boundary
									for (int j = documentStart; j <= current;++j)
										slider.append(array[j]);
									//localMinimas.add(hashString(slider.toString(),"MD5"));

									// check if this hash value exists, if not then add it
									// if it does exist, then incremenet the counter
									String hash = hashString(slider.toString(),"MD5");
									counter++;
									if (matches.get(hash)==null)
									{
										// add the new hash to the hashMap and set the occurance to 1
										matches.put(hash,1); 
									}
									else
									{
										matches.put(hash,matches.get(hash) + 1);
										matchCounter++;
									}
									//System.out.println(slider.toString());
									documentStart = current +1;// set this as the beginning of the new boundary
									start = documentStart; // new start

									if (current + localBoundry < md5Hashes.size())
										current = current+localBoundry;
									else
										end = md5Hashes.size();
									//current = start + localBoundry; // this is the new current
									slider.setLength(0);
									match = true;						
								}
							}			
							// go to the next window
							if (!match)
							{
								start++;
								current++;
							}
							match = false;
							
						}
						// we are missing the last boundary, so hash that last value
						for (int i = documentStart; i < array.length;++i )
						{
							slider.append(array[i]);
						}
						//System.out.println(slider);
						counter++;
						String hash = hashString(slider.toString(),"MD5");
						if (matches.get(hash)==null)
						{
							// add the new hash to the hashMap and set the occurance to 1
							matches.put(hash,1); 
							
						}
						else
						{
							matches.put(hash,matches.get(hash) + 1);
							matchCounter++;
						}
					} // end of the if statement
					in.close();

				} // end of the for loop

				// for (Map.Entry<String,Integer> entry : matches.entrySet())
				// 	System.out.println(entry.getKey() + ":" + entry.getValue());
				
			} // end of the dir for loop
			
		} // end of the function

/*
* reads all the files within this folder
* @param folderName - This is the foldername that we will read all the files from
*/
	private void readFile(String folderName)
	{
		File folder = new File(directory + folderName);
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












