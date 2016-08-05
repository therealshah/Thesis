import java.util.*;
import java.io.*;





public class HashDocument{

	private static byte previous_hash = -1;
	private static long hash_value = 0; // prev hash value
	private static int multiplier = 105943; // prime ##
	private static boolean hasPrev = false; // determine if a prev boundary exists


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


		// // rabin karb kash
		// StringBuilder builder = new StringBuilder(); // used as a sliding window and compute the hash value of each window
		// // only store the lower 32 bits of the md5Hash
		// hash_value = 0;
		// for (int i = start; i <= end;++i){
		// 	hash_value+= array[i] * (Math.pow(multiplier,i));
		// }	
		// md5Hashes.add(hash_value);
		// end++;	// go to next value
		// int m = end - start ;


		// while (end < array.length)
		// {
		// 	// subtract off old value
		// 	hash_value -= array[start]; 
		// 	hash_value = hash_value / multiplier;
		// 	hash_value = hash_value + (long)Math.pow(multiplier,m)*array[end];
		// 	md5Hashes.add(hash_value);

		// 	end++;
		// }
		
	} // end of method
} // end of class