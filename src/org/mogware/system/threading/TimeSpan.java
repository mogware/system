package org.mogware.system.threading;

import java.io.Serializable;
import java.time.Duration;
import org.mogware.system.OperationCanceledException;

public class TimeSpan implements Serializable, Comparable {
    public static final long ticksPerMillisecond = 10000;
    private static final double millisecondsPerTick = 1.0 / ticksPerMillisecond;
    public static final long ticksPerSecond = ticksPerMillisecond * 1000;
    private static final double secondsPerTick = 1.0 / ticksPerSecond;;
    public static final long ticksPerMinute = ticksPerSecond * 60;
    private static final double minutesPerTick = 1.0 / ticksPerMinute;
    public static final long ticksPerHour = ticksPerMinute * 60;
    private static final double hoursPerTick = 1.0 / ticksPerHour;

    public static final long ticksPerDay = ticksPerHour * 24;
    private static final double daysPerTick = 1.0 / ticksPerDay;
    private static final int millisPerSecond = 1000;
    private static final int millisPerMinute = 60000;
    private static final int millisPerHour = 3600000;
    private static final int millisPerDay = 86400000;
    private static final long minSeconds = Long.MIN_VALUE / ticksPerSecond;
    private static final long maxSeconds = Long.MAX_VALUE / ticksPerSecond;
    private static final long minMilliSeconds =
            Long.MIN_VALUE / ticksPerMillisecond;
    private static final long maxMilliSeconds =
            Long.MAX_VALUE / ticksPerMillisecond;

    private static final String TRAILING_ZEROS = "0000000";

    public static final TimeSpan zero = new TimeSpan(0);
    public static final TimeSpan minValue = new TimeSpan(Long.MIN_VALUE);
    public static final TimeSpan maxValue = new TimeSpan(Long.MAX_VALUE);

    private long ticks;

    public TimeSpan(long ticks) {
        this.ticks = ticks;
    }

    public TimeSpan(int hours, int minutes, int seconds) {
        long num = (long)hours * 3600 + (long)minutes * 60 + (long)seconds;
        if ((num > maxSeconds) || (num < minSeconds))
            throw new ArithmeticException("overflow");
        this.ticks = num * ticksPerSecond;

    }

    public TimeSpan(int days, int hours, int minutes, int seconds) {
        this(days, hours, minutes, seconds, 0);
    }

    public TimeSpan(int days, int hours, int minutes, int seconds,
            int milliseconds) {
        long num = ((long)days * 3600 * 24 + (long)hours * 3600 +
                (long)minutes * 60 + seconds) * 1000 + milliseconds;
        if ((num > maxMilliSeconds) || (num < minMilliSeconds))
            throw new ArithmeticException("overflow");
          this.ticks = num * ticksPerMillisecond;
    }

    public long getTicks() {
        return this.ticks;
    }

    public int getDays() {
        return (int)(this.ticks / ((long)ticksPerDay));
    }

    public int getHours() {
        return (int)((this.ticks / ((long)ticksPerHour)) % 24);
}

    public int getMilliseconds() {
        return (int)((this.ticks / ((long)ticksPerMillisecond)) % 1000);
    }

    public int getMinutes() {
        return (int)((this.ticks / ((long)ticksPerMinute)) % 60);
}

    public int getSeconds() {
        return (int)((this.ticks / ((long)ticksPerSecond)) % 60);
}

    public double getTotalDays() {
        return this.ticks * daysPerTick;
    }

    public double getTotalHours() {
        return this.ticks * hoursPerTick;
    }

    public double getTotalMinutes() {
            return this.ticks * minutesPerTick;
    }

    public double getTotalSeconds() {
        return this.ticks * secondsPerTick;
    }

    public double getTotalMilliseconds() {
        double num = this.ticks * millisecondsPerTick;
        if (num > maxMilliSeconds)
            return (double)maxMilliSeconds;
        if (num < minMilliSeconds)
            return (double)minMilliSeconds;
        return num;
    }

    public void sleep() throws InterruptedException {
        long tm = (long) this.getTotalMilliseconds();
        if (tm < 0 || tm > Long.MAX_VALUE)
            throw new IllegalArgumentException("sleep value of of range");
        Thread.sleep(tm);
    }

    public void sleep(CancellationToken ct) {
        CancellationTokenSource linkedTokenSource =
                CancellationTokenSource.createLinkedTokenSource(
                        ct, CancellationTokenSource.getTokenNone()
                );
        linkedTokenSource.getToken().registerInterrupt();
        try {
            if (ct.isCancellationRequested())
                throw new OperationCanceledException();
            this.sleep();
        } catch (InterruptedException ex) {
            if (ct.isCancellationRequested())
                throw new OperationCanceledException();
            throw new IllegalStateException();
        }
        finally {
            linkedTokenSource.dispose();
        }
    }

    public TimeSpan add(TimeSpan ts) {
        long ticks = this.ticks + ts.ticks;
        if (((this.ticks >> 0x3f) == (ts.ticks >> 0x3f)) &&
                ((this.ticks >> 0x3f) != (ticks >> 0x3f)))
            throw new ArithmeticException("overflow");
        return new TimeSpan(ticks);
    }

    public TimeSpan subtract(TimeSpan ts) {
        long ticks = this.ticks - ts.ticks;
        if (((this.ticks >> 0x3f) != (ts.ticks >> 0x3f)) &&
                ((this.ticks >> 0x3f) != (ticks >> 0x3f)))
            throw new ArithmeticException("overflow");
        return new TimeSpan(ticks);
    }

    public TimeSpan negate() {
        if (this.ticks == minValue.ticks)
            throw new ArithmeticException("overflow");
        return new TimeSpan(-this.ticks);
    }

    public static int compare(TimeSpan t1, TimeSpan t2) {
        if (t1.ticks > t2.ticks)
            return 1;
        if (t1.ticks < t2.ticks)
            return -1;
        return 0;
    }

    @Override
    public int compareTo(Object value) {
        if (value == null)
            return 1;
        if (!(value instanceof TimeSpan))
            throw new IllegalArgumentException("Argument must be TimeSpan.");
        return compare(this, (TimeSpan) value);
    }

    @Override
    public boolean equals(Object value) {
        if (value instanceof TimeSpan)
            return equals(this, (TimeSpan) value);
        return false;
    }

    public static boolean equals(TimeSpan t1, TimeSpan t2) {
        return (t1.ticks == t2.ticks);
    }

    @Override
    public int hashCode() {
        return (((int) this.ticks) ^ ((int) (this.ticks >> 0x20)));
    }

    public static TimeSpan fromDays(double value) {
        return interval(value, millisPerDay);
    }

    public static TimeSpan fromHours(double value) {
        return interval(value, millisPerHour);
    }

    public static TimeSpan fromMinutes(double value) {
        return interval(value, millisPerMinute);
    }

    public static TimeSpan fromSeconds(double value) {
        return interval(value, millisPerSecond);
    }

    public static TimeSpan fromMilliseconds(double value) {
        return interval(value, 1);
    }

    public static TimeSpan fromTicks(long value) {
        return new TimeSpan(value);
    }

    public TimeSpan duration() {
        if (this.ticks == minValue.ticks)
            throw new ArithmeticException("overflow");
        return new TimeSpan((this.ticks >= 0) ? this.ticks : -this.ticks);
    }

    private static TimeSpan interval(double value, int scale) {
        if (Double.isNaN(value))
            throw new IllegalArgumentException("Argument must be a number.");
        double num = value * scale;
        if ((num > maxMilliSeconds) || (num < minMilliSeconds))
            throw new ArithmeticException("overflow");
        return new TimeSpan((long)((num) * ticksPerMillisecond));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int num = (int)(this.ticks / ticksPerDay);
        long num2 = this.ticks % ticksPerDay;
        if (this.ticks < 0) {
            sb.append("-");
            num = -num;
            num2 = -num2;
        }
        if (num != 0) {
            sb.append(num);
            sb.append(".");
        }
        sb.append(String.format("%02d:", (int)((num2 / ticksPerHour) % 24)));
        sb.append(String.format("%02d:", (int)((num2 / ticksPerMinute) % 60)));
        sb.append(String.format("%02d", (int)((num2 / ticksPerSecond) % 60)));
        int num3 = (int)(num2 % ticksPerSecond);
        if (num3 != 0)
            sb.append(String.format(".%s%d", TRAILING_ZEROS.substring(
                    Integer.toString(num3).length()), num3));
        return sb.toString();
    }

    public static TimeSpan operatorMinus(TimeSpan t) {
        if (t.ticks == minValue.ticks)
            throw new ArithmeticException("overflow");
	return new TimeSpan(-t.ticks);
    }
    public static TimeSpan operatorMinus(TimeSpan t1, TimeSpan t2) {
        return t1.subtract(t2);
    }

    public static TimeSpan operatorPlus(TimeSpan t) {
        return t;
    }

    public static TimeSpan operatorPlus(TimeSpan t1, TimeSpan t2) {
        return t1.add(t2);
    }

    public static boolean operatorEqual(TimeSpan t1, TimeSpan t2) {
        return (t1.ticks == t2.ticks);
    }

    public static boolean operatorNotEqual(TimeSpan t1, TimeSpan t2) {
        return (t1.ticks != t2.ticks);
    }

    public static boolean operatorLess(TimeSpan t1, TimeSpan t2) {
        return (t1.ticks < t2.ticks);
    }

    public static boolean operatorLessEqual(TimeSpan t1, TimeSpan t2) {
        return (t1.ticks <= t2.ticks);
    }

    public static boolean operatorMore(TimeSpan t1, TimeSpan t2) {
        return (t1.ticks > t2.ticks);
    }

    public static boolean operatorMoreEqual(TimeSpan t1, TimeSpan t2) {
        return (t1.ticks >= t2.ticks);
    }
}