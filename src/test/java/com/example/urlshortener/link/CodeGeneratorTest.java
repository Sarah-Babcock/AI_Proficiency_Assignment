package com.example.urlshortener.link;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeGeneratorTest {

    private final CodeGenerator generator = new CodeGenerator();

    @Test
    void everyCodeIsSixCharacters() {
        for (int i = 0; i < 1000; i++) {
            assertThat(generator.generate()).hasSize(6);
        }
    }

    @Test
    void noCodeContainsConfusingCharacters() {
        for (int i = 0; i < 1000; i++) {
            assertThat(generator.generate())
                    .doesNotContain("0", "O", "o", "1", "l", "I");
        }
    }
}
