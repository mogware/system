package org.mogware.system.dif;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ClassWriters {
    public interface ClassWriter {
        void write(Writer out, Object o) throws IOException;
        boolean hasPrimitiveForm();
        void writePrimitiveForm(Writer out, Object o) throws IOException;
    }

    static final ThreadLocal<SimpleDateFormat> dateFormat =
            new ThreadLocal<SimpleDateFormat>() {
        public SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        }
    };

    private final List<Object[]> writers  = new ArrayList<>();
    {
        this.addWriter(String.class, new StringWriter());
        this.addWriter(Date.class, new DateWriter());
        this.addWriter(BigInteger.class, new BigIntegerWriter());
        this.addWriter(BigDecimal.class, new BigDecimalWriter());
        this.addWriter(Calendar.class, new CalendarWriter());
        this.addWriter(TimeZone.class, new TimeZoneWriter());
        this.addWriter(Locale.class, new LocaleWriter());
        this.addWriter(java.sql.Timestamp.class, new TimestampWriter());
        this.addWriter(java.sql.Date.class, new DateWriter());
        this.addWriter(URI.class, new UriWriter());
        this.addWriter(Class.class, new JavaClassWriter());
    }

    public void addWriter(Class c, ClassWriter writer)
    {
        for (Object[] item : this.writers) {
            Class clazz = (Class)item[0];
            if (clazz == c) {
                item[1] = writer;
                return;
            }
        }
        this.writers.add(new Object[] {c, writer});
    }

    public ClassWriter getClosestWriter(Class componentClass) {
        ClassWriter closestWriter = null;
        int minDistance = Integer.MAX_VALUE;

        for (Object[] item : this.writers) {
            Class clazz = (Class)item[0];
            if (clazz == componentClass) {
                closestWriter = (ClassWriter) item[1];
                break;
            }
            int distance = ClassWriters.getDistance(clazz, componentClass);
            if (distance < minDistance) {
                minDistance = distance;
                closestWriter = (ClassWriter) item[1];
            }
        }

        return closestWriter;
    }

    private static int getDistance(Class curr, Class to) {
        int distance = 0;
        while (curr != to) {
            distance++;
            if ((curr = curr.getSuperclass()) == null)
                return Integer.MAX_VALUE;
        }
        return distance;
    }

    protected static class StringWriter implements ClassWriter {
        @Override
        public void write(Writer out, Object obj) throws IOException {
            out.propertyName("value");
            out.value((String) obj);
        }

        @Override
        public boolean hasPrimitiveForm() { return true; }

        @Override
        public void writePrimitiveForm(Writer out, Object obj)
                throws IOException {
            out.value((String) obj);
        }
    }

    public static class DateWriter implements ClassWriter {
        @Override
        public void write(Writer out, Object obj) throws IOException {
            out.propertyName("value");
            out.value(((Date) obj).getTime());
        }

        @Override
        public boolean hasPrimitiveForm() { return true; }

        @Override
        public void writePrimitiveForm(Writer out, Object obj)
                throws IOException {
            out.value(((Date) obj).getTime());
        }
    }

    public static class BigIntegerWriter implements ClassWriter {
        @Override
        public void write(Writer out, Object obj) throws IOException {
            out.propertyName("value");
            out.value(((BigInteger) obj).toString(10));
        }

        @Override
        public boolean hasPrimitiveForm() { return true; }

        @Override
        public void writePrimitiveForm(Writer out, Object obj)
                throws IOException {
            out.value(((BigInteger) obj).toString(10));
        }
    }

    public static class BigDecimalWriter implements ClassWriter {
        @Override
        public void write(Writer out, Object obj) throws IOException {
            out.propertyName("value");
            out.value(((BigDecimal) obj).toPlainString());
        }

        @Override
        public boolean hasPrimitiveForm() { return true; }

        @Override
        public void writePrimitiveForm(Writer out, Object obj)
                throws IOException {
            out.value(((BigDecimal) obj).toPlainString());
        }
    }

    public static class TimestampWriter implements ClassWriter {
        @Override
        public void write(Writer out, Object obj) throws IOException {
            out.propertyName("time");
            out.value((((Timestamp) obj).getTime() / 1000) * 1000);
            out.propertyName("nanos");
            out.value(((Timestamp) obj).getNanos());
        }

        @Override
        public boolean hasPrimitiveForm() { return false; }

        @Override
        public void writePrimitiveForm(Writer out, Object obj)
                throws IOException { }
    }

    public static class CalendarWriter implements ClassWriter {
        @Override
        public void write(Writer out, Object obj) throws IOException {
            Calendar cal = (Calendar) obj;
            dateFormat.get().setTimeZone(cal.getTimeZone());
            out.propertyName("time");
            out.value(dateFormat.get().format(cal.getTime()));
            out.propertyName("zone");
            out.value(cal.getTimeZone().getID());
        }

        @Override
        public boolean hasPrimitiveForm() { return false; }

        @Override
        public void writePrimitiveForm(Writer out, Object obj)
                throws IOException { }
    }

    public static class TimeZoneWriter implements ClassWriter {
        @Override
        public void write(Writer out, Object obj) throws IOException {
            out.propertyName("zone");
            out.value(((TimeZone) obj).getID());
        }

        @Override
        public boolean hasPrimitiveForm() { return false; }

        @Override
        public void writePrimitiveForm(Writer out, Object obj)
                throws IOException { }
    }

    public static class LocaleWriter implements ClassWriter {
        @Override
        public void write(Writer out, Object obj) throws IOException {
            out.propertyName("language");
            out.value(((Locale) obj).getLanguage());
            out.propertyName("country");
            out.value(((Locale) obj).getCountry());
            out.propertyName("variant");
            out.value(((Locale) obj).getVariant());
        }

        @Override
        public boolean hasPrimitiveForm() { return false; }

        @Override
        public void writePrimitiveForm(Writer out, Object obj)
                throws IOException { }
    }

    public static class UriWriter implements ClassWriter {
        @Override
        public void write(Writer out, Object obj) throws IOException {
            out.propertyName("value");
            out.value(((URI) obj).toString());
        }

        public boolean hasPrimitiveForm() { return true; }

        public void writePrimitiveForm(Writer out, Object obj)
                throws IOException {
            out.value(((URI) obj).toString());
        }
    }

    public static class JavaClassWriter implements ClassWriter {
        @Override
        public void write(Writer out, Object obj) throws IOException {
            out.propertyName("value");
            out.value(((Class) obj).getName());
        }

        public boolean hasPrimitiveForm() { return true; }

        public void writePrimitiveForm(Writer out, Object obj)
                throws IOException {
            out.value(((Class) obj).getName());
        }
    }
}
