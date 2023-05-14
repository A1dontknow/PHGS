import java.util.concurrent.atomic.AtomicInteger;

/**
 * Non-fair lock-free Semaphore with customizable back-off strategy for high
 * contention scenarios.
 *
 * @author user2296177
 * @version 1.0
 *
 */
public class FastSemaphore {
    /**
     * Default back-off strategy to prevent busy-wait loop. Calls
     * Thread.sleep(0, 1);. Has better performance and lower CPU usage than -> No idiot sleep here. I want fast
     * Thread.yield() inside busy-wait loop.
     */
    private static Runnable defaultBackoffStrategy = () -> {
//        try {
//            Thread.sleep( 0, 0);
//        } catch ( InterruptedException e ) {
//            e.printStackTrace();
//        }
        // no idiot sleep
        return;
    };

    private AtomicInteger permitCount;
    private final Runnable backoffStrategy;

    /**
     * Construct a Semaphore instance with maxPermitCount permits and the
     * default back-off strategy.
     *
     * @param maxPermitCount
     *            Maximum number of permits that can be distributed.
     */
    public FastSemaphore(final int maxPermitCount ) {
        this( maxPermitCount, defaultBackoffStrategy );
    }

    /**
     * Construct a Semaphore instance with maxPermitCount permits and a custom
     * Runnable to run a back-off strategy during contention.
     *
     * @param maxPermitCount
     *            Maximum number of permits that can be distributed.
     * @param backoffStrategy
     *            Runnable back-off strategy to run during high contention.
     */
    public FastSemaphore(final int maxPermitCount, final Runnable backoffStrategy ) {
        permitCount = new AtomicInteger( maxPermitCount );
        this.backoffStrategy = backoffStrategy;
    }

    /**
     * Attempt to acquire one permit and immediately return.
     *
     * @return true : acquired one permits.<br>
     *         false: did not acquire one permit.
     */
    public boolean tryAcquire() {
        return tryAcquire( 1 );
    }

    /**
     * Attempt to acquire n permits and immediately return.
     *
     * @param n
     *            Number of permits to acquire.
     * @return true : acquired n permits.<br>
     *         false: did not acquire n permits.
     */
    public boolean tryAcquire( final int n ) {
        return tryDecrementPermitCount( n );
    }

    /**
     * Acquire one permit.
     */
    public void acquire() {
        acquire( 1 );
    }

    /**
     * Acquire n permits.
     *
     * @param n
     *            Number of permits to acquire.
     */
    public void acquire( final int n ) {
        while ( !tryDecrementPermitCount( n ) ) {
            backoffStrategy.run();
        }
    }

    /**
     * Release one permit.
     */
    public void release() {
        release( 1 );
    }

    /**
     * Release n permits.
     *
     * @param n
     *            Number of permits to release.
     */
    public void release( final int n ) {
        permitCount.addAndGet( n );
    }

    /**
     * Try decrementing the current number of permits by n.
     *
     * @param n
     *            The number to decrement the number of permits.
     * @return true : the number of permits was decremented by n.<br>
     *         false: decrementing the number of permits results in a negative
     *         value or zero.
     */
    private boolean tryDecrementPermitCount( final int n ) {
        int oldPermitCount;
        int newPermitCount;
        do {
            oldPermitCount = permitCount.get();
            newPermitCount = oldPermitCount - n;
            if ( newPermitCount > n ) throw new ArithmeticException( "Overflow" );
            if ( newPermitCount < 0 ) return false;
        } while ( !permitCount.compareAndSet( oldPermitCount, newPermitCount ) );
        return true;
    }
}