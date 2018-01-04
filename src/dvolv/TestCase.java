package dvolv;

import java.util.HashMap;
import java.util.Map;

public class TestCase {
    
    public Map<String,Double> inputs;
    public Map<String,Double> outputs;
    public double weight = 1;

    public String readable;

    public void addInput(String label, double value) {
        if (inputs == null) inputs = new HashMap<String,Double>();
        inputs.put(label, value);
    }

    public void addOutput(String label, double value) {
        if (outputs == null) outputs = new HashMap<String,Double>();
        outputs.put(label, value);
    }

}
