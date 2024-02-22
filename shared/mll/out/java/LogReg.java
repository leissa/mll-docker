package mll;

import static java.lang.String.format;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/** A simple implementation of logistic regression using MLL. */
public class LogReg {
	// prediction = sigma(w0 + sum w_i*x_i)
	// that's the model probability of label 1 (true)
	public static Op forwardGraph(int dim) {
		DAG dag = new DAG();

		// linear predictor
		Op eta = dag.lit0();
		for (int v = 0; v<=dim; v++) {
			var xv = v == 0 ? dag.lit1() : dag.var("x"+v);
			var wv = dag.var("w"+v);
			eta = eta.add(wv.mul(xv));
		}

		// apply logistic function
		Op result = dag.lit1().div(dag.lit1().add(eta.neg().exp()));

		return result;
	}

	// log loss on top of prediction = -(y*log(prediction) + (1-y)*log(1-prediction))
	// small = prediction good --- large = prediction bad
	public static Op lossGraph(Op forwardGraph) {
		var dag = forwardGraph.dag();
		var y = dag.var("y"); // true label
		var loss = y.mul(forwardGraph.log().neg()).add(dag.lit1().sub(y).mul(dag.lit1().sub(forwardGraph).log().neg()));
		return loss;
	}

	// read the training data (features xi + labels y)
	// CSV format: x1,x2,...,xD,y
	public static List<List<Double>> readCsv(String file) throws FileNotFoundException {
		List<List<Double>> records = new ArrayList<>();
		try (Scanner scanner = new Scanner(new File(file))) {
		    while (scanner.hasNextLine()) {
		        String line = scanner.nextLine();
		        List<Double> record = new ArrayList<>();
		        for (var value : line.split(",")) {
		        	record.add(Double.parseDouble(value));
		        }
		        records.add(record);
		    }
		}
		return records;
	}

	// train a logistic regression model
	// epochs is the number of passes through training data
	// epsilon is the step size
	public static double[] train(List<List<Double>> data, int epochs, double epsilon) throws IOException {
		// plot all the compute graphs for illustrative purposes
		int dim = data.get(0).size() - 1;

		Op forwardGraph = forwardGraph(dim);
		Util.saveDotPng(forwardGraph.dot(), "logreg-forward-"+dim);

		Op lossGraph = lossGraph(forwardGraph);
		Util.saveDotPng(lossGraph.dot(), "logreg-loss-"+dim);
		
		Grad dout = lossGraph.backwards(); // we only need this one below		
		Util.saveDotPng(dout.dot(), "logreg-diff-"+dim);

		// start training (with incremental gradient descent)
		DAG dag = dout.dag();
		double[] w = new double[dim + 1]; // initially all 0
		var env = new HashMap<Op,Double>();
		System.out.println(format("Initial weights: %s", Arrays.toString(w)));
		for (int epoch=0; epoch<epochs; epoch++) {
			double totalLoss = 0.;

			// process each example individually
			for (var example : data) {
				env.clear();

				// set data in compute graph (x1,...,xD) and label y
				// and the weights (w0=bias, w1...wD = feature weight)
				set_x(dag, env, example.subList(0, dim));
				env.put(dag.var("y"), example.get(dim));
				set_w(dag, env, w);
				
				// now run forward/backward
				dout.eval(env);
				totalLoss += dout.result(); // result holds the loss

				// obtain the gradient
				double[] dw = new double[dim + 1];
				for (int i=0; i<=dim; i++) {
					dw[i] = dout.grad("w"+i); // grad holds the partial derivatives
				}

				// and update the weights using a gradient descent step
				for (int i=0; i<=dim; i++) {
					w[i] -= epsilon * dw[i]; 
				}
			}

			// print some statistics
			System.out.println(format("Epoch %2d: avgLoss %3.4f, weights %s",
					epoch+1, totalLoss/data.size(), Arrays.toString(w)));
		}

		return w;
	}

	/** Run a trained logistic regression model on the provided inputs and print the result. */
	public static void evaluate(List<List<Double>> data, double[] w) {
		int dim = data.get(0).size() - 1;
		Op forwardGraph = forwardGraph(dim); // to predict, we only need this graph
		DAG dag = forwardGraph.dag();
		var env = new HashMap<Op,Double>();
		for (var example : data) {
			env.clear();

			// set data in compute graph (x1,...,xD)
			// and the weights (w0=bias, w1...wD = feature weight)
			set_x(dag, env, example.subList(0, dim));
			set_w(dag, env, w);
			
			// now run forward
			double pred = forwardGraph.eval(env);
			System.out.println(format("Example %s, prediction: %f", example, pred));
		}
	}

	// Set the values of all inputs (x1,...,xD) in env
	static void set_x(DAG dag, HashMap<Op,Double> env, List<Double> x) {
		for (int i=0; i<x.size(); i++) {
			env.put(dag.var("x"+(i+1)), x.get(i));
		}		
	}
	
	// Set the values of all weights (w0,w1,...,wD) in env
	static void set_w(DAG dag, HashMap<Op,Double> env, double[] w) {
		for (int i=0; i<w.length; i++) {
			env.put(dag.var("w"+i), w[i]);
		}
	}
	
	// read data, train, predict
	public static void main(String args[]) throws IOException {
		var data = readCsv("out/data/data_1d_10.csv");
		var w = train(data, 100, 0.1);
		data.sort((d1,d2) -> d1.toString().compareTo(d2.toString()));
		evaluate(data, w);
	}
}
