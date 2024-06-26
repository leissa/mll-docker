package mll;

import java.util.HashMap;

/** A compute graph. */
public class DAG {

	/** Next unused id */
    private int             nextId_  = 0;

    /** Flag to indicate whether smart constructors should rewrite/optimize expressions */
    private boolean doRewrite_;

    /** All operators in this DAG. */
    private HashMap<Op, Op> ops_ = new HashMap<Op, Op>();

    public DAG() {
    	this(true);
    }

    public DAG(boolean doRewrite) {
    	this.doRewrite_ = doRewrite;
    }

    public boolean doRewrite() { return doRewrite_; }
    
    /** Start over and forget everything. */
    public void clear() {
        nextId_ = 0;
        ops_.clear();
    }

    /** Create or return the Op for the variable with the given name.  */
    public Var var(String name) { return Var.c(this,  name); }

    /** Create or return the Op for the variable "x".  */
    public Var x() { return var("x"); }

    /** Create or return the Op for the variable "y". */
    public Var y() { return var("y"); }

    /** Create or return the Op for the variable "z". */
    public Var z() { return var("z"); }

    /** Create or return the Op for the literal with the given value. */
    public Lit lit(double f) {
    	return Lit.c(this, f);
    }

    /** Create or return the Op for the literal with value 0. */
    public Lit lit0() { return lit(0.f); }

    /** Create or return the Op for the literal with value 1. */
    public Lit lit1() { return lit(1.f); }

    /** Create or return the Op for the literal with value 2. */
    public Lit lit2() { return lit(2.f); }

    /** Produce an unused ID for a new operator. */
    int nextID() { return nextId_++; }

    /** Unify the given operator with any existing operators. Intuitively, if there
     * is an equivalent operator in the tree already, return that one. */
    Op unify(Op key) {
        if (ops_.containsKey(key)) {
            nextId_--; // use again
            return ops_.get(key);
        }
        ops_.put(key, key);
        return key;
    }
}
