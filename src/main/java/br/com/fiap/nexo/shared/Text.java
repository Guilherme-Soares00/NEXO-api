package br.com.fiap.nexo.shared;

public final class Text {
    private Text() {}
    public static String trim(String value) { return value == null ? null : value.trim(); }
}
