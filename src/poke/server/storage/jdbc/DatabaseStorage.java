/*
 * copyright 2012, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package poke.server.storage.jdbc;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.postgis.PGgeometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.conf.ServerConf.DatasourceConf;
import poke.server.storage.Storage;

import com.google.protobuf.ByteString;
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

import eye.Comm.Document;
import eye.Comm.LoginInfo;
import eye.Comm.NameSpace;
import eye.Comm.Point;
import eye.Comm.QueryInfo;
import eye.Comm.QueryNamespace;

public class DatabaseStorage implements Storage {
	protected static Logger logger = LoggerFactory.getLogger("database");

	protected BoneCP cpool;
	protected String schema;

	public DatabaseStorage() {
	}

	@Override
	public void init(DatasourceConf ds) {
		if (cpool != null)
			return;

		BoneCPConfig config ;
		try {
			this.schema = ds.getSchema();

			Class.forName("org.postgresql.Driver");
			config = new BoneCPConfig();
			config.setJdbcUrl(ds.getUrl());
			config.setUsername(ds.getUser());
			config.setPassword(ds.getPass());
			config.setMinConnectionsPerPartition(5);
			config.setMaxConnectionsPerPartition(10);
			config.setPartitionCount(1);

			cpool = new BoneCP(config);	

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gash.jdbc.repo.Repository#release()
	 */
	@Override
	public void release() {
		if (cpool == null)
			return;

		cpool.shutdown();
		cpool = null;
	}

	@Override
	public NameSpace getNameSpaceInfo(String userId) {
		NameSpace space = null;

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			// TODO complete code to retrieve through JDBC/SQL
			// select * from space where id = spaceId
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on looking up space " + userId, ex);
			try {
				conn.rollback();
			} catch (SQLException e) {
			}
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return space;
	}

	@Override
	public List<NameSpace> findNameSpaces(String namespace, QueryNamespace criteria) {
		if (criteria == null)
			return null;

		StringBuffer select = null;
		StringBuffer searchClauseBuffer = new StringBuffer();
		List<NameSpace> users = new ArrayList<NameSpace>();
		int queryParamCount = 0;

		String userId = criteria.getUserId();
		String name = criteria.getName();
		String city = criteria.getCity();
		String zipCode = criteria.getZipCode();

		select = new StringBuffer("SELECT * FROM " + schema + ".user_data WHERE ");

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

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

			Statement stmt = conn.createStatement();
			logger.debug("Querying users - operation done by user: " + namespace);

			ResultSet rs = stmt.executeQuery(select.toString());

			NameSpace user;
			while(rs.next()){

				user = NameSpace.newBuilder()
						.setUserId(rs.getString("user_id"))
						.setName(rs.getString("name"))
						.setCity(rs.getString("city"))
						.setZipCode(rs.getString("zip_code")).build();

				users.add(user);
			}
			conn.commit();

		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on querying namepaces by user " + namespace, ex);
			try {
				conn.rollback();
			} catch (SQLException e) {

			}
			return null;
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return users;
	}

	@Override
	public NameSpace createNameSpace(NameSpace space) {
		if (space == null)
			return space;

		String insert = "INSERT INTO " + schema + ".userdata (user_id, name, city, zip_code, password) VALUES ('" + space.getUserId() + "', '" 
				+ space.getName() + "', '" + space.getCity() + "', '" + space.getZipCode() + "','" + space.getPassword() + "');";

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

			Statement stmt = conn.createStatement();
			logger.debug("Creating user " + space.getUserId() + " - " + space.getName());

			stmt.executeUpdate(insert);

			conn.commit();
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on creating space " + space, ex);
			try {
				conn.rollback();
			} catch (SQLException e) {
			}

			// indicate failure
			return null;
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return space;
	}

	public String validateLogin(LoginInfo loginInfo){
		if (loginInfo == null)
			return null;
		
		String uuid = null;

		String select = "SELECT * FROM " + schema + ".userdata WHERE user_id = '" + loginInfo.getUserId()
				+ "' AND password = '" + loginInfo.getPassword() + "';";

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

			Statement stmt = conn.createStatement();
			logger.debug("Validating user " + loginInfo.getUserId());

			ResultSet rs = stmt.executeQuery(select);

			if(rs != null)
				uuid = UUID.randomUUID().toString();
			
			conn.commit();
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on validating user " + loginInfo, ex);
			try {
				conn.rollback();
			} catch (SQLException e) {
			}

			// indicate failure
			return null;
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return uuid;
	}
	@Override
	public boolean removeNameSpace(String userId) {
		String delete = "DELETE FROM  " + schema + ".userdata WHERE user_id = '" + userId + "';";

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

			Statement stmt = conn.createStatement();
			logger.debug("Deleting user - " + userId);

			stmt.executeUpdate(delete);
			conn.commit();

		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on deleting user " + userId, ex);
			try {
				conn.rollback();
			} catch (SQLException e) {
			}

			// indicate failure
			return false;
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return true;
	}

	@Override
	public boolean addDocument(String namespace, Document doc) {
		if (namespace == null)
			return false;

		if(doc == null || doc.getLocation() == null)
			return false;

		Point location = doc.getLocation();

		String insert = "INSERT INTO " + schema + ".image ( file_name, geom, data, user_id, file_type, img_time) VALUES ('" 
				+ doc.getFileName() + "', 'POINT (" + location.getX() + " " + location.getY() + ")', '"
				+ doc.getImgByte() + "','" + namespace  + "', '" + doc.getFileName() + "','" + doc.getTime() + "');";

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

			Statement stmt = conn.createStatement();
			logger.debug("Creating image - " +  doc.getId());

			stmt.executeUpdate(insert);
			conn.commit();
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on creating image " + doc.getId(), ex);
			try {
				conn.rollback();
			} catch (SQLException e) {
			}

			// indicate failure
			return false;
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return true;
	}

	@Override
	public boolean removeDocument(String namespace, long docId) {
		String delete = "DELETE FROM  " + schema + ".image WHERE id = " + docId + ";";

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

			Statement stmt = conn.createStatement();
			logger.debug("Deleting image - " + docId);

			stmt.executeUpdate(delete);
			conn.commit();

		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on deleting image " + docId, ex);
			try {
				conn.rollback();
			} catch (SQLException e) {
			}

			// indicate failure
			return false;
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return true;
	}

	@Override
	public boolean updateDocument(String namespace, Document doc) {
		if (namespace == null)
			return false;

		if(doc == null || doc.getLocation() == null)
			return false;

		Point location = doc.getLocation();

		String update = "UPDATE " + schema + ".image SET file_name = '" + doc.getFileName() + "', geom = 'POINT (" + location.getX() + " "
				+ location.getY() + ")', data = '" + doc.getImgByte() + "', file_type = '" + doc.getFileType() + "' WHERE id = " + doc.getId();

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

			Statement stmt = conn.createStatement();
			logger.debug("Updating image - " +  doc.getId());

			stmt.executeUpdate(update);
			conn.commit();
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on updating image " + doc.getId(), ex);
			try {
				conn.rollback();
			} catch (SQLException e) {
			}

			// indicate failure
			return false;
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return true;
	}

	@Override
	public List<Document> findDocuments(String namespace, QueryInfo criteria) {
		if (criteria == null || criteria.getLocation() == null)
			return null;

		StringBuffer select = null;
		StringBuffer searchClauseBuffer = new StringBuffer();
		int queryParamCount = 0;

		String userId = criteria.getUserId();
		String fileName = criteria.getName();
		long time = criteria.getTime();

		List<Document> images = new ArrayList<Document>();
		select = new StringBuffer("SELECT * FROM " + schema + ".image WHERE " );

		if(userId != null)
		{
			if(queryParamCount > 0)
				searchClauseBuffer.append(" OR ");

			queryParamCount++;
			select.append("user_id = '" + userId + "'");
		}
		if(fileName != null){
			if(queryParamCount > 0)
				searchClauseBuffer.append(" OR ");

			queryParamCount++;
			select.append(", file_name = '" + criteria.getName() + "'");
		}			
		if(time != -1){
			if(queryParamCount > 0)
				searchClauseBuffer.append(" OR ");

			queryParamCount++;
			java.sql.Date imgTime = new Date(criteria.getTime());
			select.append(", img_time = '" + imgTime + "'");
		}
		select.append(" AND geom = 'POINT(" + criteria.getLocation().getX() + " " + criteria.getLocation().getY() 
				+ ")' ;");

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

			Statement stmt = conn.createStatement();
			logger.debug("Querying files done by user" + namespace);

			ResultSet rs = stmt.executeQuery(select.toString());

			Document image;
			while(rs.next()){

				ByteString bytes = ByteString.copyFrom(rs.getBytes("data"));

				PGgeometry geom = (PGgeometry)rs.getObject("geom"); 
				double x = 0, y = 0;
				if(geom.getGeometry().getPoint(0) != null)
				{
					x = geom.getGeometry().getPoint(0).getX();
					y = geom.getGeometry().getPoint(0).getY();
				}

				Point point = Point.newBuilder().setX(x)
						.setY(y).build();
				java.sql.Date date = rs.getDate("img_time");
				long timeFromDB = -1L;

				if (date != null){
					timeFromDB = date.getTime();
				}
				image = Document.newBuilder()
						.setId(rs.getLong("id"))
						.setFileName(rs.getString("file_name"))
						.setFileType(rs.getString("file_type"))
						.setImgByte(bytes)
						.setLocation(point)
						.setTime(timeFromDB).build();

				images.add(image);
			}
			conn.commit();

		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on querying image by user " + namespace, ex);
			try {
				conn.rollback();
			} catch (SQLException e) {

			}
			return null;
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return images;
	}
}
