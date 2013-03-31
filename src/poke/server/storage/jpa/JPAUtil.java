package poke.server.storage.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA Utility used to configure EntityManagerFactory and retrieve it.
 */
public class JPAUtil {

	protected static Logger logger = LoggerFactory.getLogger("JPAUtil");
	private static EntityManagerFactory entityManagerFactory = null;

	private static InheritableThreadLocal<EntityManager> entityManagerThreadLocal = new InheritableThreadLocal<EntityManager>();

	static {
		try {
			entityManagerFactory = Persistence
					.createEntityManagerFactory("BankingApp");
		} catch (Throwable ex) {
			logger.error("Initial EntityManagerFactory creation failed."
					+ ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	/**
	 * Get the configured entityManagerFactory
	 * 
	 * @return entityManagerFactory
	 */
	public static EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	/**
	 * Get entity manager from thread
	 * 
	 * @return entity manager
	 */
	public static EntityManager getEntityManager() {
		if (entityManagerThreadLocal.get() == null || entityManagerThreadLocal.get().isOpen() == false) {
			entityManagerThreadLocal.set(entityManagerFactory
					.createEntityManager());
		}
		return entityManagerThreadLocal.get();
	}
}