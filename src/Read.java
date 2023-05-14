import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Read {
    public int[] d;
    public double[][] c;
    public int Q;
    public int max_vehicle;
    public Read(String file_name) {
        try {
            File file = new File(file_name);
            Scanner scanner = new Scanner(file);
            List<String> lines = new ArrayList<>();
            while (scanner.hasNextLine()) {
                lines.add(scanner.nextLine());
            }
            scanner.close();
            String[] line_two = lines.get(1).split("[^0-9]+");
            this.max_vehicle = Integer.parseInt(line_two[1]);
            this.Q = Integer.parseInt(lines.get(5).replaceAll("\\D+", ""));
            int dimensions = Integer.parseInt(lines.get(3).replaceAll("\\D+", ""));
            this.d = new int[dimensions];
            int[] x = new int[dimensions];
            int[] y = new int[dimensions];
            for (int i = 0; i < dimensions; i++) {
                String[] demand_line = lines.get(8 + dimensions + i).split("\\s+");
                String[] node_line = lines.get(7 + i).trim().split("\\s+");
                int demand = Integer.parseInt(demand_line[demand_line.length - 1]);
                x[i] = Integer.parseInt(node_line[1]);
                y[i] = Integer.parseInt(node_line[2]);
                d[i] = demand;
            }
            createDistanceMatrix(x, y);
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        }
    }

    private void createDistanceMatrix(int[] x, int[] y) {
        this.c = new double[x.length][x.length];
        for (int i = 0; i < x.length; i++)
            for (int j = 0; j < x.length; j++)
                this.c[i][j] = Math.sqrt((x[i] - x[j]) * (x[i] - x[j])  + (y[i] - y[j]) * (y[i] - y[j]));

    }
}
