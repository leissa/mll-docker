package mll;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Objects;

/** A literal, i.e., a node that returns a constant value. */
public class Lit extends Op {
	/** Value of this literal */
    private final double f_;

    Lit(DAG dag, double f) {
        super(dag);
        f_    = f;
        hash_ = Objects.hash(hash_, f);
    }

    public static Lit c(DAG dag, double f) {
        if (f == -0.f) f = 0.f; // ignore -0.f
        return (Lit)dag.unify(new Lit(dag, f));
    }
    
    public double get() { return f_; }
    
    public boolean is(double f) { return f == f_; }
    
    public static boolean is(Op e, double f) { return (e instanceof Lit l) && l.is(f); }
    
    @Override public String toString() { return Double.toString(get()); }
    
    @Override public boolean equals(Object obj) {
    	/// ensure that literals with different values are actually different
    	return super.equals(obj) && get() == ((Lit) obj).get(); 
    }

    @Override protected double eval_(double[] inVals) { return f_; }

    @Override protected Op diff(int inputIdx) { return lit0(); }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
    	// for literals, we do not return a variable name but the value directly
        return Double.toString(get());
    }
}
