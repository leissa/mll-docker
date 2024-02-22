package mll;

import static java.lang.String.format;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

/** An operator that stores gradient information in its inputs.
 * 
 * This "placeholder" operator is added to store the result of backpropagation using {@code Op#backwards()}. Its
 * first input is the result (w.r.t. which derivatives are taken), its subsequent inputs are the partial derivatives 
 * of the result w.r.t. to the free variables.  
 * 
 * When evaluated, the grad operator simply returns its input. It's main use is to access the partial derivates.
 */
public class Grad extends Op {
    private double[] result_; // cached result after last eval, hacky but works for us
    private Var[]    vars_; // variables w.r.t. which partial derivatives are taken (in order)

    Grad(Op[] inputs, Var[] vars) {
        super(inputs[0].dag(), inputs);
        vars_ = vars;
    }

    public static Grad c(Op[] inputs, Var[] vars) { return (Grad) inputs[0].dag().unify(new Grad(inputs, vars)); }

    /** Result of this operator when it was last evaluated */
    public double grad(String varName) { return result_[index(varName)]; }
    
    /** Returns all results of this operator (result and gradients) when it was last evaluated */
    public double[] results() { return result_; }

    /** Result of this operator when it was last evaluated */
    public double result() { return result_[0]; }
    
    /** Result of input(i) when it was last evaluated */
    public double result(int i) { return result_[i]; }

    /** Returns index of specified variable in result() */
    public int index(String varName) {
    	for (int i=0; i<vars_.length; i++) {
    		if (vars_[i].name().equals(varName)) return i+1;
    	}
    	return -1;
	};
    
    /** Returns the variables w.r.t. which partial derivatives are taken (in order of inputs 1, 2, ...) */
    public Var[] vars() { return vars_; }
    public int numVars() { return vars_.length; }
    public Var var(int i) { return vars_[i]; }

    public @Override String toString() {
        var res = "{";
        var sep = "";
        for (var input : inputs()) {
            res += sep + input;
            sep  = ", ";
        }
        return res + "}";
    }

    @Override protected double eval_(double[] inVals) {
        result_ = inVals; // cache it
        return result_[0];
    }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        throw new UnsupportedOperationException("Only llvm_store allowed on Grad op.");
    }

    @Override protected void llvm_store(HashMap<Op, String> map, Writer writer) throws IOException {
        // custom code in that it stores input instead of outputs. 
        // In particular, stores I-th input in location _outputI.
        int n          = numInputs();
        var llvmInputs = new String[n];
        for (int i = 0; i != n; ++i) llvmInputs[i] = input(i).llvm(map, writer);

        for (int i = 0, e = numInputs(); i != e; ++i) {
            var gep   = format("\t%%_output%d = getelementptr inbounds double, double* %%_output, i64 %d\n", i, i);
            var store = format("\tstore double %s, double* %%_output%d\n", llvmInputs[i], i);
            writer.append(gep + store);
        }
    }

    @Override protected Op diff(int inputIdx) {
        throw new UnsupportedOperationException("diff not allowed on Grad op (it already holds the derivatives).");
    }
}
