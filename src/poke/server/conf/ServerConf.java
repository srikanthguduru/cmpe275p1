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
package poke.server.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routing information for the server - internal use only
 * 
 * TODO refactor StorageEntry to be neutral for cache, file, and db
 * 
 * @author gash
 * 
 */
@XmlRootElement(name = "conf")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServerConf {

	protected static Logger logger = LoggerFactory.getLogger(ServerConf.class.getName());

	private List<GeneralConf> server;
	private List<ResourceConf> resource;
	private List<RouteConf> route;
	private List<DatasourceConf> datasource;

	private volatile HashMap<String, GeneralConf> idToSvr;
	private volatile HashMap<String, RouteConf> idToRte;
	private volatile HashMap<Integer, ResourceConf> idToRsc;
	private volatile HashMap<String, DatasourceConf> idToDB;

	
	// Setter & Getters 
	public List<GeneralConf> getServer(){
		return server;
	}

	public void setServer(List<GeneralConf> server) {
		this.server = server;
	}
	
	public List<RouteConf> getRoute() {
		return route;
	}

	public void setRoute(List<RouteConf> route) {
		this.route = route;
	}

	public List<ResourceConf> getResource() {
		return resource;
	}

	public void setResource(List<ResourceConf> conf) {
		this.resource = conf;
	}
	
	public List<DatasourceConf> getDatasource() {
		return datasource;
	}

	public void setDatasource(List<DatasourceConf> datasoruce) {
		this.datasource = datasoruce;
	}

	// Add Method
	public void addServer(GeneralConf entry) {
		if (entry == null)
			return;
		else if (server == null)
			server = new ArrayList<GeneralConf>();

		server.add(entry);
	}

	public void addRoute(RouteConf entry) {
		if (entry == null)
			return;
		else if (route == null)
			route = new ArrayList<RouteConf>();

		route.add(entry);
	}

	public void addResource(ResourceConf entry) {
		if (entry == null)
			return;
		else if (resource == null)
			resource = new ArrayList<ResourceConf>();
		resource.add(entry);
	}

	public void addDatasource(DatasourceConf entry) {
		if (entry == null)
			return;
		else if (datasource == null)
			datasource = new ArrayList<DatasourceConf>();
		datasource.add(entry);
	}

	//Find by Node ID
	public GeneralConf findNodeById(String id) {
		return svrAsMap().get(id);
	}

	public RouteConf findRouteById(String id) {
		return rteAsMap().get(id);
	}
	
	public ResourceConf findById(int id) {
		return rscAsMap().get(id);
	}

	public DatasourceConf findByDBId(String id) {
		return dbAsMap().get(id);
	}

	// To HashMap
	public HashMap<String, GeneralConf> svrAsMap() {
		if (idToSvr != null)
			return idToSvr;

		if (idToSvr == null) {
			synchronized (this) {
				if (idToSvr == null) {
					idToSvr = new HashMap<String, GeneralConf>();
					if (server != null) {
						for (GeneralConf entry : server) {
							idToSvr.put(entry.nodeId, entry);
						}
					}
				}
			}
		}
		return idToSvr;
	}

	private HashMap<String, RouteConf> rteAsMap() {
		if (idToRte != null)
			return idToRte;

		if (idToRte == null) {
			synchronized (this) {
				if (idToRte == null) {
					idToRte = new HashMap<String, RouteConf>();
					if (resource != null) {
						for (RouteConf entry : route) {
							idToRte.put(entry.nodeId, entry);
						}
					}
				}
			}
		}
		return idToRte;
	}
	
	public HashMap<Integer, ResourceConf> rscAsMap() {
		if (idToRsc != null)
			return idToRsc;

		if (idToRsc == null) {
			synchronized (this) {
				if (idToRsc == null) {
					idToRsc = new HashMap<Integer, ResourceConf>();
					if (resource != null) {
						for (ResourceConf entry : resource) {
							idToRsc.put(entry.id, entry);
						}
					}
				}
			}
		}

		return idToRsc;
	}

	public HashMap<String, DatasourceConf> dbAsMap() {
		if (idToDB != null)
			return idToDB;

		if (idToDB == null) {
			synchronized (this) {
				if (idToDB == null) {
					idToDB = new HashMap<String, DatasourceConf>();
					if (datasource != null) {
						for (DatasourceConf entry : datasource) {
							idToDB.put(entry.site, entry);
						}
					}
				}
			}
		}
		return idToDB;
	}
		
	@XmlRootElement(name = "entry")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class GeneralConf {
		private String nodeId;
		private String host;
		private int port;
		private int portMgmt;
		private String storage;
		
		public GeneralConf() {		
		}
		
		public String getNodeId() {
			return nodeId;
		}
		public void setNodeId(String nodeID) {
			this.nodeId = nodeID;
		}
		public int getPort() {
			return port;
		}
		public void setPort(int port) {
			this.port = port;
		}
		public int getPortMgmt() {
			return portMgmt;
		}
		public void setPortMgmt(int portMgmt) {
			this.portMgmt = portMgmt;
		}
		public String getHost() {
			return host;
		}
		public void setHost(String host) {
			this.host = host;
		}
		public String getStorage() {
			return storage;
		}
		public void setStorage(String storage) {
			this.storage = storage;
		}
	}

	@XmlRootElement(name = "entry")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class RouteConf {
		private String nodeId;
		private List<String> connected = new ArrayList<String>();

		public String getNodeId() {
			return nodeId;
		}
		public void setNodeId(String nodeId) {
			this.nodeId = nodeId;
		}
		public List<String> getConnected() {
			return connected;
		}
		public void setConnected(List<String> connected) {
			this.connected = connected;
		}	
	}
	
	@XmlRootElement(name = "entry")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class ResourceConf {
		private int id;
		private String name;
		private String clazz;
		private boolean enabled;

		public ResourceConf() {
		}

		public ResourceConf(int id, String name, String clazz) {
			this.id = id;
			this.name = name;
			this.clazz = clazz;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getClazz() {
			return clazz;
		}

		public void setClazz(String clazz) {
			this.clazz = clazz;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
	}

	@XmlRootElement(name = "entry")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class DatasourceConf {
		private String site;
		private String sUrl;
		private String sUser;
		private String sPass;
		
		public DatasourceConf() {
		}
		
		public String getSite() {
			return site;
		}
		public void setSite(String site) {
			this.site = site;
		}
		public String getsUrl() {
			return sUrl;
		}
		public void setsUrl(String sUrl) {
			this.sUrl = sUrl;
		}
		public String getsUser() {
			return sUser;
		}
		public void setsUser(String sUser) {
			this.sUser = sUser;
		}
		public String getsPass() {
			return sPass;
		}
		public void setsPass(String sPass) {
			this.sPass = sPass;
		}
	}
}
