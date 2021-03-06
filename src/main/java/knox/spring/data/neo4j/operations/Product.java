package knox.spring.data.neo4j.operations;

import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.domain.NodeSpace;
import knox.spring.data.neo4j.domain.Node.NodeType;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Product {
	
	private List<Node> rowNodes;
	
	private List<Node> colNodes;
	
	private NodeSpace productSpace;
	
	private static final Logger LOG = LoggerFactory.getLogger(Product.class);
	
	public Product(NodeSpace rowSpace, NodeSpace colSpace) {
		productSpace = new NodeSpace(0);
		
		this.rowNodes = rowSpace.depthFirstNodeTraversal();
		
		this.colNodes = colSpace.depthFirstNodeTraversal();
	}
	
    public NodeSpace getProductSpace() {
    	return productSpace;
    }
    
//    public void cartesian() {
//    	for (int i = 0; i < rowNodes.size(); i++) {
//    		for (int j = 0; j < colNodes.size(); j++) {
//    			crossNodes(i, j, 1);
//    		}
//    	}
//    }
    
    public List<Partition> unionNonOverlapPartitions(List<Partition> partitions) {
    	List<Partition> overlapPartitions = new LinkedList<Partition>();
    	
    	for (int i = 0; i < partitions.size(); i++) {
    		boolean onRepeat = false;
    		
    		while (onRepeat && i < partitions.size()) {
    			for (Partition overlapPartition : overlapPartitions) {
    				if (partitions.get(i).hasOverlap(overlapPartition)) {
    					overlapPartitions.add(partitions.remove(i));

    					onRepeat = true;
    				}
    			}
    		}
    		
    		int backI = i;
    		
    		for (int k = i + 1; k < partitions.size(); k++) {
    			if (partitions.get(i).hasOverlap(partitions.get(k))) {
    				overlapPartitions.add(partitions.remove(k));
    				
    				overlapPartitions.add(partitions.remove(i));

    				i = backI - 1;
    				
    				k = partitions.size();
    			}
    		}
    	}
    	
    	for (int i = 1; i < partitions.size(); i++) {
    		partitions.get(0).union(partitions.get(i));
    	}
    	
    	overlapPartitions.add(partitions.get(0));
    	
    	return overlapPartitions;
    }
    
    public List<Set<Edge>> modifiedStrong(List<Partition> partitions, int tolerance, Set<String> roles) {
//    	List<Partition> partitions = tensor(tolerance, degree, roles);
    	
    	partitions = unionNonOverlapPartitions(partitions);
    	
    	List<Set<Edge>> globalDiffEdges = new ArrayList<Set<Edge>>(2);
    	
    	globalDiffEdges.add(new HashSet<Edge>());
    	
    	globalDiffEdges.add(new HashSet<Edge>());
    	
    	for (Partition partition : partitions) {
    		HashMap<Integer, Set<Node>> iToDiffNodes = new HashMap<Integer, Set<Node>>();
    		
    		HashMap<Integer, Set<Node>> jToDiffNodes = new HashMap<Integer, Set<Node>>();
    		
    		if (tolerance == 1) {
    			globalDiffEdges.get(0).addAll(partition.prosectRowNodes(iToDiffNodes, 
    					jToDiffNodes, roles));
    		}
    		
    		globalDiffEdges.get(0).addAll(partition.projectRowNodes(iToDiffNodes, roles));
    	}
    	
    	for (Partition partition : partitions) {
    		HashMap<Integer, Set<Node>> iToDiffNodes = new HashMap<Integer, Set<Node>>();
    		
    		HashMap<Integer, Set<Node>> jToDiffNodes = new HashMap<Integer, Set<Node>>();
    		
    		if (tolerance == 1) {
    			globalDiffEdges.get(1).addAll(partition.prosectColNodes(iToDiffNodes, 
    					jToDiffNodes, roles));
    		}
    		
    		globalDiffEdges.get(1).addAll(partition.projectColNodes(iToDiffNodes, roles));
    	}
    	
    	return globalDiffEdges;
    }
    
//    public void strong(int tolerance, int degree, Set<String> roles) {
//    	tensor(tolerance, degree, roles);
//    	
//    	cartesian();
//    }
    
    public List<Partition> tensor(int tolerance, int degree, Set<String> roles) {
    	List<Partition> partitions = new LinkedList<Partition>();
    	
    	for (int i = 0; i < rowNodes.size(); i++) {
    		for (int j = 0; j < colNodes.size(); j++) {
    			if (rowNodes.get(i).hasEdges() && colNodes.get(j).hasEdges()) {
    				for (Edge rowEdge : rowNodes.get(i).getEdges()) {
    					for (Edge colEdge : colNodes.get(j).getEdges()) {
    						if (rowEdge.isMatching(colEdge, tolerance, roles)) {
    							int r = locateNode(rowEdge.getHead(), i, 
    									rowNodes);

    							int c = locateNode(colEdge.getHead(), j,
    									colNodes);
    							
    							int p = locatePartition(i, j, partitions);
    							
    							int q = locatePartition(r, c, partitions);
    							
    							Node productNode;
    							
    							Node productHead;
    							
    							if (p >= 0 && q >= 0) {
    								if (p > q) {
    									partitions.get(q).union(partitions.remove(p));
    									
    									productNode = partitions.get(q).getProductNode(i, j);
    									
    									productHead = partitions.get(q).getProductNode(r, c);
    								} else {
    									if (q > p) {
    										partitions.get(p).union(partitions.remove(q));
    									}
    									
    									productNode = partitions.get(p).getProductNode(i, j);
    									
    									productHead = partitions.get(p).getProductNode(r, c);
    								}
    							} else if (p >= 0 && q < 0) {
    								productNode = partitions.get(p).getProductNode(i, j);
    								
    								productHead = crossNodes(r, c, degree);
    								
    								partitions.get(p).addProductNode(r, c, productHead);
    							} else if (p < 0 && q >= 0) {
    								productNode = crossNodes(i, j, degree);
    								
    								partitions.get(q).addProductNode(i, j, productNode);
    								
    								productHead = partitions.get(q).getProductNode(r, c);
    							} else {
    								Partition partition = new Partition();
    								
    								productNode = crossNodes(i, j, degree);
    								
    								productHead = crossNodes(r, c, degree);
    								
    								partition.addProductNode(i, j, productNode);
    								
    								partition.addProductNode(r, c, productHead);
    								
    								partitions.add(partition);
    							}
    							
    							Edge productEdge = productNode.copyEdge(colEdge, 
    									productHead);
								
								if (tolerance == 1) {
    								productEdge.intersectWithEdge(rowEdge);
    							} else if (tolerance != 0) {
    								productEdge.unionWithEdge(rowEdge);
    							}
    						}
    					}
    				}
    			}
    		}
    	}
    	
    	return partitions;
    }
    
    private Node crossNodes(int i, int j, int degree) {
			Node productNode = productSpace.createNode();
			
			if ((degree == 1 && (rowNodes.get(i).isStartNode() || colNodes.get(j).isStartNode()))
					|| (degree == 2 && rowNodes.get(i).isStartNode() && colNodes.get(j).isStartNode())) {
				productNode.addNodeType(NodeType.START.getValue());
			}
			
			
			if ((degree == 1 && (rowNodes.get(i).isAcceptNode() || colNodes.get(j).isAcceptNode()))
					|| (degree == 2 && rowNodes.get(i).isAcceptNode() && colNodes.get(j).isAcceptNode())) {
				productNode.addNodeType(NodeType.ACCEPT.getValue());
			}
			
			return productNode;
    }
    
    private int locateNode(Node node, int k, List<Node> nodes) {
    	for (int i = k; i < nodes.size(); i++) {
    		if (nodes.get(i).isIdenticalTo(node)) {
    			return i;
    		}
    	}
    	
    	for (int i = 0; i < k; i++) {
    		if (nodes.get(i).isIdenticalTo(node)) {
    			return i;
    		}
    	}
    	
    	return -1;
    }
    
    private int locatePartition(int i, int j, List<Partition> partitions) {
    	for (int p = 0; p < partitions.size(); p++) {
    		if (partitions.get(p).hasProductNode(i, j)) {
    			return p;
    		}
    	}
    	
    	return -1;
    }
    
    public class Partition {
    	private HashMap<Integer, Set<Node>> rowToProductNodes;

    	private HashMap<Integer, Set<Node>> colToProductNodes;

    	public Partition() {
    		rowToProductNodes = new HashMap<Integer, Set<Node>>();

    		colToProductNodes = new HashMap<Integer, Set<Node>>();
    	}

    	public void addProductNode(int i, int j, Node productNode) {
    		if (!rowToProductNodes.containsKey(i)) {
    			rowToProductNodes.put(i, new HashSet<Node>());
    		} 

    		rowToProductNodes.get(i).add(productNode);

    		if (!colToProductNodes.containsKey(j)) {
    			colToProductNodes.put(j, new HashSet<Node>());
    		}

    		colToProductNodes.get(j).add(productNode);
    	}
    	
    	public void addDiffNode(int i, int j, Node diffNode, HashMap<Integer, Set<Node>> iToDiffNodes,
    			HashMap<Integer, Set<Node>> jToDiffNodes) {
    		if (!iToDiffNodes.containsKey(i)) {
    			iToDiffNodes.put(i, new HashSet<Node>());
    		} 

    		iToDiffNodes.get(i).add(diffNode);

    		if (!jToDiffNodes.containsKey(j)) {
    			jToDiffNodes.put(j, new HashSet<Node>());
    		}

    		jToDiffNodes.get(j).add(diffNode);
    	}
    	
    	public void deleteDiffNode(int i, int j, Node diffNode, 
    			HashMap<Integer, Set<Node>> iToDiffNodes,
    			HashMap<Integer, Set<Node>> jToDiffNodes) {
    		iToDiffNodes.get(i).remove(diffNode);
    		
    		jToDiffNodes.get(j).remove(diffNode);
    		
    		productSpace.deleteNode(diffNode);
    	}
    	
    	public void deleteProductNodes(Set<Node> productNodes) {
    		for (Integer row : rowToProductNodes.keySet()) {
    			rowToProductNodes.get(row).removeAll(productNodes);
    		}
    		
    		for (Integer col : colToProductNodes.keySet()) {
    			colToProductNodes.get(col).removeAll(productNodes);
    		}
    	}
    	
    	public Node getProductNode(int i, int j) {
    		if (rowToProductNodes.containsKey(i) && colToProductNodes.containsKey(j)) {
    			Set<Node> productNodes = new HashSet<Node>(rowToProductNodes.get(i));

    			productNodes.retainAll(colToProductNodes.get(j));

    			return productNodes.iterator().next();
    		} else {
    			return null;
    		}
    	}

    	public Node getProductNode(int i, int j, HashMap<Integer, Set<Node>> iToProductNodes, 
    			HashMap<Integer, Set<Node>> jToProductNodes) {
    		if (iToProductNodes.containsKey(i) && jToProductNodes.containsKey(j)) {
    			Set<Node> productNodes = new HashSet<Node>(iToProductNodes.get(i));

    			productNodes.retainAll(jToProductNodes.get(j));

    			return productNodes.iterator().next();
    		} else {
    			return null;
    		}
    	}
    	
    	public Node getDiffNode(int i, int j, HashMap<Integer, Set<Node>> iToDiffNodes,
    			HashMap<Integer, Set<Node>> jToDiffNodes) {
    		if (iToDiffNodes.containsKey(i) && jToDiffNodes.containsKey(j)) {
    			Set<Node> diffNodes = new HashSet<Node>(iToDiffNodes.get(i));

    			diffNodes.retainAll(jToDiffNodes.get(j));

    			return diffNodes.iterator().next();
    		} else {
    			return null;
    		}
    	}

    	public Set<Node> getRowProductNodes(int i) {
    		return rowToProductNodes.get(i);
    	}
    	
    	public Set<Node> getColumnProductNodes(int j) {
    		return colToProductNodes.get(j);
    	}

    	public Set<Integer> getRowIndices() {
    		return new HashSet<Integer>(rowToProductNodes.keySet());
    	}

    	public Set<Integer> getColumnIndices() {
    		return new HashSet<Integer>(colToProductNodes.keySet());
    	}

    	public boolean hasOverlap(Partition partition) {
    		Set<Integer> rowIndices = partition.getRowIndices();

    		rowIndices.retainAll(rowToProductNodes.keySet());

    		if (!rowIndices.isEmpty()) {
    			return true;
    		}

    		Set<Integer> colIndices = partition.getColumnIndices();

    		colIndices.retainAll(colToProductNodes.keySet());

    		return !colIndices.isEmpty();
    	}

    	public boolean hasProductNode(int i, int j) {
    		if (rowToProductNodes.containsKey(i) && colToProductNodes.containsKey(j)) {
    			Set<Node> productNodes = new HashSet<Node>(rowToProductNodes.get(i));

    			productNodes.retainAll(colToProductNodes.get(j));

    			return productNodes.size() == 1 
    					&& productSpace.hasNode(productNodes.iterator().next());
    		} else {
    			return false;
    		}
    	}
    	
    	private boolean hasProductNode(int i, int j, HashMap<Integer, Set<Node>> iToProductNodes,
    			HashMap<Integer, Set<Node>> jToProductNodes) {
    		if (iToProductNodes.containsKey(i) && jToProductNodes.containsKey(j)) {
    			Set<Node> productNodes = new HashSet<Node>(iToProductNodes.get(i));

    			productNodes.retainAll(jToProductNodes.get(j));

    			return productNodes.size() == 1 
    					&& productSpace.hasNode(productNodes.iterator().next());
    		} else {
    			return false;
    		}
    	}
    	
    	private boolean hasDiffNode(int i, int j, HashMap<Integer, Set<Node>> iToDiffNodes,
    			HashMap<Integer, Set<Node>> jToDiffNodes) {
    		if (iToDiffNodes.containsKey(i) && jToDiffNodes.containsKey(j)) {
    			Set<Node> diffNodes = new HashSet<Node>(iToDiffNodes.get(i));

    			diffNodes.retainAll(jToDiffNodes.get(j));

    			return diffNodes.size() == 1
    					&& productSpace.hasNode(diffNodes.iterator().next());
    		} else {
    			return false;
    		}
    	}
    	
    	public Set<Edge> prosectRowNodes(HashMap<Integer, Set<Node>> iToDiffNodes, 
    			HashMap<Integer, Set<Node>> jToDiffNodes, Set<String> roles) {
    		return prosectNodes(rowNodes, colNodes, rowToProductNodes, colToProductNodes,
    				iToDiffNodes, jToDiffNodes, roles);
    	}
    	
    	public Set<Edge> prosectColNodes(HashMap<Integer, Set<Node>> iToDiffNodes, 
    			HashMap<Integer, Set<Node>> jToDiffNodes, Set<String> roles) {
    		return prosectNodes(colNodes, rowNodes, colToProductNodes, rowToProductNodes,
    				iToDiffNodes, jToDiffNodes, roles);
    	}
    	
//    	private Set<Edge> prosectNodes(List<Node> iNodes, List<Node> jNodes,
//    			HashMap<Integer, Set<Node>> iToProductNodes, 
//    			HashMap<Integer, Set<Node>> jToProductNodes,
//    			HashMap<Integer, Set<Node>> iToDiffNodes, 
//    			HashMap<Integer, Set<Node>> jToDiffNodes, 
//    			Set<String> roles) {
//    		Set<Edge> localDiffEdges = new HashSet<Edge>();
//    		
//    		for (int i = 0; i < iNodes.size(); i++) {
//    			for (int j = 0; j < jNodes.size(); j++) {
//    				if (iNodes.get(i).hasEdges() && jNodes.get(j).hasEdges()) {
//    					for (Edge iEdge : iNodes.get(i).getEdges()) {
//    						for (Edge jEdge : jNodes.get(j).getEdges()) {
//    							if (iEdge.isMatching(jEdge, 1, roles)
//    									&& !iEdge.isMatching(jEdge, 0, roles)
//    									&& hasProductNode(i, j, iToProductNodes, jToProductNodes)) {
//    								int ii = locateNode(iEdge.getHead(), i, iNodes);
//
//    								int jj = locateNode(jEdge.getHead(), j, jNodes);
//
//    								if (hasProductNode(ii, jj, iToProductNodes, jToProductNodes)) {
//    									Node productNode = getProductNode(i, j, iToProductNodes,
//    											jToProductNodes);
//
//    									Node productHead = getProductNode(ii, jj, iToProductNodes,
//    											jToProductNodes);
//
//    									Set<Edge> productEdges = productNode.getEdges(productHead,
//    											iEdge.getOrientation());
//    									
//    									if (!productEdges.isEmpty()) {
//    										Edge productEdge = productEdges.iterator().next();
//    										
//    										if (iEdge.isMatching(productEdge, 1, roles) 
//        											&& !iEdge.isMatching(productEdge, 0, roles)) {
//        										Node diffNode = projectNode(i, j, iNodes.get(i),
//        												iToDiffNodes, jToDiffNodes);
//
//        										Node diffHead = projectNode(ii, jj, iNodes.get(ii),
//        												iToDiffNodes, jToDiffNodes);
//
//        										Edge diffEdge = diffNode.copyEdge(iEdge, diffHead);
//
//        										diffEdge.diffWithEdge(productEdge);
//
//        										localDiffEdges.add(diffEdge);
//        									}
//    									}
//    								}
//    							}
//    						}
//    					}
//    				}
//    			}
//    		}
//    		
//    		HashMap<String, Set<Edge>> idToIncomingEdges = productSpace.mapNodeIDsToIncomingEdges();
//    		
//    		for (int i = 0; i < iNodes.size(); i++) {
//    			for (int j = 0; j < jNodes.size(); j++) {
//    				if (hasDiffNode(i, j, iToDiffNodes, jToDiffNodes)) {
//    					Node diffNode = getDiffNode(i, j, iToDiffNodes, jToDiffNodes);
//
//    					if (!diffNode.hasEdges()) {
//    						Node productHead = getProductNode(i, j, iToProductNodes,
//    								jToProductNodes);
//
//    						for (Edge diffEdge : idToIncomingEdges.get(diffNode.getNodeID())) {
//    							localDiffEdges.add(diffEdge.getTail().copyEdge(diffEdge, productHead));
//    							
//    							localDiffEdges.remove(diffEdge);
//    							
//    							diffEdge.delete();
//    						}
//
//    						deleteDiffNode(i, j, diffNode, iToDiffNodes, jToDiffNodes);
//    					} else if (!idToIncomingEdges.containsKey(diffNode.getNodeID())) {
//    						Node productNode = getProductNode(i, j, iToProductNodes,
//    								jToProductNodes);
//    				
//    						for (Edge diffEdge : diffNode.getEdges()) {
//    							Edge edgeCopy = productNode.copyEdge(diffEdge);
//    							
//    							idToIncomingEdges.get(diffEdge.getHead().getNodeID()).add(edgeCopy);
//    							
//    							localDiffEdges.add(edgeCopy);
//    							
//    							localDiffEdges.remove(diffEdge);
//    						}
//
//    						deleteDiffNode(i, j, diffNode, iToDiffNodes, jToDiffNodes);
//    					}
//    				}
//    			}
//    		}
//    		
//    		return localDiffEdges;
//    	}
    	
    	private Set<Edge> prosectNodes(List<Node> iNodes, List<Node> jNodes,
    			HashMap<Integer, Set<Node>> iToProductNodes, 
    			HashMap<Integer, Set<Node>> jToProductNodes,
    			HashMap<Integer, Set<Node>> iToDiffNodes, 
    			HashMap<Integer, Set<Node>> jToDiffNodes, 
    			Set<String> roles) {
    		Set<Edge> localDiffEdges = new HashSet<Edge>();
    		
    		for (int i = 0; i < iNodes.size(); i++) {
    			for (int j = 0; j < jNodes.size(); j++) {
    				if (iNodes.get(i).hasEdges() && jNodes.get(j).hasEdges()) {
    					for (Edge iEdge : iNodes.get(i).getEdges()) {
    						for (Edge jEdge : jNodes.get(j).getEdges()) {
    							if (hasProductNode(i, j, iToProductNodes, jToProductNodes)) {
    								Node productNode = getProductNode(i, j, iToProductNodes,
    										jToProductNodes);

    								Set<Edge> productEdges = productNode.getEdges(iEdge.getOrientation());

    								if (!productEdges.isEmpty()) {
    									Edge diffEdge = iEdge.copy();

    									for (Edge productEdge : productEdges) {
    										diffEdge.diffWithEdge(productEdge);
    									}

    									if (diffEdge.hasComponentIDs()) {
    										int ii = locateNode(iEdge.getHead(), i, iNodes);

    										int jj = locateNode(jEdge.getHead(), j, jNodes);

    										diffEdge.setTail(projectNode(i, j, iNodes.get(i),
    												iToDiffNodes, jToDiffNodes));

    										diffEdge.setHead(projectNode(ii, jj, iNodes.get(ii),
    												iToDiffNodes, jToDiffNodes));

    										localDiffEdges.add(diffEdge);
    									}
    								}
    							}
    						}
    					}
    				}
    			}
    		}
    		
    		HashMap<String, Set<Edge>> idToIncomingEdges = productSpace.mapNodeIDsToIncomingEdges();
    		
    		for (int i = 0; i < iNodes.size(); i++) {
    			for (int j = 0; j < jNodes.size(); j++) {
    				if (hasDiffNode(i, j, iToDiffNodes, jToDiffNodes)) {
    					Node diffNode = getDiffNode(i, j, iToDiffNodes, jToDiffNodes);

    					if (!diffNode.hasEdges()) {
    						Node productHead = getProductNode(i, j, iToProductNodes,
    								jToProductNodes);

    						for (Edge diffEdge : idToIncomingEdges.get(diffNode.getNodeID())) {
    							localDiffEdges.add(diffEdge.getTail().copyEdge(diffEdge, productHead));
    							
    							localDiffEdges.remove(diffEdge);
    							
    							diffEdge.delete();
    						}

    						deleteDiffNode(i, j, diffNode, iToDiffNodes, jToDiffNodes);
    					} else if (!idToIncomingEdges.containsKey(diffNode.getNodeID())) {
    						Node productNode = getProductNode(i, j, iToProductNodes,
    								jToProductNodes);
    				
    						for (Edge diffEdge : diffNode.getEdges()) {
    							Edge edgeCopy = productNode.copyEdge(diffEdge);
    							
    							idToIncomingEdges.get(diffEdge.getHead().getNodeID()).add(edgeCopy);
    							
    							localDiffEdges.add(edgeCopy);
    							
    							localDiffEdges.remove(diffEdge);
    						}

    						deleteDiffNode(i, j, diffNode, iToDiffNodes, jToDiffNodes);
    					}
    				}
    			}
    		}
    		
    		return localDiffEdges;
    	}
   
    	private Node projectNode(int i, int j, Node node, HashMap<Integer, Set<Node>> iToDiffNodes,
    			HashMap<Integer, Set<Node>> jToDiffNodes) {
    		if (hasDiffNode(i, j, iToDiffNodes, jToDiffNodes)) {
    			return getDiffNode(i, j, iToDiffNodes, jToDiffNodes);
    		} else {
    			Node diffNode = productSpace.copyNode(node);
    			
    			addDiffNode(i, j, diffNode, iToDiffNodes, jToDiffNodes);
    			
    			return diffNode;
    		}
    	}

    	private Set<Node> projectNode(int i, List<Node> nodes, 
    			HashMap<Integer, Set<Node>> iToProductNodes,
    			HashMap<Integer, Set<Node>> iToDiffNodes, Set<Node> diffNodes) {
    		Set<Node> productNodes = new HashSet<Node>();

    		if (iToProductNodes.containsKey(i)) {
    			productNodes.addAll(iToProductNodes.get(i));
    			
    			if (iToDiffNodes.containsKey(i)) {
    				diffNodes.addAll(iToDiffNodes.get(i));
    			}
    		} else if (iToDiffNodes.containsKey(i)) {
    			diffNodes.addAll(iToDiffNodes.get(i));
    		} else {
    			Node diffNode = productSpace.copyNode(nodes.get(i));

    			if (!iToDiffNodes.containsKey(i)) {
        			iToDiffNodes.put(i, new HashSet<Node>());
        		} 
        		
        		iToDiffNodes.get(i).add(diffNode);

    			diffNodes.add(diffNode);
    		}

    		return productNodes;
    	}
    	
    	public Set<Edge> projectRowNodes(HashMap<Integer, Set<Node>> rowToDiffNodes, Set<String> roles) {
    		return projectNodes(rowNodes, rowToProductNodes, rowToDiffNodes, roles);
    	}
    	
    	public Set<Edge> projectColNodes(HashMap<Integer, Set<Node>> colToDiffNodes, Set<String> roles) {
    		return projectNodes(colNodes, colToProductNodes, colToDiffNodes, roles);
    	}

    	private Set<Edge> projectNodes(List<Node> iNodes, HashMap<Integer, Set<Node>> iToProductNodes,
    			HashMap<Integer, Set<Node>> iToDiffNodes, Set<String> roles) {
    		Set<Edge> localDiffEdges = new HashSet<Edge>();
    		
    		for (int i = 0; i < iNodes.size(); i++) {
    			Set<Node> diffNodes = new HashSet<Node>();
    			
    			Set<Node> productNodes = projectNode(i, iNodes, iToProductNodes, iToDiffNodes, 
    					diffNodes);

    			for (Edge edge : iNodes.get(i).getEdges()) {
    				int ii = locateNode(edge.getHead(), i, iNodes);
    				
    				Set<Node> diffHeads = new HashSet<Node>();
    				
    				Set<Node> productHeads = projectNode(ii, iNodes, iToProductNodes,
    						iToDiffNodes, diffHeads);

    				for (Node productNode : productNodes) {
    					for (Node productHead : productHeads) {
    						if (!productNode.hasMatchingEdge(edge, 1, roles, localDiffEdges)) {
    							localDiffEdges.add(productNode.copyEdge(edge, productHead));
    						}
    					}
    					
    					if (productHeads.isEmpty()) {
    						for (Node diffHead : diffHeads) {
    							localDiffEdges.add(productNode.copyEdge(edge, diffHead));
    						}
    					}
    				}
    				
    				if (productNodes.isEmpty()) {
    					for (Node diffNode : diffNodes) {
    						if (productHeads.isEmpty()) {
    							for (Node diffHead : diffHeads) {
    								localDiffEdges.add(diffNode.copyEdge(edge, diffHead));
    							}
    						}

    						for (Node productHead : productHeads) {
    							localDiffEdges.add(diffNode.copyEdge(edge, productHead));
    						}
    					}
    				}
    			}
    		}
    		
    		return localDiffEdges;
    	}

    	public void union(Partition partition) {
    		for (int i : partition.getRowIndices()) {
    			if (rowToProductNodes.containsKey(i)) {
    				rowToProductNodes.get(i).addAll(partition.getRowProductNodes(i));
    			} else {
    				rowToProductNodes.put(i, partition.getRowProductNodes(i));
    			}
    		}
    		
    		for (int j : partition.getColumnIndices()) {
    			if (colToProductNodes.containsKey(j)) {
    				colToProductNodes.get(j).addAll(partition.getColumnProductNodes(j));
    			} else {
    				colToProductNodes.put(j, partition.getColumnProductNodes(j));
    			}
    		}
    	}
    }

    public enum ProductType {
        TENSOR("tensor"),
        CARTESIAN("cartesian"),
        STRONG("strong");

        private final String value;

        ProductType(String value) { 
        	this.value = value;
        }

        public String getValue() {
        	return value; 
        }
    }
    
}
