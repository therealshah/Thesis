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
		- Check if the hash exisits in the table, if not make a new entry
		- if the hash exisits, then ck if this string already exisits, if it doesnt then we have a collision
		- so incremement counter
		@params
			- Original - originall string that we will be storing
			- table - hashtable that store hash value with associated strings

	*/
	public static void put_hash(String original,HashMap<String,ArrayList<String>> table){
		String hash = MD5Hash.hashString(original,"MD5");	// hash this boundary
		if (table.get(hash) == null){
			// insert it with a new arraylist
			ArrayList<String> list = new ArrayList<String>(); // make a new list to hold the values
			list.add(original); // insert the actual string in the builder
			table.put(hash,list); // add the list in there
		}
		else{
			// else there is a hash and only insert this string in the table only if it does't exist
			ArrayList<String> list = table.get(hash);
			// if this string already doesnt exist, insert it and incrememnt counter
			if (!list.contains(original)){
				list.add(original);
				duplicate_counter++; 
				System.out.println("Duplicate encountered");
			}
		}
	} // end of method

	/*
		- Basically checks if the passed in string exists in the table
		- return true if yes and no otherwise
		@params
			- Original - originall string that we will be storing
			- table - hashtable that store hash value with associated strings
	*/
	public static boolean is_string_match(String original,HashMap<String,ArrayList<String>> table){
		String hash = MD5Hash.hashString(original,"MD5");	// hash this boundaryy
		if (table.get(hash) != null){
			ArrayList<String> list = table.get(hash);
			// if this string already doesnt exist, insert it and incrememnt counter
			return list.contains(original); // ck if original is in there
		}
		else
			return false; // doesn't exist
		
	} //  end of metod

} // end of class