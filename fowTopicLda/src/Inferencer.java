package local.fow.topic.model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class Inferencer {

	public FowTopicModel trnModel;// Trained model
	public FowTopicModel newModel;// result model

	int max_iters = 100;
	int savestep = max_iters / 10;
	int curstep = 0;
	double likelihood;
	double error;

	protected double[] p;

	String textdataPath;
	String linkdataPath;
	String tex_dictFile;
	String lnk_dictFile;
	String savePath;

	public Inferencer(String confPath) {
		String dir = null;
		String trndModelName = null;
		String token = null;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(confPath), "UTF-8"));
			String line = null;
			while ((line = reader.readLine()) != null) {
				StringTokenizer tknr = new StringTokenizer(line, "= \t\r\n");
				int count = tknr.countTokens();
				if (count != 2)
					continue;
				String optstr = tknr.nextToken();
				String optval = tknr.nextToken();
				if (optstr.equalsIgnoreCase("textdata")) {
					textdataPath = optval;
				} else if (optstr.equalsIgnoreCase("linkdata")) {
					linkdataPath = optval;
				} else if (optstr.equalsIgnoreCase("tex_dict")) {
					tex_dictFile = optval;
				} else if (optstr.equalsIgnoreCase("lnk_dict")) {
					lnk_dictFile = optval;
				} else if (optstr.equalsIgnoreCase("dir")) {
					dir = optval;
				} else if (optstr.equalsIgnoreCase("trnModel")) {
					trndModelName = optval;
				} else if (optstr.equalsIgnoreCase("token")) {
					token = optval;
				} else if (optstr.equalsIgnoreCase("savestep")) {
					savestep = Integer.parseInt(optval);
				} else if (optstr.equalsIgnoreCase("MaxIters")) {
					max_iters = Integer.parseInt(optval);
				} else if (optstr.equalsIgnoreCase("savePath")) {
					savePath = optval;
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		trnModel = new FowTopicModel(dir, trndModelName);
		trnModel.initDict(tex_dictFile, lnk_dictFile);
		trnModel.loadModel(token);
		p = new double[trnModel.K];
		likelihood = 0;
	}

	void initNewModel() {
		newModel = new FowTopicModel(savePath, trnModel.modelName);
		System.out.println(trnModel.prefix);
		newModel.readParamFile(trnModel.prefix + '.' + trnModel.paramsSuffix);
		
		newModel.loadData(textdataPath,  linkdataPath);
		newModel.init();
		newModel.initialize();
		
		newModel.curstep = 0;
		newModel.max_iters = max_iters;
	}

	void inference() {
		for (int m = 0; m < newModel.M; m++) {
			String pid = newModel.pro_set.pids.get(m);
			if (newModel.textualData.did2local.containsKey(pid)) {
				int doc_m = newModel.textualData.did2local.get(pid);
				for (int n = 0; n < newModel.textualData.docs.get(doc_m).length; n++) {
					int topic = texInfSampling(m, doc_m, n);
					newModel.textualData.docs.get(doc_m).topics[n] = topic;
				}
			}
			
			if (newModel.socialData.did2local.containsKey(pid)) {
				int soc_m = newModel.socialData.did2local.get(pid);
				for (int n = 0; n < newModel.socialData.docs.get(soc_m).length; n++) {
					int behavior = behaviorInfSampling(m, soc_m, n);
					newModel.behaviorData.docs.get(soc_m).topics[n] = behavior;
					if (behavior == 0) {
						newModel.hot_nt[newModel.socialData.docs.get(soc_m).words[n]] += 1;
					} else if (behavior == 1) {
						int topic = socInfSampling(m, soc_m, n);
						newModel.socialData.docs.get(soc_m).topics[n] = topic;
					}
				}
			}
		}
	}

	
	/**
	 * do sampling for inference m: document number n: word number?
	 */
	protected int texInfSampling(int tm, int doc_m, int n) {
		int topic = newModel.textualData.docs.get(doc_m).topics[n];
		int w = newModel.textualData.docs.get(doc_m).words[n];

		newModel.nwt[w][topic] -= 1;
		newModel.ndoct[tm][topic] -= 1;
		newModel.nwsumt[topic] -= 1;
		
		for (int k = 0; k < newModel.K; k++) {
			/*
			p[k] = (trnModel.nwt[w][k] + newModel.nwt[w][k] + trnModel.beta)
					/ (trnModel.nwsumt[k] + newModel.nwsumt[k] + trnModel.Vbeta)
					* (newModel.ndoct[tm][k] + trnModel.alpha);
			*/
			p[k] = (trnModel.nwt[w][k] + trnModel.beta)
			/ (trnModel.nwsumt[k] + trnModel.Vbeta)
			* (newModel.ndoct[tm][k] + trnModel.alpha);
		}

		for (int k = 1; k < newModel.K; k++) {
			p[k] += p[k - 1];
		}

		double u = Math.random() * p[newModel.K - 1];
		for (topic = 0; topic < newModel.K; topic++) {
			if (p[topic] > u)
				break;
		}
		newModel.nwt[w][topic] += 1;
		newModel.ndoct[tm][topic] += 1;
		newModel.nwsumt[topic] += 1;
		return topic;
	}

	
	protected int socInfSampling(int m, int soc_m, int n) {
		int topic = newModel.socialData.docs.get(soc_m).topics[n];
		int w = newModel.socialData.docs.get(soc_m).words[n];
		
		newModel.nlnkt[w][topic] -= 1;
		newModel.ndoct[m][topic] -= 1;
		newModel.nlnksumt[topic] -= 1;
		
		String uid =  newModel.socialData.localDict.id2word.get(w);
		int wm =  newModel.pro_set.getPid(uid);
		if (wm > -1) {
			for (int k = 0; k <  newModel.K; k++) {
				
				p[k] = 
					((trnModel.nlnkt[w][k] + newModel.nlnkt[w][k] + newModel.delta) / (trnModel.nlnksumt[k] + newModel.nlnksumt[k] + newModel.Ldelta))
					* (newModel.ndoct[m][k] + newModel.alpha)
					* (newModel.ndoct[w][k] + trnModel.alpha);
				/*
				p[k] = 
					((trnModel.nlnkt[w][k]  + newModel.delta) / (trnModel.nlnksumt[k] + newModel.Ldelta))
					* (newModel.ndoct[m][k] + newModel.alpha)
					* (newModel.ndoct[w][k] + trnModel.alpha);
				*/
			}
		}
		else{
			for (int k = 0; k < trnModel.K; k++) {
				p[k] = 
					((trnModel.nlnkt[w][k] + newModel.nlnkt[w][k] + newModel.delta) / (trnModel.nlnksumt[k] + newModel.nlnksumt[k] + newModel.Ldelta))
					* (newModel.ndoct[m][k] + newModel.alpha);	
				
				/*
				p[k] = 
					((trnModel.nlnkt[w][k] + newModel.delta) / (trnModel.nlnksumt[k] + newModel.Ldelta))
					* (newModel.ndoct[m][k] + newModel.alpha);	
				*/
			}
		}
		
		for (int k = 1; k < newModel.K; k++) {
			p[k] += p[k - 1];
		}

		double u = Math.random() * p[newModel.K - 1];
		for (topic = 0; topic < newModel.K - 1; topic++) {
			if (p[topic] > u) // sample topic w.r.t distribution p
				break;
		}
		newModel.nlnkt[w][topic] += 1;
		newModel.ndoct[m][topic] += 1;
		newModel.nlnksumt[topic] += 1;
		return topic;
	}

	
	public int behaviorInfSampling(int m, int soc_m, int n) {
		int behavior = newModel.behaviorData.docs.get(soc_m).topics[n];
		int v = newModel.behaviorData.docs.get(soc_m).words[n];
		if (behavior == 0) {
			newModel.hot_nt[v] -= 1;
		}
		newModel.behaviors[soc_m][behavior] -= 1;
		
		int topic = newModel.socialData.docs.get(soc_m).topics[n];
		double pro_content = 0;
		double s1=0;
		double s2=0;
		String pro_id = newModel.socialData.localDict.id2word.get(v);
		int vm = newModel.pro_set.getPid(pro_id);
		if (vm > -1) {
			newModel.ndoct[m][topic] -= 1;
			for (int z = 0; z < newModel.K; z++) {
				pro_content += (newModel.ndoct[m][z] + newModel.alpha) * (newModel.ndoct[vm][z] + newModel.alpha);
				s1 += (newModel.ndoct[m][z] + newModel.alpha) * (newModel.ndoct[m][z] + newModel.alpha);
				s2 += (newModel.ndoct[vm][z] + newModel.alpha) * (newModel.ndoct[vm][z] + newModel.alpha);
			}
			newModel.ndoct[m][topic] += 1;
		//pro_content /=  (trnModel.soc_len[soc_m] + docN + trnModel.Kalpha)*(vsocN + vdocN  + trnModel.Kalpha);
			s1 = Math.sqrt(s1);
			s2 = Math.sqrt(s2);
			pro_content = pro_content / (s1 * s2) + newModel.rho;
		}else{
			newModel.soc_len[soc_m] -=1;
			pro_content = (newModel.behaviors[m][1] + newModel.mu) / (newModel.soc_len[soc_m] + newModel.B * newModel.mu);
			newModel.soc_len[soc_m] +=1;
		}	
		//pro_content = Utils.sigmoid(pro_content + trnModel.rho);
		if (pro_content  > Math.random()) {
			behavior = 1;
		}else
			behavior=0;
		newModel.behaviors[m][behavior] += 1;
		return behavior;
	}

	
	public double[][] calPhi() {
		double[][] r = new double[newModel.K][newModel.V];
		for (int k = 0; k < newModel.K; k++) {
			for (int w = 0; w < newModel.V; w++) {
				r[k][w] = (newModel.nwt[w][k] + trnModel.nwt[w][k] + trnModel.beta)
						/ (newModel.nwsumt[k] + trnModel.nwsumt[k] + trnModel.Vbeta);
			}
		}
		return r;
	}

	public double[][] cal_u_Phi() {
		double[][] r = new double[newModel.K][newModel.L];
		for (int k = 0; k < newModel.K; k++) {
			for (int w = 0; w < newModel.L; w++) {
				r[k][w] = (newModel.nlnkt[w][k] + trnModel.nlnkt[w][k] + trnModel.delta)
						/ (newModel.nlnksumt[k] + trnModel.nlnksumt[k] + trnModel.Ldelta);
			}
		}
		return r;
	}

	public double calPerplexity() {
		double[][] phi = calPhi();
		double[][] theta = newModel.theta();
		double perplexity = 0;
		int doc_lens = 0;

		for (int m = 0; m < newModel.M; m++) {
			String pid = newModel.pro_set.pids.get(m);
			double p = 0;
			if (newModel.textualData.did2local.containsKey(pid)) {
				int doc_m = newModel.textualData.did2local.get(pid);
				doc_lens += newModel.textualData.docs.get(doc_m).length;
				for (int n = 0; n < newModel.textualData.docs.get(doc_m).length; n++) {
					double t = 0;
					for (int k = 0; k < newModel.K; k++) {
						int w = newModel.textualData.docs.get(doc_m).words[n];
						t += theta[m][k] * phi[k][w];
					}
					p += Math.log(t);
				}
			}
			perplexity += p;
		}
		phi = null;
		theta = null;
		return Math.exp(-1.0 * perplexity / doc_lens);
	}

	
	public static void run(String confPath) {
		Inferencer inf = new Inferencer(confPath);
		inf.initNewModel();
		System.out.println("Sampling " + String.valueOf(inf.newModel.max_iters) + " iteration for inference!");
		int lastIter = inf.newModel.curstep;
		for (inf.newModel.curstep = lastIter + 1; inf.newModel.curstep < inf.newModel.max_iters; inf.newModel.curstep++) {
			inf.inference();
			if (inf.newModel.curstep % inf.newModel.savestep == 0) {
				inf.newModel.saveModel(String.valueOf(inf.newModel.curstep));
				double lik = inf.newModel.cal_likelihood();
				System.out.println(lik);
				if (Math.abs(lik - inf.likelihood) < inf.error) {
					inf.likelihood = lik;
					break;
				}
				inf.likelihood = lik;
				Utils.log(String.valueOf(inf.newModel.curstep) + " likelihood: "+ String.valueOf(inf.likelihood), inf.newModel.dir + "log/logs");
			}
		}
		inf.newModel.saveModel(String.valueOf(inf.newModel.curstep));
		Utils.log(String.valueOf(inf.newModel.curstep) + " likelihood: "+ String.valueOf(inf.likelihood),inf.newModel.dir + "log/logs");
	}

	
	public static void main(String[] args) {
		String confPath = "I:/fowmodel/config.txt";
		Inferencer inf = new Inferencer(confPath);
		//inf.infer();
		// inf.infer("10", "fow");
		// inf.calPerplexity()
	}
}
