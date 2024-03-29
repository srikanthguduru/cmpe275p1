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
package poke.resources;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.resources.Resource;
import poke.server.resources.ResourceUtil;
import poke.server.storage.Storage;
import poke.server.storage.StorageFactory;
import eye.Comm.Header.ReplyStatus;
import eye.Comm.Header.Routing;
import eye.Comm.ManipulateNS;
import eye.Comm.NameSpace;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;

public class NameSpaceResource implements Resource {
	protected static Logger logger = LoggerFactory.getLogger("server");

	@Override
	public Response process(Request request) {

		Routing routingId = request.getHeader().getRoutingId();
		Response.Builder r = Response.newBuilder();
		Response reply = null;
		int routingNumber = routingId.getNumber();
		PayloadReply.Builder payload = PayloadReply.newBuilder();

		Storage storage = StorageFactory.getInstance().getStorageInstance();
		
		if(routingNumber == Routing.NAMESPACEADD.getNumber())
		{
			NameSpace created = storage.createNameSpace(request.getBody().getSpace());
			if(created == null)
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.FAILURE, "Error creating user"));
			}
			else
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.SUCCESS, "User created successfully"));
				payload.addSpaces(created);
				r.setBody(payload);
			}

			reply = r.build();
		}
		else if(routingNumber == Routing.NAMESPACEFIND.getNumber()){
			List<ManipulateNS> namespaces = storage.findNameSpaces(request.getHeader().getTag(), request.getBody().getQueryUser());

			if(namespaces == null )
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.FAILURE, "Error finding users"));
			}
			else if(namespaces.size() == 0)
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.SUCCESS, "No Users Found"));
			}
			else{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.SUCCESS, "Users Found"));
				for(int index = 0; index < namespaces.size(); index ++)
					payload.addUsers(index, namespaces.get(index));
				r.setBody(payload);
			}
			reply = r.build();
		}
		else if(routingNumber == Routing.NAMESPACEREMOVE.getNumber())
		{
			String removed = storage.removeNameSpace(request.getHeader().getTag());
			if(removed.equals("User deleted successfully"))
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.SUCCESS, removed));
			}
			else
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.FAILURE, removed));
			}

			reply = r.build();
		}
		else if(routingNumber == Routing.LOGIN.getNumber())
		{
			String uuid = storage.validateLogin(request.getBody().getLogin());
			
			if(uuid == null)
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.FAILURE, "Invalid credentials"));
			}
			else
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.SUCCESS, "Login successful"));
				payload.setUuid(uuid);
			}

			reply = r.build();
		}
		
		return reply;
	}

}
