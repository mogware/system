package org.mogware.system.dif;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class ByteBuffer extends OutputStream {
    protected int cur = 0;
    protected int size = 0;
    protected byte[] buffer = new byte[512];

    public int getPosition() {
        return this.cur;
    }

    public void setPosition(int pos) {
        this.cur = pos;
    }

    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) {
        this.ensure(len);
        System.arraycopy(b, off, this.buffer, this.cur, len);
        this.cur += len;
        this.size = Math.max(this.cur, this.size);
    }

    @Override
    public void write(int b) {
        this.ensure(1);
        this.buffer[this.cur++] = (byte) (0xFF & b);
        this.size = Math.max(this.cur, this.size);
    }

    public void reset() {
        this.cur = 0;
        this.size = 0;
    }

    String asString(String encoding) throws UnsupportedEncodingException {
        return new String(buffer, 0, size, encoding);
    }

    protected void ensure(int more) {
        final int need = this.cur + more;
        if (need < this.buffer.length)
            return;
        int newSize = this.buffer.length * 2;
        if (newSize <= need)
            newSize = need + 128;
        byte[] n = new byte[newSize];
        System.arraycopy(this.buffer, 0, n, 0, this.size);
        this.buffer = n;
    }
}
