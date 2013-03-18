package poke.server.routing.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import poke.server.conf.ServerConf.GeneralConf;
import poke.server.conf.ServerConf.RouteConf;

public class OverlayNetwork {
	private Map<String,Vertex> nodes;
	private List<Edge> edges;
	private Graph graph;
	private DijkstraAlgorithm dijkstra;
	
	public OverlayNetwork(List<GeneralConf> servers, List<RouteConf> route) {
		//Update Nodes
		nodes = new HashMap<String,Vertex>();
		Iterator<GeneralConf> itr = servers.iterator();
		while(itr.hasNext()) {
			GeneralConf node = itr.next();
			Vertex point = new Vertex(node.getNodeId(),node.getNodeId());
			nodes.put(node.getNodeId(),point);
		}
		
		// Update Edges
		edges = new ArrayList<Edge>();
		Iterator<RouteConf> itr2 = route.iterator();
		int id = 1;
		while(itr2.hasNext()) {
			RouteConf currNode = itr2.next();
			String sourceLoc = currNode.getNodeId();
			List<String> connected = currNode.getConnected();
			Iterator<String> itr3 = connected.iterator();
			while(itr3.hasNext()) {
				String destLoc = itr3.next();
				edges.add(new Edge(Integer.toString(id),nodes.get(sourceLoc), nodes.get(destLoc), 1));
			}
		}
		
		graph = new Graph(nodes, edges);
		dijkstra = new DijkstraAlgorithm(graph);
	}
	
	public String getNextNode(String sourceLoc, String destLoc) {
		dijkstra.execute(nodes.get(sourceLoc));
		LinkedList<Vertex> path = dijkstra.getPath(nodes.get(destLoc));

		if(path != null) {
			return path.get(1).getId();
		}
		return null;
	}
}
