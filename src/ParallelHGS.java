import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ParallelHGS {
    /**
     * Parameter of algorithm
     */
    public static int population_size = 100;
    public static double mutation_rate = 0.1;
    public static int delta = 7;
    // theta is still experimental
    public static double theta = 0;
    public static int iterations = 1000000;

    /**
     * Shared memory variable for parallel computing
     */
    // replace individual for each thread determine by main processor
    public static int[] replace_zones;
    public static int[][] shared_population = new int[population_size][];
    public static double[] shared_costs = new double[population_size];

    // First phase simple lock
    public static boolean[] lock_thread = new boolean[Main.MAXIMUM_THREADS - 1];
    // Semaphore that synchronize between main - slave for phase 2. Note that locks are not quite well
    public static FastSemaphore[] sem_main = new FastSemaphore[Main.MAXIMUM_THREADS -1];
    public static FastSemaphore[] sem_slave = new FastSemaphore[Main.MAXIMUM_THREADS - 1];
    // store shared initial feasible solution
    public static ArrayList<Pair<int[], Double>> tmp_population = new ArrayList<>();
    // store shared random-generated gen for phase 1 to assign task each subprocess
    public static int[][] random_stuff = new int[Main.MAXIMUM_THREADS - 1][];



    // this is where main processor execute.
    public int[] PHGS(int[] d, double[][] c, int Q, int max_vehicle) {
        int p_iteration = iterations / (Main.MAXIMUM_THREADS - 1) + 1;      // number of parallel iterations
        // initialize locks and semaphore
        Arrays.fill(lock_thread, true);
        for (int i = 0; i < Main.MAXIMUM_THREADS - 1; i++) {
            sem_main[i] = new FastSemaphore(0);
            sem_slave[i] = new FastSemaphore(0);
        }
        // initialize all possible threads. -1 because the main thread is still count a thread
        for (int i = 0; i < Main.MAXIMUM_THREADS - 1; i++) {
            final int fI = i;
            Main.executor.submit(() -> sub_processor(p_iteration, fI, d, c, Q, max_vehicle));
        }


        int random_it = 0;
        // Phase 1: Establish Initial solution. This process will repeat until reached the population size
        System.out.println("Starting phase 1:");
        while (tmp_population.size() < population_size) {
            random_stuff[random_it] = createRandomChild(d.length - 1);
            random_it++;
            if (random_it == Main.MAXIMUM_THREADS - 1) {
                if (lock_thread[0]) {
                    Arrays.fill(lock_thread, false);
                }
                random_it = 0;
            }
        }

        for (int i = 0; i < population_size; i++) {
            shared_population[i] = tmp_population.get(i).getKey();
            shared_costs[i] = tmp_population.get(i).getValue();
        }
        System.out.println(Arrays.toString(shared_costs));
        double min = shared_costs[0];

        //Phase 2: Hybrid Genetic Search / Memetic Search
        replace_zones = new int[population_size / 2];
        for (int i = 0; i < replace_zones.length; i++)
            replace_zones[i] = population_size / 2 + i;
        int it = 0;
        while (it < p_iteration) {
            // unlock threads for current iteration
            for (int i = 0; i < Main.MAXIMUM_THREADS - 1; i++)
                sem_slave[i].release();
            // Second half of the population is selected to be replaced. For less headache in formula, population size now is assumed an even numbers
            shuffle(replace_zones);
            // wait for another processes done their job
            try {
                for (int i = 0; i < Main.MAXIMUM_THREADS - 1; i++)
                    sem_main[i].acquire();
            }
            catch (Exception e) {
                System.out.println(e);
            }
                

            // sort the population ascending with cost criteria
            ArrayList<Pair<int[], Double>> combined = new ArrayList<>();
            for (int i = 0; i < population_size; i++) {
                combined.add(new Pair<>(shared_population[i], shared_costs[i]));
            }
            combined.sort(Comparator.comparing(Pair::getValue));

            for (int i = 0; i < population_size; i++) {
                shared_population[i] = combined.get(i).getKey();
                shared_costs[i] = combined.get(i).getValue();
            }

            if (shared_costs[0] < min) {
                min = shared_costs[0];
                System.out.println(min);
            }

            //replace_zones = null;
            it += 1;
        }

        return shared_population[0];
    }

    public void sub_processor(int p_iteration, int threadID, int[] d, double[][] c, int Q, int max_vehicle) {
        Random rnd = ThreadLocalRandom.current();
        while (lock_thread[threadID]){}
        //System.out.println(threadID);
        try {
            while (tmp_population.size() < population_size) {
                int[] getGen = random_stuff[threadID];
                double cost = new Evaluate(getGen, d, c, Q, max_vehicle).cost;
                if (cost != Double.POSITIVE_INFINITY) {
                    addToTmp(getGen, cost);
                }
            }
        }
        // Exception without catch in parallel environment is extreme dangerous
        catch (NullPointerException e) {
            System.out.println("What is going wrong here?");
        }
        catch (Exception e) {
            System.out.println("Who are you?");
        }

        int it = 0;
        try {
            while (it < p_iteration) {
                sem_slave[threadID].acquire();
                int i = rnd.nextInt(population_size);
                int j = rnd.nextInt(population_size);
                int[] child = ox_crossover(shared_population[i], shared_population[j]);
                mutate(child);
                Pair<int[], Double> improve = LocalSearch.LS_2_opt(child, d, c, Q, max_vehicle);
                int[] improvedChild = improve.getKey();
                double cost = improve.getValue();
                if (cost < shared_costs[0])
                    update_best_individual(improvedChild, cost);
                else {
                    if (cost != Double.POSITIVE_INFINITY)
                        if (accept(child, shared_population, cost, shared_costs)) {
                            shared_population[replace_zones[threadID]] = improvedChild;
                            shared_costs[replace_zones[threadID]] = cost;
                        }
                }
                sem_main[threadID].release();
                it += 1;
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }


    }

    public void shuffle(int[] arr) {
        Random rnd = ThreadLocalRandom.current();
        for (int i = arr.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            int a = arr[index];
            arr[index] = arr[i];
            arr[i] = a;
        }

    }
    public int[] createRandomChild(int n) {
        int[] ar = new int[n];
        for (int i = 0; i < n; i++)
            ar[i] = i + 1;
        shuffle(ar);
        return ar;
    }

    public int[] ox_crossover(int[] parent1, int[] parent2) {
        int[] route1 = parent1.clone();
        int[] route2 = parent2.clone();
        int[] child1 = new int[route1.length];
        Random rnd = ThreadLocalRandom.current();
        int cut1 = rnd.nextInt(parent1.length + 1);
        int cut2 = rnd.nextInt(parent1.length + 1);
        if (cut1 > cut2) {
            int temp = cut1;
            cut1 = cut2;
            cut2 = temp;
        }

        int[] child1_middle = Arrays.copyOfRange(route1, cut1, cut2);
        ArrayList<Integer> child1_remaining = new ArrayList<>();
        for (int k : route2) {
            boolean found = false;
            for (int i : child1_middle) {
                if (k == i) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                child1_remaining.add(k);
            }
        }
        int index = 0;
        for (int i = 0; i < cut1; i++) {
            child1[index++] = child1_remaining.get(i);
        }
        for (int j : child1_middle) {
            child1[index++] = j;
        }
        for (int i = cut1; i < child1_remaining.size(); i++) {
            child1[index++] = child1_remaining.get(i);
        }
        return child1;
    }

    public boolean mutate(int[] child) {
        Random rnd = ThreadLocalRandom.current();
        if (rnd.nextDouble() <= mutation_rate) {
            int i = rnd.nextInt(child.length);
            int j = rnd.nextInt(child.length);
            int temp = child[i];
            child[i] = child[j];
            child[j] = temp;
            return true;
        }
        return false;
    }

    public int hamming_distance(int[] arr1, int[] arr2) {
        int result = 0;
        for (int i = 0; i < arr1.length; i++)
            if (arr1[i] != arr2[i])
                result++;
        return result;
    }

    public boolean accept(int[] child, int[][] population, double cost, double[] costs) {
        for (int i = 0; i < population_size; i++) {
            if (hamming_distance(child, population[i]) < delta || Math.abs(cost - costs[i]) < theta)
                return false;
        }
        return true;
    }

    public synchronized void addToTmp(int[] gen, double costs) {
        tmp_population.add(new Pair<>(gen, costs));
    }

    public synchronized void update_best_individual(int[] individual, double cost) {
        shared_population[0] = individual;
        shared_costs[0] = cost;
    }

//    public static void reloadShareData() {
//        shared_population = new int[population_size][];
//        shared_costs = new double[population_size];
//        lock_thread = new boolean[Main.MAXIMUM_THREADS - 1];
//        sem_main = new Semaphore[Main.MAXIMUM_THREADS -1];
//        sem_slave = new Semaphore[Main.MAXIMUM_THREADS - 1];
//        tmp_population = new ArrayList<>();
//        random_stuff = new int[Main.MAXIMUM_THREADS - 1][];
//    }
}