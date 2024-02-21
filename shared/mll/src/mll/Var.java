package mll;

import static java.lang.String.format;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Objects;


/** A variable, i.e., a node that returns the value of a variable. */
public class Var extends Op {
    private String name_;
    /** Used internally to generate a color in dot output. */
    double hue_;

    Var(DAG dag, String name) {
        super(dag);
        name_ = name;
        hash_ = Objects.hash(hash_, name);
    }

    public static Var c(DAG dag, String name) {
    	return (Var) dag.unify(new Var(dag, name));
    }
    
    public String name() { return name_; }

    public @Override String toString() { return name(); }

    @Override public boolean equals(Object obj) {
    	/// ensure that variables with different names are actually different
    	return super.equals(obj) && name().equals(((Var) obj).name());
    }

    @Override protected double eval_(double[] inVals) {
    	// not allows: variable value must be provided (in an env)
    	throw new UnsupportedOperationException("Trying to evaluate variable " + name());
    }

    public String color() {
        var res = "";
        res += format("color=\"%f .5 .75\",", hue_);
        return res + format("style=filled,fillcolor=\"%f .5 .75\"", hue_);
    }

    @Override protected Op diff(int inputIdx) {
    	// we are at an input, do nothing
    	return null;
    }

    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
    	/// variables will be pre-defined in LLVM code under their name, so just return
    	/// the name
        return format("%%%s", name());
    }
}
