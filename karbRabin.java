import java.util.*;
import java.io.*;
import java.math.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.security.MessageDigest;



public class karbRabin{

	private HashMap<String,Integer> matches = new HashMap<String,Integer>();

	private int matchCounter = 0;
	// best way to parse the data

	// used to store the files in the list
	private ArrayList<String> fileList = new ArrayList<String>(); 
	private ArrayList<String> folderList = new ArrayList<String>();
	private String directory = "html1/";

	// variables to the run the algo
	BigInteger mod;
	BigInteger modValue;
	private int window;
	private int totalSize=0;
	private int coverage=0;
	private int numOfPieces=0;
	private int totalWindowPieces=0;


	public static void main(String [] args) throws IOException, Exception
 	{
		karbRabin program = new karbRabin();
			//System.out.println("TESTIBG");
		program.driverRun();
	}

	private void test()
	{
		String body = "My name is Shahzaib javed and I must be something more, something better I must be a innovator.";
		String [] array = body.split(" ");

		int window = 3; // window is size 3
		int start = 0; // start of the string
		int end = start + window;
		ArrayList<String> md5Hashes = new ArrayList<String>();
		int localBoundry = 2; // used to find the local minima for the window
		// compute the hash values for the whole document
		StringBuilder slider = new StringBuilder(); // used to slide through the document
		while (end <= array.length)
		{
			//md5Hashes.add(hashString(body.substring(start,end),"MD5"));
			for (int i = start; i <end;++i)
				slider.append(array[i]);
			//System.out.println(slider.toString());
			md5Hashes.add(hashString(slider.toString(),"MD5"));
			slider.setLength(0);

			start++;
			end++; // sliding window
		}

		int documentStart = 0;
		int current = 0;
		BigInteger mod = new BigInteger("8",10);
		for (int i = 1; i < md5Hashes.size(); ++i)
		{
			current = i;
			BigInteger val = new BigInteger(md5Hashes.get(i),16);
			StringBuilder temp = new StringBuilder();
			BigInteger t = new BigInteger("6",10);
			//System.out.println(val.mod(mod));
			if (val.mod(mod).equals(t)){
				for (int j = documentStart; j< i; ++j)
					temp.append(array[j]);
				String hash = hashString(temp.toString(),"MD5");
				System.out.println(temp);
				documentStart = current;

			}

		}

		slider.setLength(0);
		for (int i = documentStart; i < array.length;++i)
			slider.append(array[i]);
		System.out.println(slider.toString());
	}


	private void driverRun() 
	{
		try {
			readDir();
			Scanner in = new Scanner(System.in);
			window = 12;
			modValue = new BigInteger("7",10); // this will be the remainder that we will be comparing to
			while(true)
			{
				System.out.print("Enter the mod value: ");
				String value = in.next();
				if (value.equals("QUIT"))
					break;
				mod = new BigInteger(value,10);			
				//System.out.print("\nwindow size: " + window + " mod value: " + i + " remainder value: " + j);
				run();
				double avgSize = (double)totalSize/(double)numOfPieces;

				System.out.println(value+" "+(double)totalWindowPieces/avgSize + " "+ (double)numOfPieces/avgSize+ " "+(double)coverage/(double)totalSize);
				matches.clear();
				totalSize = 0;
				coverage = 0;
				totalWindowPieces = 0;
				numOfPieces= 0;
			}
			in.close();	
		}
		catch (Exception e)
		{
			//System.out.println("\n" + e);
			e.printStackTrace();
		}


	}

	private void run() throws IOException,Exception
	{
			// find all the folders that contain the html files		
			// variables used to open files, read files and compute the md5 hases
			File file = null;
			InputStream in = null;
			Document doc;
			//ArrayList<String> md5Hashes = new ArrayList<String>();
			StringBuilder slider = new StringBuilder(); // used to slide through the document
			// loop through each of the folders in the directory and for each file commute the window and store the hashes
			for (String folder: folderList) // 1
			{
				// read all the files in this folder
				readFile(folder);			
				for (String fileName: fileList) // 2
				{
					file = new File(directory+folder+ "/" + fileName);
					//System.out.println("==============" + file.getName());
					in = new FileInputStream(file);
					ArrayList<String> md5Hashes = new ArrayList<String>();

					doc = Jsoup.parse(file,"UTF-8");
					if (doc.body()!=null) // 3
					{
						String [] array = doc.body().getElementsByTag("b").text().replaceAll("[^a-zA-Z ]","").split(" ");
						int start = 0; // start of the string
						int end = start + window;
/* -------------------------------------------------------------------------------------------------------
	-- We are hashing the while document here
	-- We hash the document using a sliding window
-------------------------------------------------------------------------------------------------------- */
						while (end < array.length)
						{
							for (int i = start; i <=end;++i)
								slider.append(array[i]);
							md5Hashes.add(hashString(slider.toString(),"MD5"));
							totalSize+=slider.length();//get the size
							slider.setLength(0);
							start++;
							end++; // sliding window
						}
						totalWindowPieces = md5Hashes.size(); // get total window pieces
/* -------------------------------------------------------------------------------------------------------
	-- We will start running the karb rabin hear
	-- We check for each boundary and see which should be a boundary using the mod
-------------------------------------------------------------------------------------------------------- */
						// this is used to scroll through the document boundry
						int documentStart = 0;
						BigInteger val;
						for (int i = 0; i < md5Hashes.size(); ++i)
						{ 
							val = new BigInteger(md5Hashes.get(i),16);
							// check if this is a valid boundary
							// if it is, hash the entire block (starting from the last bounary)
							if (val.mod(mod).equals(modValue))
							{
								// get all the values in this hash boundary
								for (int j = documentStart; j<=i;++j)
									slider.append(array[j]);
								String hash = hashString(slider.toString(),"MD5"); // compute the hash
								slider.setLength(0); // clear the length for the next boundary
								documentStart = i+1; // this is the start of the next boundary
								numOfPieces++; // we have another piece
								// check if this boundary exists or not yet
								if (matches.get(hash)==null)
									matches.put(hash,1); 
								else
								{
									matches.put(hash,matches.get(hash) + 1);
									// increment coverage
									coverage+= slider.length();

								}

							}							
						}
						// we have the last boundary to compute
						slider.setLength(0);
						for (int i = documentStart; i < array.length;++i)
							slider.append(array[i]);
						String hash = hashString(slider.toString(),"MD5");
						numOfPieces++;
						if (matches.get(hash)==null)
							matches.put(hash,1); 

						else
						{
							matches.put(hash,matches.get(hash) + 1);
							coverage+=slider.length();
	
						}
						
					} // 3.end of the if statement that checks if the body is null
					in.close(); // closes the inputStream
				
					slider.setLength(0);
				} // 2.end of the for loop that gets all the files
				
			} //1. end of the dir for loop
			//System.out.println(matchCounter);
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

		// clear the directory
		folderList.clear();

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












