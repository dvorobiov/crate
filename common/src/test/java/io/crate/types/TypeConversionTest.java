/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.types;

import io.crate.common.collections.Lists2;
import io.crate.test.integration.CrateUnitTest;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.LongAdder;

import static org.hamcrest.core.Is.is;

public class TypeConversionTest extends CrateUnitTest {

    private static class Repeater<T> implements Iterable<T>, Iterator<T> {

        private final LongAdder repeated;
        private final Callable<T> repeatMe;

        public Repeater(Callable<T> repeatMe, long times) {
            this.repeated = new LongAdder();
            this.repeated.add(times);
            this.repeatMe = repeatMe;
        }

        @Override
        public Iterator<T> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return repeated.longValue() > 0;
        }

        @Override
        public T next() {
            repeated.decrement();
            try {
                return repeatMe.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void remove() {
            // ignore
        }
    }

    private Iterable<Byte> bytes(int num) {
        return new Repeater<>(new Callable<Byte>() {
            @Override
            public Byte call() throws Exception {
                return randomByte();
            }
        }, num);
    }

    private Iterable<Integer> integers(final int lower, final int upper, int num) {
        return new Repeater<>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return randomIntBetween(lower, upper);
            }
        }, num);
    }

    @Test
    public void numberConversionTest() throws Exception {

        for (Byte byteVal : bytes(10)) {
            for (DataType t : DataTypes.ALLOWED_CONVERSIONS.get(DataTypes.BYTE.id())) {
                if (t.equals(DataTypes.IP)) {
                    byteVal = (byte) Math.abs(byteVal == Byte.MIN_VALUE ? byteVal >> 1 : byteVal);
                }
                t.value(byteVal);
            }
        }

        for (Integer shortVal : integers((int) Byte.MIN_VALUE, (int) Byte.MAX_VALUE, 10)) {
            for (DataType t : DataTypes.ALLOWED_CONVERSIONS.get(DataTypes.SHORT.id())) {
                shortVal = t.equals(DataTypes.IP) ? Math.abs(shortVal) : shortVal;
                t.value(shortVal.shortValue());
            }
        }

        for (Integer intValue : integers((int) Byte.MIN_VALUE, (int) Byte.MAX_VALUE, 10)) {
            for (DataType t : DataTypes.ALLOWED_CONVERSIONS.get(DataTypes.INTEGER.id())) {
                intValue = t.equals(DataTypes.IP) ? Math.abs(intValue) : intValue;
                t.value(intValue);
            }
        }

        for (Integer longValue : integers((int) Byte.MIN_VALUE, (int) Byte.MAX_VALUE, 10)) {
            for (DataType t : DataTypes.ALLOWED_CONVERSIONS.get(DataTypes.LONG.id())) {
                longValue = t.equals(DataTypes.IP) ? Math.abs(longValue) : longValue;
                t.value(longValue.longValue());
            }
        }

        for (Integer floatValue : integers((int) Byte.MIN_VALUE, (int) Byte.MAX_VALUE, 10)) {
            for (DataType t : DataTypes.ALLOWED_CONVERSIONS.get(DataTypes.FLOAT.id())) {
                floatValue = t.equals(DataTypes.IP) ? Math.abs(floatValue) : floatValue;
                t.value(floatValue.floatValue());
            }
        }

        for (Integer doubleValue : integers((int) Byte.MIN_VALUE, (int) Byte.MAX_VALUE, 10)) {
            for (DataType t : DataTypes.ALLOWED_CONVERSIONS.get(DataTypes.DOUBLE.id())) {
                doubleValue = t.equals(DataTypes.IP) ? Math.abs(doubleValue) : doubleValue;
                t.value(doubleValue.doubleValue());
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testByteOutOfRangeNegative() throws Exception {
        DataTypes.BYTE.value(-129);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testByteOutOfRangePositive() throws Exception {
        DataTypes.BYTE.value(129);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShortOutOfRangePositive() throws Exception {
        DataTypes.SHORT.value(Integer.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShortOutOfRangeNegative() throws Exception {
        DataTypes.SHORT.value(Integer.MIN_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIntOutOfRangePositive() throws Exception {
        DataTypes.INTEGER.value(Long.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIntOutOfRangeNegative() throws Exception {
        DataTypes.INTEGER.value(Long.MIN_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFloatOutOfRangePositive() throws Exception {
        DataTypes.FLOAT.value(Double.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFloatOutOfRangeNegative() throws Exception {
        DataTypes.FLOAT.value(-Double.MAX_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIpNegativeValue() throws Exception {
        DataTypes.IP.value(Long.MIN_VALUE);
    }

    @Test
    public void selfConversionTest() throws Exception {
        for (DataType<?> type : Lists2.concat(
            DataTypes.PRIMITIVE_TYPES,
            List.of(DataTypes.UNDEFINED, DataTypes.GEO_POINT, DataTypes.GEO_SHAPE, ObjectType.untyped()))) {
            assertThat(
                "type '" + type + "' is not self convertible",
                type.isConvertableTo(type), is(true));

            ArrayType<?> arrayType = new ArrayType<>(type);
            assertThat(
                "type '" +  arrayType + "' is not self convertible",
                arrayType.isConvertableTo(arrayType), is(true));
        }
    }

    @Test
    public void testNotSupportedConversion() throws Exception {
        for (DataType type : com.google.common.collect.Iterables.concat(
            DataTypes.PRIMITIVE_TYPES,
            Arrays.asList(DataTypes.GEO_POINT, DataTypes.GEO_SHAPE, ObjectType.untyped()))) {
            assertFalse(DataTypes.NOT_SUPPORTED.isConvertableTo(type));
        }
    }

    @Test
    public void testToNullConversions() throws Exception {
        for (DataType type : com.google.common.collect.Iterables.concat(
            DataTypes.PRIMITIVE_TYPES,
            Arrays.asList(DataTypes.GEO_POINT, DataTypes.GEO_SHAPE, ObjectType.untyped()))) {
            assertThat(type.isConvertableTo(DataTypes.UNDEFINED), is(false));
        }
        assertThat(DataTypes.UNDEFINED.isConvertableTo(DataTypes.UNDEFINED), is(true));
    }

    @Test
    public void testGeoPointConversion() throws Exception {
        assertThat(DataTypes.GEO_POINT.isConvertableTo(new ArrayType<>(DataTypes.DOUBLE)), is(true));
        assertThat(DataTypes.STRING.isConvertableTo(DataTypes.GEO_POINT), is(true));
    }

    @Test
    public void testGeoShapeConversion() throws Exception {
        DataType objectType = ObjectType.untyped();
        assertThat(DataTypes.STRING.isConvertableTo(DataTypes.GEO_SHAPE), is(true));
        assertThat(objectType.isConvertableTo(DataTypes.GEO_SHAPE), is(true));
    }

    @Test
    public void testTimestampToDoubleConversion() {
        assertThat(TimestampType.INSTANCE_WITH_TZ.isConvertableTo(DoubleType.INSTANCE),
            is(true));
        assertThat(TimestampType.INSTANCE_WITHOUT_TZ.isConvertableTo(DoubleType.INSTANCE),
            is(true));

    }

    @Test
    public void test_object_to_object_conversion_when_either_has_no_inner_types() {
        var objectTypeWithInner = ObjectType.builder().setInnerType("field", DataTypes.STRING).build();
        var objectTypeWithoutInner = ObjectType.untyped();

        assertThat(objectTypeWithInner.isConvertableTo(objectTypeWithoutInner), is(true));
        assertThat(objectTypeWithoutInner.isConvertableTo(objectTypeWithInner), is(true));
    }

    @Test
    public void test_object_to_object_conversion_with_not_convertible_inner_types() {
        var thisObj = ObjectType.builder().setInnerType("field", DataTypes.GEO_POINT).build();
        var thatObj = ObjectType.builder().setInnerType("field", DataTypes.INTEGER).build();

        assertThat(thisObj.isConvertableTo(thatObj), is(false));
    }

    @Test
    public void test_object_to_object_conversion_with_different_inner_fields() {
        var thisObj = ObjectType.builder().setInnerType("field1", DataTypes.INTEGER).build();
        var thatObj = ObjectType.builder().setInnerType("field2", DataTypes.INTEGER).build();

        assertThat(thisObj.isConvertableTo(thatObj), is(false));
    }
}
