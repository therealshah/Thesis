import java.util.*;
import java.io.*;
import java.nio.file.Files;



public class ReadFile{
	/*
	* reads all the files within this folder
	* @param folderName - This is the foldername that we will read all the files from
	*/
	public static void readFile(String directory,ArrayList<String> fileList){
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
}