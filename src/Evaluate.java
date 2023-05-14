import java.util.ArrayList;
import java.util.Arrays;

public class Evaluate {
    // saved the partial of the past evaluation. A little bit optimization
    public ArrayList<Integer> P_saved = null;
    public int[] individual;
    public double cost = Double.POSITIVE_INFINITY;

    public Evaluate(int[] arr, int[] d, double[][] c, int Q, int max_vehicle) {
        this.individual = arr;
        int n = arr.length;
        double[] V = new double[n + 1];
        int[] P = new int[n];
        Arrays.fill(V, Double.POSITIVE_INFINITY);
        V[0] = 0;

        for (int i = 1; i <= n; i++) {
            int L = 0;
            double C = 0;
            int j = i;
            while (j <= n && L <= Q) {
                L += d[arr[j - 1]];
                if (i == j) {
                    C = c[0][arr[j - 1]] + c[arr[j - 1]][0];
                } else {
                    C = C - c[arr[j - 2]][0] + c[arr[j - 2]][arr[j - 1]] + c[arr[j - 1]][0];
                }

                if (L <= Q) {
                    if (V[i - 1] + C < V[j]) {
                        V[j] = V[i - 1] + C;
                        P[j - 1] = i - 1;
                    }
                    j += 1;
                }
            }
        }

        this.P_saved = convertSet(P);
        if (P_saved.size() <= max_vehicle)
            this.cost = V[n];
    }

    public ArrayList<Integer> convertSet(int[] arr) {
        ArrayList<Integer> result = new ArrayList<>();
        result.add(arr[0]);
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] != result.get(result.size() - 1)) {
                result.add(arr[i]);
            }
        }
        return result;
    }

    public ArrayList<int[]> decode() {
        P_saved.add(individual.length);
        ArrayList<int[]> routes = new ArrayList<>();
        int[] route;
        for (int i = 0; i < P_saved.size() - 1; i++) {
            route = new int[P_saved.get(i + 1) - P_saved.get(i)];
            if (P_saved.get(i + 1) - P_saved.get(i) >= 0)
                System.arraycopy(individual, P_saved.get(i), route, 0, P_saved.get(i + 1) - P_saved.get(i));
            routes.add(route);
        }

        return routes;
    }

    public static int[] encode(ArrayList<int[]> routes) {
        return routes.stream().flatMapToInt(Arrays::stream).toArray();
    }
}
