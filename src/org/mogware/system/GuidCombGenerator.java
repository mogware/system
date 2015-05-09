package org.mogware.system;

import java.util.Date;

public final class GuidCombGenerator {
    private GuidCombGenerator() {
    }
    
    public static Guid generate() {
        Guid guid = Guid.newGuid();
        long oldLsb = guid.getLeastSignificantBits();
        byte[] buffer = new byte[8];
        for (int i = 0; i < 2; i++)
            buffer[i] = (byte)(oldLsb >>> 8 * (7 - i));
        long date = new Date().getTime();
        for (int i = 2; i < 8; i++)
            buffer[i] = (byte)(date >>> 8 * (7 - i));
        long newLsb = 0L;
        for (int i = 0; i < 8; i++)        
            newLsb = (newLsb << 8) | (buffer[i] & 0xFF);        
        return new Guid(guid.getMostSignificantBits(), newLsb);
    }
}
