package mll;

import static java.lang.String.format;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.BinaryOperator;

/** A node (operation) in a compute graph. */
public abstract class Op {
	/** Cached hash value of this node. */
    protected int hash_;

    /** Compute graph to which this node belongs. */
    private DAG   dag_;

    /** Unique identifier of this node. */
    private int   id_;

    /** Inputs to this node, ordered. */
    private Op[]  inputs_;

    Op(DAG dag, Op... inputs) {
        dag_    = dag;
        id_     = dag.nextID();
        inputs_ = inputs;
        hash_   = Objects.hash(getClass().hashCode());
        for (var input : inputs) hash_ = Objects.hash(hash_, input.id());
    }

    // -- getters --------------------------------------------------------------------------------

    public DAG dag() { return dag_; }
    public int id() { return id_; }
    public Op[] inputs() { return inputs_; }
    public Op input(int i) { return inputs_[i]; }
    public int numInputs() { return inputs_.length; }
    @Override public int hashCode() { return hash_; }

    /** Check for equality (same class, identical inputs). */
    @Override public boolean equals(Object obj) {
        boolean result = getClass() == obj.getClass();
        if (result) {
            var other = (Op) obj;
            result &= numInputs() == other.numInputs();
            for (int i = 0, e = numInputs(); i != e && result; ++i)
            	result &= input(i) == other.input(i);
        }
        return result;
    }

    // -- factory methods to construct Ops -------------------------------------------------------

    public Lit lit(double f) { return dag().lit(f); }
    public Lit lit0() { return lit(0.f); }
    public Lit lit1() { return lit(1.f); }
    public Lit lit2() { return lit(2.f); }

    public Op add(Op y) { return Add.c(this, y); }
    public Op mul(Op y) { 
    	Op mul = null;
    	return mul;
    	}


    // -- Compute free variables and operator usages ----------------------------------------------

    /** Used to record information that operator's output is used by op as input number index */
    static record Use(Op op, int index) {
        @Override public boolean equals(Object obj) {
            return (obj instanceof Use use) && op() == use.op() && index == use.index();
        }

        @Override public int hashCode() { return Objects.hash(op().id(), index()); }
    }

    /** Compute the free variables used below this operator */
    Var[] freeVars() { return freeVars(null); }

    Var[] freeVars(HashMap<Op, HashSet<Use>> uses) {
        var vars = new TreeSet<Var>((v, w) -> v.name().compareTo(w.name()));
        freeVars(new HashSet<Op>(), vars, uses);
        return vars.toArray(new Var[vars.size()]);
    }

    // internal
    void freeVars(HashSet<Op> done, TreeSet<Var> vars, HashMap<Op, HashSet<Use>> uses) {
        if (done.add(this)) {
            if (this instanceof Var var) {
                vars.add(var);
            } else {
                for (int i = 0, e = numInputs(); i != e; ++i) {
                    var input = input(i);
                    if (uses != null) {
                        if (!uses.containsKey(input)) uses.put(input, new HashSet<Use>());
                        uses.get(input).add(new Use(this, i));
                    }
                    input.freeVars(done, vars, uses);
                }
            }
        }
    }


    // -- Evaluation -----------------------------------------------------------------------------

    /** Return the output of this operator given values of the inputs. */
    public final double eval(double... values) {
        return this.eval(new HashMap<Op, Double>(), values);
    }

    /** Return the output of this operator given values of the inputs. All computed values are stored in
     * env as well. */
    public final double eval(HashMap<Op, Double> env, double... values) {
        var vars = freeVars();
        if (vars.length != values.length)
            throw new IllegalArgumentException("number of provided values does not match number of free variables");
        for (int i = 0, e = vars.length; i != e; ++i) env.put(vars[i], values[i]);
        return eval(env);
    }

    /** Return the output of this operator recursively, using the specified env as a cache for
     * already computed outputs. */
    public final double eval(HashMap<Op, Double> env) {
        var res = env.get(this);
        if (res != null) return res;

        var inVals = new double[numInputs()];
        for (int i = 0, e = numInputs(); i != e; ++i) inVals[i] = input(i).eval(env);

        res = eval_(inVals);
        if (res.isNaN()) {
        	System.out.println("Warning: encountered NaN value in " + this);
        	System.out.println("Inputs were: " + Arrays.toString(inVals));
        }
        if (res.isInfinite()) {
        	System.out.println("Warning: encountered infinite value in " + this);
        	System.out.println("Inputs were: " + Arrays.toString(inVals));
        }
        env.put(this, res);
        return res;
    }

    /** Evaluate this operator.
     * Needs to be implemented by subclasses. */
    abstract double eval_(double[] inVals);

    // -- DOT output -----------------------------------------------------------------------------

    /** Writes a DOT representation of the compute graph up to this operator to writer, where each
     * operator is annotated with its computed values (if any). */
    public final void dot(HashMap<Op, Double> env, Writer writer) throws IOException {
        var vars = freeVars();
        for (int i = 0, e = vars.length; i != e; ++i)
            vars[i].hue_ = (double) (i % e) / (double) e;

        writer.append("digraph {\n");
        writer.append("\trankdir=\"TB\"\n");
        writer.append("\tordering=\"in\"\n");
        dot(new HashMap<Op, String>(), env, writer);
        writer.append("}\n");
    }

    /** Same as above but returns a String. */
    public final String dot(HashMap<Op, Double> env) {
        var writer = new StringWriter();
        try {
            dot(env, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return writer.toString();
    }

    /** Same as above but uses an empty environment. */
    public final String dot() { return dot(new HashMap<Op, Double>()); }

    // internal
    protected final String dot(HashMap<Op, String> map, HashMap<Op, Double> env, Writer writer) throws IOException {
        var res = map.get(this);
        if (res != null) return res;
        res = dot_(map, env, writer);
        map.put(this, res);
        return res;
    }

    // internal
    protected String dot_(HashMap<Op, String> map, HashMap<Op, Double> env, Writer writer) throws IOException {
        var dst = format("_%d", id());
        var val = !(this instanceof Lit) && !(this instanceof Grad) && env.containsKey(this)
                ? "\\n" + env.get(this).toString()
                : "";
        var col = (this instanceof Var var) ? var.color() : "";

        writer.append(format("\t%s[label=\"%s%s\",%s];\n", dst, opString(), val, col));
        for (int i = 0, e = numInputs(); i != e; ++i) {
            var in   = input(i);
            var src  = in.dot(map, env, writer);
            var attr = this instanceof Grad grad && i >= 1 ? grad.var(i - 1).color() : "";
            writer.append(format("\t%s -> %s[%s];\n", src, dst, attr));
        }
        return dst;
    }

    public String opString() {
    	return numInputs() == 0 ? toString() : getClass().getSimpleName().toLowerCase();
    }

    // -- Backpropagation ------------------------------------------------------------------------

    /** Return an operator that computes the partial derivatives of this operator's output w.r.t. to
     * each of the free variables in the compute graph. */
    public Grad backwards() {
        var uses    = new HashMap<Op, HashSet<Use>>();
        var vars    = freeVars(uses);
        var dcache  = new HashMap<Op, Op>();
        var gradInputs = new Op[vars.length + 1];
        gradInputs[0] = this; // the value
        for (int i = 0, e = vars.length; i != e; ++i)
        	gradInputs[i + 1] = vars[i].backwards(dcache, uses, this); // the partial derivatives
        return Grad.c(gradInputs, vars);
    }

    /** Return an operator that computes the partial derivative of {@code result} w.r.t. to this operator's
     * output using backpropagation.
     * If necessary, adds required operators to the compute graph.
     * Local partial derivatives of each node are obtained using diff(int), which see.
     *
     * @param dcache operator -> partial derivative of {@code result} w.r.t. to that operator's output
     * @param uses operator -> all uses of that operator's output in the compute graph
     * @param result partial derivates are computed w.r..t to this operator
     */
    protected Op backwards(HashMap<Op, Op> dcache, HashMap<Op, HashSet<Use>> uses, Op result) {
        // if this operator is the result, its partial derivative is 1 (bootstraps backpropagation)
    	if (this == result) return lit1();

        // if we computed the required partial derivative already, return it
        var outputDerivative = dcache.get(this);
        if (outputDerivative != null) return outputDerivative;

        // otherwise, run backpropagation
        outputDerivative = lit0();
        for (var use : uses.get(this)) {
        	// we first compute the partial derivative of each usage of this operator
            var usedBy              = use.op();
            var usedIndex           = use.index();
            var useOutputDerivative = usedBy.backwards(dcache, uses, result); // dresult / duse_out
            var useLocalDerivative  = usedBy.diff(usedIndex); // duse_out / duse_in
            var useInputDerivative  = useOutputDerivative.mul(useLocalDerivative); // chain rule: dres / duse_in

            // and then add them all up to obtain the result (multivariate chain rule)
            outputDerivative = outputDerivative.add(useInputDerivative);
        }

        dcache.put(this, outputDerivative);
        return outputDerivative;
    }

    /** Return an operator that computes the partial derivative of this operator w.r.t. to the specified input.
     * The operator must be added to the compute graph. Implementations may use #input(...) to use required
     * input values. */
    abstract protected Op diff(int inputIdx);

    // -- LLVM -----------------------------------------------------------------------------------

    /** Write LLVM code to compute this operator's output to the specified writer. */
    public final void llvm(Writer writer) throws IOException {
        // declare LLVM intrinsics we might use
        writer.append("declare double @llvm.pow.f64(double %Val, double %Power)\n");
        writer.append("declare double @llvm.log.f64(double %Val)\n");
        writer.append("declare double @llvm.exp.f64(double %Val)\n");
        writer.append("declare double @llvm.sin.f64(double %Val)\n");
        writer.append("declare double @llvm.cos.f64(double %Val)\n\n");

        // mll signature
        writer.append("define void @mll(double* noundef noalias %_input, double* noundef noalias %_output) {\n");

        // load vars
        var map = new HashMap<Op, String>();
        int i   = 0;
        for (var var : freeVars()) {
            var name = format("%%%s", var);
            var gep  = format("\t%%_in%d = getelementptr inbounds double, double* %%_input, i64 %d\n", i, i);
            var load = format("\t%s = load double, double* %%_in%d\n", name, i);
            writer.append(gep + load);
            map.put(var, name);
            ++i;
        }

        // emit final store and recursively the body to compute it
        llvm_store(map, writer);

        // ret void
        writer.append("\tret void\n");
        writer.append("}\n");
    }

    /** Same as above but returns the LLVM code as String. */
    public final String llvm() {
        var writer = new StringWriter();
        try {
            llvm(writer);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return writer.toString();
    }

    /** Return LLVM variable that holds this operator's output. If necessary, adds relevant
     * LLVM code to the specified writer (computing the value of the return variable) and
     * caches it. */
    protected final String llvm(HashMap<Op, String> cache, Writer writer) throws IOException {
        var res = cache.get(this);
        if (res != null) return res;
        res = llvm_(cache, writer);
        cache.put(this, res);
        return res;
    }

    /** Generate LLVM code that stores this operator's output in location _output. */
    protected void llvm_store(HashMap<Op, String> cache, Writer writer) throws IOException {
        var res = llvm(cache, writer);
        writer.append(format("\tstore double %s, double* %%_output\n", res));
    }

    /** Return LLVM variable that holds this operator's output (not cached).
     * Adds relevant code to the specified writer (computing the value of the returned variable).
     * Implementations should use llvm(cache,writer) to process their inputs first. */
    protected abstract String llvm_(HashMap<Op, String> cache, Writer writer) throws IOException;
}

/** A unary operator (one input). */
abstract class UnOp extends Op {
    UnOp(Op arg) { super(arg.dag(), arg); }

    /** Returns the input of this operator */
    public Op arg() { return input(0); }

    @Override public String toString() { return format("(%s(%s))", opString(), arg()); }
}

/** A binary operator (two inputs). */
abstract class BinOp extends Op {
    BinOp(Op lhs, Op rhs) { super(lhs.dag(), lhs, rhs); }

    /** Returns first input (left-hand side) */
    public Op lhs() { return input(0); }

    /** Returns second input (right-hand side) */
    public Op rhs() { return input(1); }

    @Override public String toString() { return format("(%s %s %s)", lhs(), opString(), rhs()); }
}
