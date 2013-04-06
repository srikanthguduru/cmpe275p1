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
package poke.demo;

import java.io.File;
import java.io.FileInputStream;
import java.util.Random;

import poke.client.ClientConnection;

import com.google.protobuf.ByteString;

import eye.Comm.Document;
import eye.Comm.Finger;
import eye.Comm.Header;
import eye.Comm.Header.Routing;
import eye.Comm.LoginInfo;
import eye.Comm.ManipulateNS;
import eye.Comm.NameSpace;
import eye.Comm.Payload;
import eye.Comm.QueryDocument;
import eye.Comm.Request;


public class Jab {
	private String userId;
	private String server;
	private int requestType;
	private int port;
	private int count;

	public Jab(String user, String server, int port, int request) {
		this.userId = user;
		this.server = server;
		this.port = port;
		this.requestType = request;
	}

	public Jab() {
		// TODO Auto-generated constructor stub
	}

	public void run() {
		ClientConnection cc = ClientConnection.initConnection(server, port);

		switch (requestType) {
		case 0: // Add Poke
			cc.sendRequest(buildPoke(userId, count));
			break;
		case 1: // Add NameSpace
			cc.sendRequest(createNameSpace(Routing.NAMESPACEADD));
			break;
		case 3: // Add Document
			cc.sendRequest(createDocRequest(Routing.DOCADD));
			break;
		case 4: // Update Document
			cc.sendRequest(createDocRequest(Routing.DOCUPDATE));
			break;
		case 5: // Find Document
			cc.sendRequest(findDoc(Routing.DOCFIND));
			break;
		case 6: // Remove Doc --> Change to NameSpace Find
			cc.sendRequest(findDoc(Routing.DOCREMOVE));
			break;
		case 7: // Find  User
			cc.sendRequest(findNameSpace(Routing.NAMESPACEFIND));
			break;
		case 8: // Remove User
			cc.sendRequest(removeNameSpace(Routing.NAMESPACEREMOVE));
			break;
		case 9: // Add NameSpace JPA
			cc.sendRequest(createNameSpace(Routing.NAMESPACEADDJPA));
			break;
		case 11: // Find  User JPA
			cc.sendRequest(findNameSpace(Routing.NAMESPACEFINDJPA));
			break;
		case 12: // Remove User JPA
			cc.sendRequest(removeNameSpace(Routing.NAMESPACEREMOVEJPA));
			break;
		default:
			break;
		}
	}

	public Request buildPoke(String tag, int num) {
		// data to send
		Finger.Builder f = eye.Comm.Finger.newBuilder();
		f.setTag(tag);
		f.setNumber(num);

		// payload containing data
		Request.Builder r = Request.newBuilder();
		eye.Comm.Payload.Builder p = Payload.newBuilder();
		p.setFinger(f.build());
		r.setBody(p.build());

		// header with routing info
		eye.Comm.Header.Builder h = Header.newBuilder();
		h.setOriginator("client");
		h.setTag(tag+num+System.currentTimeMillis());
		h.setRoutingId(eye.Comm.Header.Routing.FINGER);
		r.setHeader(h.build());
		return r.build();
	}

	public Request createNameSpace(Routing msgRoute) {
		// data to send
		NameSpace.Builder ns = eye.Comm.NameSpace.newBuilder();
		ns.setName("Sug");
		ns.setPassword("cmpe275");
		ns.setUserId(userId);
		ns.setCity("Sunnyvale");
		ns.setZipCode("94086");

		// payload containing data
		Request.Builder r = Request.newBuilder();
		eye.Comm.Payload.Builder p = Payload.newBuilder();
		p.setSpace(ns.build());
		r.setBody(p.build());

		// header with routing info
		eye.Comm.Header.Builder h = Header.newBuilder();
		h.setOriginator("client");
		h.setTag(userId);
		h.setRoutingId(msgRoute);
		r.setHeader(h.build());
		return r.build();
	}

	public Request createDocRequest(Routing msgRoute){
		Document.Builder doc = Document.newBuilder();

		if(msgRoute == Routing.DOCUPDATE){
			doc.setId(3);
			doc.setFileName("scene");
		}
		else{
			doc.setId(new Random().nextLong());
			doc.setFileName("My Image");
		}
		doc.setNameSpace(userId);
		doc.setFileType("jpg");
		eye.Comm.Point.Builder pnt = eye.Comm.Point.newBuilder().setX(35).setY(-122);
		doc.setLocation(pnt);
		doc.setTime(System.currentTimeMillis());


		File image = new File("resources/DSC_1832.jpg");
		byte [] imgAsBytes = new byte[(int)image.length()];
		try {
			FileInputStream fis = new FileInputStream(image);
			fis.read(imgAsBytes);
			fis.close();
		} catch(Exception e){
			System.out.println("Should not be here " +e.getMessage());
		}
		doc.setImgByte(ByteString.copyFrom(imgAsBytes));

		// payload containing data
		Request.Builder r = Request.newBuilder();
		eye.Comm.Payload.Builder p = Payload.newBuilder();
		p.setDoc(doc.build());
		r.setBody(p.build());

		// header with routing info
		eye.Comm.Header.Builder h = Header.newBuilder();
		h.setOriginator("client");
		h.setTag(userId);
		h.setRoutingId(msgRoute);
		r.setHeader(h.build());
		return r.build();
	}

	public Request findDoc(Routing msgRoute) {
		QueryDocument qd;
		if(msgRoute == Routing.DOCFIND) {
			eye.Comm.Point pnt = eye.Comm.Point.newBuilder().setX(34.6).setY(-121.5).build();
			qd =	QueryDocument.newBuilder().setLocation(pnt)
					.setUserId(userId)
					.setTime(-1L)
					.build();
		} else {
			qd =	QueryDocument.newBuilder().setUserId(userId)
					.setName("My Image").build();
		}
		Request.Builder r = Request.newBuilder();
		eye.Comm.Payload.Builder p = Payload.newBuilder();

		p.setQuery(qd);
		r.setBody(p.build());

		// header with routing info
		eye.Comm.Header.Builder h = Header.newBuilder();
		h.setOriginator("client");
		h.setTag(userId);
		h.setRoutingId(msgRoute);
		r.setHeader(h.build());
		return r.build();
	}

	public Request findNameSpace(Routing msgRoute) {
		ManipulateNS.Builder qd = ManipulateNS.newBuilder();
		qd.setUserId(userId);

		Request.Builder r = Request.newBuilder();
		eye.Comm.Payload.Builder p = Payload.newBuilder();
		p.setQueryUser(qd.build());
		r.setBody(p.build());

		// header with routing info
		eye.Comm.Header.Builder h = Header.newBuilder();
		h.setOriginator("client");
		h.setTag(userId);
		h.setRoutingId(msgRoute);
		r.setHeader(h.build());
		return r.build();

	}

	public Request removeNameSpace(Routing msgRoute) {
		ManipulateNS.Builder qd = ManipulateNS.newBuilder();
		qd.setUserId(userId);

		Request.Builder r = Request.newBuilder();
		eye.Comm.Payload.Builder p = Payload.newBuilder();
		p.setQueryUser(qd.build());
		r.setBody(p.build());

		// header with routing info
		eye.Comm.Header.Builder h = Header.newBuilder();
		h.setOriginator("client");
		h.setTag(userId);
		h.setRoutingId(msgRoute);
		r.setHeader(h.build());
		return r.build();

	}

	public static void main(String[] args) {
		try {
			if(args.length != 4) {
				System.out.println("Usage jab <User_Id> <Server> <Port> <RequestType>");
				System.out.println("Request Type - [0 - Poke, 1 - UserAdd, 3 - ImageUpload, 4 - Update Image ");
				System.out.println("Request Type - [5 - Find Image, 6 - Remove Image, 7 - Find User, 8 - Remove User ]");
				System.out.println("Request Type - [9 - UserAdd-JPA, 11 - Find User-JPA, 12 - Remove User-JPA ");
				return;
			}
			System.out.println("UserID: " + args[0] + ", Server: " + args[1] + ", Port: " + args[2] + ", Request: " + args[3]);
			Jab jab = new Jab(args[0], args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
			jab.run();

			Thread.sleep(5000);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
