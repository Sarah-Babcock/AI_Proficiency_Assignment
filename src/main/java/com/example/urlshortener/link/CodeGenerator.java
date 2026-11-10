package com.example.urlshortener.link;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Creates random short codes that are easy to read aloud over the phone.
 * Characters that are easily confused are left out: 0 O o 1 l I.
 */
@Component
public class CodeGenerator {

    public static final int CODE_LENGTH = 6;

    /** a-z, A-Z and 2-9, minus 0 O o 1 l I (56 characters). */
    static final String ALPHABET =
            "abcdefghijkmnpqrstuvwxyz"
            + "ABCDEFGHJKLMNPQRSTUVWXYZ"
            + "23456789";

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
