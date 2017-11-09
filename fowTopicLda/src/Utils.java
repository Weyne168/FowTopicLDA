package local.fow.topic.model;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;


public class Utils {

	public static String getJsonValue(String jsonStr,String key) throws JSONException{
		JSONObject json= new JSONObject(jsonStr);
		return json.getString(key);
	}
	
	
	public static void main(String[] args) throws JSONException {
		// TODO Auto-generated method stub
		String jsonStr="";
		Utils.getJsonValue(jsonStr,"");
	}
	
	
	public static void log(String logs,String logFile) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(logFile,true));
			writer.write(logs+"\n");
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static double sigmoid(double x) {
		double r = Math.exp(-1 * x);
		r += 1;
		return 1 / r;
	}
}
