package local.fow.topic.model;

import java.util.Vector;

public class Doc {
	public int[] words;// record the id of each word in dictionary
	public int[] topics;// record topic of each word
	public String index_str;
	public int length;
	
	public Doc(int length) {
		this.length = length;
		initAssgin();
		index_str = "";
		words = new int[length];
	}

	public Doc(int length, int[] words) {
		this.length = length;
		initAssgin();
		index_str = "";
		this.words = new int[length];
		for (int i = 0; i < length; ++i) {
			this.words[i] = words[i];
		}
	}

	public Doc(int length, int[] words, String dco_id) {
		this.length = length;
		initAssgin();
		this.index_str = dco_id;
		this.words = new int[length];
		for (int i = 0; i < length; ++i) {
			this.words[i] = words[i];
		}
	}

	public Doc(Vector<Integer> doc) {
		this.length = doc.size();
		initAssgin();
		index_str = "";
		this.words = new int[length];
		for (int i = 0; i < length; i++) {
			this.words[i] = doc.get(i);
		}
	}

	public Doc(Vector<Integer> doc, Vector<Integer> topics, String dco_id) {
		this.length = doc.size();
		initAssgin();
		this.index_str = dco_id;
		this.words = new int[length];
		for (int i = 0; i < length; i++) {
			this.words[i] = doc.get(i);
			this.topics[i] = topics.get(i);
		}
	}
	
	public Doc(Vector<Integer> doc, String dco_id) {
		this.length = doc.size();
		initAssgin();
		this.index_str = dco_id;
		this.words = new int[length];
		for (int i = 0; i < length; i++) {
			this.words[i] = doc.get(i);
		}
	}
	
	public void initAssgin() {
		this.topics = new int[length];
	}
}
