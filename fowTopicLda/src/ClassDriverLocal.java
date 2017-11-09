package local.fow.topic.model;

public class ClassDriverLocal {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		if (args[0].equalsIgnoreCase("est")) {
			Estimator.run(args[1]);
		}

		else if (args[0].equalsIgnoreCase("estBatch")) {
			Estimator.run_batch(args[1]);
		}

		else if (args[0].equalsIgnoreCase("inf")) {
			Inferencer.run(args[1]);
		}

		else if (args[0].equalsIgnoreCase("dict")) {
			DataSet.run(args[1], args[2]);
		} else {
			System.out.println("There is no " + args[0] + " class");
		}
	}
}
