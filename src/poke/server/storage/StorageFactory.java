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

import java.beans.Beans;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.conf.ServerConf;
import poke.server.conf.ServerConf.GeneralConf;
import poke.server.storage.jpa.JPAStorage;

/**
 * Resource factory provides how the server manages resource creation. We hide
 * the resource creation to control how resource instances are managed (created)
 * as different strategies will affect memory and thread isolation. A couple of
 * options are:
 * <p>
 * <ol>
 * <li>instance-per-request - best isolation, worst object reuse and control
 * <li>pool w/ dynamic growth - best object reuse, better isolation (drawback,
 * instances can be dirty), poor resource control
 * <li>fixed pool - favor resource control over throughput (in this case failure
 * due to no space must be handled)
 * </ol>
 * 
 * @author gash
 * 
 */
public class StorageFactory {
	protected static Logger logger = LoggerFactory.getLogger("StorageFactory");

	private static Storage storage;
	private static JPAStorage jpaStorage;	
	private static AtomicReference<StorageFactory> factory = new AtomicReference<StorageFactory>();
	

	public static void initialize(ServerConf cfg, GeneralConf gcf) {
		try {
			StorageFactory.storage = (Storage) Beans.instantiate(StorageFactory.class.getClassLoader(), gcf.getStorage());
			StorageFactory.storage.init(cfg.findDatasourceById(gcf.getNodeId()));
			
			StorageFactory.jpaStorage = new JPAStorage();
			StorageFactory.jpaStorage.init(gcf);
			
			factory.compareAndSet(null, new StorageFactory());	
			
		} catch (Exception e) {
			logger.error("failed to initialize ResourceFactory", e);
		}
	}

	public static StorageFactory getInstance() {
		StorageFactory sf = factory.get();
		if (sf == null)
			throw new RuntimeException("Server not intialized");

		return sf;
	}

	private StorageFactory() {
	}

	/**
	 * Obtain a resource
	 * 
	 * @param route
	 * @return
	 */
	public Storage getStorageInstance() {
		return storage;
	}
	
	public JPAStorage getJPAStorageInstance() {
		return jpaStorage;
	}
}
