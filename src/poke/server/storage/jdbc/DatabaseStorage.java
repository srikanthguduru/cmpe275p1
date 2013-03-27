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
import java.util.Properties;

import org.postgis.PGgeometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.conf.ServerConf;
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

public class DatabaseStorage implements Storage {
	protected static Logger logger = LoggerFactory.getLogger("database");

	protected BoneCP cpool;
	protected String schema;

	protected DatabaseStorage() {
	}

	public DatabaseStorage(ServerConf conf, String siteid) {
		init(conf, siteid);
	}

	@Override
	public void init(Properties cnf) {

	}

	public void init(ServerConf conf, String siteid) {
		if (cpool != null)
			return;

		BoneCPConfig config ;

		try {
			DatasourceConf ds = conf.findByDBId(siteid);

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
	public List<NameSpace> findNameSpaces(NameSpace criteria) {
		List<NameSpace> list = null;

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			// TODO complete code to search through JDBC/SQL
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on find", ex);
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

		return list;
	}

	@Override
	public NameSpace createNameSpace(NameSpace space) {
		if (space == null)
			return space;

		String insert = "INSERT INTO " + schema + ".userdata (user_id, name, city, zip_code, password) VALUES ('" + space.getUserId() + "', '" 
				+ space.getName() + "', '" + space.getCity() + "', '" + space.getZipCode() + "'," + space.getPassword() + "');";

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

			Statement stmt = conn.createStatement();
			logger.debug("Creating user " + space.getUserId() + " - " + space.getName());

			stmt.executeUpdate(insert);

			// TODO complete code to use JDBC
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

	public boolean validateLogin(LoginInfo loginInfo){
		if (loginInfo == null)
			return false;

		boolean valid = false;

		String select = "SELECT * FROM " + schema + ".userdata WHERE user_id = '" + loginInfo.getUserId()
				+ "' AND password = '" + loginInfo.getPassword() + "';";

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

			Statement stmt = conn.createStatement();
			logger.debug("Validating user " + loginInfo.getUserId());

			ResultSet rs = stmt.executeQuery(select);

			if(rs != null)
				valid = true;

			// TODO complete code to use JDBC
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("failed/exception on validating user " + loginInfo, ex);
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
		return valid;
	}
	@Override
	public boolean removeNameSpace(String userId) {
		String delete = "DELETE FROM  " + schema + ".userdata WHERE user_id = '" + userId + "';";

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

			Statement stmt = conn.createStatement();
			logger.debug("Deleting user - " + userId);

			stmt.executeUpdate(delete);

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

		String insert = "INSERT INTO " + schema + ".image (id, file_name, geom, data, user_id, file_type, time) VALUES (" 
				+ doc.getId() + ", '" + doc.getFileName() + "', 'POINT (" + location.getX() + " " + location.getY() + ")', '"
				+ doc.getImgByte() + "','" + namespace  + "', '" + doc.getFileName() + "','" + doc.getTime() + "');";

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

			Statement stmt = conn.createStatement();
			logger.debug("Creating image - " +  doc.getId());

			stmt.executeUpdate(insert);

			// TODO complete code to use JDBC
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
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

			Statement stmt = conn.createStatement();
			logger.debug("Deleting image - " + docId);

			stmt.executeUpdate(delete);

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
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

			Statement stmt = conn.createStatement();
			logger.debug("Updating image - " +  doc.getId());

			stmt.executeUpdate(update);

			// TODO complete code to use JDBC
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
		List<Document> images = new ArrayList<Document>();
		select = new StringBuffer("SELECT * FROM " + schema + ".image WHERE user_id = '" + criteria.getUserId()
				+ "'");
		if(criteria.getName()!= null)
			select.append(", file_name = '" + criteria.getName() + "'");
		if(criteria.getTime() != -1){
			java.sql.Date imgTime = new Date(criteria.getTime());
			select.append(", time = '" + imgTime + "'");
		}
		select.append(" AND geom = 'POINT(" + criteria.getLocation().getX() + " " + criteria.getLocation().getY() 
				+ ")' ;");

		Connection conn = null;
		try {
			conn = cpool.getConnection();
			conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

			Statement stmt = conn.createStatement();
			logger.debug("Querying files done by user" + namespace);

			ResultSet rs = stmt.executeQuery(select.toString());

			Document image;
			while(rs.next()){

				ByteString bytes = ByteString.copyFrom(rs.getBytes("data"));

				PGgeometry geom = (PGgeometry)rs.getObject("geom"); 
				long x = 0, y = 0;
				if(geom.getGeometry().getPoint(0) != null)
				{
					x = (long)geom.getGeometry().getPoint(0).getX();
					y = (long)geom.getGeometry().getPoint(0).getY();
				}

				Point point = Point.newBuilder().setX(x)
						.setY(y).build();
				java.sql.Date date = rs.getDate("time");
				long time = -1L;

				if (date != null){
					time = date.getTime();
				}
				image = Document.newBuilder()
						.setId(rs.getLong("id"))
						.setFileName(rs.getString("file_name"))
						.setFileType(rs.getString("file_type"))
						.setImgByte(bytes)
						.setLocation(point)
						.setTime(time).build();
						
						images.add(image);
			}

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
