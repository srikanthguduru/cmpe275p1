package poke.server.storage.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.entity.User;
import poke.server.conf.ServerConf.GeneralConf;

public class JPAStorage {
	protected static Logger logger = LoggerFactory.getLogger("JPAStorage");
	private static EntityManagerFactory entityManagerFactory;
	private static InheritableThreadLocal<EntityManager> entityManagerThreadLocal = new InheritableThreadLocal<EntityManager>();

	public void init(GeneralConf gcf){
		entityManagerFactory = Persistence
				.createEntityManagerFactory(gcf.getNodeId());
	}

	public User createNameSpace(User user) {
		EntityManager manager  = getEntityManager();
		try{
			manager.getTransaction().begin();
			manager.persist(user);

			manager.getTransaction().commit();
		}
		catch(Exception ex){
			logger.error("Error occurred while creating user " , ex);
			return null;
		}
		finally{
			manager.close();
		}
		return 	user;
	}

	public List<User> findNameSpaces(String originator, User criteria) {
		List<User> users = new ArrayList<User>();
		EntityManager manager = getEntityManager();
		try{
			
			StringBuffer select = null;
			StringBuffer searchClauseBuffer = new StringBuffer();

			int queryParamCount = 0;

			String userId = criteria.getUserId();
			String name = criteria.getName();
			String city = criteria.getCity();
			String zipCode = criteria.getZipCode();

			select = new StringBuffer("SELECT * FROM user_data WHERE ");

			if(userId != null)
			{
				if(queryParamCount > 0)
					searchClauseBuffer.append(" OR ");

				queryParamCount++;
				select.append("user_id = '" + userId + "'");
			}
			if(name != null)
			{
				if(queryParamCount > 0)
					searchClauseBuffer.append(" OR ");

				queryParamCount++;
				select.append("name = '" + name + "'");
			}
			if(city != null)
			{
				if(queryParamCount > 0)
					searchClauseBuffer.append(" OR ");

				queryParamCount++;
				select.append("city = '" + city + "'");
			}
			if(zipCode != null)
			{
				if(queryParamCount > 0)
					searchClauseBuffer.append(" OR ");

				queryParamCount++;
				select.append("zip_code = '" + zipCode + "'");
			}
			logger.debug("Select user query " + select.toString());
			manager.getTransaction().begin();
			users = manager.createQuery(select.toString()).getResultList();
			manager.getTransaction().commit();
		}
		catch(Exception ex){
			logger.error("Error occurred while searching for user " , ex);
			return null;
		}
		finally{
			manager.close();
		}
		return users;
	}


	public boolean removeNameSpace(String userId) {
		EntityManager manager = getEntityManager();
		try{
			User user;
			user = manager.getReference(User.class, userId);
			manager.getTransaction().begin();
			manager.remove(user);
			manager.getTransaction().commit();
		}
		catch(Exception ex){
			logger.error("Error occurred while deleting user " , ex);
			return false;
		}
		finally{
			manager.close();
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public String validateLogin(User user){
		if (user == null)
			return null;

		EntityManager manager = getEntityManager();
		String uuid = null;
		List<User> users = new ArrayList<User>();

		String select = "SELECT * FROM userdata WHERE user_id :userId AND password = :pswd";

		try {
			manager.getTransaction().begin();
			users = manager.createQuery(select)
					.setParameter("userId", user.getUserId())
					.setParameter("pswd", user.getPassword())
					.getResultList();

			if(users.size() > 0 )
				uuid = UUID.randomUUID().toString();

			manager.getTransaction().commit();
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on validating user ", ex);
			// indicate failure
			return null;
		}
		finally{
			manager.close();
		}
		return uuid;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}
	/**
	 * Get entity manager from thread
	 * 
	 * @return entity manager
	 */
	public EntityManager getEntityManager() {
		if (entityManagerThreadLocal.get() == null || entityManagerThreadLocal.get().isOpen() == false) {
			entityManagerThreadLocal.set(entityManagerFactory
					.createEntityManager());
		}
		return entityManagerThreadLocal.get();
	}
}
