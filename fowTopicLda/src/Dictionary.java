package local.fow.topic.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;


public class Dictionary {
	public HashMap<String, Integer> word2id;
	public HashMap<Integer, String> id2word;
	int size;
	
	public Dictionary() {
		word2id = new HashMap<String, Integer>();
		id2word = new HashMap<Integer, String>();
		size=0;
	}
	
	
	public int getDictSize(){
		return size;
	}
	
	
	public void setDictSize(int size){
		this.size=size;
	}
	
	
	public String getWord(Integer id) {
		return id2word.get(id);
	}

	
	public int getID(String word) {
		return word2id.get(word);
	}

	
	/**
	 * check if this dictionary contains a specified word
	 */
	public boolean contains(String word) {
		return word2id.containsKey(word);
	}

	
	public boolean contains(int id) {
		return id2word.containsKey(id);
	}

	
	/**
	 * add a word into this dictionary return the corresponding id
	 */
	public int addWord(String word) {
		if (!contains(word)) {
			int id = word2id.size();
			/*if (id > 60000) {
				id = (int) Math.floor(Math.random() * id);
				word2id.remove(id2word.get(id));
			}*/
			word2id.put(word, id);
			id2word.put(id, word);
			size++;
			return id;
		} else
			return getID(word);
	}

	
	/**
	 * read dictionary from local file system
	 */
	public boolean readWordMap(String wordMapFile) {
		File file = new File(wordMapFile);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			// read map
			int k=0;
			while ((line = reader.readLine()) != null) {
				StringTokenizer tknr = new StringTokenizer(line, "=\t\n\r");
				k++;
				if (tknr.countTokens() != 2)
					continue;
				String word = tknr.nextToken();
				String id = tknr.nextToken();
				id2word.put(Integer.parseInt(id), word);
				word2id.put(word, Integer.parseInt(id));
				if(size>0 && id2word.size()>size)
					break;
			}
			reader.close();
			setDictSize(id2word.size());
			return true;

		} catch (Exception e) {
			System.out.println("Error while reading dictionary:"+ e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
	
	public static Dictionary loadDictionary(String dictFile) {
		Dictionary dict = new Dictionary();
		dict.readWordMap(dictFile);
		return dict;
	}

	public static synchronized void saveDictionary(Dictionary dict, String savePath) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(savePath));
			Set<?> set = dict.word2id.entrySet();
			Iterator<?> it = set.iterator();
			while (it.hasNext()) {
				writer.write(it.next().toString() + "\n");
			}
			writer.close();
		} catch (Exception e) {
		}
	}
	
	
	public static Dictionary getInstance() {
		return new Dictionary();
	}
	
	public static void main(String[] args) {
		Dictionary dict = new Dictionary();
		dict.readWordMap("I:/fowmodel/expr/test/soc.dict");
		System.out.println(dict.word2id.size());
		System.out.println(dict.id2word.size());
		
	}
}
