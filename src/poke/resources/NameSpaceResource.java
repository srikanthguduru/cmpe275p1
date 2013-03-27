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
import eye.Comm.Header.ReplyStatus;
import eye.Comm.Header.Routing;
import eye.Comm.NameSpace;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;

public class NameSpaceResource implements Resource {
	protected static Logger logger = LoggerFactory.getLogger("server");
	protected Storage storage;

	@Override
	public Response process(Request request) {

		Routing routingId = request.getHeader().getRoutingId();
		Response.Builder r = Response.newBuilder();
		Response reply = null;
		int routingNumber = routingId.getNumber();
		PayloadReply.Builder payload = PayloadReply.newBuilder();

		if(routingNumber == 10)
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
				payload.setSpaces(0, created);
				r.setBody(payload);
			}

			reply = r.build();
		}
		else if(routingNumber == 11){
			List<NameSpace> namespaces = storage.findNameSpaces(request.getBody().getSpace());

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
					payload.setSpaces(index, namespaces.get(index));
				r.setBody(payload);
			}
			r.build();
		}
		if(routingNumber == 13)
		{
			boolean removed = storage.removeNameSpace(request.getBody().getLoginUser().getUserId());
			if(!removed)
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.FAILURE, "Error deleting user"));
			}
			else
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.SUCCESS, "User deleted successfully"));
			}

			reply = r.build();
		}
		
		return reply;
	}

	@Override
	public void setStorage(Storage storage) {
		this.storage = storage;

	}

}
