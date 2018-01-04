package dvolv;

import java.util.List;
import java.util.Random;

public class DVMath {

    private static Random random = new Random();
    
    public static int randInt(int min, int max) {
        return min + (int)(random.nextDouble() * (1 + max - min));
    }

    public static int randIntExcl(int min, int max) {
        return min + (int)(random.nextDouble() * (max - min));
    }

    public static int choose(int range) {
        return (int)(random.nextDouble() * range);
    }

    public static <T> T choose(List<T> list) {
        return list.get(choose(list.size()));
    }

    public static <T> T choose(T[] list) {
        return list[choose(list.length)];
    }

    public static double rand(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    public static double score(double expected, double actual) {
        return (1.0 - Math.abs(expected - actual));
    }

    public static void initConst() {
        random.setSeed(1L);
    }

    public static void initRand() {
        random.setSeed(System.currentTimeMillis());
    }

    public static boolean tryProb(float prob) {
        return (random.nextFloat() < prob);
    }

    public static boolean tryProb(double prob) {
        return (random.nextDouble() < prob);
    }

} 
