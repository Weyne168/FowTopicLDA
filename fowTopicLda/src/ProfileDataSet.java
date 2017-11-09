package local.fow.topic.model;

import java.util.HashMap;
import java.util.Vector;


public class ProfileDataSet {
	HashMap<String, Integer> profile2id;
	Vector<String> pids;

	public ProfileDataSet(DataSet textualSet, DataSet socialSet) {
		profile2id = new HashMap<String, Integer>();
		pids = new Vector<String>();

		for (int i = 0; i < textualSet.docs.size(); i++)
			if (textualSet.docs.get(i).length > 0)
				addProfile(textualSet.docs.get(i).index_str);

		for (int i = 0; i < socialSet.docs.size(); i++)
			if (socialSet.docs.get(i).length > 0)
				addProfile(socialSet.docs.get(i).index_str);
	}
	
	void addProfile(String profile_id) {
		if (!profile2id.containsKey(profile_id)) {
			profile2id.put(profile_id, pids.size());
			pids.add(profile_id);
		}
	}

	public int getPid(String index_str) {
		if (profile2id.containsKey(index_str)) {
			return profile2id.get(index_str);
		}
		return -1;
	}
}
