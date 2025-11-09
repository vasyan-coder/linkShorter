package com.linkshorter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ShortCodeGeneratorTest {

    private ShortCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ShortCodeGenerator(6);
    }

    @Test
    void testGenerateShortCode() {
        String url = "https://example.com";
        UUID userId = UUID.randomUUID();

        String code = generator.generateShortCode(url, userId);

        assertNotNull(code);
        assertEquals(6, code.length());
    }

    @Test
    void testGenerateShortCodeWithNullUrl() {
        assertThrows(IllegalArgumentException.class, () ->
                generator.generateShortCode(null, UUID.randomUUID())
        );
    }

    @Test
    void testGenerateShortCodeWithEmptyUrl() {
        assertThrows(IllegalArgumentException.class, () ->
                generator.generateShortCode("", UUID.randomUUID())
        );
    }

    @Test
    void testGenerateShortCodeWithNullUserId() {
        assertThrows(IllegalArgumentException.class, () ->
                generator.generateShortCode("https://example.com", null)
        );
    }

    @Test
    void testDifferentUsersGetDifferentCodes() {
        String url = "https://example.com";
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        String code1 = generator.generateShortCode(url, user1);
        String code2 = generator.generateShortCode(url, user2);

        assertNotEquals(code1, code2);
    }

    @Test
    void testSameUserSameUrlGetsSameCode() {
        String url = "https://example.com";
        UUID userId = UUID.randomUUID();

        String code1 = generator.generateShortCode(url, userId);
        String code2 = generator.generateShortCode(url, userId);

        assertEquals(code1, code2);
    }

    @Test
    void testDifferentUrlsGetDifferentCodes() {
        UUID userId = UUID.randomUUID();
        String url1 = "https://example.com";
        String url2 = "https://different.com";

        String code1 = generator.generateShortCode(url1, userId);
        String code2 = generator.generateShortCode(url2, userId);

        assertNotEquals(code1, code2);
    }

    @Test
    void testGenerateMultipleCodesUniqueness() {
        UUID userId = UUID.randomUUID();
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            String url = "https://example" + i + ".com";
            String code = generator.generateShortCode(url, userId);
            codes.add(code);
        }

        // Should generate 100 unique codes
        assertEquals(100, codes.size());
    }

    @Test
    void testIsValidShortCode() {
        assertTrue(generator.isValidShortCode("aBc123"));
        assertTrue(generator.isValidShortCode("XYZxyz"));
        assertTrue(generator.isValidShortCode("123456"));
    }

    @Test
    void testIsInvalidShortCode() {
        assertFalse(generator.isValidShortCode(null));
        assertFalse(generator.isValidShortCode(""));
        assertFalse(generator.isValidShortCode("abc")); // too short
        assertFalse(generator.isValidShortCode("abcdefgh")); // too long
        assertFalse(generator.isValidShortCode("abc@12")); // invalid character
        assertFalse(generator.isValidShortCode("abc 12")); // space
    }

    @Test
    void testDifferentCodeLengths() {
        ShortCodeGenerator gen4 = new ShortCodeGenerator(4);
        ShortCodeGenerator gen8 = new ShortCodeGenerator(8);

        String url = "https://example.com";
        UUID userId = UUID.randomUUID();

        String code4 = gen4.generateShortCode(url, userId);
        String code8 = gen8.generateShortCode(url, userId);

        assertEquals(4, code4.length());
        assertEquals(8, code8.length());
    }

    @Test
    void testInvalidCodeLength() {
        assertThrows(IllegalArgumentException.class, () -> new ShortCodeGenerator(0));
        assertThrows(IllegalArgumentException.class, () -> new ShortCodeGenerator(-1));
    }
}

