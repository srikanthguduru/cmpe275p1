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

	private String userID ;
	private String server;
	private int port;
	private Jab jab;
	private ClientConnection cc;
	private int count;
	
	@Before
	public void initialize() {
		userID = "Sugandhi";
		server = "localhost";
		port = 5570;
		cc = ClientConnection.initConnection(server, port);
	}
	
	@Test
	public void testSequence()
	{
		testPoke();
		testCreateUser();
		testImageUpload();
		testUpdateImage();
		testFindImage();
		testRemoveImage();
		testFindUser();
		testRemoveUser();
		testCreateUserJPA();
		testFindUserJPA();
		testRemoveUserJPA();
	}
	
	public void testPoke()
	{
		jab = new Jab(userID, server, port, 1);
		cc.sendRequest(jab.buildPoke(userID, count));
	}
	
	public void testCreateUser()
	{
		jab = new Jab(userID, server, port, 1);
		cc.sendRequest(jab.createNameSpace(Routing.NAMESPACEADD));
	}
	
	public void testImageUpload()
	{
		jab = new Jab(userID, server, port, 3);
		cc.sendRequest(jab.createDocRequest(Routing.DOCADD));
	}
	
	public void testUpdateImage()
	{
		jab = new Jab(userID, server, port, 4);
		cc.sendRequest(jab.createDocRequest(Routing.DOCUPDATE));
	}
	
	public void testFindImage()
	{
		jab = new Jab(userID, server, port, 5);
		cc.sendRequest(jab.findDoc(Routing.DOCFIND));
	}
	
	public void testRemoveImage()
	{
		jab = new Jab(userID, server, port, 6);
		cc.sendRequest(jab.findDoc(Routing.DOCREMOVE));
	}
	
	public void testFindUser()
	{
		jab = new Jab(userID, server, port, 7);
		cc.sendRequest(jab.findNameSpace(Routing.NAMESPACEFIND));
	}
	public void testRemoveUser()
	{
		jab = new Jab(userID, server, port, 8);
		cc.sendRequest(jab.removeNameSpace(Routing.NAMESPACEREMOVE));
	}
	
	public void testCreateUserJPA()
	{
		jab = new Jab(userID, server, port, 9);
		cc.sendRequest(jab.createNameSpace(Routing.NAMESPACEADDJPA));
	}
	
	public void testFindUserJPA()
	{
		jab = new Jab(userID, server, port, 11);
		cc.sendRequest(jab.removeNameSpace(Routing.NAMESPACEFINDJPA));
	}
	
	public void testRemoveUserJPA()
	{
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
