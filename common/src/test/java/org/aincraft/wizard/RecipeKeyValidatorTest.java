package org.aincraft.wizard;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecipeKeyValidatorTest {

    @Test
    void validate_empty_fails() {
        assertFalse(RecipeKeyValidator.isValid(""));
    }

    @Test
    void validate_specialChars_fails() {
        assertFalse(RecipeKeyValidator.isValid("my potion!"));
        assertFalse(RecipeKeyValidator.isValid("my_potion"));
    }

    @Test
    void validate_valid_passes() {
        assertTrue(RecipeKeyValidator.isValid("my-potion"));
        assertTrue(RecipeKeyValidator.isValid("speed2"));
        assertTrue(RecipeKeyValidator.isValid("a"));
    }

    @Test
    void normalize_lowercasesAndReplacesSpaces() {
        assertEquals("my-potion", RecipeKeyValidator.normalize("My Potion"));
        assertEquals("speed2", RecipeKeyValidator.normalize("SPEED2"));
    }

    @Test
    void validate_hexColor_valid() {
        assertTrue(RecipeKeyValidator.isValidHex("FF4500"));
        assertTrue(RecipeKeyValidator.isValidHex("ff4500"));
        assertTrue(RecipeKeyValidator.isValidHex("000000"));
    }

    @Test
    void validate_hexColor_invalid() {
        assertFalse(RecipeKeyValidator.isValidHex("#FF4500")); // leading #
        assertFalse(RecipeKeyValidator.isValidHex("FF450"));   // 5 chars
        assertFalse(RecipeKeyValidator.isValidHex("FF450G"));  // non-hex char
    }
}
