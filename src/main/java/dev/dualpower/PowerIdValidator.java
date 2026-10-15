package dev.dualpower;
import java.util.regex.Pattern;
/** Reject malformed identifiers before any server-side Awakened API call. */
public final class PowerIdValidator {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private PowerIdValidator() {}
    public static String requireValid(String value) {
        if (value == null || !ID.matcher(value).matches())
            throw new IllegalArgumentException("Invalid namespaced power ID: " + value);
        return value;
    }
}
