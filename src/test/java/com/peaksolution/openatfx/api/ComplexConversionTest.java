package com.peaksolution.openatfx.api;
import com.peaksolution.datamodel.Complex;
import com.peaksolution.datamodel.DoubleComplex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class ComplexConversionTest {

    private static List<Float> toScalarList(Complex[] values) {
        List<Float> result = new ArrayList<>();
        for (Complex value : values) {
            result.add(value.getR());
            result.add(value.getI());
        }
        return result;
    }

    private static List<Double> toScalarList(DoubleComplex[] values) {
        List<Double> result = new ArrayList<>();
        for (DoubleComplex value : values) {
            result.add(value.getR());
            result.add(value.getI());
        }
        return result;
    }

    @Test
    void testGetComplexValuesRealImaginaryOrderAndLength() {
        Complex[] complex = Complex.getComplexValues(
                Arrays.asList(1.1f, 0.1f, -2.2f, 3.3f, Float.MIN_VALUE, Float.MAX_VALUE));

        assertEquals(3, complex.length);
        assertEquals(1.1f, complex[0].getR());
        assertEquals(0.1f, complex[0].getI());
        assertEquals(-2.2f, complex[1].getR());
        assertEquals(3.3f, complex[1].getI());
        assertEquals(Float.MIN_VALUE, complex[2].getR());
        assertEquals(Float.MAX_VALUE, complex[2].getI());
    }

    @Test
    void testGetComplexValuesOddLengthIgnoresTrailingScalar() {
        Complex[] complex = Complex.getComplexValues(Arrays.asList(10f, 20f, 99f));

        assertEquals(1, complex.length);
        assertEquals(10f, complex[0].getR());
        assertEquals(20f, complex[0].getI());
    }

    @Test
    void testGetComplexValuesEmptyInput() {
        Complex[] complex = Complex.getComplexValues(Collections.emptyList());

        assertEquals(0, complex.length);
    }

    @Test
    void testComplexRoundtripPreservesOrderAndValues() {
        Complex[] original = new Complex[] {
                new Complex(1.25f, -2.5f),
                new Complex(0f, 3f),
                new Complex(Float.MIN_VALUE, Float.MAX_VALUE)
        };

        Complex[] roundtrip = Complex.getComplexValues(toScalarList(original));

        assertEquals(original.length, roundtrip.length);
        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i], roundtrip[i]);
        }
    }

    @Test
    void testGetDoubleComplexValuesRealImaginaryOrderAndLength() {
        DoubleComplex[] complex = DoubleComplex
                .getDoubleComplexValues(Arrays.asList(1.11, 0.11, -2.22, 3.33, Double.MIN_VALUE, Double.MAX_VALUE));

        assertEquals(3, complex.length);
        assertEquals(1.11, complex[0].getR());
        assertEquals(0.11, complex[0].getI());
        assertEquals(-2.22, complex[1].getR());
        assertEquals(3.33, complex[1].getI());
        assertEquals(Double.MIN_VALUE, complex[2].getR());
        assertEquals(Double.MAX_VALUE, complex[2].getI());
    }

    @Test
    void testGetDoubleComplexValuesOddLengthIgnoresTrailingScalar() {
        DoubleComplex[] complex = DoubleComplex.getDoubleComplexValues(Arrays.asList(10d, 20d, 99d));

        assertEquals(1, complex.length);
        assertEquals(10d, complex[0].getR());
        assertEquals(20d, complex[0].getI());
    }

    @Test
    void testGetDoubleComplexValuesEmptyInput() {
        DoubleComplex[] complex = DoubleComplex.getDoubleComplexValues(Collections.emptyList());

        assertEquals(0, complex.length);
    }

    @Test
    void testDoubleComplexRoundtripPreservesOrderAndValues() {
        DoubleComplex[] original = new DoubleComplex[] {
                new DoubleComplex(1.125, -2.25),
                new DoubleComplex(0d, 3d),
                new DoubleComplex(Double.MIN_VALUE, Double.MAX_VALUE)
        };

        DoubleComplex[] roundtrip = DoubleComplex.getDoubleComplexValues(toScalarList(original));

        assertEquals(original.length, roundtrip.length);
        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i], roundtrip[i]);
        }
    }
}
