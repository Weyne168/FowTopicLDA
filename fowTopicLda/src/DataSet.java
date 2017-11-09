package local.fow.topic.model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public class DataSet {
	public Dictionary localDict; // local dictionary
	public ArrayList<Doc> docs; // a list of documents
	public HashMap<String, Integer> did2local;
	
	public DataSet() {
		localDict = Dictionary.getInstance();
		//M = 0;
		//V = 0;
		docs = new ArrayList<Doc>();
		did2local = new HashMap<String, Integer>();
		//max_len = 0;
	}

	public DataSet(Dictionary dict) {
		localDict = dict;
		//M = 0;
		//V = dict.size;
		docs = new ArrayList<Doc>();
		did2local = new HashMap<String, Integer>();
		//max_len = 0;
	}

	/**
	 * set the document at the index idx if idx is greater than 0 and less than
	 * M
	 * 
	 * @param doc
	 *            document to be set
	 * @param idx
	 *            index in the document array
	 */
	public void setDoc(Doc doc, String idx) {
		Integer size = docs.size();
		did2local.put(idx, size);
		docs.add(doc);
		/*M++;
		if (max_len < doc.length)
			max_len = doc.length;*/
	}

	/**
	 * set the document at the index idx if idx is greater than 0 and less than
	 * M
	 * 
	 * @param str
	 *            string contains doc
	 * @param idx
	 *            index in the document array
	 */
	public void setDoc(String str) {
		String[] words = str.trim().split("[, \\t\\r\\n]");// doc_id word_list
		Vector<Integer> wids = new Vector<Integer>();
		
		for (int w = 1; w < words.length; w++) {
			if (localDict.contains(words[w])) {
				wids.add(localDict.getID(words[w]));
			}
		}

		if (wids.size() > 0) {
			Doc doc = new Doc(wids, words[0]);
			did2local.put(words[0], docs.size());
			docs.add(doc);
			/*M++;
			if (max_len < doc.length)
				max_len = doc.length;*/
		}
	}

	/**
	 * set the document at the index idx if idx is greater than 0 and less than
	 * M
	 * 
	 * @param str
	 *            string contains doc
	 * @param idx
	 *            index in the document array
	 */
	public void setDoc(String str, boolean noDict) {
		String[] words = str.trim().split("[, \\t\\r\\n]");// doc_id word_list
		Vector<Integer> wids = new Vector<Integer>();
		for (int w = 1; w < words.length; w++) {
			int wid = localDict.addWord(words[w]);
			wids.add(wid);
		}
		// if (wids.size() > 0) {
		Doc doc = new Doc(wids, words[0]);
		did2local.put(words[0], docs.size());
		docs.add(doc);
		/*M++;
		if (max_len < doc.length)
			max_len = doc.length;
		// }*/
	}

	/**
	 * read a dataset from a stream, create and new dictionary
	 * 
	 * @return dataset if success and null otherwise
	 */
	public static DataSet readDataSet(String filename) {// do not have
														// dictionary
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(filename), "UTF-8"));
			DataSet data = readDataSet(reader);
			reader.close();
			//data.V = data.localDict.getDictSize();
			return data;
		} catch (Exception e) {
			System.out.println("Read Dataset Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * read a dataset from a stream and create a new dictionary
	 * 
	 * @return dataset if success and null otherwise
	 */
	public static DataSet readDataSet(BufferedReader reader) {
		try {
			String line;
			DataSet data = new DataSet();
			while ((line = reader.readLine()) != null) {
				data.setDoc(line, true);
			}
			return data;
		} catch (Exception e) {
			System.out.println("Read Dataset Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * read a dataset from a file with a preknown vocabulary
	 * 
	 * @param filename
	 *            file from which we read dataset
	 * @param dictFile
	 *            is the path of dictionary
	 * @return dataset if success and null otherwise
	 */
	public static DataSet readDataSet(String filename, Dictionary dict) {
		// Dictionary dict = Dictionary.loadDictionary(dictFile);
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			DataSet data = readDataSet(reader, dict);
			reader.close();
			//data.V = data.localDict.getDictSize();
			return data;
		} catch (Exception e) {
			System.out.println("Read Dataset Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * read a dataset from a stream with respect to a specified dictionary
	 * 
	 * @param reader
	 *            stream from which we read dataset
	 * @param dict
	 *            the dictionary
	 * @return dataset if success and null otherwise
	 */
	public static DataSet readDataSet(BufferedReader reader, Dictionary dict) {
		try {
			// read number of document
			String line;
			DataSet data = new DataSet(dict);
			while ((line = reader.readLine()) != null) {
				data.setDoc(line);
			}
			return data;
		} catch (Exception e) {
			System.out.println("Read Dataset Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public static void run(String fileName, String savePath) {
		DataSet data = readDataSet(fileName);
		Dictionary.saveDictionary(data.localDict, savePath);
	}

	public static void main(String[] args) {
		DataSet data = readDataSet("I:/fowmodel/expr/test/rel.fol.t");
		Dictionary.saveDictionary(data.localDict, "I:/fowmodel/expr/test/"
				+ "soc.dict");
	}
}
