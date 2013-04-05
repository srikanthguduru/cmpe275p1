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

import java.io.File;
import java.io.FileWriter;

import org.junit.Before;
import org.junit.Test;

import poke.client.ClientConnection;
import poke.demo.Jab;
import poke.server.conf.ServerConf.GeneralConf;
import poke.server.conf.ServerConf.ResourceConf;
import eye.Comm.Header;
import eye.Comm.Header.Routing;

public class ServerConfTest {

	private String userID = "TestUser"+ System.currentTimeMillis();;
	private String server = "localhost";
	private int port = 5570;
	private Jab jab;
	private ClientConnection cc;
		
	@Before
	public void initialize() {
		if(cc == null)
			cc = ClientConnection.initConnection(server, port);
	}
	
	@Test
	public void testSequence() throws InterruptedException
	{
		testPoke();
		Thread.sleep(3000);
		testCreateUser();
		Thread.sleep(3000);
		testImageUpload();
		Thread.sleep(3000);
		testUpdateImage();
		Thread.sleep(3000);
		testFindImage();
		Thread.sleep(3000);
		testRemoveImage();
		Thread.sleep(3000);
		testFindUser();
		Thread.sleep(3000);
		testRemoveUser();
		Thread.sleep(3000);
		testCreateUserJPA();
		Thread.sleep(3000);
		testFindUserJPA();
		Thread.sleep(3000);
		testRemoveUserJPA();
		Thread.sleep(3000);
	}

	
	public void testPoke() {
		System.out.println("Poke");
		jab = new Jab(userID, server, port, 1);
		cc.sendRequest(jab.buildPoke(userID, 100));
	}
	
	public void testCreateUser() {
		System.out.println("Create User");
		jab = new Jab(userID, server, port, 1);
		cc.sendRequest(jab.createNameSpace(Routing.NAMESPACEADD));
	}
	
	public void testImageUpload() {
		System.out.println("Upload Image");		
		jab = new Jab(userID, server, port, 3);
		cc.sendRequest(jab.createDocRequest(Routing.DOCADD));
	}
	
	public void testUpdateImage() {
		System.out.println("Update Image");		
		jab = new Jab(userID, server, port, 4);
		cc.sendRequest(jab.createDocRequest(Routing.DOCUPDATE));
	}
	
	public void testFindImage() {
		System.out.println("Find Image");
		jab = new Jab(userID, server, port, 5);
		cc.sendRequest(jab.findDoc(Routing.DOCFIND));
	}
	
	public void testRemoveImage() {
		System.out.println("Remove Image");
		jab = new Jab(userID, server, port, 6);
		cc.sendRequest(jab.findDoc(Routing.DOCREMOVE));
	}
	
	public void testFindUser() {
		System.out.println("Find User");		
		jab = new Jab(userID, server, port, 7);
		cc.sendRequest(jab.findNameSpace(Routing.NAMESPACEFIND));
	}

	public void testRemoveUser() {
		System.out.println("Remove User");		
		jab = new Jab(userID, server, port, 8);
		cc.sendRequest(jab.removeNameSpace(Routing.NAMESPACEREMOVE));
	}
	
	public void testCreateUserJPA() {
		System.out.println("Create User JPA");		
		jab = new Jab(userID, server, port, 9);
		cc.sendRequest(jab.createNameSpace(Routing.NAMESPACEADDJPA));
	}
	
	public void testFindUserJPA() {
		System.out.println("Find User JPA");				
		jab = new Jab(userID, server, port, 11);
		cc.sendRequest(jab.removeNameSpace(Routing.NAMESPACEFINDJPA));
	}
	
	public void testRemoveUserJPA() {
		System.out.println("Remove User JPA");		
		jab = new Jab(userID, server, port, 12);
		cc.sendRequest(jab.findNameSpace(Routing.NAMESPACEREMOVEJPA));
	}
	
	@Test
	public void testBasicConf() throws Exception {
		
		ServerConf conf = new ServerConf();
		GeneralConf svc = new GeneralConf();
		svc.setNodeId("100");
		svc.setPort(5570);
		svc.setPortMgmt(5670);
		svc.setStorage("poke.server.storage.InMemoryStorage");
		conf.addServer(svc);

		ResourceConf rsc = new ResourceConf();
		rsc.setName("finger");
		rsc.setId(Header.Routing.FINGER_VALUE);
		rsc.setClazz("poke.resources.PokeResource");
		conf.addResource(rsc);

		// we can have a resource support multiple requests by having duplicate
		// entries map to the same class
		rsc = new ResourceConf();
		rsc.setName("namespace.list");
		rsc.setId(Header.Routing.NAMESPACEFIND_VALUE);
		rsc.setClazz("poke.resources.NameSpaceResource");
		conf.addResource(rsc);

		rsc = new ResourceConf();
		rsc.setName("namespace.add");
		rsc.setId(Header.Routing.NAMESPACEADD_VALUE);
		rsc = new ResourceConf();
		rsc.setClazz("poke.resources.NameSpaceResource");
		conf.addResource(rsc);

		rsc.setName("namespace.remove");
		rsc.setId(Header.Routing.NAMESPACEREMOVE_VALUE);
		rsc.setClazz("poke.resources.NameSpaceResource");
		conf.addResource(rsc);

		String json = JsonUtil.encode(conf);
		FileWriter fw = null;
		try {
			fw = new FileWriter(new File("/tmp/poke.cfg"));
			fw.write(json);
			fw.close();

			System.out.println("JSON: " + json);
		} finally {
			fw.close();
		}
	}
	
}
