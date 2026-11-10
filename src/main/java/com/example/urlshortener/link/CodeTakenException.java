package com.example.urlshortener.link;

/** Thrown when an agent asks for a custom code that another link already uses. */
public class CodeTakenException extends RuntimeException {

    private final String code;

    public CodeTakenException(String code) {
        this(code, "The code \"" + code + "\" is already taken. Please choose another.");
    }

    private CodeTakenException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** For codes the app itself uses as page addresses, like "dashboard". */
    public static CodeTakenException reserved(String code) {
        return new CodeTakenException(code,
                "The code \"" + code + "\" is reserved by the app. Please choose another.");
    }

    public String getCode() {
        return code;
    }
}
