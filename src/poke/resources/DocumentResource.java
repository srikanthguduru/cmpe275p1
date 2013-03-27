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

import poke.server.resources.Resource;
import poke.server.resources.ResourceUtil;
import poke.server.storage.Storage;
import eye.Comm.Document;
import eye.Comm.Header.ReplyStatus;
import eye.Comm.Header.Routing;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;

public class DocumentResource implements Resource {

	protected Storage storage;
	
	@Override
	public Response process(Request request) {
		
		Routing routingId = request.getHeader().getRoutingId();
		Response.Builder r = Response.newBuilder();
		Response reply = null;
		int routingNumber = routingId.getNumber();
		PayloadReply.Builder payload = PayloadReply.newBuilder();

		if(routingNumber == 20)
		{
			boolean added = storage.addDocument(request.getBody().getLoginUser().getUserId(), request.getBody().getDoc());
			if(!added)
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.FAILURE, "Error adding image"));
			}
			else
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.SUCCESS, "Image added successfully"));
			}

			reply = r.build();
		}
		else if(routingNumber == 21){
			List<Document> documents = storage.findDocuments(request.getBody().getLoginUser().getUserId(), request.getBody().getQuery());

			if(documents == null )
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.FAILURE, "Error finding images"));
			}
			else if(documents.size() == 0)
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.SUCCESS, "No Images Found"));
			}
			else{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.SUCCESS, "Images Found"));
				for(int index = 0; index < documents.size(); index ++)
					payload.setDocs(index, documents.get(index));
				r.setBody(payload);
			}
			r.build();
		}
		if(routingNumber == 22)
		{
			boolean updated = storage.updateDocument(request.getBody().getLoginUser().getUserId(), request.getBody().getDoc());
			if(!updated)
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.FAILURE, "Error updating image"));
			}
			else
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.SUCCESS, "Image updated successfully"));
			}

			reply = r.build();
		}
		if(routingNumber == 23)
		{
			boolean removed = storage.removeDocument(request.getBody().getLoginUser().getUserId(), request.getBody().getDoc().getId());
			if(!removed)
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.FAILURE, "Error deleting image"));
			}
			else
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.SUCCESS, "Image deleted successfully"));
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
