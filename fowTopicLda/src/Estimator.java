package local.fow.topic.model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class Estimator {
	protected FowTopicModel trnModel;
	double error;
	double likelihood;

	protected double[] p;
	public String tex_dictFile = null;
	public String soc_dictFile = null;
	public String dir = null;
	public String modelName = null;
	public String paramPath = null;
	public String textdataPath = null;
	public String linkdataPath = null;

	public Estimator(String config) {
		setDefaultValues();
		config(config);
		p = new double[trnModel.K];
	}

	public void setDefaultValues() {
		error = 0.001;
		likelihood = 0;
	}

	public void config(String confPath) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(confPath), "UTF-8"));
			String line = null;
			while ((line = reader.readLine()) != null) {
				StringTokenizer tknr = new StringTokenizer(line, "= \t\r\n");
				int count = tknr.countTokens();
				if (count != 2)
					continue;
				String optstr = tknr.nextToken();
				String optval = tknr.nextToken();
				if (optstr.equalsIgnoreCase("param")) {
					paramPath = optval;
				} else if (optstr.equalsIgnoreCase("textdata")) {
					textdataPath = optval;
				} else if (optstr.equalsIgnoreCase("linkdata")) {
					linkdataPath = optval;
				} else if (optstr.equalsIgnoreCase("tex_dict")) {
					tex_dictFile = optval;
				} else if (optstr.equalsIgnoreCase("soc_dict")) {
					soc_dictFile = optval;
				} else if (optstr.equalsIgnoreCase("dir")) {
					dir = optval;
				} else if (optstr.equalsIgnoreCase("modelName")) {
					modelName = optval;
				} else if (optstr.equalsIgnoreCase("error")) {
					error = Double.parseDouble(optval);
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		trnModel = new FowTopicModel(dir, modelName);
		trnModel.readParamFile(paramPath);
	}

	public void estimate() {
		for (int m = 0; m < trnModel.M; m++) {
			String pid = trnModel.pro_set.pids.get(m);
			if (trnModel.textualData.did2local.containsKey(pid)) {
				int doc_m = trnModel.textualData.did2local.get(pid);
				for (int n = 0; n < trnModel.textualData.docs.get(doc_m).length; n++) {
					int topic = textualSampling(m, doc_m, n);// sample from p(z_i|z_-i,w)
					trnModel.textualData.docs.get(doc_m).topics[n] = topic;
				}
			}
				
			if (trnModel.socialData.did2local.containsKey(pid)) {
				int soc_m = trnModel.socialData.did2local.get(pid);
				for (int n = 0; n < trnModel.socialData.docs.get(soc_m).length; n++) {
					int behavior = behaviorSampling(m, soc_m, n);
					trnModel.behaviorData.docs.get(soc_m).topics[n] = behavior;
					if (behavior == 0) {
						trnModel.hot_nt[trnModel.socialData.docs.get(soc_m).words[n]] += 1;
					} else if (behavior == 1) {
						int topic = socialSampling(m, soc_m, n);
						trnModel.socialData.docs.get(soc_m).topics[n] = topic;
					}
				}
			}
		}	
	}

	public int textualSampling(int m, int doc_m, int n) {
		int topic = trnModel.textualData.docs.get(doc_m).topics[n];
		int w = trnModel.textualData.docs.get(doc_m).words[n];

		trnModel.nwt[w][topic] -= 1;
		trnModel.ndoct[m][topic] -= 1;
		trnModel.nwsumt[topic] -= 1;
		
		for (int k = 0; k < trnModel.K; k++) {
			p[k] = (trnModel.nwt[w][k] + trnModel.beta)/ (trnModel.nwsumt[k] + trnModel.Vbeta)
					* (trnModel.ndoct[m][k] + trnModel.alpha);
		}

		for (int k = 1; k < trnModel.K; k++) {
			p[k] += p[k - 1];
		}

		double u = Math.random() * p[trnModel.K - 1];
		for (topic = 0; topic < trnModel.K - 1; topic++) {
			if (p[topic] > u) // sample topic w.r.t distribution p
				break;
		}
		trnModel.nwt[w][topic] += 1;
		trnModel.ndoct[m][topic] += 1;
		trnModel.nwsumt[topic] += 1;
		return topic;
	}

	public int socialSampling(int m, int soc_m, int n) {
		int topic = trnModel.socialData.docs.get(soc_m).topics[n];
		int w = trnModel.socialData.docs.get(soc_m).words[n];
		//following relation
		trnModel.nlnkt[w][topic] -= 1;
		trnModel.ndoct[m][topic] -= 1;
		trnModel.nlnksumt[topic] -= 1;
		
		String uid = trnModel.socialData.localDict.id2word.get(w);
		int wm = trnModel.pro_set.getPid(uid);
		if (wm > -1) {
			for (int k = 0; k < trnModel.K; k++) {
				p[k] = 
					((trnModel.nlnkt[w][k] + trnModel.delta) / (trnModel.nlnksumt[k] + trnModel.Ldelta))
					* (trnModel.ndoct[m][k] + trnModel.alpha)			
					* (trnModel.ndoct[wm][k] + trnModel.alpha);
			}
		}
		else{
			for (int k = 0; k < trnModel.K; k++) {
				p[k] = 
					((trnModel.nlnkt[w][k] + trnModel.delta) / (trnModel.nlnksumt[k] + trnModel.Ldelta))
					* (trnModel.ndoct[m][k] + trnModel.alpha);		
			}
		}
		
		for (int k = 1; k < trnModel.K; k++) {
			p[k] += p[k - 1];
		}

		double u = Math.random() * p[trnModel.K - 1];
		for (topic = 0; topic < trnModel.K - 1; topic++) {
			if (p[topic] > u) // sample topic w.r.t distribution p
				break;
		}
		trnModel.nlnkt[w][topic] += 1;
		trnModel.ndoct[m][topic] += 1;
		trnModel.nlnksumt[topic] += 1;
		return topic;
	}

	public int behaviorSampling(int m, int soc_m, int n) {
		int behavior = trnModel.behaviorData.docs.get(soc_m).topics[n];
		int v = trnModel.behaviorData.docs.get(soc_m).words[n];
		if (behavior == 0) {
			trnModel.hot_nt[v] -= 1;
		}
		trnModel.behaviors[m][behavior] -= 1;
		
		int topic = trnModel.socialData.docs.get(soc_m).topics[n];
		double pro_content = 0;
		double s1=0;
		double s2=0;
		String pro_id = trnModel.socialData.localDict.id2word.get(v);
		int vm = trnModel.pro_set.getPid(pro_id);
		if (vm > -1) {
			trnModel.ndoct[m][topic] -= 1;
			for (int z = 0; z < trnModel.K; z++) {
				pro_content += (trnModel.ndoct[m][z] + trnModel.alpha) * (trnModel.ndoct[vm][z] + trnModel.alpha);
				s1 += (trnModel.ndoct[m][z] + trnModel.alpha) * (trnModel.ndoct[m][z] + trnModel.alpha);
				s2 += (trnModel.ndoct[vm][z] + trnModel.alpha) * (trnModel.ndoct[vm][z] + trnModel.alpha);
			}
			trnModel.ndoct[m][topic] += 1;
		//pro_content /=  (trnModel.soc_len[soc_m] + docN + trnModel.Kalpha)*(vsocN + vdocN  + trnModel.Kalpha);
			s1 = Math.sqrt(s1);
			s2 = Math.sqrt(s2);
			pro_content = pro_content / (s1 * s2) + trnModel.rho;
		}else{
			trnModel.soc_len[soc_m] -=1;
			pro_content = (trnModel.behaviors[m][1] + trnModel.mu) / (trnModel.soc_len[soc_m] + trnModel.B * trnModel.mu);
			trnModel.soc_len[soc_m] +=1;
		}	
		//pro_content = Utils.sigmoid(pro_content + trnModel.rho);
		if (pro_content  > Math.random()) {
			behavior = 1;
		}else
			behavior=0;
		trnModel.behaviors[m][behavior] += 1;
		return behavior;
	}

	
	public static void run(String confPath) {
		Estimator est = new Estimator(confPath);
		est.trnModel.initDict(est.tex_dictFile, est.soc_dictFile);
		est.trnModel.init();
		est.trnModel.loadData(est.textdataPath,  est.linkdataPath);
		est.trnModel.initialize();
		int lastIter = est.trnModel.curstep;
		for (est.trnModel.curstep = lastIter + 1; est.trnModel.curstep < est.trnModel.max_iters; est.trnModel.curstep++) {
			est.estimate();
			if (est.trnModel.curstep % est.trnModel.savestep == 0) {
				est.trnModel.saveModel(String.valueOf(est.trnModel.curstep));
				double lik = est.trnModel.cal_likelihood();
				System.out.println(lik);
				if (Math.abs(lik - est.likelihood) < est.error) {
					est.likelihood = lik;
					break;
				}
				est.likelihood = lik;
				Utils.log(String.valueOf(est.trnModel.curstep) + " likelihood: "+ String.valueOf(est.likelihood), est.trnModel.dir + "log/logs");
			}
		}
		est.trnModel.saveModel(String.valueOf(est.trnModel.curstep));
		Utils.log(String.valueOf(est.trnModel.curstep) + " likelihood: "+ String.valueOf(est.likelihood), est.trnModel.dir + "log/logs");
	}

	
	public static void run_batch(String confPath) {
		Estimator est = new Estimator(confPath);
		est.trnModel.initDict(est.tex_dictFile, est.soc_dictFile);
		est.trnModel.init();
		String []texFiles=est.textdataPath.split(";");
		String []socFiles=est.linkdataPath.split(";");
		
		for (int i=0;i<texFiles.length;i++){
			est.trnModel.loadData(texFiles[i],  socFiles[i]);
			est.trnModel.initialize();
			est.trnModel.curstep=0;
			int lastIter = est.trnModel.curstep;
			for (est.trnModel.curstep = lastIter + 1; est.trnModel.curstep < est.trnModel.max_iters; est.trnModel.curstep++) {
				est.estimate();
				if (est.trnModel.curstep % est.trnModel.savestep == 0) {
					//est.trnModel.saveModel(String.valueOf(est.trnModel.curstep));
					double lik = est.trnModel.cal_likelihood();
					Utils.log(String.valueOf(est.trnModel.curstep) + " likelihood: "+ String.valueOf(lik), est.trnModel.dir + "log/logs");
				}
			}
			est.trnModel.saveModel(String.valueOf(est.trnModel.curstep)+"_part_"+String.valueOf(i));
		}
	}

	public static void main(String[] args) {
		String confPath = "I:/fowmodel/est/config.exp2";
		run(confPath);
	}
}
