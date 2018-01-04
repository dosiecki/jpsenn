package dvolv;

import dvolv.DVConn.FuncType;
import static dvolv.DVConn.MAX_WEIGHT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;

public class DVNet extends Object {

    private static final AtomicInteger IDS = new AtomicInteger(0);

    public static final Gson JsonHelper = new Gson();
    
    public static final double connCost = 1.0;
    public double costPen = (1.0/1.0);

    public transient int id;
    public Map<Integer,DVNode> nodes = new LinkedHashMap<Integer,DVNode>();
    private Map<String,Integer> nodesByLabel = new HashMap<String,Integer>();
    public List<DVConn> conns = new ArrayList<DVConn>();
    public double cost;
    public double score;
    public double grade;
    public String lineage;
    public String label;
    public boolean bred = false;

    public void newId() {
        id = IDS.incrementAndGet();
        lineage = ""+id;
    }

    public void add(DVNode node) {
        nodes.put(node.id, node);
        if (node.label != null) nodesByLabel.put(node.label, node.id);
    }

    private void addNodeWithId(int id) {
        if (Integer.MAX_VALUE == id) return;
        if (nodes.get(id) != null) return;
        DVNode node = new DVNode(id);
        add(node);
    }

    public void add(DVConn conn) {
        conns.add(conn);
    }

    public void reset() {
        nodepreloop: for (DVNode node : nodes.values()) {
            if (node.isInput) continue nodepreloop;
            node.value = 0.0;
        }
    }

    public void proc() {
        cost = 1.0;

        nodepreloop: for (DVNode node : nodes.values()) {
            if (node.isInput) continue nodepreloop;
            node.preProc();
        }

        connloop: for (DVConn conn : conns) {
            if (! conn.enable) continue connloop;
            procConn(conn);
            cost += connCost;
        }

        nodepostloop: for (DVNode node : nodes.values()) {
            if (node.isInput) continue nodepostloop;
            node.postProc();
        }
    }

    private void procConn(DVConn conn) {
        switch (conn.funcType) {
            case BIAS:
                nodes.get(conn.to).nextValueAccum += (conn.weight);
                break;
            case MULT:
                nodes.get(conn.to).nextValueAccum *= (nodes.get(conn.from).value * conn.weight);
                break;
            case ADD:
            default:
                nodes.get(conn.to).nextValueAccum += (nodes.get(conn.from).value * conn.weight);
        }
    }

    public void enhance() {
        int rand = DVMath.randInt(0, 1000);
         if (rand > 800) {
            addNode();
        } else {
            addConn();
        }
    }
    
    public void dropConn() {
        if (conns.size() < 2) return;
        DVConn conn = DVMath.choose(conns);
        conns.remove(conn);
    }

    public DVConn.WeightMutation getConnMutation() {
        DVConn conn = DVMath.choose(conns);
        return conn.getMutation();
    }

    public void addConn() {
        List<DVNode> fromNodes = new ArrayList<DVNode>();
        List<DVNode> toNodes = new ArrayList<DVNode>();
        for (DVNode node : this.nodes.values()) {
            if (!node.isOutput) fromNodes.add(node);
            if (!node.isInput) toNodes.add(node);
        }
        DVNode from = DVMath.choose(fromNodes);
        DVNode to = DVMath.choose(toNodes);
        if (from.id == to.id) return;
        for (DVConn conn : conns) {
            if (conn.from == from.id && conn.to == to.id) return;
            if (conn.from == to.id && conn.to == from.id) return;
        }
        int rand = DVMath.randInt(0, 100);
        if (rand > 66) {
            add(new DVConn(null, to, DVMath.rand(-1.0*MAX_WEIGHT, MAX_WEIGHT), FuncType.BIAS));
        } else {
            connectNodes(from, to);
        }
    }

    public void addNode() {
        List<DVNode> fromNodes = new ArrayList<DVNode>();
        List<DVNode> toNodes = new ArrayList<DVNode>();
        for (DVNode node : this.nodes.values()) {
            if (!node.isOutput) fromNodes.add(node);
            if (!node.isInput) toNodes.add(node);
        }
        DVNode from = DVMath.choose(fromNodes);
        DVNode to = DVMath.choose(toNodes);
        DVNode newNode = new DVNode();
        add(newNode);
        connectNodes(from, newNode);
        connectNodes(newNode, to);
    }

    private void connectNodes(DVNode from, DVNode to) {
        add(new DVConn(from, to, DVMath.rand(-1.0*MAX_WEIGHT, MAX_WEIGHT), FuncType.ADD));
    }

    public boolean isConnIO(DVConn conn) {
        if ((null != nodes.get(conn.from)) && nodes.get(conn.from).isInput) return true;
        if ((null != nodes.get(conn.to)) && nodes.get(conn.to).isOutput) return true;
        return false;
    }

    public static DVNet fromTestSet(TestSet ts, int size, int depth) {
        DVNet net = new DVNet();
        Set<String> inputNames = new HashSet<String>();
        Set<String> outputNames = new HashSet<String>();
        for (TestCase tc : ts.cases) {
            for (Map.Entry<String,Double> input : tc.inputs.entrySet()) {
                inputNames.add(input.getKey());
            }
            for (Map.Entry<String,Double> output : tc.outputs.entrySet()) {
                outputNames.add(output.getKey());
            }
        }
        List<Set<DVNode>> hiddens = new ArrayList<Set<DVNode>>(depth);
        Set<DVNode> prevLayer = null;
        for (int j=0; j<depth; j++) {
            Set<DVNode> layer = new HashSet<DVNode>(size);
            for (int i=0; i<size; i++) {
                DVNode node = new DVNode();
                node.label = "pre";
                layer.add(node);
                net.add(node);
                if (null != prevLayer) {
                    for (DVNode prevNode : prevLayer) {
                        net.add(new DVConn(prevNode, node, 0.0, FuncType.ADD));
                    }
                }
            }
            hiddens.add(layer);
            prevLayer = layer;
        }
        // link inputs to first layer
        for (String name : inputNames) {
            DVNode node = new DVNode();
            node.label = name;
            node.isInput = true;
            net.add(node);
            for (DVNode nexus : hiddens.get(0)) {
                net.add(new DVConn(node, nexus, 0.0, FuncType.ADD));
            }
        }
        // link outputs to last layer
        for (String name : outputNames) {
            DVNode node = new DVNode();
            node.label = name;
            node.isOutput = true;
            net.add(node);
            for (DVNode nexus : hiddens.get(depth-1)) {
                net.add(new DVConn(nexus, node, 0.0, FuncType.ADD));
            }
        }
        return net;
    }

    public void inheritFrom(DVNet parent, double prob) {
        for (DVConn conn : parent.conns) {
            if ( DVMath.tryProb(prob) ) {
                DVConn newConn = conn.clone();
                if ((null != parent.nodes.get(conn.from)) && parent.nodes.get(conn.from).isInput) {
                    newConn.from = nodesByLabel.get(parent.nodes.get(conn.from).label);
                }
                if (parent.nodes.get(conn.to).isOutput) {
                    newConn.to = nodesByLabel.get(parent.nodes.get(conn.to).label);
                }
                conns.add(newConn);
                addNodeWithId(conn.from);
                addNodeWithId(conn.to);
            }
        }
        for (DVNode node : nodes.values()) {
            if (null != parent.nodes.get(node.id)) {
                node.label = parent.nodes.get(node.id).label;
            }
        }
    }

    public String freeze() {
        return JsonHelper.toJson(this);
    }

    public DVNet thaw(String frozen) {
        return JsonHelper.fromJson(frozen, DVNet.class);
    }

    public DVNet clone() {
        DVNet net = thaw(this.freeze());
        net.id = this.id;
        return net;
    }

    public void setValueByLabel(String label, double value) {
        nodes.get(nodesByLabel.get(label)).value = value;
    }

    public double getValueByLabel(String label) {
        return nodes.get(nodesByLabel.get(label)).value;
    }

    public double test(TestCase testCase, int cycles) {
        reset();
        for (Map.Entry<String,Double> input : testCase.inputs.entrySet()) {
            setValueByLabel(input.getKey(), input.getValue());
        }
        for (int i=0; i<cycles; i++) proc();
        double scoreNumerator = 0.0;
        for (Map.Entry<String,Double> output: testCase.outputs.entrySet()) {
            scoreNumerator += DVMath.score(output.getValue(), getValueByLabel(output.getKey()));
        }
        return scoreNumerator / testCase.outputs.size();
    }

    public void test(TestSet testSet, int cycles) {
        double scoreNumerator = 0.0;
        double scoreDenom = 0.0;
        for (TestCase testCase : testSet.cases) {   
            double score = test(testCase, cycles);
            scoreNumerator += (score * testCase.weight);
            scoreDenom += testCase.weight;
        }
        score = scoreNumerator / scoreDenom;
        grade = score / Math.pow(cost, costPen);

        StringBuilder s = new StringBuilder("DVN_");
        s.append(testSet.name);
        s.append("_g").append(String.format("%.7f", grade));
        s.append("_c").append(String.format("%.1f", cost));
        s.append("_s").append(String.format("%.7f", score));
        label = s.toString();
    }

    public void test(TestSet testSet, int cycles, int regens) {
        double scoreNumerator = 0.0;
        double scoreDenom = 0.0;
        for (int i=0; i<regens; i++ ) { 
            for (TestCase testCase : testSet.cases) {   
                double score = test(testCase, cycles);
                scoreNumerator += (score * testCase.weight);
                scoreDenom += testCase.weight;
            }
            testSet.regen();
        }
        score = scoreNumerator / scoreDenom;
        grade = score / Math.pow(cost, costPen);

        StringBuilder s = new StringBuilder("DVN_");
        s.append(testSet.name);
        s.append("_g").append(String.format("%.7f", grade));
        s.append("_c").append(String.format("%.1f", cost));
        s.append("_s").append(String.format("%.7f", score));
        label = s.toString();
    }

    public void print() {
        inputloop: for (DVNode node : nodes.values()) {
            if (!node.isInput) continue inputloop;
            System.out.println( "in (" + node.label + "): " + node.value);
        }
        outputloop: for (DVNode node : nodes.values()) {
            if (!node.isOutput) continue outputloop;
            System.out.println( "out (" + node.label + "): " + node.value);
        }
        System.out.println( "cost: " + cost);
    }

    public String toGraphViz() {
        StringBuilder s = new StringBuilder();
        s.append("digraph g {\n");
        s.append("graph [overlap=\"false\"];\n");
        for (DVConn conn : conns) {
            s.append("N").append(conn.from).append(" -> N").append(conn.to);
            String etype = "";
            if (conn.funcType == FuncType.ADD) {
                etype = "ADD";
            } else if (conn.funcType == FuncType.MULT) {
                etype = "MULT";
            } else if (conn.funcType == FuncType.BIAS) {
                etype = "BIAS";
            }
            s.append(" [label=\"").append(String.format("%.3f", conn.weight)).append(" ").append(etype).append("\"]");
            s.append(";\n");
        }
        for (DVNode node : nodes.values()) {
            s.append("N").append(node.id).append(" [");
            String labelOut = "";
            if (node.label != null) labelOut = node.label;
            s.append("label=\"").append(labelOut).append(" ").append("\",");
            if (node.isOutput) {
                s.append("fillcolor=\"lightblue\",style=\"filled\"");
            } else if (node.isInput) {
                s.append("fillcolor=\"pink\",style=\"filled\"");
            } else {
                s.append("fillcolor=\"white\",style=\"filled\"");
            }
            s.append("];\n");
        }
        s.append("{rank=min; ");
        for (DVNode node : nodes.values()) {
            if (node.isInput) {
                s.append("N").append(node.id).append("; ");
            }
        }
        s.append("}\n");
        s.append("{rank=max; ");
        for (DVNode node : nodes.values()) {
            if (node.isOutput) {
                s.append("N").append(node.id).append("; ");
            }
        }
        s.append("}\n");
        s.append("labelloc=\"t\"\n");
        s.append("label=\"").append(lineage).append("\"\n");
        s.append("}\n");
        return s.toString();
    }

}
