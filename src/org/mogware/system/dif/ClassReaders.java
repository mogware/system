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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ClassReaders {
    public interface ClassReader {
        Object read(Object obj, LinkedList<Target> stack)
                throws IOException;
    }

    static final ThreadLocal<SimpleDateFormat> dateFormat =
            new ThreadLocal<SimpleDateFormat>() {
        public SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        }
    };

    private static final Primitives prims = new Primitives();

    private final List<Object[]> readers  = new ArrayList<>();
    {
        this.addReader(String.class, new StringReader());
        this.addReader(Date.class, new DateReader());
        this.addReader(BigInteger.class, new BigIntegerReader());
        this.addReader(BigDecimal.class, new BigDecimalReader());
        this.addReader(Calendar.class, new CalendarReader());
        this.addReader(TimeZone.class, new TimeZoneReader());
        this.addReader(Locale.class, new LocaleReader());
        this.addReader(java.sql.Timestamp.class, new TimestampReader());
        this.addReader(java.sql.Date.class, new SqlDateReader());
        this.addReader(URI.class, new UriReader());
        this.addReader(Class.class, new JavaClassReader());
    }

    public void addReader(Class c, ClassReader reader)
    {
        for (Object[] item : this.readers) {
            Class clazz = (Class)item[0];
            if (clazz == c) {
                item[1] = reader;
                return;
            }
        }
        this.readers.add(new Object[] {c, reader});
    }

    public ClassReader getClosestReader(Class componentClass) {
        ClassReader closestReader = null;
        int minDistance = Integer.MAX_VALUE;

        for (Object[] item : this.readers) {
            Class clazz = (Class)item[0];
            if (clazz == componentClass) {
                closestReader = (ClassReader) item[1];
                break;
            }
            int distance = ClassReaders.getDistance(clazz, componentClass);
            if (distance < minDistance) {
                minDistance = distance;
                closestReader = (ClassReader) item[1];
            }
        }

        return closestReader;
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

    public static class StringReader implements ClassReader {
        @Override
        public Object read(Object obj, LinkedList<Target> stack)
                throws IOException {
            if (obj instanceof String)
                return obj;
            if (ClassReaders.prims.isPrimitive(obj.getClass()))
                return obj.toString();
            if (obj instanceof Target && ((Target) obj).isObjectType())
                return ((ObjectType) obj).get("value");
            throw new IOException("unable to parse string: " + obj);
        }
    }

    public static class DateReader implements ClassReader {
        @Override
        public Object read(Object obj, LinkedList<Target> stack)
                throws IOException {
            if (obj instanceof Long)
                return new Date((Long) obj);
            if (obj instanceof Target && ((Target) obj).isObjectType()) {
                Object val = ((ObjectType) obj).get("value");
                if (val instanceof Long)
                    return new Date((Long) val);
                throw new IOException("unable to parse date: " + obj);
            }
            throw new IOException("unable to parse date: " + obj);
        }
    }

    public static class SqlDateReader extends DateReader {
        @Override
        public Object read(Object obj, LinkedList<Target> stack)
                throws IOException {
            return new java.sql.Date(((Date) super.read(obj, stack)).getTime());
        }
    }

    public static class BigIntegerReader implements ClassReader {
        @Override
        public Object read(Object obj, LinkedList<Target> stack)
                throws IOException {
            Object value = obj;
            if (obj instanceof Target && ((Target) obj).isObjectType())
                value = ((ObjectType) obj).get("value");
            return bigIntegerFrom(value);
        }
    }

    private static BigInteger bigIntegerFrom(Object value) throws IOException {
        if (value == null)
            return null;
        if (value instanceof BigInteger)
            return (BigInteger) value;
        if (value instanceof String) {
            String s = (String) value;
            if ("".equals(s.trim()))
                return null;
            try {
                return new BigInteger(s.trim());
            } catch (Exception ex) {
                throw new IOException("Could not parse '" +
                        value + "' as BigInteger.", ex);
            }
        }
        if (value instanceof BigDecimal)
            return ((BigDecimal) value).toBigInteger();
        if (value instanceof Boolean)
            return new BigInteger(((Boolean) value) ? "1" : "0");
        if (value instanceof Double || value instanceof Float)
            return new BigDecimal(((Number)value).doubleValue()).toBigInteger();
        else if (value instanceof Long || value instanceof Integer ||
                value instanceof Short || value instanceof Byte)
            return new BigInteger(value.toString());
        throw new IOException("Could not convert value: " +
                value.toString() + " to BigInteger.");
    }

    public static class BigDecimalReader implements ClassReader {
        @Override
        public Object read(Object obj, LinkedList<Target> stack)
                throws IOException {
            Object value = obj;
            if (obj instanceof Target && ((Target) obj).isObjectType())
                value = ((ObjectType) obj).get("value");
            return bigDecimalFrom(value);
        }
    }

    private static BigDecimal bigDecimalFrom(Object value) throws IOException {
        if (value == null)
            return null;
        if (value instanceof BigDecimal)
            return (BigDecimal) value;
        if (value instanceof String) {
            String s = (String) value;
            if ("".equals(s.trim()))
                return null;
            try {
                return new BigDecimal(s.trim());
            } catch (Exception ex) {
                throw new IOException("could not parse '" +
                        value + "' as BigDecimal", ex);
            }
        }
        if (value instanceof BigInteger)
            return new BigDecimal((BigInteger) value);
        if (value instanceof Boolean)
            return new BigDecimal(((Boolean) value) ? "1" : "0");
        if (value instanceof Double || value instanceof Float)
            return new BigDecimal(((Number)value).doubleValue());
        else if (value instanceof Long || value instanceof Integer ||
                value instanceof Short || value instanceof Byte)
            return new BigDecimal(value.toString());
        throw new IOException("could not convert value: " +
                value.toString() + " to BigDecimal");
    }


    public static class CalendarReader implements ClassReader {
        @Override
        public Object read(Object obj, LinkedList<Target> stack)
                throws IOException {
            String time = null;
            if (obj instanceof Target && ((Target) obj).isObjectType()) {
                try {
                    time = (String)((ObjectType) obj).get("time");
                    if (time == null)
                        throw new IOException("calendar missing 'time' field");
                    Date date = dateFormat.get().parse(time);
                    Class clazz;
                    if (((Target) obj).getTarget() != null)
                        clazz = ((Target) obj).getTarget().getClass();
                    else
                        clazz = Decoder.classForName(
                                ((ObjectType) obj).getType()
                        );
                    Calendar calendar = (Calendar) Decoder.newInstance(clazz);
                    calendar.setTime(date);
                    ((Target) obj).setTarget(calendar);
                    String zone = (String)((ObjectType) obj).get("zone");
                    if (zone != null)
                        calendar.setTimeZone(TimeZone.getTimeZone(zone));
                    return calendar;
                } catch (Exception ex) {
                    throw new IOException("failed to parse time: " + time);
                }
            }
            throw new IOException("unable to parse calendar: " + obj);
        }
    }

    public static class TimeZoneReader implements ClassReader {
        @Override
        public Object read(Object obj, LinkedList<Target> stack)
                throws IOException {
            if (obj instanceof Target && ((Target) obj).isObjectType()) {
                String zone = (String)((ObjectType) obj).get("zone");
                if (zone == null)
                    throw new IOException("timezone missing 'zone' field");
                TimeZone timezone = TimeZone.getTimeZone((String) zone);
                ((Target) obj).setTarget(timezone);
                return timezone;
            }
            throw new IOException("unable to parse timezone: " + obj);
        }
    }

    public static class LocaleReader implements ClassReader {
        @Override
        public Object read(Object obj, LinkedList<Target> stack)
                throws IOException {
            if (obj instanceof Target && ((Target) obj).isObjectType()) {
                String language = (String)((ObjectType) obj).get("language");
                if (language == null)
                    throw new IOException("locale missing 'language' field");
                String country = (String)((ObjectType) obj).get("country");
                String variant = (String)((ObjectType) obj).get("variant");
                Locale locale;
                if (country == null)
                    locale = new Locale((String) language);
                else if (variant == null)
                    locale = new Locale((String) language, (String) country);
                else
                    locale = new Locale((String) language,
                            (String) country, (String) variant);
                ((Target) obj).setTarget(locale);
                return locale;
            }
            throw new IOException("unable to parse locale: " + obj);
        }
    }

    public static class TimestampReader implements ClassReader {
        @Override
        public Object read(Object obj, LinkedList<Target> stack)
                throws IOException {
            if (obj instanceof Target && ((Target) obj).isObjectType()) {
                Long time = (Long)((ObjectType) obj).get("time");
                if (time == null)
                    throw new IOException("timestamp missing 'time' field");
                Timestamp timestamp = new Timestamp(time);
                Integer nanos = (Integer)((ObjectType) obj).get("nanos");
                if (nanos == null)
                    timestamp.setNanos(Integer.valueOf(nanos));
                ((Target) obj).setTarget(timestamp);
                return timestamp;
            }
            throw new IOException("unable to parse timestamp: " + obj);

        }
    }

    public static class UriReader implements ClassReader {
        @Override
        public Object read(Object obj, LinkedList<Target> stack)
                throws IOException {
            if (obj instanceof String)
                return URI.create((String)obj);
            if (obj instanceof Target && ((Target) obj).isObjectType())
                return URI.create((String)((ObjectType) obj).get("value"));
            throw new IOException("unable to parse URI: " + obj);
        }
    }

    public static class JavaClassReader implements ClassReader {
        @Override
        public Object read(Object obj, LinkedList<Target> stack)
                throws IOException {
            if (obj instanceof String)
                return Decoder.classForName((String)obj);
            if (obj instanceof Target && ((Target) obj).isObjectType())
                return Decoder.classForName(
                        (String)((ObjectType) obj).get("value")
                );
            throw new IOException("unable to parse class: " + obj);
        }
    }
}
