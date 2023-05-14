import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class LocalSearch {
    /**
    Parallel this part is dumb
*/
    public static double improve(ArrayList<int[]> routes, double[][] c) {
        for (int[] route : routes) {
            if (route.length >= 2) {
                for (int x = 0; x < route.length - 1; x++) {
                    for (int v = x + 1; v < route.length; v++) {
                        double delta;
                        if (x == 0 && v == route.length - 1) {
                            delta = c[0][route[v]] + c[route[x]][0] - c[0][route[x]] - c[route[v]][0];
                        } else if (x == 0) {
                            delta = c[0][route[v]] + c[route[x]][route[v + 1]] - c[0][route[x]] - c[route[v]][route[v + 1]];
                        } else if (v == route.length - 1) {
                            delta = c[route[x - 1]][route[v]] + c[route[x]][0] - c[route[x - 1]][route[x]] - c[route[v]][0];
                        } else {
                            delta = c[route[x - 1]][route[v]] + c[route[x]][route[v + 1]] - c[route[x - 1]][route[x]] - c[route[v]][route[v + 1]];
                        }

                        if (delta < -0.01) {
                            int i = x;
                            int j = v;
                            while (i < j) {
                                int temp = route[i];
                                route[i] = route[j];
                                route[j] = temp;
                                i++;
                                j--;
                            }
                            return delta;
                        }
                    }
                }
            }
        }
        return 0;
    }

    public static Pair<int[], Double> LS_2_opt(int[] individual, int[] d, double[][] c, int Q, int max_vehicle) {
        Evaluate eva = new Evaluate(individual, d, c, Q, max_vehicle);
        double cost = eva.cost;
        if (cost == Double.POSITIVE_INFINITY)
            return new Pair<>(individual, Double.POSITIVE_INFINITY);
        ArrayList<int[]> routes = eva.decode();
        double delta = -1;
        while (delta < 0) {
            delta = improve(routes, c);
            cost += delta;
        }
        return new Pair<>(Evaluate.encode(routes), cost);
    }
}
