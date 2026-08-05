package beringela.software.security;

/** Publicado quando uma conta fica bloqueada por demasiadas tentativas de login. */
public record LoginLockoutEvent(String accountKey, String scope) {
}
