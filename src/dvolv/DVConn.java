package dvolv;

import com.google.gson.Gson;

public class DVConn extends Object {

    public static final double MUT_FRAC = 20.0;
    public static final double MAX_WEIGHT = 15.0;

    public static final Gson JsonHelper = new Gson();
    
    public enum FuncType { ADD, MULT, BIAS };
    
    public int from;
    public int to;
    public double weight;
    public FuncType funcType = FuncType.ADD;
    public boolean enable = true;

    public DVConn(int from, int to, double weight, FuncType funcType) {
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.funcType = funcType;
    }

    public DVConn(DVNode from, DVNode to, double weight, FuncType funcType) {
        this(safeNodeID(from), to.id, weight, funcType);
        //System.out.println("new conn " + safeNodeID(from) + " -> " + to.id);
    }

    public WeightMutation getMutation() {
        return new WeightMutation(this);
    }

    private static int safeNodeID(DVNode node) {
        if (node == null) return Integer.MAX_VALUE;
        return node.id;
    }

    public class WeightMutation {
        public DVConn conn;
        private double target;

        public WeightMutation(DVConn conn) {
            this.conn = conn;
            target = DVMath.rand(-1.0*MAX_WEIGHT, MAX_WEIGHT);
        }

        public void apply() {
            conn.weight = (((conn.weight*MUT_FRAC) + target)/(MUT_FRAC+1.0));
        }

        public void reverse() {
            conn.weight = (((conn.weight * (MUT_FRAC+1.0)) - target)/MUT_FRAC);
        }
    }

    public String freeze() {
        return JsonHelper.toJson(this);
    }

    public DVConn thaw(String frozen) {
        return JsonHelper.fromJson(frozen, DVConn.class);
    }

    public DVConn clone() {
        return thaw(this.freeze());
    }

} 
