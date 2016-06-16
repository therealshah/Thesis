import java.util.*;
import java.io.*;





public class HashDocument{


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
	public static void hashDocument(byte [] array, ArrayList<Long> md5Hashes, int start, int end ){

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