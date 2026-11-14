package com.android.calculator2.abacus.solver;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PixelSolverEngineTest {

    @Test
    @DisplayName("Empty or null query returns valid help response")
    void testNullOrEmptyQuery() throws Exception {
        String resNull = PixelSolverEngine.solve(null);
        assertNotNull(resNull);
        JSONObject jsonNull = new JSONObject(resNull);
        assertEquals("IN_SCOPE", jsonNull.getString("scope"));
        assertTrue(jsonNull.has("response"));

        String resEmpty = PixelSolverEngine.solve("   ");
        assertNotNull(resEmpty);
        JSONObject jsonEmpty = new JSONObject(resEmpty);
        assertEquals("IN_SCOPE", jsonEmpty.getString("scope"));
    }

    @Test
    @DisplayName("Help query returns tool explanation")
    void testHelpQuery() throws Exception {
        String res = PixelSolverEngine.solve("help");
        assertNotNull(res);
        JSONObject json = new JSONObject(res);
        assertEquals("IN_SCOPE", json.getString("scope"));
        assertTrue(json.getString("response").toLowerCase().contains("help"));
    }

    @Test
    @DisplayName("Basic arithmetic expression evaluation")
    void testArithmeticEvaluation() throws Exception {
        String res = PixelSolverEngine.solve("25 * 4 + 10");
        assertNotNull(res);
        JSONObject json = new JSONObject(res);
        assertEquals("IN_SCOPE", json.getString("scope"));
        assertTrue(json.has("response"));
    }

    @Test
    @DisplayName("Quadratic equation solver (x^2 - 5x + 6 = 0)")
    void testQuadraticSolver() throws Exception {
        String res = PixelSolverEngine.solve("x^2 - 5x + 6 = 0");
        assertNotNull(res);
        JSONObject json = new JSONObject(res);
        assertEquals("IN_SCOPE", json.getString("scope"));
        assertTrue(json.getJSONObject("summary").getString("final_answer_latex").contains("2"));
        assertTrue(json.getJSONObject("summary").getString("final_answer_latex").contains("3"));
    }

    @Test
    @DisplayName("Temperature conversion (Celsius to Fahrenheit)")
    void testTemperatureConversion() throws Exception {
        String res = PixelSolverEngine.solve("100 celsius to fahrenheit");
        assertNotNull(res);
        JSONObject json = new JSONObject(res);
        assertEquals("IN_SCOPE", json.getString("scope"));
        assertTrue(json.getString("response").contains("212"));
    }

    @Test
    @DisplayName("Length unit conversion (10 km to miles)")
    void testUnitConversion() throws Exception {
        String res = PixelSolverEngine.solve("10 km to miles");
        assertNotNull(res);
        JSONObject json = new JSONObject(res);
        assertEquals("IN_SCOPE", json.getString("scope"));
        assertTrue(json.getString("response").contains("km") || json.getString("response").contains("mi"));
    }

    @Test
    @DisplayName("Bill split calculation")
    void testSplitBill() throws Exception {
        String res = PixelSolverEngine.solve("split bill 100 between 4 people with 20% tip");
        assertNotNull(res);
        JSONObject json = new JSONObject(res);
        assertEquals("IN_SCOPE", json.getString("scope"));
        assertTrue(json.has("response"));
    }
}
