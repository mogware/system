package org.mogware.system.threading;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.mogware.system.delegates.Func0;

public class SpinWait {
    public static final long DEFAULT_MAX_SPINS = 50;
    public static final long DEFAULT_MAX_YIELDS = 50;
    public static final long DEFAULT_MIN_PARK_PERIOD_NS =
            TimeUnit.NANOSECONDS.toNanos(1);
    public static final long DEFAULT_MAX_PARK_PERIOD_NS =
            TimeUnit.MICROSECONDS.toNanos(100);

    private enum State { INITIAL, SPINNING, YIELDING, PARKING };

    private final long maxSpins;
    private final long maxYields;
    private final long minParkPeriodNs;
    private final long maxParkPeriodNs;

    private State state;
    private long spins;
    private long yields;
    private long parkPeriodNs;
    private int counter;

    public SpinWait() {
        this(DEFAULT_MAX_SPINS, DEFAULT_MAX_YIELDS,
                DEFAULT_MIN_PARK_PERIOD_NS, DEFAULT_MAX_PARK_PERIOD_NS);
    }

    public SpinWait(final long maxSpins, final long maxYields,
            final long minParkPeriodNs, final long maxParkPeriodNs) {
        this.counter = 0;
        this.maxSpins = maxSpins;
        this.maxYields = maxYields;
        this.minParkPeriodNs = minParkPeriodNs;
        this.maxParkPeriodNs = maxParkPeriodNs;
        this.state = State.INITIAL;
    }

    public boolean nextSpinWillYield() {
        return this.state != State.INITIAL &&
            (this.state != State.SPINNING || this.spins >= this.maxSpins);
    }

    public void spinOnce() {
        switch (this.state) {
        case INITIAL:
            this.state = State.SPINNING;
            this.spins = 0;
        case SPINNING:
            if (this.spins++ < this.maxSpins) {
                if (this.counter > 0) {
                    if (ThreadLocalRandom.current().nextInt() > 0)
                            this.counter--;
                }
                else
                    this.counter = 64;
                break;
            }
            this.state = State.YIELDING;
            this.yields = 0;
        case YIELDING:
            if (this.yields++ < this.maxYields) {
                Thread.yield();
                break;
            }
            this.state = State.PARKING;
            this.parkPeriodNs = minParkPeriodNs;
        case PARKING:
            LockSupport.parkNanos(this.parkPeriodNs);
            this.parkPeriodNs = Math.min(
                this.parkPeriodNs << 1, this.maxParkPeriodNs
            );
            break;
        }
    }

    public void reset() {
        this.state = State.INITIAL;
    }

    public static boolean spinUntil(Func0<Boolean> cond) {
        return spinUntil(cond, Long.MAX_VALUE);
    }

    public static boolean spinUntil(Func0<Boolean> cond, TimeSpan timeout) {
        long tm = (long) timeout.getTotalMilliseconds();
        if (tm < 0 || tm > Long.MAX_VALUE)
            throw new IllegalArgumentException("sleep value of of range");
        return spinUntil(cond, tm);
    }

    public static boolean spinUntil(Func0<Boolean> cond, long timeout) {
        if (timeout < 0L)
            throw new IllegalArgumentException("timeout value of of range");
        if (cond == null)
            throw new NullPointerException("condition must not be null.");
        long startMillis = 0L;
        if (timeout != 0L && timeout != Long.MAX_VALUE)
            startMillis = System.currentTimeMillis();
        SpinWait spinner = new SpinWait();
        while (!cond.call()) {
            if (timeout == 0)
                return false;
            spinner.spinOnce();
            if (timeout != Long.MAX_VALUE && spinner.nextSpinWillYield())
                if (timeout <= System.currentTimeMillis() - startMillis)
                    return false;
        }
        return true;
    }
}
