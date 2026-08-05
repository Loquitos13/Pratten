package beringela.software.security;

/** Tipo de autenticação presente no JWT. */
public enum PrincipalKind {
    STAFF,
    PLATFORM,
    /** Sessão remota de suporte - superadmin actua como OWNER. */
    REMOTE
}
