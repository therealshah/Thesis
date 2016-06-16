import java.util.*;
import java.io.*;


public class Test1{

	private static ArrayList<Integer> t =new ArrayList<Integer>();;


	public  void  run(){
		if (t.size() !=0){
			System.out.println("oho");
			System.out.println(t.get(0));
		}
		else{
			t.add(1);
		}
	}


}