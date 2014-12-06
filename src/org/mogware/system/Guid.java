package org.mogware.system;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

public final class Guid implements Comparable<Guid> {
    private final long mostSigBits;
    private final long leastSigBits;

    private static volatile SecureRandom numberGenerator = null;

    private Guid(byte[] data) {
        long msb = 0;
        long lsb = 0;
        assert data.length == 16;
        for (int i=0; i<8; i++)
            msb = (msb << 8) | (data[i] & 0xff);
        for (int i=8; i<16; i++)
            lsb = (lsb << 8) | (data[i] & 0xff);
        this.mostSigBits = msb;
        this.leastSigBits = lsb;
    }

    public Guid(long mostSigBits, long leastSigBits) {
        this.mostSigBits = mostSigBits;
        this.leastSigBits = leastSigBits;
    }

    public long getLeastSignificantBits() {
        return this.leastSigBits;
    }

    public long getMostSignificantBits() {
        return this.mostSigBits;
    }

    protected boolean equals(Guid other) {
        return this.mostSigBits == other.mostSigBits &&
                this.leastSigBits == other.leastSigBits;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || (this.getClass() != other.getClass()))
            return false;
        return this.equals((Guid) other);
    }

    @Override
    public int hashCode() {
        return (int)((this.mostSigBits >> 32) ^ this.mostSigBits ^
                (this.leastSigBits >> 32) ^ this.leastSigBits);
    }

    @Override
    public int compareTo(Guid guid) {
        return (this.mostSigBits < guid.mostSigBits ? -1 :
            (this.mostSigBits > guid.mostSigBits ? 1 :
                (this.leastSigBits < guid.leastSigBits ? -1 :
                    (this.leastSigBits > guid.leastSigBits ? 1 :
                        0))));
    }

    private static String digits(long val, int digits) {
        long hi = 1L << (digits * 4);
        return Long.toHexString(hi | (val & (hi - 1))).substring(1);
    }

    public String toString() {
        return (digits(mostSigBits >> 32, 8) + "-" +
                digits(mostSigBits >> 16, 4) + "-" +
                digits(mostSigBits, 4) + "-" +
                digits(leastSigBits >> 48, 4) + "-" +
                digits(leastSigBits, 12));
    }

    public byte[] toByteArray() {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(this.mostSigBits);
        bb.putLong(this.leastSigBits);
        return bb.array();
    }

    public static Guid newGuid() {
        SecureRandom ng = numberGenerator;
        if (ng == null)
            numberGenerator = ng = new SecureRandom();

        byte[] randomBytes = new byte[16];
        ng.nextBytes(randomBytes);
        randomBytes[6]  &= 0x0f;  /* clear version        */
        randomBytes[6]  |= 0x40;  /* set to version 4     */
        randomBytes[8]  &= 0x3f;  /* clear variant        */
        randomBytes[8]  |= 0x80;  /* set to IETF variant  */
        return new Guid(randomBytes);
    }

    public static Guid valueOf(byte[] bytes) {
        return new Guid(bytes);
    }
}