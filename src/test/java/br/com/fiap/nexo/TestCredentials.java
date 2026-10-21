package br.com.fiap.nexo;

final class TestCredentials {
    static final String TOKEN = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    static final String TOKEN_PROPERTY = "NEXO_ADMIN_TOKEN=" + TOKEN;
    static final String AUTHORIZATION = "Bearer " + TOKEN;

    private TestCredentials() {}
}
