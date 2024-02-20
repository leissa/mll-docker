package mll;

import java.io.IOException;
import java.util.HashMap;

public class Main {
	public static void testSetup() throws IOException {
		// 1) Test plotting a function
		var x = Util.getx(-10,  10, 100);
		var y = Util.gety(x, x_ -> 1 / (1 + Math.exp(-x_)));
		Util.savePlot(x, y, "example");

		// 2) Test plotting a graph via dot
		String dot = """
				digraph g {
					a -> b
					c -> b
					b -> d
				}
				""";
		Util.saveDotPng(dot, "example");
		

		// 3) Test compilation via LLVM
		Util.runLLVM("helloworld");

		// 4) Test code optimization with LLVM
		Util.saveLLVMOpt("helloworld", 3);
		Util.runLLVM("helloworld-O3");
	}
	
	
	public static void main(String args[]) throws IOException {
		testSetup();
	}
}
