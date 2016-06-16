


/*
	-- Helper class to run all the CDC timing algos with just this methid
	-- saves times rather having to run each one individually

	-- NOTE
		-- we make a method for each cdc type and within each method we create a local variable. We do this because we dont want java to 
		-- store the variable information 
*/



public class RunTiming{

	private static String dir = "../thesis/gcc/";


	public static void main(String [] arg) throws Exception{

		KarbRabinTiming.run(dir);
		WinnowingTiming.run(dir);
		TdddTiming.run(dir);
		LocalMinimaTiming.run(dir);
	}

	
}