package mll;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;

public class Main {
	
	public static void main(String[] args) throws IOException {

		// 1) Plot function
		int min = -10;
		int max = 10;
		float stepsize = .1f;
		int length = (int) ((max - min) / stepsize);
        float[] xValues = new float[length];
        float[] yValues = new float[length];
        for (int i = 0; i < length; i++) {
        	float x = min + stepsize * i;
            xValues[i] = x;
            yValues[i] = (float) (1 / (1 + Math.exp(-x)));
        }
        String plot = MLLRunner.saveFunctionPlot(xValues, yValues, "Example");
        System.out.println("Plot saved as: " + plot);
		
        
        
		// 2) Plot graph
        String dot = """
        		digraph g {
        			a -> b
        			c -> b
        			b -> d
        		}
        		""";
        String filepath = Paths.get("dot", "example.png").toString(); 
        Graphviz.fromString(dot).height(500).render(Format.PNG)
        	.toFile(new File(filepath));
        System.out.println("Graph saved as: " + filepath);
        
        
        
		// 3) Compile LLVM
        MLLRunner.runLLVM("helloworld");
		

        
		// 4) Optimize LLVM code
        String opt = MLLRunner.saveOpt("helloworld", 3);
        System.out.println("Optimzed program saved as: " + opt);
		
                
	}
	
}
