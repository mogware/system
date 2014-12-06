package org.mogware.system.dif;

public class TestObject {
    private final boolean first;
    private final long second;
    private final double third;

    TestObject(boolean first, long second, double third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 37 * result + (this.first ? 1 : 0);
        result = 37 * result + (int) this.second;
        result = 37 * result + (int)(this.second ^(this.second >> 32));
        long third = Double.doubleToLongBits(this.third);
        result = 37 * result + (int)(third ^(third >> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final TestObject other = (TestObject) obj;
        return other.first == this.first &&
                other.second == this.second &&
                other.third == this.third;
    }
}
