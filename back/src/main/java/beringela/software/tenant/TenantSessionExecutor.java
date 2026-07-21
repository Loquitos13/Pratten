package beringela.software.tenant;

import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import java.util.function.Function;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.stereotype.Component;

/**
 * Runs work inside a Hibernate session bound to an explicit tenant.
 *
 * <p>Needed by flows that only learn the tenant mid-request (login, onboarding):
 * with {@code open-in-view} enabled, the request-scoped session is opened before
 * the tenant is known and would otherwise stamp/filter under the sentinel tenant.
 * This opens a dedicated, short-lived session with the correct tenant identifier,
 * independent of the request-bound one.
 */
@Component
public class TenantSessionExecutor {

    private final SessionFactory sessionFactory;

    public TenantSessionExecutor(EntityManagerFactory entityManagerFactory) {
        this.sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    }

    public <T> T inTenant(UUID tenantId, Function<Session, T> work) {
        try (Session session = sessionFactory.withOptions().tenantIdentifier(tenantId).openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                T result = work.apply(session);
                transaction.commit();
                return result;
            } catch (RuntimeException ex) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw ex;
            }
        }
    }
}
