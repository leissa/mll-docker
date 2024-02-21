package mll;

import static java.lang.String.format;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

/** Operator to adds two inputs. */
public class Add extends BinOp {
    Add(Op lhs, Op rhs) { super(lhs, rhs); }

    // Smart constructor
    public static Op c(Op x, Op y) {
        var dag = x.dag();
        return dag.unify(new Add(x, y));
    }

    // Custom operator string
    @Override public String opString() { return "+"; }


    // Required implementation (see Op#eval_)
    @Override protected double eval_(double[] inVals) {
    	return inVals[0] + inVals[1];
    }

    // Required implementation (see Op#diff)
    @Override protected Op diff(int inputIdx) { return lit1(); }

    // Required implementation (see Op#llvm_)
    @Override protected String llvm_(HashMap<Op, String> map, Writer writer) throws IOException {
        var l = lhs().llvm(map, writer);
        var r = rhs().llvm(map, writer);
        var x = format("%%_%d", id());
        writer.append(format("\t%s = fadd double %s, %s\n", x, l, r));
        return x;
    }
}
