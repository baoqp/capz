package com.capz.core.http.impl;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.AsciiString;
import io.netty.util.HashingStrategy;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import static io.netty.util.AsciiString.CASE_INSENSITIVE_HASHER;
import static io.netty.util.AsciiString.CASE_SENSITIVE_HASHER;

/**
 * 类似于HashMap,但是在这里相同的key不会覆盖，而是可以有多个value，以链表连接。
 */
public class CapzHttpHeaders extends HttpHeaders {

    private final CapzHttpHeaders.MapEntry[] entries = new CapzHttpHeaders.MapEntry[16];
    private final CapzHttpHeaders.MapEntry head = new CapzHttpHeaders.MapEntry(-1, null, null);

    public CapzHttpHeaders() {
        head.before = head.after = head;
    }

    @Override
    public int size() {
        return names().size();
    }

    private static int index(int hash) {
        return hash & 0x0000000F;
    }

    public boolean contentLengthSet() {
        return contains(com.capz.core.http.HttpHeaders.CONTENT_LENGTH);
    }

    public boolean contentTypeSet() {
        return contains(com.capz.core.http.HttpHeaders.CONTENT_TYPE);
    }

    @Override
    public CapzHttpHeaders add(CharSequence name, Object value) {
        int h = AsciiString.hashCode(name);
        int i = index(h);
        add0(h, i, name, (CharSequence) value);
        return this;
    }

    public CapzHttpHeaders add(final String name, final String strVal) {
        int h = AsciiString.hashCode(name);
        int i = index(h);
        add0(h, i, name, strVal);
        return this;
    }

    @Override
    public CapzHttpHeaders add(String name, Iterable values) {
        int h = AsciiString.hashCode(name);
        int i = index(h);
        for (Object vstr : values) {
            add0(h, i, name, (String) vstr);
        }
        return this;
    }


    private void add0(int h, int i, final CharSequence name, final CharSequence value) {
        // Update the hash table.
        CapzHttpHeaders.MapEntry e = entries[i];
        CapzHttpHeaders.MapEntry newEntry;
        entries[i] = newEntry = new CapzHttpHeaders.MapEntry(h, name, value);
        newEntry.next = e;

        // Update the linked list.
        newEntry.addBefore(head);
    }

    @Override
    public CapzHttpHeaders remove(final String name) {
        Objects.requireNonNull(name, "name");
        int h = AsciiString.hashCode(name);
        int i = index(h);
        remove0(h, i, name);
        return this;
    }

    private void remove0(int h, int i, CharSequence name) {
        CapzHttpHeaders.MapEntry e = entries[i];
        if (e == null) {
            return;
        }

        for (; ; ) {
            if (e.hash == h && AsciiString.contentEqualsIgnoreCase(name, e.key)) {
                e.remove();
                CapzHttpHeaders.MapEntry next = e.next;
                if (next != null) {
                    entries[i] = next;
                    e = next;
                } else {
                    entries[i] = null;
                    return;
                }
            } else {
                break;
            }
        }

        for (; ; ) {
            CapzHttpHeaders.MapEntry next = e.next;
            if (next == null) {
                break;
            }
            if (next.hash == h && AsciiString.contentEqualsIgnoreCase(name, next.key)) {
                e.next = next.next;
                next.remove();
            } else {
                e = next;
            }
        }
    }


    public HttpHeaders set(final String name, final String strVal) {
        return set0(name, strVal);
    }

    private CapzHttpHeaders set0(final CharSequence name, final CharSequence strVal) {
        int h = AsciiString.hashCode(name);
        int i = index(h);
        remove0(h, i, name);
        add0(h, i, name, strVal);
        return this;
    }

    @Override
    public CapzHttpHeaders set(final String name, final Iterable values) {
        Objects.requireNonNull(values, "values");

        int h = AsciiString.hashCode(name);
        int i = index(h);

        remove0(h, i, name);
        for (Object v : values) {
            if (v == null) {
                break;
            }
            add0(h, i, name, (String) v);
        }

        return this;
    }

    @Override
    public CapzHttpHeaders clear() {
        for (int i = 0; i < entries.length; i++) {
            entries[i] = null;
        }
        head.before = head.after = head;
        return this;
    }

    @Override
    public boolean contains(String name, String value, boolean ignoreCase) {
        int h = AsciiString.hashCode(name);
        int i = index(h);
        CapzHttpHeaders.MapEntry e = entries[i];
        HashingStrategy<CharSequence> strategy = ignoreCase ? CASE_INSENSITIVE_HASHER : CASE_SENSITIVE_HASHER;
        while (e != null) {
            if (e.hash == h && AsciiString.contentEqualsIgnoreCase(name, e.key)) {
                if (strategy.equals(value, e.getValue())) {
                    return true;
                }
            }
            e = e.next;
        }
        return false;
    }

    @Override
    public boolean contains(CharSequence name, CharSequence value, boolean ignoreCase) {
        int h = AsciiString.hashCode(name);
        int i = index(h);
        CapzHttpHeaders.MapEntry e = entries[i];
        HashingStrategy<CharSequence> strategy = ignoreCase ? CASE_INSENSITIVE_HASHER : CASE_SENSITIVE_HASHER;
        while (e != null) {
            if (e.hash == h && AsciiString.contentEqualsIgnoreCase(name, e.key)) {
                if (strategy.equals(value, e.getValue())) {
                    return true;
                }
            }
            e = e.next;
        }
        return false;
    }

    @Override
    public String get(final String name) {
        return get((CharSequence) name);
    }

    private CharSequence get0(CharSequence name) {
        int h = AsciiString.hashCode(name);
        int i = index(h);
        CapzHttpHeaders.MapEntry e = entries[i];
        while (e != null) {
            if (e.hash == h && AsciiString.contentEqualsIgnoreCase(name, e.key)) {
                return e.getValue();
            }
            e = e.next;
        }
        return null;
    }

    @Override
    public List<String> getAll(final String name) {
        return getAll((CharSequence) name);
    }

    @Override
    public void forEach(Consumer<? super Map.Entry<String, String>> action) {
        CapzHttpHeaders.MapEntry e = head.after;
        while (e != head) {
            action.accept(new AbstractMap.SimpleEntry<>(e.key.toString(), e.value.toString()));
            e = e.after;
        }
    }

    @Override
    public List<Map.Entry<String, String>> entries() {
        List<Map.Entry<String, String>> all = new ArrayList<>(size());
        CapzHttpHeaders.MapEntry e = head.after;
        while (e != head) {
            final MapEntry f = e;
            all.add(new Map.Entry<String, String>() {
                @Override
                public String getKey() {
                    return f.key.toString();
                }

                @Override
                public String getValue() {
                    return f.value.toString();
                }

                @Override
                public String setValue(String value) {
                    return f.setValue(value).toString();
                }

                @Override
                public String toString() {
                    return getKey() + ": " + getValue();
                }
            });
            e = e.after;
        }
        return all;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return entries().iterator();
    }

    @Override
    public boolean contains(String name) {
        return contains((CharSequence) name);
    }

    @Override
    public boolean isEmpty() {
        return head == head.after;
    }

    @Override
    public Set<String> names() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        CapzHttpHeaders.MapEntry e = head.after;
        while (e != head) {
            names.add(e.getKey().toString());
            e = e.after;
        }
        return names;
    }

    @Override
    public String get(CharSequence name) {
        Objects.requireNonNull(name, "name");
        CharSequence ret = get0(name);
        return ret != null ? ret.toString() : null;
    }

    @Override
    public List<String> getAll(CharSequence name) {
        Objects.requireNonNull(name, "name");

        LinkedList<String> values = new LinkedList<>();

        int h = AsciiString.hashCode(name);
        int i = index(h);
        CapzHttpHeaders.MapEntry e = entries[i];
        while (e != null) {
            if (e.hash == h && AsciiString.contentEqualsIgnoreCase(name, e.key)) {
                values.addFirst(e.getValue().toString());
            }
            e = e.next;
        }
        return values;
    }

    @Override
    public boolean contains(CharSequence name) {
        return get0(name) != null;
    }


    @Override
    public CapzHttpHeaders add(CharSequence name, Iterable values) {
        String n = name.toString();
        for (Object seq : values) {
            add(n, seq.toString());
        }
        return this;
    }


    @Override
    public CapzHttpHeaders set(CharSequence name, Iterable values) {
        remove(name);
        String n = name.toString();
        for (Object seq : values) {
            add(n, seq.toString());
        }
        return this;
    }

    @Override
    public CapzHttpHeaders remove(CharSequence name) {
        return remove(name.toString());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : this) {
            sb.append(entry).append('\n');
        }
        return sb.toString();
    }

    @Override
    public Integer getInt(CharSequence name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getInt(CharSequence name, int defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Short getShort(CharSequence name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public short getShort(CharSequence name, short defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long getTimeMillis(CharSequence name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getTimeMillis(CharSequence name, long defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Map.Entry<CharSequence, CharSequence>> iteratorCharSequence() {
        return new Iterator<Map.Entry<CharSequence, CharSequence>>() {
            CapzHttpHeaders.MapEntry current = head.after;

            @Override
            public boolean hasNext() {
                return current != head;
            }

            @Override
            public Map.Entry<CharSequence, CharSequence> next() {
                Map.Entry<CharSequence, CharSequence> next = current;
                current = current.after;
                return next;
            }
        };
    }

    @Override
    public HttpHeaders add(String name, Object value) {
        return add((CharSequence) name, (CharSequence) value);
    }

    @Override
    public HttpHeaders addInt(CharSequence name, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders addShort(CharSequence name, short value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders set(String name, Object value) {
        return set0(name, (CharSequence) value);
    }

    @Override
    public HttpHeaders setInt(CharSequence name, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders setShort(CharSequence name, short value) {
        throw new UnsupportedOperationException();
    }

    private static final class MapEntry implements Map.Entry<CharSequence, CharSequence> {
        final int hash;
        final CharSequence key;
        CharSequence value;
        CapzHttpHeaders.MapEntry next;
        CapzHttpHeaders.MapEntry before, after;

        MapEntry(int hash, CharSequence key, CharSequence value) {
            this.hash = hash;
            this.key = key;
            this.value = value;
        }

        void remove() {
            before.after = after;
            after.before = before;
        }

        void addBefore(CapzHttpHeaders.MapEntry e) {
            after = e;
            before = e.before;
            before.after = this;
            after.before = this;
        }

        @Override
        public CharSequence getKey() {
            return key;
        }

        @Override
        public CharSequence getValue() {
            return value;
        }

        @Override
        public CharSequence setValue(CharSequence value) {
            Objects.requireNonNull(value, "value");
            CharSequence oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public String toString() {
            return getKey() + ": " + getValue();
        }
    }
}
