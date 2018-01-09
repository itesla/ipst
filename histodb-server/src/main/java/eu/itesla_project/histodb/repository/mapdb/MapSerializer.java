/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb.repository.mapdb;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.serializer.GroupSerializer;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
public class MapSerializer implements GroupSerializer<Map<String, Object>>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public void serialize(DataOutput2 out, Map<String, Object> map) throws IOException {
        Objects.requireNonNull(out);
        Objects.requireNonNull(map);

        out.packInt(map.size());

        for (String k : map.keySet()) {
            Object o = map.get(k);
            if (o instanceof Double) {
                out.writeUTF("D" + k);
                out.writeDouble((Double) o);
            } else if (o instanceof Long) {
                out.writeUTF("L" + k);
                out.writeLong((Long) o);
            } else if (o instanceof String) {
                out.writeUTF("S" + k);
                out.writeUTF((String) o);
            } else if (o instanceof Integer) {
                out.writeUTF("I" + k);
                out.writeInt((Integer) o);
            } else {
                throw new RuntimeException("Unexpected type " + o.getClass());
            }
        }
    }

    @Override
    public Map<String, Object> deserialize(DataInput2 input, int available) throws IOException {
        Objects.requireNonNull(input);
        Map<String, Object> map = new TreeMap();
        int size = input.unpackInt();
        for (int i = 0; i < size; i++) {

            String k = input.readUTF();

            if (k.startsWith("D")) {
                map.put(k.substring(1), input.readDouble());
            } else if (k.startsWith("L")) {
                map.put(k.substring(1), input.readLong());
            } else if (k.startsWith("S")) {
                map.put(k.substring(1), input.readUTF());
            } else if (k.startsWith("I")) {
                map.put(k.substring(1), input.readInt());
            }
        }
        return map;
    }

    @Override
    public int valueArraySearch(Object keys, Map<String, Object> key) {
        return 0;
    }

    @Override
    public int valueArraySearch(Object keys, Map<String, Object> key, Comparator comparator) {
        return 0;
    }

    @Override
    public void valueArraySerialize(DataOutput2 out, Object vals) throws IOException {
        Objects.requireNonNull(vals);
        if (vals instanceof TreeMap) {
            this.serialize(out, (TreeMap) vals);
        } else {
            Object[] maps = (Object[]) vals;
            for (Object t : maps) {
                this.serialize(out, (Map<String, Object>) t);
            }
        }
    }

    @Override
    public Object valueArrayDeserialize(DataInput2 in, int size) throws IOException {
        Objects.requireNonNull(in);
        List<TreeMap> maplist = new ArrayList();
        for (int i = 0; i < size; i++) {
            maplist.add((TreeMap) deserialize(in, 1));
        }
        return maplist.toArray();
    }

    @Override
    public Map<String, Object> valueArrayGet(Object vals, int pos) {
        Objects.requireNonNull(vals);
        return (Map<String, Object>) ((Object[]) vals)[pos];
    }

    @Override
    public int valueArraySize(Object vals) {
        Objects.requireNonNull(vals);
        return ((Object[]) vals).length;
    }

    @Override
    public Object valueArrayEmpty() {
        return new TreeMap[0];
    }

    @Override
    public Object valueArrayPut(Object vals, int pos, Map<String, Object> newValue) {
        Objects.requireNonNull(vals);
        Objects.requireNonNull(newValue);

        Object[] array = (Object[]) vals;
        final Object[] ret = Arrays.copyOf(array, array.length + 1);
        if (pos < array.length) {
            System.arraycopy(array, pos, ret, pos + 1, array.length - pos);
        }
        ret[pos] = newValue;
        return ret;
    }

    @Override
    public Object valueArrayUpdateVal(Object vals, int pos, Map<String, Object> newValue) {
        Objects.requireNonNull(vals);
        Objects.requireNonNull(newValue);

        Object[] array = (Object[]) vals;
        final Object[] ret = Arrays.copyOf(array, array.length);
        ret[pos] = newValue;
        return ret;
    }

    @Override
    public Object valueArrayFromArray(Object[] objects) {
        Objects.requireNonNull(objects);
        final Object[] ret = Arrays.copyOf(objects, objects.length);
        return ret;
    }

    @Override
    public Object valueArrayCopyOfRange(Object vals, int from, int to) {
        Objects.requireNonNull(vals);
        Object[] array = (Object[]) vals;
        final Object[] ret = Arrays.copyOfRange(array, from, to);
        return ret;
    }

    @Override
    public Object valueArrayDeleteValue(Object vals, int pos) {
        Objects.requireNonNull(vals);
        Object[] array = (Object[]) vals;
        Object[] ret = new Object[array.length - 1];
        System.arraycopy(array, 0, ret, 0, pos - 1);
        System.arraycopy(array, pos, ret, pos - 1, ret.length - (pos - 1));
        return ret;
    }
}
