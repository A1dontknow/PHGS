import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static int MAXIMUM_THREADS = 11;
    public static ExecutorService executor;
    public double run(String path) {
        executor = Executors.newFixedThreadPool(MAXIMUM_THREADS);
        Read instance = new Read(path);
        ParallelHGS s = new ParallelHGS();
        int[] best_individual = s.PHGS(instance.d, instance.c, instance.Q, instance.max_vehicle);
        System.out.println();
        Evaluate eva = new Evaluate(best_individual, instance.d, instance.c, instance.Q, instance.max_vehicle);

        System.out.println("Best cost: " +  eva.cost);
        System.out.println("Solution:");
        for (int[] routes : eva.decode())
            System.out.println(Arrays.toString(routes));
        executor.shutdown();
        return eva.cost;
    }

    public static void main(String[] args) {
        Main main = new Main();
        String path = "/home/who/IdeaProjects/PHGS_v2/src/Dataset/E-n101-k8.txt";
        long startTime = System.currentTimeMillis();
        main.run(path);
        long endTime = System.currentTimeMillis();
        while (!executor.isTerminated()) {}
        double timeTaken = (endTime - startTime) / 1000.0;
        System.out.println("Time execution = " + timeTaken + " s");
    }
}

