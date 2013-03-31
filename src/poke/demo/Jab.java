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
import eye.Comm.NameSpace;
import eye.Comm.Payload;
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

	public void run() {
		ClientConnection cc = ClientConnection.initConnection(server, port);
		switch (requestType) {
			case 0: // Add Poke
				cc.sendRequest(buildPoke(userId, count));
				break;
			case 1: // Add NameSpace
				cc.sendRequest(createNameSpace(Routing.NAMESPACEADD));
				break;
			case 2: // Add Document
				cc.sendRequest(createDocRequest(Routing.DOCADD));
				break;

			default:
				break;
		}
	}
	
	private Request buildPoke(String tag, int num) {
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

	private Request createNameSpace(Routing msgRoute) {
		// data to send
		NameSpace.Builder ns = eye.Comm.NameSpace.newBuilder();
		ns.setName("Srinath");
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
		
	private Request createDocRequest(Routing msgRoute){
		// data to send
		Document.Builder doc = Document.newBuilder();
		doc.setId(new Random().nextLong());
		doc.setNameSpace(userId);
		doc.setFileName("My Image");
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
	
	public static void main(String[] args) {
		try {
			if(args.length != 4) {
				System.out.println("Usage jab <User_Id> <Server> <Port> <RequestType [0 - Poke, 1 - UserAdd, 2 - ImageUpload>");
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
