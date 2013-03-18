package poke.server.routing.graph;

import java.util.List;
import java.util.Map;

public class Graph {
	  private final Map<String,Vertex> vertexes;
	  private final List<Edge> edges;

	  public Graph(Map<String,Vertex> vertexes, List<Edge> edges) {
	    this.vertexes = vertexes;
	    this.edges = edges;
	  }

	  public Map<String,Vertex> getVertexes() {
	    return vertexes;
	  }

	  public List<Edge> getEdges() {
	    return edges;
	  }
}