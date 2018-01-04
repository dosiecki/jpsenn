package dvolv;

import java.util.ArrayList;
import java.util.List;

public class StatAccum {

    private List<Double> vals;

    public StatAccum() {
        this.vals = new ArrayList<Double>();
    }

    public void put(double val) {
        this.vals.add(val);
    }

    public double avg() {
        double sum = 0L;
        for (double val : vals) sum += val;
        return (sum / vals.size());
    }

    public double max() {
        double max = 0L;
        for (double val : vals) {
            if (val > max) max = val;
        }
        return max;
    }

    public int n() {
        return vals.size();
    }

} 
