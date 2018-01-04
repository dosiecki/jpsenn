package dvolv;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class DVNode extends Object {

    private static final AtomicInteger IDS = new AtomicInteger(0);

    public int id;
    public String label;
    public double value;
    public double nextValueAccum;
    public boolean isInput;
    public boolean isOutput;

    public DVNode() {
        this.id = IDS.incrementAndGet();
    }

    public DVNode(int id) {
        this.id = id;
    }

    public void preProc() {
        //System.out.println("proc node: zer0d");
        nextValueAccum = 0.0;
    }

    public void postProc() {
        //System.out.print("proc node post: " + nextValueAccum);
        value = activ(nextValueAccum);
        //System.out.println(" -> " + value);
    }

    private double activ(double pre) {
        //return Math.tanh(pre);
        //return (1/( 1 + Math.pow(Math.E,(-1*pre))));
        //return (1/( 1 + Math.pow(Math.E,(-1*Math.PI*pre))));
        //return (1/( 1 + Math.pow(Math.E,(-4.9*pre))));
        return (1/( 1 + Math.pow(Math.E,(-2*pre))));
    }

}

