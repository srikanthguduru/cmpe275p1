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

import java.util.List;

import poke.server.conf.ServerConf.DatasourceConf;
import eye.Comm.Document;
import eye.Comm.LoginInfo;
import eye.Comm.ManipulateNS;
import eye.Comm.NameSpace;
import eye.Comm.QueryDocument;

public interface Storage {

	void init(DatasourceConf ds);

	void release();

	NameSpace getNameSpaceInfo(String userId);

	List<ManipulateNS> findNameSpaces(String user_id, ManipulateNS criteria);

	NameSpace createNameSpace(NameSpace space);

	String removeNameSpace(String userId);

	boolean addDocument(String user_id, Document doc);

	String removeDocument(String user_id, QueryDocument doc);

	boolean updateDocument(String user_id, Document doc);

	List<Document> findDocuments(String user_id, QueryDocument criteria);
	
	String validateLogin (LoginInfo loginInfo);
}
