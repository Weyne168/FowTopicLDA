package local.fow.topic.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;

public class FowTopicModel {

	public double alpha, beta, delta, mu, rho, epsilon; // model hyperparameters

	public int M; // dataset size (i.e., number of docs)
	public int V; // vocabulary size
	public int L; // user vocabulary size
	public int K; // number of topics
	public int B;
	// public int C;

	public String dir;
	public String modelName;

	// define the suffixes of result files
	public static String text_tassignSuffix; // suffix for topic assignment file
	public static String link_tassignSuffix; // suffix for topic assignment file
	// public static String comm_tassignSuffix; // suffix for topic assignment
	// file
	public static String beh_tassignSuffix; // suffix for topic assignment file
	public static String hot_tassignSuffix; // suffix for topic assignment file

	public String paramsSuffix; // suffix for containing other parameters
	public String prefix;

	public int modelStatus; // see Constants class for status of model

	public DataSet textualData;
	public DataSet socialData;
	public DataSet behaviorData;
	// public DataSet communityData;

	Dictionary wordDict;
	Dictionary nodeDict;

	public ProfileDataSet pro_set;

	public int max_iters; // total number of iteration
	public int savestep; // saving period
	public int curstep;

	protected int[][] behaviors;
	protected int[] behsum;
	protected int bsum;
	protected int[] hot_nt;//

	// Temp variables while sampling
	protected int[][] nwt; // nwdt[i][j]: number of instances of word/term i
							// assigned to topic j, size V x K
	protected int[][] ndoct; // ndoct[i][j]: number of words in document i
								// assigned to topic j, size M x K
	protected int[] nwsumt; // nwdsumt[j]: total number of words assigned to
							// topic j, size K
	protected int[] doc_len; // doc_len[i]: total number of words in document i,
								// size M

	protected int[][] nlnkt; // nlnkt[i][j]: number of instances of social link
								// i assigned to topic j, size M x K
	// protected int[][] nsoct; // nsoct[i][j]: number of links in social
	// document i assigned to topic j, size M x K
	protected int[] nlnksumt; // nlnksumt[j]: total number of links assigned to
								// topic j, size K
	protected int[] soc_len; // soc_len[i]: total number of links in social
								// document i, size M

	protected int[] fowed_len;
	// protected int[][] ncomt; // nlnkt[i][j]: number of instances of social
	// link i assigned to topic j, size M x K
	// protected int[][] ncoct; // nsoct[i][j]: number of links in social
	// document i assigned to topic j, size M x K
	// protected int[] ncomsumt; // nlnksumt[j]: total number of links assigned
	// to topic j, size K

	double Kalpha;
	double Vbeta;
	double Ldelta;

	// double Bmu;
	// double Cnu;

	public FowTopicModel() {
		setDefaultValues();
	}

	public FowTopicModel(String dir) {
		setDefaultValues();
		this.dir = dir;
		prefix = dir + modelName;
	}

	public FowTopicModel(String dir, String model_name) {
		setDefaultValues();
		this.dir = dir;
		this.modelName = model_name;
		prefix = dir + modelName;
		readParamFile(prefix + '.' + paramsSuffix);
	}

	public void setDir(String dir) {
		this.dir = dir;
		prefix = dir + modelName;
	}

	/**
	 * Set default values for variables
	 */
	public void setDefaultValues() {
		dir = "./";
		modelName = "fow";
		prefix = dir + modelName;
		text_tassignSuffix = "txt.assign";
		link_tassignSuffix = "lnk.assign";
		// comm_tassignSuffix = "com.assign";
		beh_tassignSuffix = "beh.assign";
		hot_tassignSuffix = "stars";
		paramsSuffix = "params";

		M = 0;
		V = 0;
		L = 0;
		K = 50;
		B = 2;

		alpha = 50.0 / K;
		beta = 0.01;
		delta = 0.01;

		mu = 1.0;
		rho = 0.1 / B;

		epsilon = 0.01;

		max_iters = 100;
		savestep = max_iters / 10;// 每进行迭代次数的十分之一次时保存中间结果
		curstep = 0;
	}

	public void setParams(double alpha, double beta, double delta, double mu,
			int maxIters, int save_step, int K) {
		this.alpha = alpha;
		this.beta = beta;
		this.delta = delta;
		this.mu = mu;
		this.max_iters = maxIters;
		this.savestep = save_step;
		this.K = K;
	}

	public void initDataSet() {
		ndoct = new int[M][K];
		doc_len = new int[M];
		soc_len = new int[M];
		behaviors = new int[M][B];
	}

	
	public void initialize() {
		M = pro_set.pids.size();
		initDataSet();

		for (int m = 0; m < M; m++) {
			String pid = pro_set.pids.get(m);
			if (textualData.did2local.containsKey(pid)) {
				int doc_m = textualData.did2local.get(pid);
				// int tm = pro_set.doc2id.get(pid);
				int N = textualData.docs.get(doc_m).length;

				for (int n = 0; n < N; n++) {
					int topic = (int) Math.floor(Math.random() * K);
					textualData.docs.get(doc_m).topics[n] = topic;
					ndoct[m][topic] += 1;
					nwt[textualData.docs.get(doc_m).words[n]][topic] += 1;
					nwsumt[topic] += 1;
				}
				doc_len[m] = N;
			}

			if (socialData.did2local.containsKey(pid)) {
				int soc_m = socialData.did2local.get(pid);
				// int tm = socialData.localDict.word2id.get(pid);
				int l = socialData.docs.get(soc_m).length;

				for (int n = 0; n < l; n++) {
					int behavior = (int) Math.floor(Math.random() * B);
					behaviorData.docs.get(soc_m).topics[n] = behavior;
					behaviors[m][behavior] += 1;
					behsum[behavior] += 1;
					bsum += 1;
					if (behavior == 0) // {// reason for popular star
						hot_nt[socialData.docs.get(soc_m).words[n]] += 1;
					
					else{
						int topic = (int) Math.floor(Math.random() * K);
						socialData.docs.get(soc_m).topics[n] = topic;
						ndoct[m][topic] += 1;
						nlnkt[socialData.docs.get(soc_m).words[n]][topic] += 1;
						nlnksumt[topic] += 1;
					}
				}
				soc_len[m] = l;
			}
		}
	}

	public void recover() {
		for (int m = 0; m < M; m++) {
			String pid = pro_set.pids.get(m);
			if (textualData.did2local.containsKey(pid)) {
				int doc_m = textualData.did2local.get(pid);
				
				int N = textualData.docs.get(doc_m).length;

				for (int n = 0; n < N; n++) {
					int topic = textualData.docs.get(doc_m).topics[n];
					ndoct[m][topic] += 1;
					nwt[textualData.docs.get(doc_m).words[n]][topic] += 1;
					nwsumt[topic] += 1;
				}
				doc_len[doc_m] = N;
			}

			if (socialData.did2local.containsKey(pid)) {
				int soc_m = socialData.did2local.get(pid);
				int tm = socialData.localDict.word2id.get(pid);
				int l = socialData.docs.get(soc_m).length;

				for (int n = 0; n < l; n++) {
					int topic = socialData.docs.get(soc_m).topics[n];
					ndoct[tm][topic] += 1;
					nlnkt[socialData.docs.get(soc_m).words[n]][topic] += 1;
					nlnksumt[topic] += 1;

					ndoct[socialData.docs.get(soc_m).words[n]][topic] += 1;
					nlnkt[tm][topic] += 1;
					nlnksumt[topic] += 1;
					fowed_len[socialData.docs.get(soc_m).words[n]] += 1;

					int behavior = behaviorData.docs.get(soc_m).topics[n];
					behaviors[soc_m][behavior] += 1;
					behsum[behavior] += 1;
				}
				soc_len[soc_m] = l;
			}
		}
	}

	public void initDict(String tex_dictFile,String soc_dictFile){
		nodeDict = Dictionary.loadDictionary(soc_dictFile);
		wordDict = Dictionary.loadDictionary(tex_dictFile);
		
		V = wordDict.size;
		L = nodeDict.size;

		Vbeta = V * beta;
		Ldelta = L * delta;
		Kalpha = K * alpha;
	}
	
	public void init() {
		nwt = new int[V][K];
		nlnkt = new int[L][K];
		behaviors = new int[L][B];
		
		nwsumt = new int[K];
		nlnksumt = new int[K];
		behsum = new int[B];
		hot_nt = new int[L];
	}
	
	/**
	 * initialize new model for estimation
	 */
	public boolean loadData(String textFile,String socialFile) {
		textualData = DataSet.readDataSet(textFile, wordDict);
		if (textualData == null) {
			System.out.println("Fail to read training data!\n");
			return false;
		}
		
		socialData = DataSet.readDataSet(socialFile, nodeDict);
		if (socialData == null) {
			System.out.println("Fail to read training data!\n");
			return false;
		}
		behaviorData = DataSet.readDataSet(socialFile, nodeDict);
		
		pro_set = new ProfileDataSet(textualData, socialData);
		return true;
	}

	/**
	 * read other file to get parameters conf file
	 * 
	 * @throws IOException
	 */
	protected boolean readParamFile(String paramFile) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(paramFile), "UTF-8"));
			String line = null;

			while ((line = reader.readLine()) != null) {
				StringTokenizer tknr = new StringTokenizer(line, "= \t\r\n");
				int count = tknr.countTokens();
				if (count != 2)
					continue;
				String optstr = tknr.nextToken();
				String optval = tknr.nextToken();

				if (optstr.equalsIgnoreCase("alpha")) {
					alpha = Double.parseDouble(optval);
				} else if (optstr.equalsIgnoreCase("mu")) {
					mu = Double.parseDouble(optval);
				} else if (optstr.equalsIgnoreCase("rho")) {
					rho = Double.parseDouble(optval);
				} else if (optstr.equalsIgnoreCase("beta")) {
					beta = Double.parseDouble(optval);
				} else if (optstr.equalsIgnoreCase("delta")) {
					delta = Double.parseDouble(optval);
				} else if (optstr.equalsIgnoreCase("epsilon")) {
					epsilon = Double.parseDouble(optval);
				} else if (optstr.equalsIgnoreCase("savestep")) {
					savestep = Integer.parseInt(optval);
				} else if (optstr.equalsIgnoreCase("MaxIters")) {
					max_iters = Integer.parseInt(optval);
				} else if (optstr.equalsIgnoreCase("K")) {
					K = Integer.parseInt(optval);
				} else if (optstr.equalsIgnoreCase("V")) {
					V = Integer.parseInt(optval);
				} else if (optstr.equalsIgnoreCase("L")) {
					L = Integer.parseInt(optval);
				} else if (optstr.equalsIgnoreCase("B")) {
					B = Integer.parseInt(optval);
				} else if (optstr.equalsIgnoreCase("M")) {
					M = Integer.parseInt(optval);
				}
			}
			reader.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	protected DataSet readTAssignFile(String tassignFile) {
		DataSet data = new DataSet();
		int j;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(tassignFile), "UTF-8"));
			String line = null;
			while ((line = reader.readLine()) != null) {// every line is a doc
				StringTokenizer tknr = new StringTokenizer(line, "\t");
				int count = tknr.countTokens();
				if (count != 2)
					continue;
				String doc_id = tknr.nextToken();
				String txt = tknr.nextToken();

				String[] tokens = txt.split(" ");
				Vector<Integer> words = new Vector<Integer>();
				Vector<Integer> topics = new Vector<Integer>();
				int length = tokens.length;
				for (j = 0; j < length; j++) {
					StringTokenizer tknr2 = new StringTokenizer(tokens[j], ":");// word_id:topic_id
					if (tknr2.countTokens() != 2) {
						continue;
					}
					words.add(Integer.parseInt(tknr2.nextToken()));
					topics.add(Integer.parseInt(tknr2.nextToken()));
				}

				Doc doc = new Doc(words, topics, doc_id);
				data.setDoc(doc, doc_id);
			}
			reader.close();
			return data;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected ArrayList readTAssignFile(String tassignFile,int m,int n) {
		int j;
		int word;
		int topic;
		int[][]data=new int[m][n];
		int []sumD=new int[n];
		ArrayList res=new ArrayList();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tassignFile), "UTF-8"));
			String line = null;
			while ((line = reader.readLine()) != null) {// every line is a doc
				String[] tknr = line.split( "\t");
				if (tknr.length != 2)
					continue;
				
				String[] tokens = tknr[1].split(" ");
				int length = tokens.length;
				for (j = 0; j < length; j++) {
					StringTokenizer tknr2 = new StringTokenizer(tokens[j], ":");// word_id:topic_id
					if (tknr2.countTokens() != 2) {
						continue;
					}
					word=Integer.parseInt(tknr2.nextToken());
					topic=Integer.parseInt(tknr2.nextToken());
					data[word][topic]+=1;
					sumD[topic]+=1;
				}
			}
			reader.close();
			res.add(data);
			res.add(sumD);
			return res;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	protected void readHotStars(String tassignFile) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader( new FileInputStream(tassignFile), "UTF-8"));
			String line = null;
			while ((line = reader.readLine()) != null) {// every line is a doc
				StringTokenizer tknr = new StringTokenizer(line, "\t");
				int count = tknr.countTokens();
				if (count != 2)
					continue;
				String idx = tknr.nextToken();
				String t = tknr.nextToken();
				int i = socialData.localDict.word2id.get(idx);
				hot_nt[i] = Integer.parseInt(t);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public boolean loadModel(String token) {
		String paramPath = prefix + '.' + paramsSuffix;
		if (!readParamFile(paramPath))
			return false;
		ArrayList res = null;

		String text_data = prefix + '.' + text_tassignSuffix + '.' + token;
		res = readTAssignFile(text_data, V, K);
		nwt = (int[][]) res.get(0);
		nwsumt = (int[]) res.get(1);
		if (res == null)
			return false;

		String social_data = prefix + '.' + link_tassignSuffix + '.' + token;
		res = readTAssignFile(social_data, L, K);
		nlnkt = (int[][]) res.get(0);
		nlnksumt = (int[]) res.get(1);
		if (res == null)
			return false;
		
		String behavior_data = prefix + '.' + beh_tassignSuffix + '.' + token;
		res = readTAssignFile(behavior_data, L, 2);
		behaviors = (int[][]) res.get(0);
		behsum = (int[]) res.get(1);
		if (res == null)
			return false;
		
		String hot_data = prefix + '.' + hot_tassignSuffix + '.' + token;
		readHotStars(hot_data);
		return true;
	}

	/**
	 * Save word-topic assignments for this model
	 */
	public boolean saveModelTAssign(String filename, DataSet data) {
		int i, j;
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			StringBuffer line = new StringBuffer();
			for (i = 0; i < data.docs.size(); i++) {
				line.setLength(0);
				line.append(data.docs.get(i).index_str + '\t');
				for (j = 0; j < data.docs.get(i).length; ++j) {
					line.append(String.valueOf(data.docs.get(i).words[j]) + ':' + String.valueOf(data.docs.get(i).topics[j]) + ' ');
				}
				writer.write(line.substring(0, line.length() - 1) + '\n');
			}
			writer.flush();
			line = null;
			writer.close();
			writer.close();
		} catch (Exception e) {
			System.out.println("Error while saving model tassign: "
					+ e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean saveHotStars(String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			for (int i = 0; i < hot_nt.length; i++) {
				String line = socialData.localDict.id2word.get(i) + '\t'
						+ String.valueOf(hot_nt[i]) + '\n';
				writer.write(line);
			}
			writer.close();
		} catch (Exception e) {
			System.out.println("Error while saving hot stars: "
					+ e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean saveParams(String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			String line = "alpha=" + String.valueOf(alpha) + '\n' + "mu="
					+ String.valueOf(mu) + '\n' + "rho=" + String.valueOf(rho)
					+ '\n' + "beta=" + String.valueOf(beta) + '\n' + "delta="
					+ String.valueOf(delta) + '\n' + "epsilon="
					+ String.valueOf(epsilon) + '\n' + "K=" + String.valueOf(K)
					+ '\n' + "B=" + String.valueOf(B) + '\n' + "V="
					+ String.valueOf(V) + '\n' + "L=" + String.valueOf(L)
					+ '\n' + "M=" + String.valueOf(M) + '\n';
			writer.write(line);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void saveModel(String token) {
		String text_data = prefix + "." + text_tassignSuffix + "." + token;
		String social_data = prefix + "." + link_tassignSuffix + "." + token;
		String beh_data = prefix + "." + beh_tassignSuffix + "." + token;
		String hot_data = prefix + "." + hot_tassignSuffix + "." + token;
		
		if (!saveModelTAssign(text_data, textualData)) {
			System.out.println("error while save " + text_data);
			System.exit(0);
		}
		if (!saveModelTAssign(social_data, socialData)) {
			System.out.println("error while save " + social_data);
			System.exit(0);
		}
		if (!saveModelTAssign(beh_data, behaviorData)) {
			System.out.println("error while save " + beh_data);
			System.exit(0);
		}
		if (!saveHotStars(hot_data)) {
			System.out.println("error while save " + hot_data);
			System.exit(0);
		}
		saveParams(prefix + '.' + paramsSuffix);
		System.out.println("save success iter: " + token);
	}

	/**
	 * calculate the theta which combine the textual and social information
	 * linearly
	 * */
	public double[][] theta() {
		double[][] r = new double[M][K];
		for (int m = 0; m < M; m++) {
			for (int k = 0; k < K; k++) {
				r[m][k] = (ndoct[m][k] + alpha)
						/ (doc_len[m] + soc_len[m] + Kalpha);
			}
		}
		return r;
	}

	public double[][] calPhi() {
		double[][] r = new double[K][V];
		for (int k = 0; k < K; k++) {
			for (int w = 0; w < V; w++) {
				r[k][w] = (nwt[w][k] + beta) / (nwsumt[k] + Vbeta);
			}
		}
		return r;
	}

	public double[][] cal_u_Phi() {
		double[][] r = new double[K][L];
		for (int k = 0; k < K; k++) {
			for (int w = 0; w < L; w++) {
				r[k][w] = (nlnkt[w][k] + delta) / (nlnksumt[k] + Ldelta);
			}
		}
		return r;
	}

	public double[][] calBehavior() {
		double[][] r = new double[M][B];
		double bmu = B * mu;
		for (int m = 0; m < M; m++) {
			for (int k = 0; k < B; k++) {
				r[m][k] = (behaviors[m][k] + mu) / (soc_len[m] + bmu);
			}
		}
		return r;
	}

	public double[] calPopularity() {
		double[] r = new double[L];
		double sLepsilon = L * epsilon + behsum[0];
		for (int v = 0; v < L; v++) {
			r[v] = (hot_nt[v] + epsilon) / sLepsilon;
		}
		return r;
	}

	public double cal_likelihood() {
		double[][] theta = theta();
		double[][] phi = calPhi();
		double[][] u_phi = cal_u_Phi();
		double[][] beh = calBehavior();
		double[] pop = calPopularity();
		double likelihood = 0;

		for (int m = 0; m < M; m++) {
			String pid = pro_set.pids.get(m);
			if (textualData.did2local.containsKey(pid)) {
				int doc_m = textualData.did2local.get(pid);
				for (int n = 0; n < textualData.docs.get(doc_m).length; n++) {
					int w = textualData.docs.get(doc_m).words[n];
					double s = 0;
					for (int k = 0; k < K; k++) {
						s += theta[m][k] * phi[k][w];
					}
					likelihood += Math.log(s);
				}
			}

			if (socialData.did2local.containsKey(pid)) {
				int soc_m = socialData.did2local.get(pid);
				for (int n = 0; n < socialData.docs.get(soc_m).length; n++) {
					int w = socialData.docs.get(soc_m).words[n];
					String pro_id = socialData.localDict.id2word.get(w);
					int wm = pro_set.getPid(pro_id);
					double s = 0;
					if (wm > -1) {
						for (int k = 0; k < K; k++) {
							s += theta[m][k] * theta[wm][k] * u_phi[k][w];
						}
					} else {
						for (int k = 0; k < K; k++) {
							s += theta[m][k] * 1.0 / K * u_phi[k][w];
						}
					}
					s *= beh[m][1];
					s += beh[m][0] * pop[w];
					likelihood += Math.log(s);
				}
			}
		}
		theta = null;
		phi = null;
		u_phi = null;
		beh = null;
		pop = null;
		System.gc();
		return likelihood;
	}

	public static void main(String[] args) {
		FowTopicModel fm = new FowTopicModel();
		String confPath = "I:/config.txt";
		System.out.println(confPath.substring(0, confPath.length() - 1));
		// fm.config(confPath);
	}
}
