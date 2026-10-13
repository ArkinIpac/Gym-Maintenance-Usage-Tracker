// Json.java
// Part of: Arkin Ipac  (Integration / web version)
// What it does: Builds JSON text by hand for the web API, so we don't need an
//               extra library (Gson, Jackson...). Only the few things we use.
// Used by: ApiRoutes

package web;

import model.Equipment;
import model.MaintenanceRecord;

// A tiny JSON writer. Example:
//   new Json().obj().key("count").val(5000).key("online").val(true).end()
//   -> {"count":5000,"online":true}
// It keeps track of commas for us: every value after the first one in an
// object or array gets a comma in front of it.
public class Json {

    private final StringBuilder sb = new StringBuilder();
    // needsComma[depth] = true once something was written at that depth
    private final boolean[] needsComma = new boolean[32];
    private int depth = 0;
    private boolean afterKey = false;   // a key was just written, so the next value needs no comma

    private void beforeValue() {
        if (afterKey) { afterKey = false; return; }
        if (depth > 0 && needsComma[depth]) sb.append(',');
        if (depth > 0) needsComma[depth] = true;
    }

    // closers[depth] = the bracket that closes what was opened at that depth
    private final char[] closers = new char[32];

    public Json obj() { beforeValue(); sb.append('{'); closers[++depth] = '}'; needsComma[depth] = false; return this; }
    public Json arr() { beforeValue(); sb.append('['); closers[++depth] = ']'; needsComma[depth] = false; return this; }
    public Json end() { sb.append(closers[depth--]); return this; }

    public Json key(String k) {
        if (needsComma[depth]) sb.append(',');
        needsComma[depth] = true;
        str(k);
        sb.append(':');
        afterKey = true;
        return this;
    }

    public Json val(String s) { beforeValue(); if (s == null) sb.append("null"); else str(s); return this; }
    public Json val(long n) { beforeValue(); sb.append(n); return this; }
    public Json val(double d) { beforeValue(); sb.append(Double.isFinite(d) ? String.valueOf(d) : "null"); return this; }
    public Json val(boolean b) { beforeValue(); sb.append(b); return this; }

    // Shortcuts: key + value in one call.
    public Json put(String k, String v) { return key(k).val(v); }
    public Json put(String k, long v) { return key(k).val(v); }
    public Json put(String k, double v) { return key(k).val(v); }
    public Json put(String k, boolean v) { return key(k).val(v); }

    // One equipment as a JSON object (used by almost every endpoint).
    public Json equipment(Equipment e) {
        return obj()
                .put("id", e.getEquipmentId())
                .put("name", e.getName())
                .put("category", e.getCategory().name())
                .put("location", e.getLocation())
                .put("condition", e.getCondition().name())
                .put("usage", e.getUsageCount())
                .end();
    }

    public Json record(MaintenanceRecord r) {
        return obj()
                .put("id", r.getEquipmentId())
                .put("note", r.getNote())
                .put("date", r.getDateServiced().toString())
                .put("before", r.getConditionBefore().name())
                .put("after", r.getConditionAfter().name())
                .end();
    }

    // Writes a string with quotes, escaping the characters JSON doesn't allow raw.
    private void str(String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }

    @Override
    public String toString() { return sb.toString(); }
}
