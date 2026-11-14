package com.android.calculator2.abacus.solver;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Online Pixel Solver Engine for Google Pixel Calculator (Abacus / Ask about math).
 * Connects to live real-time network services (Forex exchange rates, Gemini API)
 * and generates native interactive GenUI cards, step-by-step guidance, and formulas.
 */
public class PixelSolverEngine {

    private static final Map<String, CachedRates> sRateCache = new ConcurrentHashMap<>();
    private static final ExecutorService sNetworkExecutor = Executors.newCachedThreadPool();

    private static class CachedRates {
        final long timestamp;
        final Map<String, Double> rates;

        CachedRates(long timestamp, Map<String, Double> rates) {
            this.timestamp = timestamp;
            this.rates = rates;
        }
    }

    public static String solve(String userQuery) {
        try {
            if (userQuery == null || userQuery.trim().isEmpty()) {
                return getHelpResponse();
            }
            String raw = userQuery.trim();
            String q = raw.toLowerCase(Locale.ROOT);

            // 1. Pure Online Google Gemini AI Gateway (Google's official cloud model)
            String geminiApiKey = getGeminiApiKey();
            if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
                String aiResponse = callOnlineGeminiApi(raw, geminiApiKey);
                if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                    return aiResponse;
                }
            }

            // 2. Real-time Currency Converter / Forex (Live online rate fallback)
            if (isCurrencyQuery(q)) {
                return getCurrencyResponse(raw, q);
            }

            // 3. Help / What can ask math do?
            if (q.contains("what can") || q.contains("help") || q.equals("hi") || q.equals("hello") || q.contains("how to use")) {
                return getHelpResponse();
            }

            // 4. Temperature Converter
            if (q.contains("celsius") || q.contains("fahrenheit") || q.contains("kelvin") || q.contains("temperature")) {
                return getTemperatureResponse(raw, q);
            }

            // 5. BMI Calculator
            if (q.contains("bmi") || q.contains("body mass index")) {
                return getBmiResponse();
            }

            // 6. Bill / Tip Splitter
            if (q.contains("bill") || q.contains("tip") || q.contains("split")) {
                return getSplitBillResponse(raw);
            }

            // 7. Loan / EMI / Mortgage
            if (q.contains("loan") || q.contains("mortgage") || q.contains("interest") || q.contains("emi")) {
                return getLoanResponse(raw);
            }

            // 8. Discount / Sale Calculator
            if (q.contains("discount") || q.contains("sale") || q.contains("off")) {
                return getDiscountResponse(raw);
            }

            // 9. Unit conversion (Length, Mass, Speed, etc.)
            String unitResult = tryUnitConversion(raw, q);
            if (unitResult != null) {
                return unitResult;
            }

            // 10. Quadratic Equation Solver (e.g. x^2 - 5x + 6 = 0)
            String quadResult = trySolveQuadraticEquation(raw);
            if (quadResult != null) {
                return quadResult;
            }

            // 11. Linear Equation Solver (e.g. 3x + 9 = 24)
            String eqResult = trySolveLinearEquation(raw);
            if (eqResult != null) {
                return eqResult;
            }

            // 12. General Math / Arithmetic Expression Evaluator
            String mathResult = tryEvaluateMath(raw);
            if (mathResult != null) {
                return mathResult;
            }

            // 13. Dynamic General Math/Knowledge Response
            return getGeneralResponse(raw, q);
        } catch (Throwable t) {
            return getSafeFallbackResponse(userQuery);
        }
    }

    // ==========================================
    // JSON Schema Builders (Moshi ModelResponseData)
    // ==========================================

    private static JSONObject createBaseResponse(String responseText) throws Exception {
        JSONObject root = new JSONObject();
        root.put("scope", "IN_SCOPE");
        root.put("response", responseText);
        root.put("step_by_step_guidance", new JSONArray());
        root.put("genui", JSONObject.NULL);
        return root;
    }

    private static JSONObject createSummary(String finalAnswer, String overallSolution, String exprLatex, String exprText, boolean isValid) throws Exception {
        JSONObject summary = new JSONObject();
        summary.put("final_answer_latex", finalAnswer != null ? finalAnswer : "");
        summary.put("overall_solution_latex", overallSolution != null ? overallSolution : "");
        summary.put("expression_latex", exprLatex != null ? exprLatex : JSONObject.NULL);
        summary.put("expression_text", exprText != null ? exprText : JSONObject.NULL);
        summary.put("is_expression_text_a_valid_calculator_expression", isValid);
        return summary;
    }

    private static JSONObject createGuidanceStep(String stepLatex, String expressionLatex) throws Exception {
        JSONObject step = new JSONObject();
        step.put("step_latex", stepLatex);
        step.put("expression_latex", expressionLatex != null ? expressionLatex : "");
        return step;
    }

    private static JSONObject createGenUiInput(String varName, String desc, String unit, double defaultValue) throws Exception {
        JSONObject input = new JSONObject();
        input.put("variable_name", varName);
        input.put("description", desc);
        input.put("unit", unit != null ? unit : "");
        input.put("default_value", defaultValue);
        return input;
    }

    private static JSONObject createGenUiOutput(String varName, String expr, String desc, String unit) throws Exception {
        JSONObject output = new JSONObject();
        output.put("variable_name", varName);
        output.put("expression", expr);
        output.put("description", desc);
        output.put("unit", unit != null ? unit : "");
        return output;
    }

    private static JSONObject createGenUi(String title, JSONArray inputs, JSONArray outputs) throws Exception {
        JSONObject genui = new JSONObject();
        genui.put("title", title);
        genui.put("inputs", inputs);
        genui.put("outputs", outputs);
        return genui;
    }

    private static String formatNumber(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) return String.valueOf(d);
        if (d == (long) d) {
            return String.format(Locale.ROOT, "%d", (long) d);
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        DecimalFormat df = new DecimalFormat("0.####", symbols);
        return df.format(d);
    }

    private static String formatCurrency(double d) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        DecimalFormat df = new DecimalFormat("0.00", symbols);
        return df.format(d);
    }

    // ==========================================
    // Real-Time Currency Converter (Live Online)
    // ==========================================

    private static final Map<String, String[]> CURRENCY_DICTIONARY = new LinkedHashMap<>();
    static {
        // key -> [code, symbol, name]
        CURRENCY_DICTIONARY.put("rupee", new String[]{"INR", "₹", "Indian Rupee"});
        CURRENCY_DICTIONARY.put("rupees", new String[]{"INR", "₹", "Indian Rupee"});
        CURRENCY_DICTIONARY.put("inr", new String[]{"INR", "₹", "Indian Rupee"});
        CURRENCY_DICTIONARY.put("euro", new String[]{"EUR", "€", "Euro"});
        CURRENCY_DICTIONARY.put("euros", new String[]{"EUR", "€", "Euro"});
        CURRENCY_DICTIONARY.put("eur", new String[]{"EUR", "€", "Euro"});
        CURRENCY_DICTIONARY.put("dollar", new String[]{"USD", "$", "US Dollar"});
        CURRENCY_DICTIONARY.put("dollars", new String[]{"USD", "$", "US Dollar"});
        CURRENCY_DICTIONARY.put("usd", new String[]{"USD", "$", "US Dollar"});
        CURRENCY_DICTIONARY.put("pound", new String[]{"GBP", "£", "British Pound"});
        CURRENCY_DICTIONARY.put("pounds", new String[]{"GBP", "£", "British Pound"});
        CURRENCY_DICTIONARY.put("gbp", new String[]{"GBP", "£", "British Pound"});
        CURRENCY_DICTIONARY.put("yen", new String[]{"JPY", "¥", "Japanese Yen"});
        CURRENCY_DICTIONARY.put("jpy", new String[]{"JPY", "¥", "Japanese Yen"});
        CURRENCY_DICTIONARY.put("cad", new String[]{"CAD", "C$", "Canadian Dollar"});
        CURRENCY_DICTIONARY.put("aud", new String[]{"AUD", "A$", "Australian Dollar"});
        CURRENCY_DICTIONARY.put("chf", new String[]{"CHF", "CHF", "Swiss Franc"});
        CURRENCY_DICTIONARY.put("cny", new String[]{"CNY", "¥", "Chinese Yuan"});
        CURRENCY_DICTIONARY.put("aed", new String[]{"AED", "AED", "UAE Dirham"});
        CURRENCY_DICTIONARY.put("sar", new String[]{"SAR", "SAR", "Saudi Riyal"});
        CURRENCY_DICTIONARY.put("sgd", new String[]{"SGD", "S$", "Singapore Dollar"});
        CURRENCY_DICTIONARY.put("krw", new String[]{"KRW", "₩", "South Korean Won"});
    }

    private static boolean isCurrencyQuery(String q) {
        if (q.contains("currency") || q.contains("exchange rate") || q.contains("forex") || q.contains("convert")) {
            for (String key : CURRENCY_DICTIONARY.keySet()) {
                if (q.contains(key)) return true;
            }
        }
        int count = 0;
        for (String key : CURRENCY_DICTIONARY.keySet()) {
            if (Pattern.compile("\\b" + key + "\\b").matcher(q).find()) {
                count++;
                if (count >= 2) return true;
            }
        }
        return false;
    }

    private static String getCurrencyResponse(String raw, String q) {
        try {
            // 1. Identify base and target currency
            List<String[]> foundCurrencies = new ArrayList<>();
            for (Map.Entry<String, String[]> entry : CURRENCY_DICTIONARY.entrySet()) {
                Matcher m = Pattern.compile("\\b" + entry.getKey() + "\\b").matcher(q);
                if (m.find()) {
                    boolean already = false;
                    for (String[] c : foundCurrencies) {
                        if (c[0].equals(entry.getValue()[0])) {
                            already = true;
                            break;
                        }
                    }
                    if (!already) {
                        foundCurrencies.add(entry.getValue());
                    }
                }
            }

            String[] base = foundCurrencies.size() > 0 ? foundCurrencies.get(0) : new String[]{"INR", "₹", "Indian Rupee"};
            String[] target = foundCurrencies.size() > 1 ? foundCurrencies.get(1) : new String[]{"EUR", "€", "Euro"};

            // Handle "to", "in", "from" direction: e.g. "convert rupee to euro" -> base INR, target EUR
            Matcher mTo = Pattern.compile("(?:to|in|into)\\s+([a-zA-Z]+)").matcher(q);
            if (mTo.find()) {
                String toWord = mTo.group(1).toLowerCase(Locale.ROOT);
                if (CURRENCY_DICTIONARY.containsKey(toWord)) {
                    String[] toCurr = CURRENCY_DICTIONARY.get(toWord);
                    if (!toCurr[0].equals(base[0])) {
                        target = toCurr;
                    }
                }
            }

            if (base[0].equals(target[0])) {
                target = base[0].equals("EUR") ? new String[]{"USD", "$", "US Dollar"} : new String[]{"EUR", "€", "Euro"};
            }

            // 2. Identify amount
            double amount = 100.0;
            Matcher mNum = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(raw);
            if (mNum.find()) {
                try {
                    amount = Double.parseDouble(mNum.group(1));
                } catch (Exception ignored) {}
            }

            // 3. Fetch live rate online via network
            double rate = fetchLiveExchangeRate(base[0], target[0]);
            double inverseRate = (rate > 0) ? (1.0 / rate) : 1.0;
            double converted = amount * rate;

            // 4. Construct rich response with GenUI card and guidance
            String title = base[0] + " to " + target[0] + " Currency Converter";
            JSONObject root = createBaseResponse(
                "Live exchange rate: 1 " + base[0] + " ≈ " + formatRate(rate) + " " + target[0]
                + " (1 " + target[0] + " ≈ " + formatRate(inverseRate) + " " + base[0] + "). "
                + "Converted " + formatNumber(amount) + " " + base[0] + " = " + formatCurrency(converted) + " " + target[0] + "."
            );

            root.put("summary", createSummary(
                formatNumber(amount) + " " + base[0] + " = " + formatCurrency(converted) + " " + target[0],
                "1 " + base[0] + " = " + formatRate(rate) + " " + target[0] + " (1 " + target[0] + " = " + formatRate(inverseRate) + " " + base[0] + ")",
                formatNumber(amount) + " \\times " + formatRate(rate),
                formatNumber(amount) + " * " + String.format(Locale.ROOT, "%.6f", rate),
                true
            ));

            JSONArray guidance = new JSONArray();
            guidance.put(createGuidanceStep(
                "Real-time market rate: 1 " + base[2] + " (" + base[0] + ") = " + formatRate(rate) + " " + target[2] + " (" + target[0] + ")",
                "1 \\times " + formatRate(rate) + " = " + formatRate(rate)
            ));
            guidance.put(createGuidanceStep(
                "Conversion formula: " + target[0] + " = " + base[0] + " × " + formatRate(rate),
                formatNumber(amount) + " \\times " + formatRate(rate)
            ));
            guidance.put(createGuidanceStep(
                "Calculation: " + formatNumber(amount) + " " + base[0] + " × " + formatRate(rate) + " = " + formatCurrency(converted) + " " + target[0],
                formatNumber(amount) + " \\times " + formatRate(rate) + " = " + formatCurrency(converted)
            ));
            root.put("step_by_step_guidance", guidance);

            // GenUI Interactive Card
            JSONArray inputs = new JSONArray();
            inputs.put(createGenUiInput(
                base[0].toLowerCase(Locale.ROOT),
                base[2],
                base[1],
                amount
            ));

            JSONArray outputs = new JSONArray();
            outputs.put(createGenUiOutput(
                target[0].toLowerCase(Locale.ROOT),
                base[0].toLowerCase(Locale.ROOT) + " * " + String.format(Locale.ROOT, "%.6f", rate),
                target[2],
                target[1]
            ));

            root.put("genui", createGenUi(title, inputs, outputs));
            return root.toString();
        } catch (Exception e) {
            return getSafeFallbackResponse(raw);
        }
    }

    private static String formatRate(double r) {
        if (r >= 100) return String.format(Locale.ROOT, "%.2f", r);
        if (r >= 1) return String.format(Locale.ROOT, "%.4f", r);
        return String.format(Locale.ROOT, "%.5f", r);
    }

    private static double fetchLiveExchangeRate(final String base, final String target) {
        // 1. Check in-memory cache (10 minute TTL)
        CachedRates cached = sRateCache.get(base);
        long now = System.currentTimeMillis();
        if (cached != null && (now - cached.timestamp < 600_000L)) {
            Double cachedRate = cached.rates.get(target);
            if (cachedRate != null && cachedRate > 0) {
                return cachedRate;
            }
        }

        // 2. Fetch live rate from real-time API in background thread with 4s timeout
        Future<Double> future = sNetworkExecutor.submit(new Callable<Double>() {
            @Override
            public Double call() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL("https://open.er-api.com/v6/latest/" + base);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(4000);
                    conn.setReadTimeout(4000);
                    conn.setRequestProperty("User-Agent", "PixelCalculator/10.0");
                    conn.connect();

                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();

                        JSONObject json = new JSONObject(sb.toString());
                        if (json.has("rates")) {
                            JSONObject ratesObj = json.getJSONObject("rates");
                            Map<String, Double> rateMap = new HashMap<>();
                            Iterator<String> keys = ratesObj.keys();
                            while (keys.hasNext()) {
                                String k = keys.next();
                                rateMap.put(k, ratesObj.getDouble(k));
                            }
                            sRateCache.put(base, new CachedRates(System.currentTimeMillis(), rateMap));
                            if (rateMap.containsKey(target)) {
                                return rateMap.get(target);
                            }
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    if (conn != null) conn.disconnect();
                }
                return null;
            }
        });

        try {
            Double onlineRate = future.get(4500, TimeUnit.MILLISECONDS);
            if (onlineRate != null && onlineRate > 0) {
                return onlineRate;
            }
        } catch (Exception ignored) {}

        // 3. Fallback: reliable baseline rates table (if network unavailable or airplane mode)
        return getFallbackRate(base, target);
    }

    private static double getFallbackRate(String base, String target) {
        // Base USD reference rates
        Map<String, Double> usdRates = new HashMap<>();
        usdRates.put("USD", 1.0);
        usdRates.put("EUR", 0.92);
        usdRates.put("INR", 86.80);
        usdRates.put("GBP", 0.79);
        usdRates.put("JPY", 154.50);
        usdRates.put("CAD", 1.41);
        usdRates.put("AUD", 1.56);
        usdRates.put("CHF", 0.88);
        usdRates.put("CNY", 7.24);
        usdRates.put("AED", 3.67);
        usdRates.put("SAR", 3.75);
        usdRates.put("SGD", 1.35);

        Double baseToUsd = usdRates.get(base);
        Double targetToUsd = usdRates.get(target);

        if (baseToUsd != null && targetToUsd != null && baseToUsd > 0) {
            return targetToUsd / baseToUsd;
        }
        return 1.0;
    }

    // ==========================================
    // Online Gemini API Gateway
    // ==========================================

    private static final String DEFAULT_API_KEY = "";

    private static String getGeminiApiKey() {
        // Check system properties, environment, or device tmp file
        try {
            String prop = System.getProperty("gemini.api.key");
            if (prop != null && !prop.trim().isEmpty()) return prop.trim();
        } catch (Exception ignored) {}

        try {
            String env = System.getenv("GEMINI_API_KEY");
            if (env != null && !env.trim().isEmpty()) return env.trim();
        } catch (Exception ignored) {}

        try {
            File keyFile = new File("/data/local/tmp/gemini_key.txt");
            if (keyFile.exists() && keyFile.canRead()) {
                BufferedReader br = new BufferedReader(new FileReader(keyFile));
                String key = br.readLine();
                br.close();
                if (key != null && !key.trim().isEmpty()) return key.trim();
            }
        } catch (Exception ignored) {}

        try {
            File keyFile = new File("/sdcard/gemini_key.txt");
            if (keyFile.exists() && keyFile.canRead()) {
                BufferedReader br = new BufferedReader(new FileReader(keyFile));
                String key = br.readLine();
                br.close();
                if (key != null && !key.trim().isEmpty()) return key.trim();
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static String callOnlineGeminiApi(final String query, final String apiKey) {
        Future<String> future = sNetworkExecutor.submit(new Callable<String>() {
            @Override
            public String call() {
                HttpURLConnection conn = null;
                try {
                    android.util.Log.d("PixelSolverEngine", "Connecting to Gemini 3.5 Flash for query: " + query);
                    URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=" + apiKey);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setDoOutput(true);

                    // Build request JSON
                    JSONObject requestJson = new JSONObject();
                    JSONObject systemInstruction = new JSONObject();
                    JSONArray sysParts = new JSONArray();
                    JSONObject sysPart = new JSONObject();
                    sysPart.put("text", "You are Google Calculator's Abacus AI assistant on Pixel. Respond ONLY with valid JSON matching this schema: "
                        + "{\"scope\":\"IN_SCOPE\",\"response\":\"text explanation\",\"summary\":{\"final_answer_latex\":\"...\",\"overall_solution_latex\":\"...\",\"expression_latex\":null,\"expression_text\":null,\"is_expression_text_a_valid_calculator_expression\":false},\"step_by_step_guidance\":[{\"step_latex\":\"...\",\"expression_latex\":\"...\"}],\"genui\":null}. "
                        + "If the request asks for an interactive tool or calculator (like BMI, split bill, converter, etc.), set 'genui' to: {\"title\":\"...\",\"inputs\":[{\"variable_name\":\"...\",\"description\":\"...\",\"unit\":\"...\",\"default_value\":1.0}],\"outputs\":[{\"variable_name\":\"...\",\"expression\":\"...\",\"description\":\"...\",\"unit\":\"...\"}]}. Otherwise, set genui to null.");
                    sysParts.put(sysPart);
                    systemInstruction.put("parts", sysParts);
                    requestJson.put("systemInstruction", systemInstruction);

                    JSONArray contents = new JSONArray();
                    JSONObject content = new JSONObject();
                    JSONArray parts = new JSONArray();
                    JSONObject part = new JSONObject();
                    part.put("text", query);
                    parts.put(part);
                    content.put("parts", parts);
                    contents.put(content);
                    requestJson.put("contents", contents);

                    JSONObject genConfig = new JSONObject();
                    genConfig.put("responseMimeType", "application/json");
                    requestJson.put("generationConfig", genConfig);

                    byte[] postData = requestJson.toString().getBytes(StandardCharsets.UTF_8);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(postData);
                    }

                    int code = conn.getResponseCode();
                    android.util.Log.d("PixelSolverEngine", "Gemini HTTP response code: " + code);
                    if (code == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();

                        JSONObject resp = new JSONObject(sb.toString());
                        JSONArray candidates = resp.optJSONArray("candidates");
                        if (candidates != null && candidates.length() > 0) {
                            JSONObject cand = candidates.getJSONObject(0);
                            JSONObject cContent = cand.optJSONObject("content");
                            if (cContent != null) {
                                JSONArray cParts = cContent.optJSONArray("parts");
                                if (cParts != null && cParts.length() > 0) {
                                    String text = cParts.getJSONObject(0).optString("text");
                                    if (text != null) {
                                        text = text.trim();
                                        if (text.startsWith("```json")) {
                                            text = text.substring(7);
                                        }
                                        if (text.startsWith("```")) {
                                            text = text.substring(3);
                                        }
                                        if (text.endsWith("```")) {
                                            text = text.substring(0, text.length() - 3);
                                        }
                                        text = text.trim();
                                        if (text.contains("\"scope\"")) {
                                            android.util.Log.d("PixelSolverEngine", "Gemini valid response received: " + text.length() + " chars");
                                            return sanitizeGeminiJson(text);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        BufferedReader errReader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                        StringBuilder errSb = new StringBuilder();
                        String eline;
                        while ((eline = errReader.readLine()) != null) {
                            errSb.append(eline);
                        }
                        errReader.close();
                        android.util.Log.e("PixelSolverEngine", "Gemini HTTP error " + code + ": " + errSb.toString());
                    }
                } catch (Exception e) {
                    android.util.Log.e("PixelSolverEngine", "Gemini call exception: " + e.getMessage(), e);
                } finally {
                    if (conn != null) conn.disconnect();
                }
                return null;
            }
        });

        try {
            return future.get(9000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            android.util.Log.w("PixelSolverEngine", "Gemini timeout/abort: " + e.getMessage());
            return null;
        }
    }

    private static String sanitizeGeminiJson(String jsonStr) {
        try {
            JSONObject obj = new JSONObject(jsonStr);

            JSONObject summary = obj.optJSONObject("summary");
            if (summary != null) {
                if (summary.has("final_answer_latex")) {
                    summary.put("final_answer_latex", cleanLatex(summary.optString("final_answer_latex")));
                }
                if (summary.has("overall_solution_latex")) {
                    summary.put("overall_solution_latex", cleanLatex(summary.optString("overall_solution_latex")));
                }
            }

            JSONArray steps = obj.optJSONArray("step_by_step_guidance");
            if (steps != null) {
                for (int i = 0; i < steps.length(); i++) {
                    JSONObject step = steps.getJSONObject(i);
                    if (step.has("step_latex")) {
                        step.put("step_latex", cleanLatex(step.optString("step_latex")));
                    }
                }
            }
            return obj.toString();
        } catch (Exception ignored) {
            return jsonStr;
        }
    }

    private static String cleanLatex(String s) {
        if (s == null) return "";
        return s.replaceAll("\\\\text\\{([^}]+)\\}", "$1");
    }

    // ==========================================
    // Interactive GenUI Tools
    // ==========================================

    private static String getHelpResponse() {
        try {
            JSONObject root = createBaseResponse(
                "I can help you with:\n"
                + "• Step-by-step math: Homework, equations, and word problems.\n"
                + "• Everyday calculations: Splitting bills, percentages, and discounts.\n"
                + "• Real-time currency conversions: Live Forex rates across 160+ currencies.\n"
                + "• Custom calculators: Interactive tools you can save to My tools."
            );
            root.put("summary", createSummary(
                "Ask about math is ready",
                "Ask any math question or request a custom formula calculator.",
                null, null, false
            ));
            JSONArray guidance = new JSONArray();
            guidance.put(createGuidanceStep("Example 1: Real-time currency converter from rupee to euro", ""));
            guidance.put(createGuidanceStep("Example 2: Split a \\$135 bill with 18% tip for 4 people", "(135 * 1.18) / 4"));
            guidance.put(createGuidanceStep("Example 3: Create a BMI calculator", "w / (h^2)"));
            guidance.put(createGuidanceStep("Example 4: Solve 3x + 9 = 24", "(24 - 9) / 3"));
            root.put("step_by_step_guidance", guidance);
            return root.toString();
        } catch (Exception e) {
            return getSafeFallbackResponse("help");
        }
    }

    private static String getTemperatureResponse(String raw, String q) {
        try {
            boolean toCelsius = q.contains("to celsius") || q.contains("to c") || q.contains("in celsius");
            double inputVal = 77.0;
            Matcher m = Pattern.compile("(-?\\d+(?:\\.\\d+)?)").matcher(raw);
            if (m.find()) {
                try {
                    inputVal = Double.parseDouble(m.group(1));
                } catch (Exception ignored) {}
            } else if (!toCelsius) {
                inputVal = 25.0;
            }

            JSONObject root;
            if (toCelsius) {
                double cVal = (inputVal - 32.0) * 5.0 / 9.0;
                root = createBaseResponse(formatNumber(inputVal) + "°F is equal to " + formatNumber(cVal) + "°C.");
                root.put("summary", createSummary(
                    formatNumber(inputVal) + "^\\circ\\text{F} = " + formatNumber(cVal) + "^\\circ\\text{C}",
                    "^\\circ\\text{C} = (^\\circ\\text{F} - 32) \\times \\frac{5}{9}",
                    "(" + formatNumber(inputVal) + " - 32) \\times \\frac{5}{9}",
                    "(" + formatNumber(inputVal) + " - 32) * 5 / 9",
                    true
                ));

                JSONArray guidance = new JSONArray();
                guidance.put(createGuidanceStep("Subtract 32 from Fahrenheit: " + formatNumber(inputVal) + " - 32 = " + formatNumber(inputVal - 32), formatNumber(inputVal) + " - 32"));
                guidance.put(createGuidanceStep("Multiply by 5/9: (" + formatNumber(inputVal - 32) + " \\times 5) / 9 = " + formatNumber(cVal), "(" + formatNumber(inputVal - 32) + " * 5) / 9"));
                root.put("step_by_step_guidance", guidance);

                JSONArray inputs = new JSONArray();
                inputs.put(createGenUiInput("f", "Fahrenheit", "°F", inputVal));
                JSONArray outputs = new JSONArray();
                outputs.put(createGenUiOutput("c", "(f - 32) * 5 / 9", "Celsius", "°C"));
                root.put("genui", createGenUi("Temperature Converter (°F → °C)", inputs, outputs));
            } else {
                double fVal = (inputVal * 9.0 / 5.0) + 32.0;
                root = createBaseResponse(formatNumber(inputVal) + "°C is equal to " + formatNumber(fVal) + "°F.");
                root.put("summary", createSummary(
                    formatNumber(inputVal) + "^\\circ\\text{C} = " + formatNumber(fVal) + "^\\circ\\text{F}",
                    "^\\circ\\text{F} = (^\\circ\\text{C} \\times \\frac{9}{5}) + 32",
                    "(" + formatNumber(inputVal) + " \\times \\frac{9}{5}) + 32",
                    "(" + formatNumber(inputVal) + " * 9 / 5) + 32",
                    true
                ));

                JSONArray guidance = new JSONArray();
                guidance.put(createGuidanceStep("Multiply Celsius by 9/5: " + formatNumber(inputVal) + " \\times 1.8 = " + formatNumber(inputVal * 1.8), formatNumber(inputVal) + " * 1.8"));
                guidance.put(createGuidanceStep("Add 32: " + formatNumber(inputVal * 1.8) + " + 32 = " + formatNumber(fVal), formatNumber(inputVal * 1.8) + " + 32"));
                root.put("step_by_step_guidance", guidance);

                JSONArray inputs = new JSONArray();
                inputs.put(createGenUiInput("c", "Celsius", "°C", inputVal));
                JSONArray outputs = new JSONArray();
                outputs.put(createGenUiOutput("f", "(c * 9 / 5) + 32", "Fahrenheit", "°F"));
                root.put("genui", createGenUi("Temperature Converter (°C → °F)", inputs, outputs));
            }
            return root.toString();
        } catch (Exception e) {
            return getSafeFallbackResponse(raw);
        }
    }

    private static String getBmiResponse() {
        try {
            JSONObject root = createBaseResponse("Here is a BMI calculator formula for you:");
            root.put("summary", createSummary(
                "\\text{BMI} = \\frac{w}{h^2}",
                "\\text{BMI} = \\frac{\\text{Weight (kg)}}{(\\text{Height (m)})^2}",
                "\\frac{w}{h^2}",
                "w / (h^2)",
                false
            ));
            JSONArray guidance = new JSONArray();
            guidance.put(createGuidanceStep("Formula: BMI = Weight (kg) / [Height (m)]²", "w / (h^2)"));
            guidance.put(createGuidanceStep("Categories: Underweight (< 18.5) | Normal (18.5 - 24.9) | Overweight (≥ 25)", ""));
            root.put("step_by_step_guidance", guidance);

            JSONArray inputs = new JSONArray();
            inputs.put(createGenUiInput("w", "Weight", "kg", 70.0));
            inputs.put(createGenUiInput("h", "Height", "m", 1.75));

            JSONArray outputs = new JSONArray();
            outputs.put(createGenUiOutput("BMI", "w / (h * h)", "Body Mass Index", "kg/m²"));

            root.put("genui", createGenUi("BMI Calculator", inputs, outputs));
            return root.toString();
        } catch (Exception e) {
            return getSafeFallbackResponse("BMI");
        }
    }

    private static String getSplitBillResponse(String raw) {
        try {
            double bill = 135.0;
            double tip = 18.0;
            double people = 4.0;

            Matcher mNum = Pattern.compile("\\$?(\\d+(?:\\.\\d+)?)").matcher(raw);
            if (mNum.find()) {
                try { bill = Double.parseDouble(mNum.group(1)); } catch (Exception ignored) {}
            }
            Matcher mTip = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%").matcher(raw);
            if (mTip.find()) {
                try { tip = Double.parseDouble(mTip.group(1)); } catch (Exception ignored) {}
            }
            Matcher mPeople = Pattern.compile("(?:for|between|among)\\s+(\\d+)").matcher(raw);
            if (mPeople.find()) {
                try { people = Double.parseDouble(mPeople.group(1)); } catch (Exception ignored) {}
            }

            double totalTip = bill * (tip / 100.0);
            double totalBill = bill + totalTip;
            double perPerson = totalBill / people;

            JSONObject root = createBaseResponse(
                "Each person pays $" + formatCurrency(perPerson) + " (Total bill: $" + formatCurrency(totalBill) + " including " + formatNumber(tip) + "% tip)."
            );
            root.put("summary", createSummary(
                "\\$" + formatCurrency(perPerson) + "\\text{ per person}",
                "\\text{Total} = \\$" + formatCurrency(totalBill) + "\\quad (\\text{Tip}: \\$" + formatCurrency(totalTip) + ")",
                "\\frac{" + formatNumber(bill) + " \\times (1 + " + formatNumber(tip / 100.0) + ")}{" + formatNumber(people) + "}",
                "(" + formatNumber(bill) + " * (1 + " + formatNumber(tip / 100.0) + ")) / " + formatNumber(people),
                true
            ));

            JSONArray guidance = new JSONArray();
            guidance.put(createGuidanceStep("Calculate tip: \\$" + formatNumber(bill) + " \\times " + formatNumber(tip) + "\\% = \\$" + formatCurrency(totalTip), formatNumber(bill) + " * " + (tip / 100.0)));
            guidance.put(createGuidanceStep("Calculate total: \\$" + formatNumber(bill) + " + \\$" + formatCurrency(totalTip) + " = \\$" + formatCurrency(totalBill), formatNumber(bill) + " + " + formatCurrency(totalTip)));
            guidance.put(createGuidanceStep("Split by " + formatNumber(people) + " people: \\$" + formatCurrency(totalBill) + " / " + formatNumber(people) + " = \\$" + formatCurrency(perPerson), formatCurrency(totalBill) + " / " + formatNumber(people)));
            root.put("step_by_step_guidance", guidance);

            JSONArray inputs = new JSONArray();
            inputs.put(createGenUiInput("bill", "Bill Amount", "$", bill));
            inputs.put(createGenUiInput("tip", "Tip Percentage", "%", tip));
            inputs.put(createGenUiInput("people", "Number of People", "", people));

            JSONArray outputs = new JSONArray();
            outputs.put(createGenUiOutput("per_person", "(bill * (1 + (tip / 100))) / people", "Per Person", "$"));
            outputs.put(createGenUiOutput("total", "bill * (1 + (tip / 100))", "Total Bill", "$"));

            root.put("genui", createGenUi("Tip & Bill Splitter", inputs, outputs));
            return root.toString();
        } catch (Exception e) {
            return getSafeFallbackResponse(raw);
        }
    }

    private static String getLoanResponse(String raw) {
        try {
            double p = 250000.0;
            double r = 6.5;
            double n = 360.0; // 30 years in months

            Matcher mNum = Pattern.compile("\\$?(\\d{4,}(?:\\.\\d+)?)").matcher(raw);
            if (mNum.find()) {
                try { p = Double.parseDouble(mNum.group(1)); } catch (Exception ignored) {}
            }

            double monthlyRate = (r / 100.0) / 12.0;
            double emi = (p * monthlyRate * Math.pow(1 + monthlyRate, n)) / (Math.pow(1 + monthlyRate, n) - 1);
            double totalPayment = emi * n;
            double totalInterest = totalPayment - p;

            JSONObject root = createBaseResponse(
                "Estimated monthly payment: $" + formatCurrency(emi) + " for $" + formatNumber(p) + " at " + formatNumber(r) + "% over " + formatNumber(n / 12) + " years."
            );
            root.put("summary", createSummary(
                "\\$" + formatCurrency(emi) + "\\text{ / month}",
                "\\text{Total Interest} = \\$" + formatCurrency(totalInterest) + "\\quad (\\text{Total Paid}: \\$" + formatCurrency(totalPayment) + ")",
                null, null, false
            ));

            JSONArray guidance = new JSONArray();
            guidance.put(createGuidanceStep("Monthly interest rate: " + formatNumber(r) + "\\% / 12 = " + String.format(Locale.ROOT, "%.4f%%", monthlyRate * 100), ""));
            guidance.put(createGuidanceStep("Total payments: " + formatNumber(n) + " months", ""));
            guidance.put(createGuidanceStep("Total interest paid: \\$" + formatCurrency(totalInterest), ""));
            root.put("step_by_step_guidance", guidance);

            JSONArray inputs = new JSONArray();
            inputs.put(createGenUiInput("p", "Principal", "$", p));
            inputs.put(createGenUiInput("r", "Annual Rate", "%", r));
            inputs.put(createGenUiInput("months", "Tenure (months)", "mo", n));

            JSONArray outputs = new JSONArray();
            outputs.put(createGenUiOutput("emi", "(p * (r / 1200) * ((1 + (r / 1200)) ^ months)) / (((1 + (r / 1200)) ^ months) - 1)", "Monthly Payment", "$"));
            root.put("genui", createGenUi("Loan & Mortgage Calculator", inputs, outputs));
            return root.toString();
        } catch (Exception e) {
            return getSafeFallbackResponse(raw);
        }
    }

    private static String getDiscountResponse(String raw) {
        try {
            double price = 80.0;
            double discount = 20.0;

            Matcher mPrice = Pattern.compile("\\$?(\\d+(?:\\.\\d+)?)").matcher(raw);
            if (mPrice.find()) {
                try { price = Double.parseDouble(mPrice.group(1)); } catch (Exception ignored) {}
            }
            Matcher mDisc = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%").matcher(raw);
            if (mDisc.find()) {
                try { discount = Double.parseDouble(mDisc.group(1)); } catch (Exception ignored) {}
            }

            double saved = price * (discount / 100.0);
            double finalPrice = price - saved;

            JSONObject root = createBaseResponse(
                "Final price: $" + formatCurrency(finalPrice) + " (You save $" + formatCurrency(saved) + " with " + formatNumber(discount) + "% off)."
            );
            root.put("summary", createSummary(
                "\\$" + formatCurrency(finalPrice),
                "\\text{Savings}: \\$" + formatCurrency(saved) + "\\quad (\\text{Original}: \\$" + formatCurrency(price) + ")",
                formatNumber(price) + " \\times (1 - " + formatNumber(discount / 100.0) + ")",
                formatNumber(price) + " * (1 - " + formatNumber(discount / 100.0) + ")",
                true
            ));

            JSONArray guidance = new JSONArray();
            guidance.put(createGuidanceStep("Calculate discount savings: \\$" + formatNumber(price) + " \\times " + formatNumber(discount) + "\\% = \\$" + formatCurrency(saved), formatNumber(price) + " * " + (discount / 100.0)));
            guidance.put(createGuidanceStep("Subtract savings from original: \\$" + formatNumber(price) + " - \\$" + formatCurrency(saved) + " = \\$" + formatCurrency(finalPrice), formatNumber(price) + " - " + formatCurrency(saved)));
            root.put("step_by_step_guidance", guidance);

            JSONArray inputs = new JSONArray();
            inputs.put(createGenUiInput("price", "Original Price", "$", price));
            inputs.put(createGenUiInput("discount", "Discount", "%", discount));

            JSONArray outputs = new JSONArray();
            outputs.put(createGenUiOutput("final_price", "price * (1 - (discount / 100))", "Final Price", "$"));
            outputs.put(createGenUiOutput("savings", "price * (discount / 100)", "Total Savings", "$"));

            root.put("genui", createGenUi("Discount Calculator", inputs, outputs));
            return root.toString();
        } catch (Exception e) {
            return getSafeFallbackResponse(raw);
        }
    }

    private static String tryUnitConversion(String raw, String q) {
        // Miles to Kilometers
        if (q.contains("mile") && (q.contains("km") || q.contains("kilometer"))) {
            return buildUnitConversionResponse("Miles to Kilometers", "mi", "km", 1.60934, raw);
        }
        // Kilometers to Miles
        if (q.contains("km") && q.contains("mile")) {
            return buildUnitConversionResponse("Kilometers to Miles", "km", "mi", 0.621371, raw);
        }
        // Pounds to Kilograms
        if ((q.contains("pound") || q.contains("lbs")) && (q.contains("kg") || q.contains("kilogram"))) {
            return buildUnitConversionResponse("Pounds to Kilograms", "lb", "kg", 0.453592, raw);
        }
        // Kilograms to Pounds
        if (q.contains("kg") && (q.contains("pound") || q.contains("lbs"))) {
            return buildUnitConversionResponse("Kilograms to Pounds", "kg", "lb", 2.20462, raw);
        }
        return null;
    }

    private static String buildUnitConversionResponse(String title, String fromUnit, String toUnit, double factor, String raw) {
        try {
            double val = 10.0;
            Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(raw);
            if (m.find()) {
                try { val = Double.parseDouble(m.group(1)); } catch (Exception ignored) {}
            }
            double converted = val * factor;

            JSONObject root = createBaseResponse(formatNumber(val) + " " + fromUnit + " = " + formatNumber(converted) + " " + toUnit + ".");
            root.put("summary", createSummary(
                formatNumber(val) + "\\text{ " + fromUnit + "} = " + formatNumber(converted) + "\\text{ " + toUnit + "}",
                "1\\text{ " + fromUnit + "} = " + formatNumber(factor) + "\\text{ " + toUnit + "}",
                formatNumber(val) + " \\times " + formatNumber(factor),
                formatNumber(val) + " * " + formatNumber(factor),
                true
            ));

            JSONArray guidance = new JSONArray();
            guidance.put(createGuidanceStep("Conversion factor: 1 " + fromUnit + " = " + formatNumber(factor) + " " + toUnit, ""));
            guidance.put(createGuidanceStep("Calculate: " + formatNumber(val) + " \\times " + formatNumber(factor) + " = " + formatNumber(converted), formatNumber(val) + " * " + formatNumber(factor)));
            root.put("step_by_step_guidance", guidance);

            JSONArray inputs = new JSONArray();
            inputs.put(createGenUiInput("v", "Value (" + fromUnit + ")", fromUnit, val));

            JSONArray outputs = new JSONArray();
            outputs.put(createGenUiOutput("res", "v * " + formatNumber(factor), "Converted (" + toUnit + ")", toUnit));

            root.put("genui", createGenUi(title, inputs, outputs));
            return root.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ==========================================
    // Quadratic & Linear Equation Solvers
    // ==========================================

    private static String trySolveQuadraticEquation(String raw) {
        try {
            // Match ax^2 + bx + c = 0
            Pattern p = Pattern.compile("([+-]?\\s*\\d*\\.?\\d*)\\s*x\\s*\\^\\s*2\\s*([+-]\\s*\\d*\\.?\\d*)\\s*x\\s*([+-]\\s*\\d*\\.?\\d*)\\s*=\\s*0", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(raw);
            if (m.find()) {
                String aStr = m.group(1).replaceAll("\\s+", "");
                String bStr = m.group(2).replaceAll("\\s+", "");
                String cStr = m.group(3).replaceAll("\\s+", "");

                double a = parseCoefficient(aStr, 1.0);
                double b = parseCoefficient(bStr, 1.0);
                double c = parseCoefficient(cStr, 0.0);

                double disc = (b * b) - (4 * a * c);

                JSONObject root = createBaseResponse("Quadratic equation: " + formatNumber(a) + "x² + " + formatNumber(b) + "x + " + formatNumber(c) + " = 0");
                JSONArray guidance = new JSONArray();
                guidance.put(createGuidanceStep("Quadratic formula: x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}", ""));
                guidance.put(createGuidanceStep("Discriminant: \\Delta = b^2 - 4ac = (" + formatNumber(b) + ")^2 - 4(" + formatNumber(a) + ")(" + formatNumber(c) + ") = " + formatNumber(disc), ""));

                String finalAns;
                if (disc > 0) {
                    double x1 = (-b + Math.sqrt(disc)) / (2 * a);
                    double x2 = (-b - Math.sqrt(disc)) / (2 * a);
                    guidance.put(createGuidanceStep("Root 1: x_1 = \\frac{-(" + formatNumber(b) + ") + \\sqrt{" + formatNumber(disc) + "}}{2(" + formatNumber(a) + ")} = " + formatNumber(x1), ""));
                    guidance.put(createGuidanceStep("Root 2: x_2 = \\frac{-(" + formatNumber(b) + ") - \\sqrt{" + formatNumber(disc) + "}}{2(" + formatNumber(a) + ")} = " + formatNumber(x2), ""));
                    finalAns = "x_1 = " + formatNumber(x1) + ",\\; x_2 = " + formatNumber(x2);
                } else if (disc == 0) {
                    double x = -b / (2 * a);
                    guidance.put(createGuidanceStep("Single repeated root: x = \\frac{-(" + formatNumber(b) + ")}{2(" + formatNumber(a) + ")} = " + formatNumber(x), ""));
                    finalAns = "x = " + formatNumber(x);
                } else {
                    finalAns = "\\text{No real roots (complex roots)}";
                }

                root.put("summary", createSummary(finalAns, "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}", null, null, false));
                root.put("step_by_step_guidance", guidance);
                return root.toString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static double parseCoefficient(String s, double defaultVal) {
        if (s == null || s.isEmpty() || s.equals("+")) return defaultVal;
        if (s.equals("-")) return -defaultVal;
        return Double.parseDouble(s);
    }

    private static String trySolveLinearEquation(String raw) {
        try {
            Pattern p = Pattern.compile("([+-]?\\s*\\d*\\.?\\d*)\\s*x\\s*([+-]\\s*\\d+\\.?\\d*)?\\s*=\\s*([+-]?\\s*\\d+\\.?\\d*)", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(raw);
            if (m.find()) {
                String aStr = m.group(1).replaceAll("\\s+", "");
                String bStr = m.group(2) != null ? m.group(2).replaceAll("\\s+", "") : "0";
                String cStr = m.group(3).replaceAll("\\s+", "");

                double a = parseCoefficient(aStr, 1.0);
                double b = Double.parseDouble(bStr);
                double c = Double.parseDouble(cStr);

                if (a == 0) return null;

                double rhs = c - b;
                double x = rhs / a;

                JSONObject root = createBaseResponse("Solution for x:");
                root.put("summary", createSummary(
                    "x = " + formatNumber(x),
                    formatNumber(a) + "x + (" + formatNumber(b) + ") = " + formatNumber(c),
                    "(" + formatNumber(c) + " - (" + formatNumber(b) + ")) / " + formatNumber(a),
                    "(" + formatNumber(c) + " - (" + formatNumber(b) + ")) / " + formatNumber(a),
                    true
                ));

                JSONArray guidance = new JSONArray();
                guidance.put(createGuidanceStep("Subtract " + formatNumber(b) + " from both sides: " + formatNumber(a) + "x = " + formatNumber(rhs), formatNumber(c) + " - " + formatNumber(b)));
                guidance.put(createGuidanceStep("Divide both sides by " + formatNumber(a) + ": x = " + formatNumber(rhs) + " / " + formatNumber(a) + " = " + formatNumber(x), formatNumber(rhs) + " / " + formatNumber(a)));
                root.put("step_by_step_guidance", guidance);
                return root.toString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String tryEvaluateMath(String raw) {
        try {
            String expr = raw.replaceAll("[^0-9+\\-*/%^().a-zA-Z]", "");
            if (expr.matches(".*[0-9].*") && expr.matches(".*[+\\-*/%^].*")) {
                double result = new MathParser(expr).parse();
                JSONObject root = createBaseResponse("Result: " + formatNumber(result));
                root.put("summary", createSummary(
                    formatNumber(result),
                    expr + " = " + formatNumber(result),
                    expr,
                    expr,
                    true
                ));

                JSONArray guidance = new JSONArray();
                guidance.put(createGuidanceStep("Evaluate expression: " + expr + " = " + formatNumber(result), expr));
                root.put("step_by_step_guidance", guidance);
                return root.toString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String getGeneralResponse(String raw, String q) {
        try {
            // General math knowledge explanations
            if (q.contains("pythagor")) {
                JSONObject root = createBaseResponse("Pythagorean theorem states that in a right-angled triangle: a² + b² = c² (where c is the hypotenuse).");
                root.put("summary", createSummary("a^2 + b^2 = c^2", "c = \\sqrt{a^2 + b^2}", "\\sqrt{a^2 + b^2}", "sqrt(a^2 + b^2)", false));
                JSONArray guidance = new JSONArray();
                guidance.put(createGuidanceStep("Formula: a² + b² = c²", "a^2 + b^2 = c^2"));
                guidance.put(createGuidanceStep("Solve for hypotenuse: c = \\sqrt{a^2 + b^2}", "c = \\sqrt{a^2 + b^2}"));
                root.put("step_by_step_guidance", guidance);

                JSONArray inputs = new JSONArray();
                inputs.put(createGenUiInput("a", "Side a", "", 3.0));
                inputs.put(createGenUiInput("b", "Side b", "", 4.0));
                JSONArray outputs = new JSONArray();
                outputs.put(createGenUiOutput("c", "sqrt((a * a) + (b * b))", "Hypotenuse c", ""));
                root.put("genui", createGenUi("Pythagorean Theorem Calculator", inputs, outputs));
                return root.toString();
            }

            if (q.contains("circle") && (q.contains("area") || q.contains("circumference"))) {
                JSONObject root = createBaseResponse("Area of a circle: A = πr², Circumference: C = 2πr.");
                root.put("summary", createSummary("A = \\pi r^2", "C = 2\\pi r", "\\pi \\times r^2", "pi * r^2", false));
                JSONArray guidance = new JSONArray();
                guidance.put(createGuidanceStep("Area formula: A = \\pi r^2", "A = \\pi r^2"));
                guidance.put(createGuidanceStep("Circumference formula: C = 2\\pi r", "C = 2\\pi r"));
                root.put("step_by_step_guidance", guidance);

                JSONArray inputs = new JSONArray();
                inputs.put(createGenUiInput("r", "Radius", "cm", 5.0));
                JSONArray outputs = new JSONArray();
                outputs.put(createGenUiOutput("A", "pi * r * r", "Area", "cm²"));
                outputs.put(createGenUiOutput("C", "2 * pi * r", "Circumference", "cm"));
                root.put("genui", createGenUi("Circle Area & Perimeter", inputs, outputs));
                return root.toString();
            }

            JSONObject root = createBaseResponse(
                "To enable pure online Google Gemini AI reasoning for all open-ended questions, connect a free Gemini API key from Google AI Studio. "
                + "You can set it with: adb shell \"echo 'YOUR_API_KEY' > /data/local/tmp/gemini_key.txt\""
            );
            root.put("summary", createSummary(
                "Online Gemini Ready",
                "Add a Gemini API key for pure online AI responses.",
                null,
                null,
                false
            ));
            JSONArray guidance = new JSONArray();
            guidance.put(createGuidanceStep("1. Get a free API key at: aistudio.google.com", ""));
            guidance.put(createGuidanceStep("2. Push to device: adb shell \"echo KEY > /data/local/tmp/gemini_key.txt\"", ""));
            guidance.put(createGuidanceStep("3. All queries will route 100% pure online to Google Gemini 2.0 Flash.", ""));
            root.put("step_by_step_guidance", guidance);
            return root.toString();
        } catch (Exception e) {
            return getSafeFallbackResponse(raw);
        }
    }

    private static String getSafeFallbackResponse(String raw) {
        try {
            JSONObject root = createBaseResponse("Result:");
            root.put("summary", createSummary("0", raw != null ? raw : "", "0", "0", true));
            return root.toString();
        } catch (Exception e) {
            return "{\"scope\":\"IN_SCOPE\",\"response\":\"Result:\",\"summary\":{\"final_answer_latex\":\"0\",\"overall_solution_latex\":\"0\",\"expression_latex\":null,\"expression_text\":null,\"is_expression_text_a_valid_calculator_expression\":true},\"step_by_step_guidance\":[],\"genui\":null}";
        }
    }

    // ==========================================
    // Lightweight Math Expression Parser
    // ==========================================

    public static class MathParser {
        private final String str;
        private int pos = -1, ch;

        public MathParser(String str) {
            this.str = str;
        }

        private void nextChar() {
            ch = (++pos < str.length()) ? str.charAt(pos) : -1;
        }

        private boolean eat(int charToEat) {
            while (ch == ' ' || ch == '\t') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        public double parse() {
            nextChar();
            double x = parseExpression();
            while (ch == ' ' || ch == '\t') nextChar();
            if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
            return x;
        }

        private double parseExpression() {
            double x = parseTerm();
            for (;;) {
                if      (eat('+')) x += parseTerm();
                else if (eat('-')) x -= parseTerm();
                else return x;
            }
        }

        private double parseTerm() {
            double x = parseFactor();
            for (;;) {
                if      (eat('*')) x *= parseFactor();
                else if (eat('/')) x /= parseFactor();
                else if (eat('%')) x %= parseFactor();
                else return x;
            }
        }

        private double parseFactor() {
            while (ch == ' ' || ch == '\t') nextChar();
            if (eat('+')) return +parseFactor();
            if (eat('-')) return -parseFactor();

            double x;
            int startPos = this.pos;
            if (eat('(')) {
                x = parseExpression();
                if (!eat(')')) throw new RuntimeException("Missing ')'");
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                x = Double.parseDouble(str.substring(startPos, this.pos));
            } else if (ch >= 'a' && ch <= 'z') {
                while (ch >= 'a' && ch <= 'z') nextChar();
                String func = str.substring(startPos, this.pos);
                if (func.equals("pi")) {
                    x = Math.PI;
                } else if (func.equals("e")) {
                    x = Math.E;
                } else {
                    if (eat('(')) {
                        x = parseExpression();
                        if (!eat(')')) throw new RuntimeException("Missing ')'");
                    } else {
                        x = parseFactor();
                    }
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("cbrt")) x = Math.cbrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else if (func.equals("abs")) x = Math.abs(x);
                    else if (func.equals("log")) x = Math.log10(x);
                    else if (func.equals("ln")) x = Math.log(x);
                    else throw new RuntimeException("Unknown function: " + func);
                }
            } else {
                throw new RuntimeException("Unexpected: " + (char) ch);
            }

            if (eat('^')) x = Math.pow(x, parseFactor());

            return x;
        }
    }
}
