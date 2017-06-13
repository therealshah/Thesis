import java.util.*;
import java.io.*;



/*
	This class contains method for inserting/getting the hash in the table
	All the CDC methods use this method, hence its spearated for modularity purposes
*/


public class HashClass{
	public static int duplicate_counter = 0; // this counts the number of duplicates for the hash value. Not must be reset every time
	public static int max_list_length = 0; // just a check to see which duplicate has the biggest length
	/*
		- Insert the hash into the table. Probability of collision is slim for MD5, hence strings were not compared
		- if the the hash was already present
		@params
			- Original - originall string that we will be storing
			- table - hashtable that store hash value with associated strings

	*/
	public static void put_hash(String original,HashSet<String> table){
		String hash = MD5Hash.hashString(original,"MD5");	// hash this boundary. USe MD5 to reduce the probability of collisio
		table.add(hash);
	} // end of method

	/*
		- Basically checks if the passed in string exists in the table
		- return true if yes and no otherwise
		@params
			- Original - originall string that we will be storing
			- table - hashtable that store hash value with associated strings
	*/
	public static boolean is_string_match(String original,HashSet<String> table){
		String hash = MD5Hash.hashString(original,"MD5");	// hash this boundary
		return table.contains(hash);
		
	} //  end of metod

} // end of class