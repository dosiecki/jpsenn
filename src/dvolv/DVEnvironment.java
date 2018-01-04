package dvolv;

import java.io.File;
import java.io.PrintWriter;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DVEnvironment {
    
    public static final int WORKERS = 4;

    public int poolSize = 4;
    public long dayLength = 4000L;
    public int days = 250;
    public int ponders = 6;
    public int maxMutations = 3;
    public int broodSize = 2;
    public double costPenDenom = 1000.0;
    public double costPen = (1.0/costPenDenom);
    public int tryBreed = 0;
    public double weightP1 = 0.15;
    public double weightP2 = 0.85;
    public int initLayerWidth = 7;
    public int initDepth = 1;

    private ExecutorService executor;
    private List<NetHolder> nets;
    private DVNet masterNet;

    public TestSet testSet;

    public DVEnvironment() {
        executor = Executors.newFixedThreadPool(WORKERS);
        nets = new ArrayList<NetHolder>(poolSize);
    }

    public class NetHolder {
        public DVNet net;
        public NetHolder(DVNet net) {
            this.net = net;
        }
    }

    private void init(TestSet testSet) {
        this.testSet = testSet;

        masterNet = DVNet.fromTestSet(testSet, initLayerWidth, initDepth);
        masterNet.costPen = this.costPen;

        for (int i=0; i<poolSize; i++) {
            DVNet net = masterNet.clone();
            net.newId();
            NetHolder holder = new NetHolder(net);
            nets.add(holder);
            //mutate(net);
            //net.test(testSet, ponders);
        }
    }

    private void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(1L, TimeUnit.HOURS);
        } catch (InterruptedException ie) {
        }
        for (NetHolder holder : nets) {
            //cleanout(holder.net);
        }
    }

    private void runAll() {
        System.out.print("running");
        for (int day = 1; day <= days; day++) {
            System.out.print(".");
            runDay();
        }
        System.out.println("done");
    } 

    private void runDay() {
        testSet.regen();
        List<Future> futures = new ArrayList<Future>(poolSize);
        for (final NetHolder holder : nets) {
            Runnable runner = new Runnable() {
                public void run() {
                    improveNet(holder);
                }
            };
            futures.add(executor.submit(runner));
        }
        for (Future future : futures) {
            try {
                future.get();
            } catch (InterruptedException ie) {
            } catch (ExecutionException ee) {
                throw new RuntimeException(ee.getCause());
            }
        }
        /*
        double worstScore = 1.1;
        NetHolder worstNet = null;
        StatAccum scores = new StatAccum();
        for (NetHolder holder : nets) {
            scores.put(holder.net.grade);
            //System.out.println("grade: " + String.format("%.5f", holder.net.grade) + " " + holder.net.lineage);
            if (holder.net.grade < worstScore) {
                worstScore = holder.net.grade;
                worstNet = holder;
            }
        }
        //System.out.println("worst score: " + String.format("%.5f", worstScore) + "  avg score: " + String.format("%.5f", scores.avg()) );
        //improveNet(worstNet);
        */
        for (int i=0; i<tryBreed; i++) {
            int progenitorIndex1 = DVMath.randInt(0, nets.size()-1);
            int progenitorIndex2 = DVMath.randInt(0, nets.size()-1);
            DVNet offspring = masterNet.clone();
            offspring.newId();
            offspring.inheritFrom(nets.get(progenitorIndex1).net, weightP1);
            offspring.inheritFrom(nets.get(progenitorIndex2).net, weightP2);
            offspring.lineage += "(" + nets.get(progenitorIndex1).net.lineage + " + " + nets.get(progenitorIndex2).net.lineage + ")";
            offspring.test(testSet, ponders);
            offspring.bred = true;
            //System.out.println("parent1 score: " + String.format("%.5f", nets.get(progenitorIndex1).net.grade) + " cost:" + nets.get(progenitorIndex1).net.cost );
            //System.out.println("parent2 score: " + String.format("%.5f", nets.get(progenitorIndex2).net.grade) + " cost:" + nets.get(progenitorIndex2).net.cost);
            //System.out.println("child   score: " + String.format("%.5f", offspring.grade) + " cost:" + offspring.cost);
            learnDay(offspring);
            learnDay(offspring);
            //System.out.println("child   score: " + String.format("%.5f", offspring.grade) + " cost:" + offspring.cost);
            //System.out.println("parent1: " + nets.get(progenitorIndex1).net.toGraphViz());
            //System.out.println("parent2: " + nets.get(progenitorIndex2).net.toGraphViz());
            //System.out.println("child: " + offspring.toGraphViz());
            if ((offspring.grade > nets.get(progenitorIndex1).net.grade) && (offspring.grade > nets.get(progenitorIndex2).net.grade)) {
                nets.add(new NetHolder(offspring));
            }
        }
    }

    private void learnDay(DVNet net) {
        long endTime = System.currentTimeMillis() + dayLength;
        while (System.currentTimeMillis() < endTime) {
            learn(net);
        }
    }

    private void improveNet(NetHolder holder) {
        holder.net.test(testSet, ponders);
        double oldBestGrade = holder.net.grade;
        double bestGrade = holder.net.grade;
        DVNet bestNet = holder.net;
        for (int i=0; i<broodSize; i++) {
            DVNet net = holder.net.clone();
            mutate(net);
            net.test(testSet, ponders);
            learnDay(net);
            if (net.grade > bestGrade) {
                bestNet = net;
                bestGrade = net.grade;
            }
        }
        learnDay(bestNet);
        holder.net = bestNet;
        //System.out.println(String.format("grade : %.7f", holder.net.grade));
    }

    private void mutate(DVNet net) {
        int changes = DVMath.randInt(0, maxMutations);
        for (int i=0; i<changes; i++) {
            net.enhance();
        }
    }

    private void learn(DVNet net) {
        if (net.conns.size() < 1) return;
        DVConn.WeightMutation mutation = net.getConnMutation();
        double oldGrade = net.grade;
        boolean keepGoing = true;
        int rounds = 0;
        while (keepGoing && (rounds<55)) {
            rounds++;
            mutation.apply();
            net.test(testSet, ponders);
            //System.out.println(String.format("old: %.7f new: %.7f", oldGrade, net.grade));
            if (net.grade <= oldGrade) {
                mutation.reverse();
                net.grade = oldGrade;
                keepGoing = false;
            }
            oldGrade = net.grade;
        }
    }

    private void cleanout(DVNet net) {
        List<DVConn> trashConns = new ArrayList<DVConn>();
        for (DVConn conn : net.conns) {
            double oldGrade = net.grade;
            conn.enable = false;
            net.test(testSet, ponders);
            if (net.grade >= oldGrade) {
                trashConns.add(conn);
            } else {
                conn.enable = true;
                net.grade = oldGrade;
            }
        }
        for (DVConn conn : trashConns) {
            net.conns.remove(conn);
        }
        net.test(testSet, ponders);
    }
            

    public static void main(String[] args) {
        
        try {

        DVEnvironment env = new DVEnvironment();

        //String frozenDVTS = "";
        //frozenDVTS = new String(Files.readAllBytes(Paths.get("xor.dvts")), "UTF-8");
        //TestSet testSet = JsonHelper.fromJson(frozenDVTS, TestSet.class);

        TestSet testSet = new TestSetText();

        String runName = "DV_" + (System.currentTimeMillis()/1000L) + "_" + testSet.name + "_n" + env.poolSize + "_days" + env.days + "x" + env.dayLength + "_cPen" + env.costPenDenom + "_maxMut" + env.maxMutations + "_initW" + env.initLayerWidth + "_initD" + env.initDepth + "_brood" + env.broodSize + "_tryBreed" + env.tryBreed + "_weightP1" + env.weightP1 + "_weightP2" + env.weightP2;
        System.out.println(runName);
        new File(runName).mkdirs();
        
        // ensure the mode here, since TestSets can change them
        DVMath.initRand();

        StatAccum averages = new StatAccum();
        StatAccum maxes = new StatAccum();
        StatAccum allCosts = new StatAccum();
        StatAccum bredaverages = new StatAccum();
        StatAccum bredmaxes = new StatAccum();
        StatAccum bredallCosts = new StatAccum();

        for (int i=0; i<32; i++) {
            env = new DVEnvironment();
            env.init(testSet);
            
            env.runAll();

            env.shutdown();

            StatAccum scores = new StatAccum();
            StatAccum bredscores = new StatAccum();

            DVNet bestRun = null;

            for (NetHolder holder : env.nets) {
                DVNet net = holder.net;
                net.test(testSet, env.ponders, 25);
                if (!net.bred) {
                    allCosts.put(net.cost);
                    scores.put(net.score);
                } else {
                    bredallCosts.put(net.cost);
                    bredscores.put(net.score);
                }
                PrintWriter pw = new PrintWriter(runName + File.separator + net.label + ".dot", "UTF-8");
                pw.print(net.toGraphViz());
                pw.close();
                PrintWriter pw2 = new PrintWriter(runName + File.separator + net.label + ".net.json", "UTF-8");
                pw2.print(net.freeze());
                pw2.close();

                if ((bestRun == null) || (net.score > bestRun.score)) {
                    bestRun = net;
                }
            }

            averages.put(scores.avg());
            maxes.put(scores.max());
            if (bredscores.n() > 0) {
                bredaverages.put(bredscores.avg());
                bredmaxes.put(bredscores.max());
            }

            PrintWriter pw = new PrintWriter(runName + File.separator + bestRun.label + ".sims", "UTF-8");
            for (int k=0; k<10000; k++) {
                TestCase randCase = testSet.randomCase();
                double score = bestRun.test(randCase, env.ponders);
                pw.println( String.format("%.7f %s", score, randCase.readable) );
            }
            pw.close();
        }

        System.out.println( "avg cost:  " + String.format("%.1f", allCosts.avg()) );
        System.out.println( "avg max score: " + String.format("%.7f", maxes.avg()) );
        System.out.println( "avg avg score: " + String.format("%.7f", averages.avg()) );
        System.out.println( "bred count: " + bredallCosts.n());
        System.out.println( "bred avg cost:  " + String.format("%.1f", bredallCosts.avg()) );
        System.out.println( "bred avg max score: " + String.format("%.7f", bredmaxes.avg()) );
        System.out.println( "bred avg avg score: " + String.format("%.7f", bredaverages.avg()) );

        PrintWriter pw = new PrintWriter(runName + File.separator + runName + ".stats", "UTF-8");
        pw.println( "avg cost:  " + String.format("%.1f", allCosts.avg()) );
        pw.println( "avg max score: " + String.format("%.7f", maxes.avg()) );
        pw.println( "avg avg score: " + String.format("%.7f", averages.avg()) );
        pw.println( "bred count:  " + bredallCosts.n() );
        pw.println( "bred avg cost:  " + String.format("%.1f", bredallCosts.avg()) );
        pw.println( "bred avg max score: " + String.format("%.7f", bredmaxes.avg()) );
        pw.println( "bred avg avg score: " + String.format("%.7f", bredaverages.avg()) );
        pw.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
