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
package poke.server.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import poke.server.conf.ServerConf.DatasourceConf;
import eye.Comm.Document;
import eye.Comm.LoginInfo;
import eye.Comm.ManipulateNS;
import eye.Comm.NameSpace;
import eye.Comm.QueryDocument;

/**
 * A memory-based storage.
 * 
 * @author gash
 * 
 */
public class InMemoryStorage implements Storage {
	private static String sNoName = "";
	private HashMap<String, DataNameSpace> data = new HashMap<String, DataNameSpace>();
	private List<NameSpace> users = new ArrayList<NameSpace>();

	@Override
	public boolean addDocument(String userId, Document doc) {
		if (doc == null)
			return false;

		DataNameSpace dns = null;
		NameSpace ns = null;

		if(userId != null)
			ns = lookupByID(userId);				
		if (ns == null)
			throw new RuntimeException("Unknown namspace: " + userId);

			
		Long key = null;
		if (!doc.hasId()) {
			// note because we store the protobuf instance (read-only)
			key = createKey();
			Document.Builder bldr = Document.newBuilder(doc);
			bldr.setId(key);
			doc = bldr.build();
		}
		if (data.containsKey(userId))
			dns  = data.get(userId);
		else {
			dns = new DataNameSpace();
		}
		dns.add(doc);

		DataNameSpace updated = data.put(userId, dns);
		return (updated !=null); 
	}

	@Override
	public String removeDocument(String userId, QueryDocument doc) {
		/*if (userId == null)
			userId = sNoName;

		boolean rtn = false;
		NameSpace list = lookupByID(userId);
		if (list != null) {
			DataNameSpace dns = data.get(userId);
			dns.remove(docId);
		}	

		return rtn;*/
		return null;
	}

	@Override
	public boolean updateDocument(String namespace, Document doc) {
		return addDocument(namespace, doc);
	}

	@Override
	public List<Document> findDocuments(String userId, QueryDocument criteria) {
		// TODO locating documents can be have several implementations that
		// allow for exact matching to not equal to gt to lt

		// return the namespace as queries are not implemented
		DataNameSpace list = data.get(userId);
		if (list == null)
			return null;
		else
			return new ArrayList<Document>(list.data.values());
	}

	@Override
	public eye.Comm.NameSpace getNameSpaceInfo(String userId) {
		return lookupByID(userId);
	}

	@Override
	public List<eye.Comm.ManipulateNS> findNameSpaces(String userId, ManipulateNS criteria) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NameSpace createNameSpace(eye.Comm.NameSpace user) {
		if (user == null)
			return null;

		NameSpace dns = lookupByID(user.getUserId());
		if (dns != null)
			throw new RuntimeException("Namespace already exists");

		NameSpace.Builder bldr = NameSpace.newBuilder(user);
		NameSpace ns = bldr.build();
		users.add(ns);

		return ns;
	}

	@Override
	public String removeNameSpace(String user_id) {
		/*DataNameSpace dns = data.remove(user_id);
		try {
			return (dns != null);
		} finally {
			if (dns != null)
				dns.release();
			dns = null;
		}*/
		return null;
	}

	private NameSpace lookupByID(String user_id) {
		if (user_id == null)
			return null;

		for (NameSpace ns : users) {
			if (ns.getUserId().equals(user_id))
				return ns;
		}
		return null;
	}

	private long createKey() {
		// TODO need key generator
		return System.currentTimeMillis();
	}

	private static class DataNameSpace {
		// store the builder to allow continued updates to the metadata
		HashMap<Long, Document> data = new HashMap<Long,Document>();
		
		public void release() {
			if (data != null) {
				data.clear();
				data = null;
			}
		}

		public boolean add(Document doc) {
			Document uploaded =  data.put(doc.getId(),doc);
			if (uploaded == null)
				return false;
			else {
				return true;
			}
		}

		public boolean remove(long docId) {
			Document removed = data.remove(docId);
			if (removed == null)
				return false;
			else {
				return true;
			}
		}
	}

	@Override
	public void init(DatasourceConf cfg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String validateLogin(LoginInfo loginInfo) {
		// TODO Auto-generated method stub
		return null;
	}
}
