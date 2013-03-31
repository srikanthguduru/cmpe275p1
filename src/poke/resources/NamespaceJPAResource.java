package poke.resources;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.entity.User;
import poke.server.resources.Resource;
import poke.server.resources.ResourceUtil;
import poke.server.storage.StorageFactory;
import poke.server.storage.jpa.JPAStorage;
import eye.Comm.Header.ReplyStatus;
import eye.Comm.Header.Routing;
import eye.Comm.LoginInfo;
import eye.Comm.ManipulateNS;
import eye.Comm.NameSpace;
import eye.Comm.PayloadReply;
import eye.Comm.Request;
import eye.Comm.Response;

public class NamespaceJPAResource implements Resource {
	protected static Logger logger = LoggerFactory.getLogger("NamespaceJPAResource");

	@Override
	public Response process(Request request) {

		Routing routingId = request.getHeader().getRoutingId();
		Response.Builder r = Response.newBuilder();
		Response reply = null;
		int routingNumber = routingId.getNumber();
		PayloadReply.Builder payload = PayloadReply.newBuilder();

		JPAStorage jpaStorage = StorageFactory.getInstance().getJPAStorageInstance();

		if(routingNumber == Routing.NAMESPACEADD.getNumber())
		{
			User user = new User();
			NameSpace space = request.getBody().getSpace();

			if(space == null){
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.FAILURE, "No User Detail sent"));
				reply = r.build();
				return reply;
			}

			user.setUserId(space.getUserId());
			user.setName(space.getName());
			user.setPassword(space.getPassword());
			user.setCity(space.getCity());
			user.setZipCode(space.getZipCode());

			User created = jpaStorage.createNameSpace(user);
			if(created == null)
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.FAILURE, "Error creating user"));
			}
			else
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.SUCCESS, "User created successfully"));
				payload.addSpaces(space);
				r.setBody(payload);
			}

			reply = r.build();
		}
		else if(routingNumber == Routing.NAMESPACEFIND.getNumber()){

			User user = new User();
			ManipulateNS queryUser = request.getBody().getQueryUser();

			if(queryUser == null)
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.FAILURE, "No user query detail sent"));
				reply = r.build();
				return reply;
			}

			user.setUserId(queryUser.getUserId());
			user.setName(queryUser.getName());
			user.setCity(queryUser.getCity());
			user.setZipCode(queryUser.getZipCode());

			List<User> users = jpaStorage.findNameSpaces(request.getHeader().getTag(), user);
			NameSpace namespace = null;

			if(users == null )
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.FAILURE, "Error finding users"));
			}
			else if(users.size() == 0)
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.SUCCESS, "No Users Found"));
			}
			else{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.SUCCESS, "Users Found"));
				for(int index = 0; index < users.size(); index ++)
				{					
					namespace = NameSpace.newBuilder()
							.setUserId(users.get(index).getUserId())
							.setName(users.get(index).getName())
							.setCity(users.get(index).getCity())
							.setZipCode(users.get(index).getZipCode()).build();
					payload.addSpaces(index, namespace);
				}
				r.setBody(payload);
			}
			r.build();
		}
		else if(routingNumber == Routing.NAMESPACEREMOVE.getNumber())
		{
			boolean removed = jpaStorage.removeNameSpace(request.getHeader().getTag());
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
		else if(routingNumber == Routing.LOGIN.getNumber())
		{
			User user = new User();
			LoginInfo loginInfo = request.getBody().getLogin();
			
			if(loginInfo == null)
			{
				r.setHeader(ResourceUtil.buildHeaderFrom(request.getHeader(),
						ReplyStatus.FAILURE, "No user login information sent"));
				reply = r.build();
				return reply;
			}
			
			user.setUserId(loginInfo.getUserId());
			user.setPassword(loginInfo.getPassword());
			
			String uuid = jpaStorage.validateLogin(user);

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

