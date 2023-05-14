import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Test {
    public static int MAXIMUM_THREADS = 2;
    public static ExecutorService executor;
    public static int sum = 0;
    public static final Object o = new Object();
    public static FastSemaphore s = new FastSemaphore(0);
    public static FastSemaphore m = new FastSemaphore(0);
    public static void test() {
        while (sum < 100000) {
            try {
                synchronized (o) {
                    o.wait();
                }
            } catch (InterruptedException e) {
                System.out.println("fun");
            }
            catch (Exception e) {
                e.printStackTrace();
                return;
            }
            sum++;
        }
    }

    public static void test2() {
        while (sum < 100000) {
            try {
                s.acquire();
                sum++;
                m.release();
            } catch (Exception e) {
                System.out.println("fun");
            }
        }
    }


    public static void main(String[] args) {
        executor = Executors.newFixedThreadPool(MAXIMUM_THREADS);
        executor.submit(Test::test2);
        long startTime = System.currentTimeMillis();
//        while (sum < 100000)
//            synchronized (o) {
//                o.notify();
//            }
        try {
            while (sum < 100000) {
                System.out.println(sum);
                s.release();
                m.acquire();
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }
        long endTime = System.currentTimeMillis();
        double timeTaken = (endTime - startTime) / 1000.0;
        System.out.println(sum);
        System.out.println("Time execution = " + timeTaken);
        executor.shutdown();
    }
}
