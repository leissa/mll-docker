package mll;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XYChart;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;

public class Util {

	/**
	 * Plot a single function y(x) and store the result as a PNG file on disk.
	 */
	public static String savePlot(double[] x, double[] y, String plotName) {
		try {
			XYChart chart = getChart(x, y, plotName);
			Files.createDirectories(Paths.get("out/plots"));
			String filepath = Paths.get("out/plots", plotName + ".png").toString();
			BitmapEncoder.saveBitmapWithDPI(chart, filepath, BitmapEncoder.BitmapFormat.PNG, 300);
			System.out.println("Saved plot: " + filepath);
			return filepath;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Plot two functions y1(x) and y2(x) and store the result as a PNG file on disk.
	 */
	public static String savePlot(double[] x, double[] y1, double[] y2, String plotName) {
		try {
			XYChart chart = getChart(x, y1, y2, plotName);
			Files.createDirectories(Paths.get("out/plots"));
			String filepath = Paths.get("out/plots", plotName + ".png").toString();
			BitmapEncoder.saveBitmapWithDPI(chart, filepath, BitmapEncoder.BitmapFormat.PNG, 300);
			System.out.println("Saved plot: " + filepath);
			return filepath;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String saveDotPng(String dot, String name) throws IOException {
		String filepath = Paths.get("out/dot", name + ".png").toString();
		Graphviz.fromString(dot).render(Format.PNG).toFile(new File(filepath));
		System.out.println("Rendered dot file: " + filepath);
		return filepath;
	}

	public static BufferedImage viewDot(String dot) {
		return Graphviz.fromString(dot).render(Format.PNG).toImage();
	}

	/**
	 * Run the provided LLVM program.
	 */
	public static void runLLVM(String filename) {
		String[] command;
		if (isLLVMAvailable()) {
			command = new String[] { "lli", "out/llvm/" + filename + ".ll" };
		} else {
			command = new String[] { "docker", "exec", "-t", "mll_docker", "lli",
					"/home/jovyan/mll/out/llvm/" + filename + ".ll" };
		}
		runCommand(command);
	}


	/**
	 * Execute LLVM optimization and saves output to disk.
	 */
	public static String saveLLVMOpt(String filename, int optLevel) throws IOException {

		Files.createDirectories(Paths.get("out/llvm"));
		String filepath = Paths.get("out/llvm", filename + getOptLevelCode(optLevel) + ".ll").toString();
		String[] command;
		if (isLLVMAvailable()) {
			command = new String[] { "opt", getOptLevelCode(optLevel), "out/llvm/" + filename + ".ll", "-So",
					"out/llvm/" + (filename + getOptLevelCode(optLevel) + ".ll") };
		} else {
			command = new String[] { "docker", "exec", "-t", "mll_docker", "opt", getOptLevelCode(optLevel),
					"/home/jovyan/mll/out/llvm/" + filename + ".ll", "-So",
					"/home/jovyan/mll/out/llvm/" + (filename + getOptLevelCode(optLevel) + ".ll") };
		}
		runCommand(command);
		System.out.println("Optimized LLVM code: " + filepath);

		return filepath;
	}

	/**
	 * Display plot of a single function y(x) in a Jupyter notebook.
	 */
	public static BufferedImage viewPlot(double[] x, double[] y, String plotName) {
		XYChart chart = getChart(x, y, plotName);
		return BitmapEncoder.getBufferedImage(chart);
	}

	/**
	 * Display plot of a two functions y1(x) and y2(x) in a Jupyter notebook.
	 */
	public static BufferedImage viewPlot(double[] x, double[] y1, double[] y2, String plotName) {
		XYChart chart = getChart(x, y1, y2, plotName);
		return BitmapEncoder.getBufferedImage(chart);
	}

	public static double[] getx(double xmin, double xmax, int length) {
		var x = new double[length];
		double stepsize = (xmax-xmin)/length;
		for (int i = 0; i < length; i++) {
			x[i] = xmin + stepsize*i;
		}
		x[length-1]=xmax; // to avoid rounding errors
		return x;
	}
	
	public static double[] gety(double[] x, final Function<Double, Double> f) {
		int length = x.length;
		var y = new double[length];
		for (int i = 0; i < length; i++) {
			y[i] = f.apply(x[i]);
		}
		return y;
	}
	
	public static double[] gety(double[] x, final Op out) {
		return gety(x, x_ -> out.eval(x_));
	}
	
	private static XYChart getChart(double[] x, double[] y, String plotName) {
		return QuickChart.getChart(plotName, "", "", "f", x, y);
	}

	private static XYChart getChart(double[] x, double[] y1, double[] y2, String plotName) {
		XYChart chart = getChart(x, y1, plotName);
		chart.addSeries("g", x, y2);
		chart.getStyler().setMarkerSize(0);
		return chart;
	}

	private static String getOptLevelCode(int optLevel) {
		return switch (optLevel) {
		case 0 -> "-O0";
		case 1 -> "-O1";
		case 2 -> "-O2";
		case 3 -> "-O3";
		default -> "-O3";
		};
	}

	private static boolean isLLVMAvailable() {
		try {
			var process = new ProcessBuilder("clang", "--version").start();
			int exitCode = process.waitFor();
			if (exitCode == 0) {
				return true;
			}

			return false;

		} catch (IOException | InterruptedException e) {
			return false;
		}
	}

	private static void printInputStream(InputStream inputStream) throws IOException {
		try (var isr = new InputStreamReader(inputStream); var reader = new BufferedReader(isr);) {
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}
		}
	}

	private static void runCommand(String[] command) {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(command);
			Process process = processBuilder.start();
			var inputStream = process.getInputStream();
			var errorStream = process.getErrorStream();

			printInputStream(inputStream);
			printInputStream(errorStream);

			int exitCode = process.waitFor();

			if (exitCode != 0) {
				System.err.println("Program execution failed with exit code " + exitCode);
			}

		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
