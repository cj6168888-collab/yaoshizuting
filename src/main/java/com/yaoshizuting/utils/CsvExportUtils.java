package com.yaoshizuting.utils;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CsvExportUtils {

    private CsvExportUtils() {
    }

    public static byte[] toCsv(List<String> headers, List<? extends List<?>> rows) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        appendRow(csv, headers);
        for (List<?> row : rows) {
            appendRow(csv, row);
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendRow(StringBuilder csv, List<?> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(values.get(i)));
        }
        csv.append('\n');
    }

    private static String escape(Object value) {
        String text = value == null ? "" : value.toString();
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
