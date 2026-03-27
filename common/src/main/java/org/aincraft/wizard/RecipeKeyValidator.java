package org.aincraft.wizard;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validates recipe keys and color hex values.
 * <p>
 * Recipe keys must be lowercase alphanumeric with hyphens.
 * Hex colors must be exactly 6 hexadecimal digits without '#' prefix.
 */
public final class RecipeKeyValidator {

    private static final Pattern KEY_PATTERN =
        Pattern.compile("^[a-z0-9-]+$");
    private static final Pattern HEX_PATTERN =
        Pattern.compile("^[0-9a-fA-F]{6}$");

    private RecipeKeyValidator() {}

    /**
     * Checks if a key is valid (lowercase alphanumeric with hyphens).
     *
     * @param key the key to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String key) {
        return key != null && !key.isEmpty() && KEY_PATTERN.matcher(key).matches();
    }

    /**
     * Normalizes input to a valid key by lowercasing and replacing spaces with hyphens.
     *
     * @param input the input string
     * @return normalized key
     */
    public static String normalize(String input) {
        return input.toLowerCase(Locale.ENGLISH).replace(' ', '-');
    }

    /**
     * Checks if a hex string is valid (6 hexadecimal digits, no '#' prefix).
     *
     * @param hex the hex string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidHex(String hex) {
        return hex != null && HEX_PATTERN.matcher(hex).matches();
    }
}
