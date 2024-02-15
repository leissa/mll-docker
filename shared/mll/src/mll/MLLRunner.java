package mll;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XYChart;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MLLRunner {

	
	/**
	 * Runs the provided LLVM program.
	 */
	public static void runLLVM(String filename) {
		
		String[] command;
		if (isLLVMAvailable()) {
			command = new String[]{"lli", "out/llvm/" + filename + ".ll"};
		} else {
			command = new String[]{"docker","exec", "-t", "mll_docker", "lli", "/home/jovyan/mll/out/llvm/" + filename + ".ll"};
		}

	    runCommand(command);
	}
	
    
   /**
     * Stores a single function plot on disk.
     */
    public static String saveFunctionPlot(float[] x, float[] y, String plotName) {
        try {
            XYChart chart = getChart(x, y, plotName);
            
            Files.createDirectories(Paths.get("out/plots"));
            
            String filepath = Paths.get("out/plots", plotName + ".png").toString();
            
            BitmapEncoder.saveBitmapWithDPI(
            		chart, 
            		filepath, 
            		BitmapEncoder.BitmapFormat.PNG, 300
            		);
            
            return filepath;
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stores a plot with two functions on disk.
     */
    public static String saveFunctionPlot(float[] x, float[] y1, float[] y2, String plotName) {
        try {
            XYChart chart = getDoubleChart(x, y1, y2, plotName);
            
            Files.createDirectories(Paths.get("out/plots"));
            
            String filepath = Paths.get("out/plots", plotName + ".png").toString();
 
            
            BitmapEncoder.saveBitmapWithDPI(
            		chart,
            		filepath,
            		BitmapEncoder.BitmapFormat.PNG, 300
            		);
            
            return filepath;
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * Executes LLVM optimization and saves output to disk.
	 */
	public static String saveOpt(String filename, int optLevel) throws IOException {
	
		Files.createDirectories(Paths.get("out/llvm"));
		
		String filepath = Paths.get("out/llvm", filename + getOptLevelCode(optLevel) + ".ll").toString();
		
		String[] command;
		if (isLLVMAvailable()) {
			command = new String[]{"opt", getOptLevelCode(optLevel), 
		    		"out/llvm/" + filename + ".ll", "-So",
		    		"out/llvm/" + (filename + getOptLevelCode(optLevel) + ".ll")};
		} else {
			command = new String[]{"docker", "exec", "-t", "mll_docker", "opt", getOptLevelCode(optLevel), 
		    		"/home/jovyan/mll/out/llvm/" + filename + ".ll", "-So",
		    		"/home/jovyan/mll/out/llvm/" + (filename + getOptLevelCode(optLevel) + ".ll")};
		}

	    runCommand(command);
	    
	    return filepath;
	}
	

	/**
	 * Displays plot of a single function in a Jupyter-Notebook.
	 */
	public static BufferedImage viewFunctionPlot(float[] x, float[] y, String plotName) {
	    XYChart chart = getChart(x, y, plotName);
	    return BitmapEncoder.getBufferedImage(chart);
	}


	/**
	 * Displays a plot with two functions y1(x) and y2(x).
	 */
	public static BufferedImage viewFunctionPlot(float[] x, float[] y1, float[] y2, String plotName) {
        XYChart chart = getDoubleChart(x, y1, y2, plotName);
        return BitmapEncoder.getBufferedImage(chart);
    }





    private static XYChart getChart(float[] x, float[] y, String plotName) {
	    return QuickChart.getChart(plotName, "", "", "f", getDoubleArray(x), getDoubleArray(y));
	}


	private static double[] getDoubleArray(float[] arr) {
	    double[] arr2 = new double[arr.length];
	    for (int i = 0; i < arr.length; i++) {
	        arr2[i] = arr[i];
	    }
	    return arr2;
	}


	private static XYChart getDoubleChart(float[] x, float[] y1, float[] y2, String plotName) {
	    XYChart chart = getChart(x, y1, plotName);
	    chart.addSeries("g", getDoubleArray(x), getDoubleArray(y2));
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
		try (
			var isr = new InputStreamReader(inputStream);
			var reader = new BufferedReader(isr);
		) {
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
