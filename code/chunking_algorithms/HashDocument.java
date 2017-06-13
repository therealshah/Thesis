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
	-------------------------------------------------------------------------------------------------------- */
	public static void hashDocument(byte [] array, ArrayList<Long> md5Hashes, int start, int end ){

		zsync_adler32_hash(array,md5Hashes,start,end);

	} // end of method

		/* -------------------------------------------------------------------------------------------------------
	This method:
		-- Takes in four params: 
				1. array - this is the byte array that actually holds the document contents
				2. md5Hashes - will store the hash values of the entire document hashed
				3. Start - starting point of the hash window (most likely 0)
				4. End - ending point of the hash window 
		-- We are hashing only the portion we are passed in and return it

	-------------------------------------------------------------------------------------------------------- */
	public static long hashValue(byte [] array, int start, int end ){

		return zsync_adler32_hash(array,start,end); // hash a single value

	} // end of m

	/*
		-- hashes the document using md5hash
		-- We will compute the md5Hash and only store the lower 32 bits (4bytes each)
	*/
	private static void compute_md5_hash(byte [] array, ArrayList<Long> md5Hashes, int start, int end){

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

	/*
		-- Computes the adler32 hash of the entire document
	*/
	private static void zsync_adler32_hash(byte [] array, ArrayList<Long> md5Hashes, int start, int end){
		long s1 = 0;
		long s2 = 0;
		long s3 = 0;
		long s4 = 0;
		long hash_value;
		int len = 0;

		// get initial values
		for(int i = start; i <= end; i++){
		 s1 += array[i];
		 s2 += s1;
		 s3 += s2;
		 s4 += s3;
		 len++;
		}
		s1 &= 0xff;  s2 &= 0xff;  s3 &= 0xff;  s4 &= 0xff; // make numbers unsigned
  		hash_value = (s4 << 24) | (s3 << 16) | (s2 << 8) | s1;
		md5Hashes.add(hash_value);
		start++;
		end++; // go to next window

		// compute rolling hash
		while (end < array.length){
			// s1 = hash_value       & 0xff;
			// s2 = (hash_value>>8)  & 0xff;
			// s3 = (hash_value>>16) & 0xff;
			// s4 = (hash_value>>24) & 0xff;
			int temp = len * (len + 1) / 2;

			/* Trim off the first byte from the checksum */
			s1 -= array[start-1];
			s2 -= array[start-1] * len;
			s3 -= array[start-1] * temp;
			temp = (temp * (len + 2)) / 3;
			s4 -= array[start-1] * temp;

			/* Add on the next byte to the checksum */
			s1 += array[end];
			s2 += s1;
			s3 += s2;
			s4 += s3;

			s1 &= 0xff;  s2 &= 0xff;  s3 &= 0xff;  s4 &= 0xff;
			hash_value = (s4 << 24) | (s3 << 16) | (s2 << 8) | s1;
			md5Hashes.add(hash_value);
			end++;
			start++;
		}
	} //  end of method

	/*
		-- Computes the adler32 hash of the small section of the array
	*/
	private static long zsync_adler32_hash(byte [] array, int start, int end){
		long s1 = 0;
		long s2 = 0;
		long s3 = 0;
		long s4 = 0;
		long hash_value;
		int len = 0;

		// get initial values
		for(int i = start; i <= end; i++){
		 s1 += array[i];
		 s2 += s1;
		 s3 += s2;
		 s4 += s3;
		 len++;
		}
		s1 &= 0xff;  s2 &= 0xff;  s3 &= 0xff;  s4 &= 0xff; // make numbers unsigned
  		hash_value = (s4 << 24) | (s3 << 16) | (s2 << 8) | s1;
		return hash_value;
	} //  end of method

} // end of class