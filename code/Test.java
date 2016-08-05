import java.util.*;
import java.io.*;
import java.math.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.zip.*;
import java.lang.*;

/*
	Author: Shahzaib Javed
	Purpose: Research for NYU Tandon University



	Abstract: LocalMinima is a content dependant chunking method. It determines the boundaries for the document using the local minima. 
	All content dependant algorithms first hash the document using a sliding window of length w, which we will call the hash array. (12 for all these experiments). This step is true for all content dependant chunking algorithms.
	Next the cut points for the document are determined from the hash array, using a content dependant method, (Local Minima in this case).
	The original document is divided into chunks using the cut points as boundaries between the chunks. Different versions of the documents are
	using where the first chunks of the document are stored, whereas the second version is simply used to see of that portion of the document
	occurred.


	LocalMinima: This algorithm has a parameter, which we will call B or boundarySize associated with it. The algorithm declares a hash a cutpoint
	only if the hash is strictly less than the B hashes before it and B hashes after it. Continue if the current hash fails the conditions.


*/

public class Test{

	public static void main(String [] args){

		String test = "test";
		String t = "t";
		String a = "a";


		System.out.println();

		 BigInteger val = new BigInteger(MD5Hash.hashString(test.toString(),"MD5"),16);
		 BigInteger val2 = new BigInteger(MD5Hash.hashString(t.toString(),"MD5"),16);
		 BigInteger val3 = new BigInteger(MD5Hash.hashString(a.toString(),"MD5"),16);
		 val = val.subtract(val2);
		 val = val.add(val3);
		 System.out.println(val.toString(16));


	}





	public static void hashDocument(byte [] array, ArrayList<Long> md5Hashes, int start, int end ){
		// rolling hash
		StringBuilder builder = new StringBuilder(); // used as a sliding window and compute the hash value of each window
			// only store the lower 32 bits of the md5Hash
		while (end < array.length)
		{
			for (int i = start; i <= end;++i){
				builder.append(array[i]);  // store the byte in a stringbuilder which we will use to compute hashvalue
			}		
			String hash = MD5Hash.hashString(builder.toString(),"MD5"); // compute the hash value
			long val = Long.parseLong(hash.substring(24),16); // compute the int value of the lower 32 bits
			md5Hashes.add(val); // put the hash value
			start++; // increment the starting of the sliding window
			end++; // increment the ending of the sliding window
			builder.setLength(0); // to store the sum of the next window
		}
	}
}
