/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.quaware.util;

import de.qaware.util.ObjectUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static de.qaware.util.ObjectUtils.isEmpty;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link ObjectUtils}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Sam Brannen
 */
public class ObjectUtilsTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();



	@Test
	public void isEmptyNull() {
		assertTrue(isEmpty(null));
	}

	@Test
	public void isEmptyArray() {
		assertTrue(isEmpty(new Object[0]));
		assertTrue(isEmpty(new Integer[0]));

		assertFalse(isEmpty(new Integer[] { new Integer(42) }));
	}




	@Test
	public void addObjectToArraySunnyDay() {
		String[] array = new String[] {"foo", "bar"};
		String newElement = "baz";
		Object[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertEquals(3, newArray.length);
		assertEquals(newElement, newArray[2]);
	}

	@Test
	public void addObjectToArrayWhenEmpty() {
		String[] array = new String[0];
		String newElement = "foo";
		String[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertEquals(1, newArray.length);
		assertEquals(newElement, newArray[0]);
	}

	@Test
	public void addObjectToSingleNonNullElementArray() {
		String existingElement = "foo";
		String[] array = new String[] {existingElement};
		String newElement = "bar";
		String[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertEquals(2, newArray.length);
		assertEquals(existingElement, newArray[0]);
		assertEquals(newElement, newArray[1]);
	}

	@Test
	public void addObjectToSingleNullElementArray() {
		String[] array = new String[] {null};
		String newElement = "bar";
		String[] newArray = ObjectUtils.addObjectToArray(array, newElement);
		assertEquals(2, newArray.length);
		assertEquals(null, newArray[0]);
		assertEquals(newElement, newArray[1]);
	}

	@Test
	public void addObjectToNullArray() throws Exception {
		String newElement = "foo";
		String[] newArray = ObjectUtils.addObjectToArray(null, newElement);
		assertEquals(1, newArray.length);
		assertEquals(newElement, newArray[0]);
	}

	@Test
	public void addNullObjectToNullArray() throws Exception {
		Object[] newArray = ObjectUtils.addObjectToArray(null, null);
		assertEquals(1, newArray.length);
		assertEquals(null, newArray[0]);
	}

	@Test
	public void nullSafeEqualsWithArrays() throws Exception {
		assertTrue(ObjectUtils.nullSafeEquals(new String[] {"a", "b", "c"}, new String[] {"a", "b", "c"}));
		assertTrue(ObjectUtils.nullSafeEquals(new int[] {1, 2, 3}, new int[] {1, 2, 3}));
	}

	@Test
	public void nullSafeHashCodeWithBooleanArray() {
		int expected = 31 * 7 + Boolean.TRUE.hashCode();
		expected = 31 * expected + Boolean.FALSE.hashCode();

		boolean[] array = {true, false};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithBooleanArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((boolean[]) null));
	}

	@Test
	public void nullSafeHashCodeWithByteArray() {
		int expected = 31 * 7 + 8;
		expected = 31 * expected + 10;

		byte[] array = {8, 10};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithByteArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((byte[]) null));
	}

	@Test
	public void nullSafeHashCodeWithCharArray() {
		int expected = 31 * 7 + 'a';
		expected = 31 * expected + 'E';

		char[] array = {'a', 'E'};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithCharArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((char[]) null));
	}

	@Test
	public void nullSafeHashCodeWithDoubleArray() {
		long bits = Double.doubleToLongBits(8449.65);
		int expected = 31 * 7 + (int) (bits ^ (bits >>> 32));
		bits = Double.doubleToLongBits(9944.923);
		expected = 31 * expected + (int) (bits ^ (bits >>> 32));

		double[] array = {8449.65, 9944.923};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithDoubleArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((double[]) null));
	}

	@Test
	public void nullSafeHashCodeWithFloatArray() {
		int expected = 31 * 7 + Float.floatToIntBits(9.6f);
		expected = 31 * expected + Float.floatToIntBits(7.4f);

		float[] array = {9.6f, 7.4f};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithFloatArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((float[]) null));
	}

	@Test
	public void nullSafeHashCodeWithIntArray() {
		int expected = 31 * 7 + 884;
		expected = 31 * expected + 340;

		int[] array = {884, 340};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithIntArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((int[]) null));
	}

	@Test
	public void nullSafeHashCodeWithLongArray() {
		long lng = 7993l;
		int expected = 31 * 7 + (int) (lng ^ (lng >>> 32));
		lng = 84320l;
		expected = 31 * expected + (int) (lng ^ (lng >>> 32));

		long[] array = {7993l, 84320l};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithLongArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((long[]) null));
	}

	@Test
	public void nullSafeHashCodeWithObject() {
		String str = "Luke";
		assertEquals(str.hashCode(), ObjectUtils.nullSafeHashCode(str));
	}

	@Test
	public void nullSafeHashCodeWithObjectArray() {
		int expected = 31 * 7 + "Leia".hashCode();
		expected = 31 * expected + "Han".hashCode();

		Object[] array = {"Leia", "Han"};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithObjectArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((Object[]) null));
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingBooleanArray() {
		Object array = new boolean[] {true, false};
		int expected = ObjectUtils.nullSafeHashCode((boolean[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingByteArray() {
		Object array = new byte[] {6, 39};
		int expected = ObjectUtils.nullSafeHashCode((byte[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingCharArray() {
		Object array = new char[] {'l', 'M'};
		int expected = ObjectUtils.nullSafeHashCode((char[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingDoubleArray() {
		Object array = new double[] {68930.993, 9022.009};
		int expected = ObjectUtils.nullSafeHashCode((double[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingFloatArray() {
		Object array = new float[] {9.9f, 9.54f};
		int expected = ObjectUtils.nullSafeHashCode((float[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingIntArray() {
		Object array = new int[] {89, 32};
		int expected = ObjectUtils.nullSafeHashCode((int[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingLongArray() {
		Object array = new long[] {4389, 320};
		int expected = ObjectUtils.nullSafeHashCode((long[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingObjectArray() {
		Object array = new Object[] {"Luke", "Anakin"};
		int expected = ObjectUtils.nullSafeHashCode((Object[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectBeingShortArray() {
		Object array = new short[] {5, 3};
		int expected = ObjectUtils.nullSafeHashCode((short[]) array);
		assertEqualHashCodes(expected, array);
	}

	@Test
	public void nullSafeHashCodeWithObjectEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((Object) null));
	}

	@Test
	public void nullSafeHashCodeWithShortArray() {
		int expected = 31 * 7 + 70;
		expected = 31 * expected + 8;

		short[] array = {70, 8};
		int actual = ObjectUtils.nullSafeHashCode(array);

		assertEquals(expected, actual);
	}

	@Test
	public void nullSafeHashCodeWithShortArrayEqualToNull() {
		assertEquals(0, ObjectUtils.nullSafeHashCode((short[]) null));
	}


	private void assertEqualHashCodes(int expected, Object array) {
		int actual = ObjectUtils.nullSafeHashCode(array);
		assertEquals(expected, actual);
		assertTrue(array.hashCode() != actual);
	}


	enum Tropes { FOO, BAR, baz }

}
