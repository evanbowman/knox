package knox.spring.data.neo4j.repositories;

import knox.spring.data.neo4j.domain.Branch;
import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.Node;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author nicholas roehner
 * @since 12.14.15
 */
@RepositoryRestResource(collectionResourceRel = "knox", path = "knox")
public interface DesignSpaceRepository extends GraphRepository<DesignSpace> {
    @Query(
        "MATCH (s:Snapshot)<-[:CONTAINS]-(c:Commit)<-[:CONTAINS]-(b:Branch {branchID: {targetBranchID}})<-[:ARCHIVES]-(target:DesignSpace {spaceID: {targetSpaceID}})-[h:SELECTS]->(:Branch), (b)-[:LATEST]->(c) "
        + "DELETE h "
        + "CREATE (target)-[:SELECTS]->(b) "
        + "SET target.idIndex = s.idIndex "
        + "WITH target, s "
        + "MATCH (s)-[:CONTAINS]->(n:Node) "
        +
        "FOREACH(ignoreMe IN CASE WHEN NOT exists(n.nodeType) THEN [1] ELSE [] END | "
        +
        "CREATE (target)-[:CONTAINS]->(:Node {nodeID: n.nodeID, copyIndex: ID(n)})) "
        +
        "FOREACH(ignoreMe IN CASE WHEN exists(n.nodeType) THEN [1] ELSE [] END | "
        +
        "CREATE (target)-[:CONTAINS]->(:Node {nodeID: n.nodeID, copyIndex: ID(n), nodeType: n.nodeType})) "
        + "WITH target, s, n as m "
        + "MATCH (m)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(s) "
        +
        "FOREACH(ignoreMe IN CASE WHEN NOT exists(e.componentIDs) AND NOT exists(e.componentRoles) THEN [1] ELSE [] END | "
        +
        "CREATE UNIQUE (target)-[:CONTAINS]->(:Node {copyIndex: ID(m)})-[:PRECEDES]->(:Node {copyIndex: ID(n)})<-[:CONTAINS]-(target)) "
        +
        "FOREACH(ignoreMe IN CASE WHEN exists(e.componentIDs) AND exists(e.componentRoles) THEN [1] ELSE [] END | "
        +
        "CREATE UNIQUE (target)-[:CONTAINS]->(:Node {copyIndex: ID(m)})-[:PRECEDES {componentIDs: e.componentIDs, componentRoles: e.componentRoles}]->(:Node {copyIndex: ID(n)})<-[:CONTAINS]-(target))")
    void checkoutBranch(@Param("targetSpaceID") String targetSpaceID,
                        @Param("targetBranchID") String targetBranchID);

    @Query(
        "MATCH (input:DesignSpace {spaceID: {inputSpaceID}})-[:ARCHIVES]->(bo:Branch {branchID: {outputBranchID}})-[:LATEST]->(co:Commit)-[:CONTAINS]->(so:Snapshot {idIndex: 0}), (bo)-[:CONTAINS]->(co) "
        + "SET so.idIndex = input.idIndex "
        + "WITH input, so "
        + "MATCH (input)-[:CONTAINS]->(n:Node) "
        +
        "FOREACH(ignoreMe IN CASE WHEN NOT exists(n.nodeType) THEN [1] ELSE [] END | "
        + "CREATE (so)-[:CONTAINS]->(:Node {nodeID: n.nodeID})) "
        +
        "FOREACH(ignoreMe IN CASE WHEN exists(n.nodeType) THEN [1] ELSE [] END | "
        +
        "CREATE (so)-[:CONTAINS]->(:Node {nodeID: n.nodeID, nodeType: n.nodeType})) "
        + "WITH input, so, n as m "
        + "MATCH (m)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(target) "
        +
        "FOREACH(ignoreMe IN CASE WHEN NOT exists(e.componentIDs) AND NOT exists(e.componentRoles) THEN [1] ELSE [] END | "
        +
        "CREATE UNIQUE (so)-[:CONTAINS]->(:Node {nodeID: m.nodeID})-[:PRECEDES]->(:Node {nodeID: n.nodeID})<-[:CONTAINS]-(so)) "
        +
        "FOREACH(ignoreMe IN CASE WHEN exists(e.componentIDs) AND exists(e.componentRoles) THEN [1] ELSE [] END | "
        +
        "CREATE UNIQUE (so)-[:CONTAINS]->(:Node {nodeID: m.nodeID})-[:PRECEDES {componentIDs: e.componentIDs, componentRoles: e.componentRoles}]->(:Node {nodeID: n.nodeID})<-[:CONTAINS]-(so))")
    void copyDesignSpaceToSnapshot(@Param("inputSpaceID") String inputSpaceID,
                                   @Param("outputBranchID")
                                   String outputBranchID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:SELECTS]->(hb:Branch)-[:LATEST]->(lc:Commit), (target)-[:ARCHIVES]->(hb), (hb)-[:CONTAINS]->(lc) "
        +
        "CREATE (target)-[:ARCHIVES]->(b:Branch {branchID: {outputBranchID}, idIndex: hb.idIndex}) "
        + "CREATE (lc)<-[:LATEST]-(b)-[:CONTAINS]->(lc) "
        + "WITH hb, b "
        + "MATCH (hb)-[:CONTAINS]->(c:Commit) "
        + "WHERE NOT (hb)-[:LATEST]->(c) "
        + "CREATE (b)-[:CONTAINS]->(c)")
    void copyHeadBranch(@Param("targetSpaceID") String targetSpaceID,
                        @Param("outputBranchID") String outputBranchID);

    @Query(
        "MATCH (:DesignSpace {spaceID: {inputSpaceID}})-[:ARCHIVES]->(:Branch {branchID: {inputBranchID}})-[:CONTAINS]->(ci:Commit)-[:CONTAINS]->(si:Snapshot) "
        + "WITH ci, si "
        +
        "MATCH (:DesignSpace {spaceID: {outputSpaceID}})-[:ARCHIVES]->(:Branch {branchID: {outputBranchID}})-[:CONTAINS]->(co:Commit {copyIndex: ID(ci)})-[:CONTAINS]->(so:Snapshot {idIndex: 0}) "
        + "SET so.idIndex = si.idIndex "
        + "WITH si, so "
        + "MATCH (si)-[:CONTAINS]->(n:Node) "
        +
        "FOREACH(ignoreMe IN CASE WHEN NOT exists(n.nodeType) THEN [1] ELSE [] END | "
        + "CREATE (so)-[:CONTAINS]->(:Node {nodeID: n.nodeID})) "
        +
        "FOREACH(ignoreMe IN CASE WHEN exists(n.nodeType) THEN [1] ELSE [] END | "
        +
        "CREATE (so)-[:CONTAINS]->(:Node {nodeID: n.nodeID, nodeType: n.nodeType})) "
        + "WITH si, so, n as m "
        + "MATCH (m)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(si) "
        +
        "FOREACH(ignoreMe IN CASE WHEN NOT exists(e.componentIDs) AND NOT exists(e.componentRoles) THEN [1] ELSE [] END | "
        +
        "CREATE UNIQUE (so)-[:CONTAINS]->(:Node {nodeID: m.nodeID})-[:PRECEDES]->(:Node {nodeID: n.nodeID})<-[:CONTAINS]-(so)) "
        +
        "FOREACH(ignoreMe IN CASE WHEN exists(e.componentIDs) AND exists(e.componentRoles) THEN [1] ELSE [] END | "
        +
        "CREATE UNIQUE (so)-[:CONTAINS]->(:Node {nodeID: m.nodeID})-[:PRECEDES {componentIDs: e.componentIDs, componentRoles: e.componentRoles}]->(:Node {nodeID: n.nodeID})<-[:CONTAINS]-(so))")
    void copySnapshots(@Param("inputSpaceID") String inputSpaceID,
                       @Param("inputBranchID") String inputBranchID,
                       @Param("outputSpaceID") String outputSpaceID,
                       @Param("outputBranchID") String outputBranchID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch {branchID: {targetBranchID}}) "
        +
        "CREATE (b)-[:LATEST]->(c:Commit {commitID: 'c'+ b.idIndex})<-[:CONTAINS]-(b) "
        + "CREATE (c)-[:CONTAINS]->(s:Snapshot {idIndex: 0}) "
        + "SET b.idIndex = b.idIndex + 1 "
        + "WITH b, c "
        + "MATCH (b)-[l:LATEST]->(d:Commit)<-[:CONTAINS]-(b) "
        + "WHERE NOT ID(c) = ID(d) "
        + "FOREACH(ignoreMe IN CASE WHEN l IS NOT NULL THEN [1] ELSE [] END | "
        + "DELETE l) "
        + "FOREACH(ignoreMe IN CASE WHEN d IS NOT NULL THEN [1] ELSE [] END | "
        + "CREATE (c)-[:SUCCEEDS]->(d))")
    void createCommit(@Param("targetSpaceID") String targetSpaceID,
                      @Param("targetBranchID") String targetBranchID);

    @Query(
        "MATCH (tail:Node {nodeID: {targetTailID}})<-[:CONTAINS]-(:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(head:Node {nodeID: {targetHeadID}}) "
        +
        "CREATE (tail)-[:PRECEDES {componentIDs: {componentIDs}, componentRoles: {componentRoles}}]->(head)")
    void createComponentEdge(
        @Param("targetSpaceID") String targetSpaceID,
        @Param("targetTailID") String targetTailID,
        @Param("targetHeadID") String targetHeadID,
        @Param("componentIDs") ArrayList<String> componentIDs,
        @Param("componentRoles") ArrayList<String> componentRoles);

    @Query(
        "MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch {branchID: {targetBranchID}})-[:LATEST]->(c:Commit)-[:CONTAINS]->(s:Snapshot)-[:CONTAINS]->(tail:Node {nodeID: {targetTailID}}), (s)-[:CONTAINS]->(head:Node {nodeID: {targetHeadID}}), (b)-[:CONTAINS]->(c) "
        +
        "CREATE (tail)-[:PRECEDES {componentIDs: {componentIDs}, componentRoles: {componentRoles}}]->(head)")
    void createComponentEdge(
        @Param("targetSpaceID") String targetSpaceID,
        @Param("targetBranchID") String targetBranchID,
        @Param("targetTailID") String targetTailID,
        @Param("targetHeadID") String targetHeadID,
        @Param("componentIDs") ArrayList<String> componentIDs,
        @Param("componentRoles") ArrayList<String> componentRoles);

    @Query(
        "CREATE (output:DesignSpace {spaceID: {outputSpaceID}, idIndex: 0, mergeIndex: 0})-[:ARCHIVES]->(b:Branch {branchID: {outputSpaceID}, idIndex: 0}) "
        + "CREATE (output)-[:SELECTS]->(b)")
    void createDesignSpace(@Param("outputSpaceID") String outputSpaceID);

    @Query(
        "CREATE (output:DesignSpace {spaceID: {outputSpaceID}, idIndex: 2, mergeIndex: 0})-[:ARCHIVES]->(b:Branch {branchID: {outputSpaceID}, idIndex: 0}) "
        + "CREATE (output)-[:SELECTS]->(b) "
        +
        "CREATE (output)-[:CONTAINS]->(m:Node {nodeID: 'n0', nodeType: 'start'}) "
        +
        "CREATE (output)-[:CONTAINS]->(n:Node {nodeID: 'n1', nodeType: 'accept'}) "
        +
        "CREATE (m)-[:PRECEDES {componentIDs: {componentIDs}, componentRoles: {componentRoles}}]->(n)")
    void createDesignSpace(
        @Param("outputSpaceID") String outputSpaceID,
        @Param("componentIDs") ArrayList<String> componentIDs,
        @Param("componentRoles") ArrayList<String> componentRoles);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}}) "
        +
        "CREATE (target)-[:CONTAINS]->(n:Node {nodeID: 'n' + target.idIndex}) "
        + "SET target.idIndex = target.idIndex + 1 "
        + "RETURN n.nodeID as nodeID")
    String createNode(@Param("targetSpaceID") String targetSpaceID);

    @Query(
        "MATCH (tail:Node {nodeID: {targetTailID}})<-[:CONTAINS]-(:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(head:Node {nodeID: {targetHeadID}}) "
        + "CREATE (tail)-[:PRECEDES]->(head)")
    void createEdge(@Param("targetSpaceID") String targetSpaceID,
                    @Param("targetTailID") String targetTailID,
                    @Param("targetHeadID") String targetHeadID);

    @Query(
        "MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch {branchID: {targetBranchID}})-[:LATEST]->(c:Commit)-[:CONTAINS]->(s:Snapshot)-[:CONTAINS]->(tail:Node {nodeID: {targetTailID}}), (s)-[:CONTAINS]->(head:Node {nodeID: {targetHeadID}}), (b)-[:CONTAINS]->(c) "
        + "CREATE (tail)-[:PRECEDES]->(head)")
    void createEdge(@Param("targetSpaceID") String targetSpaceID,
                    @Param("targetBranchID") String targetBranchID,
                    @Param("targetTailID") String targetTailID,
                    @Param("targetHeadID") String targetHeadID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}}) "
        +
        "CREATE (target)-[:CONTAINS]->(n:Node {nodeID: 'n' + target.idIndex, nodeType: {nodeType}}) "
        + "SET target.idIndex = target.idIndex + 1 "
        + "RETURN n")
    Set<Node> createTypedNode(@Param("targetSpaceID") String targetSpaceID,
                              @Param("nodeType") String nodeType);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch {branchID: {targetBranchID}})-[:LATEST]->(c:Commit)-[:CONTAINS]->(s:Snapshot), (b)-[:CONTAINS]->(c) "
        +
        "CREATE (s)-[:CONTAINS]->(n:Node {nodeID: 'n' + s.idIndex, nodeType: {nodeType}}) "
        + "SET s.idIndex = s.idIndex + 1 "
        + "RETURN n")
    Set<Node> createTypedNode(@Param("targetSpaceID") String targetSpaceID,
                              @Param("targetBranchID") String targetBranchID,
                              @Param("nodeType") String nodeType);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch {branchID: {targetBranchID}}) "
        + "DETACH DELETE b")
    void deleteBranch(@Param("targetSpaceID") String targetSpaceID,
                      @Param("targetBranchID") String targetBranchID);

    @Query(
        "MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch {branchID: {targetBranchID}})-[:CONTAINS]->(c:Commit) "
        + "REMOVE c.copyIndex")
    void deleteCommitCopyIndices(@Param("targetSpaceID") String targetSpaceID,
                                 @Param("targetBranchID")
                                 String targetBranchID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}}) "
        + "OPTIONAL MATCH (target)-[:CONTAINS]->(n:Node) "
        + "OPTIONAL MATCH (target)-[:ARCHIVES]->(b:Branch)-[:CONTAINS]->(c:Commit)-[:CONTAINS]->(s:Snapshot) "
        + "OPTIONAL MATCH (s)-[:CONTAINS]->(sn:Node) "
        + "DETACH DELETE target "
        + "DETACH DELETE n "
        + "DETACH DELETE b "
        + "DETACH DELETE c "
        + "DETACH DELETE s "
        + "DETACH DELETE sn")
    void deleteDesignSpace(@Param("targetSpaceID") String targetSpaceID);

    @Query(
        "MATCH (tail:Node {nodeID: {targetTailID}})<-[:CONTAINS]-(:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(head:Node {nodeID: {targetHeadID}}), (tail)-[e:PRECEDES]->(head) "
        + "DELETE e")
    void deleteEdge(@Param("targetSpaceID") String targetSpaceID,
                    @Param("targetTailID") String targetTailID,
                    @Param("targetHeadID") String targetHeadID);

    @Query(
        "MATCH (tail:Node {nodeID: {targetTailID}})<-[:CONTAINS]-(:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(head:Node {nodeID: {targetHeadID}}), (tail)-[e:PRECEDES]->(head) "
        + "DELETE e")
    void deleteEdges(@Param("targetSpaceID") String targetSpaceID,
                     @Param("targetTailID") String targetTailID,
                     @Param("targetHeadID") String targetHeadID);

    @Query(
        "MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(target:Node {nodeID: {targetNodeID}}) "
        + "DETACH DELETE target")
    void deleteNode(@Param("targetSpaceID") String targetSpaceID,
                    @Param("targetNodeID") String targetNodeID);

    @Query(
        "MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node) "
        + "REMOVE n.copyIndex")
    void deleteNodeCopyIndices(@Param("targetSpaceID") String targetSpaceID);

    @Query(
        "MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch {branchID: {targetBranchID}})-[:LATEST]->(c:Commit)-[:CONTAINS]->(:Snapshot)-[:CONTAINS]->(n:Node), (b)-[:CONTAINS]->(c) "
        + "REMOVE n.copyIndex")
    void deleteNodeCopyIndices(@Param("targetSpaceID") String targetSpaceID,
                               @Param("targetBranchID") String targetBranchID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node) "
        + "DETACH DELETE n")
    void deleteAllNodes(@Param("targetSpaceID") String targetSpaceID);

    @Query(
        "MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node {nodeID: {targetNodeID}}) "
        + "REMOVE n.nodeType")
    void deleteNodeType(@Param("targetSpaceID") String targetSpaceID,
                        @Param("targetNodeID") String targetNodeID);

    @Query(
        "MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch {branchID: {targetBranchID}})-[:LATEST]->(c:Commit)-[:CONTAINS]->(:Snapshot)-[:CONTAINS]->(n:Node {nodeID: {targetNodeID}}), (b)-[:CONTAINS]->(c) "
        + "REMOVE n.nodeType")
    void deleteNodeType(@Param("targetSpaceID") String targetSpaceID,
                        @Param("targetBranchID") String targetBranchID,
                        @Param("targetNodeID") String targetNodeID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(:Node {nodeID: {targetNodeID}})-[e:PRECEDES]->(n:Node), (target)-[:CONTAINS]->(n) "
        + "DELETE e")
    void deleteOutgoingEdges(@Param("targetSpaceID") String targetSpaceID,
                             @Param("targetNodeID") String targetNodeID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch {branchID: {targetBranchID}})-[:LATEST]->(c:Commit)-[:CONTAINS]->(s:Snapshot)-[:CONTAINS]->(:Node {nodeID: {targetNodeID}})-[e:PRECEDES]->(n:Node), (b)-[:CONTAINS]->(c), (s)-[:CONTAINS]->(n) "
        + "DELETE e")
    void deleteOutgoingEdges(@Param("targetSpaceID") String targetSpaceID,
                             @Param("targetBranchID") String targetBranchID,
                             @Param("targetNodeID") String targetNodeID);

    @Query(
        "MATCH (c1:Commit)<-[l1:LATEST]-(b1:Branch {branchID: {targetBranchID1}})<-[:ARCHIVES]-(target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b2:Branch {branchID: {targetBranchID2}})-[:LATEST]->(c2:Commit), (b1)-[:CONTAINS]->(c1), (c1)<-[:CONTAINS]-(b2)-[:CONTAINS]->(c2) "
        + "DELETE l1 "
        + "CREATE (b1)-[:LATEST]->(c2) "
        + "SET b1.idIndex = b2.idIndex "
        + "WITH b1, b2 "
        + "MATCH (b2)-[:CONTAINS]->(c:Commit) "
        + "WHERE NOT (b1)-[:CONTAINS]->(c:Commit) "
        + "CREATE (b1)-[:CONTAINS]->(c)")
    void fastForwardBranch(@Param("targetSpaceID") String targetSpaceID,
                           @Param("targetBranchID1") String targetBranchID1,
                           @Param("targetBranchID2") String targetBranchID2);

    @Query(
        "MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch {branchID: {targetBranchID}}) "
        + "RETURN b")
    Set<Branch> findBranch(@Param("targetSpaceID") String targetSpaceID,
                           @Param("targetBranchID") String targetBranchID);

    DesignSpace findBySpaceID(@Param("spaceID") String spaceID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(:Node {nodeID: {targetTailID}})-[e:PRECEDES]->(n:Node {nodeID: {targetHeadID}}), (target)-[:CONTAINS]->(n) "
        + "RETURN e")
    Set<Edge> findEdge(@Param("targetSpaceID") String targetSpaceID,
                       @Param("targetTailID") String targetTailID,
                       @Param("targetHeadID") String targetHeadID);

    @Query(
        "MATCH (:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node {nodeID: {targetNodeID}}) "
        + "RETURN n")
    Set<Node> findNode(@Param("targetSpaceID") String targetSpaceID,
                       @Param("targetNodeID") String targetNodeID);

    @Query(
        "MATCH (:DesignSpace {spaceID: {targetSpaceID1}})-[:CONTAINS]->(n1:Node {nodeID: {targetNodeID}}), (:DesignSpace {spaceID: {targetSpaceID2}})-[:CONTAINS]->(n2:Node {copyIndex: ID(n1)}) "
        + "RETURN n2")
    Set<Node> findNodeCopy(@Param("targetSpaceID1") String targetSpaceID1,
                           @Param("targetNodeID") String targetNodeID,
                           @Param("targetSpaceID2") String targetSpaceID2);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b1:Branch {branchID: {targetBranchID1}})-[:LATEST]->(c1:Commit)-[:CONTAINS]->(:Snapshot)-[:CONTAINS]->(n1:Node {nodeID: {targetNodeID}}), (b1)-[:CONTAINS]->(c1), (target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b2:Branch {branchID: {targetBranchID2}})-[:LATEST]->(c2:Commit)-[:CONTAINS]->(:Snapshot)-[:CONTAINS]->(n2:Node {copyIndex: ID(n1)}), (b2)-[:CONTAINS]->(c2) "
        + "RETURN n2")
    Set<Node> findNodeCopy(@Param("targetSpaceID") String targetSpaceID,
                           @Param("targetBranchID1") String targetBranchID1,
                           @Param("targetNodeID") String targetNodeID,
                           @Param("targetBranchID2") String targetBranchID2);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node), (target)-[:CONTAINS]->(n) "
        + "WHERE NOT exists(e.componentIDs) AND NOT exists(e.componentRoles) "
        + "RETURN e")
    Set<Edge> getBlankEdges(@Param("targetSpaceID") String targetSpaceID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch) "
        + "RETURN b.branchID as branchID")
    Set<String> getBranchIDs(@Param("targetSpaceID") String targetSpaceID);

    @Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}}) "
           + "RETURN ID(target) as graphID")
    Set<Integer> getDesignSpaceGraphID(@Param("targetSpaceID") String targetSpaceID);
        
    @Query("MATCH (target:Commit {commitID: {targetCommitID}})-[:CONTAINS]->(subTarget:Snapshot) "
    		+ "RETURN ID(subTarget) as graphID")
    Set<Integer> getSnapshotGraphID(@Param("targetCommitID") String targetCommitID);
    
//    @Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(:Branch {branchID: {targetBranchID}})-[:CONTAINS]->(target:Commit {commitID: {targetCommitID}})-[:CONTAINS]->(subTarget:Snapshot) "
//    		+ "RETURN ID(subTarget) as graphID")
//    Set<Integer> getSnapshotGraphID(@Param("targetSpaceID") String targetSpaceID,
//    								@Param("targetBranchID") String targetBranchID,
//    								@Param("targetCommitID") String targetCommitID);

    @Query(
        "MATCH (b:Branch)<-[:ARCHIVES]-(target:DesignSpace {spaceID: {targetSpaceID}})-[:SELECTS]->(b:Branch) "
        + "RETURN b.branchID as headBranchID")
    Set<String> getHeadBranchID(@Param("targetSpaceID") String targetSpaceID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node {nodeType: {nodeType}}) "
        + "RETURN n")
    Set<Node> getNodesByType(@Param("targetSpaceID") String targetSpaceID,
                             @Param("nodeType") String nodeType);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch {branchID: {targetBranchID}})-[:LATEST]->(c:Commit)-[:CONTAINS]->(:Snapshot)-[:CONTAINS]->(n:Node {nodeType: {nodeType}}), (b)-[:CONTAINS]->(c)  "
        + "RETURN n")
    Set<Node> getNodesByType(@Param("targetSpaceID") String targetSpaceID,
                             @Param("targetBranchID") String targetBranchID,
                             @Param("nodeType") String nodeType);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node) "
        + "RETURN n.nodeID")
    Set<String> getNodeIDs(@Param("targetSpaceID") String targetSpaceID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node {nodeType: {nodeType}}) "
        + "RETURN n.nodeID")
    Set<String> getNodeIDsByType(@Param("targetSpaceID") String targetSpaceID,
                                 @Param("nodeType") String nodeType);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch {branchID: {targetBranchID}})-[:LATEST]->(c:Commit)-[:CONTAINS]->(:Snapshot)-[:CONTAINS]->(n:Node {nodeType: {nodeType}}), (b)-[:CONTAINS]->(c)  "
        + "RETURN n.nodeID")
    Set<String> getNodeIDsByType(@Param("targetSpaceID") String targetSpaceID,
                                 @Param("targetBranchID") String targetBranchID,
                                 @Param("nodeType") String nodeType);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(:Node {nodeID: {targetNodeID}})-[e:PRECEDES]->(n:Node), (target)-[:CONTAINS]->(n) "
        + "RETURN e")
    Set<Edge> getOutgoingEdges(@Param("targetSpaceID") String targetSpaceID,
                               @Param("targetNodeID") String targetNodeID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch {branchID: {targetBranchID}})-[:LATEST]->(c:Commit)-[:CONTAINS]->(s:Snapshot)-[:CONTAINS]->(:Node {nodeID: {targetNodeID}})-[e:PRECEDES]->(n:Node), (b)-[:CONTAINS]->(c), (s)-[:CONTAINS]->(n) "
        + "RETURN e")
    Set<Edge> getOutgoingEdges(@Param("targetSpaceID") String targetSpaceID,
                               @Param("targetBranchID") String targetBranchID,
                               @Param("targetNodeID") String targetNodeID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(:Branch {branchID: {targetBranchID}})-[:CONTAINS]->(c:Commit) "
        + "WHERE NOT exists(c.mergeID) "
        + "WITH target, collect(distinct c) as cs "
        + "SET target.mergeIndex = target.mergeIndex + 1 "
        + "FOREACH(c IN cs| "
        + "SET c.mergeID = 'm' + (target.mergeIndex - 1))")
    void indexVersionMerger(@Param("targetSpaceID") String targetSpaceID,
                            @Param("targetBranchID") String targetBranchID);

    @Query(
        "MATCH (b:Branch)<-[:ARCHIVES]-(target:DesignSpace {spaceID: {targetSpaceID}})-[:SELECTS]->(hb:Branch) "
        + "OPTIONAL MATCH (b)-[:LATEST]->(lc:Commit) "
        + "WHERE NOT exists(lc.mergeID) "
        +
        "OPTIONAL MATCH (b)-[:CONTAINS]->(c:Commit)-[:SUCCEEDS]->(d:Commit)<-[:CONTAINS]-(b) "
        + "WHERE NOT exists(c.mergeID) AND NOT exists(d.mergeID) "
        +
        "RETURN target.spaceID as spaceID, hb.branchID as headBranchID, lc.commitID as latestCommitID, ID(lc) as latestCopyIndex, b.branchID as branchID, "
        +
        "c.commitID as tailID, ID(c) as tailCopyIndex, d.commitID as headID, ID(d) as headCopyIndex "
        + "UNION "
        +
        "MATCH (b:Branch)<-[:ARCHIVES]-(target:DesignSpace {spaceID: {targetSpaceID}})-[:SELECTS]->(hb:Branch) "
        + "OPTIONAL MATCH (b)-[:LATEST]->(lc:Commit) "
        + "WHERE exists(lc.mergeID) "
        +
        "OPTIONAL MATCH (b)-[:CONTAINS]->(cm:Commit)-[:SUCCEEDS]->(dm:Commit)<-[:CONTAINS]-(b) "
        + "WHERE exists(cm.mergeID) AND exists(dm.mergeID) "
        +
        "RETURN target.spaceID as spaceID, hb.branchID as headBranchID, lc.commitID + lc.mergeID as latestCommitID, ID(lc) as latestCopyIndex, b.branchID as branchID, "
        +
        "cm.commitID + cm.mergeID as tailID, ID(cm) as tailCopyIndex, dm.commitID + dm.mergeID as headID, ID(dm) as headCopyIndex "
        + "UNION "
        +
        "MATCH (b:Branch)<-[:ARCHIVES]-(target:DesignSpace {spaceID: {targetSpaceID}})-[:SELECTS]->(hb:Branch) "
        + "OPTIONAL MATCH (b)-[:LATEST]->(lc:Commit) "
        + "WHERE NOT exists(lc.mergeID) "
        +
        "OPTIONAL MATCH (b)-[:CONTAINS]->(ch:Commit)-[:SUCCEEDS]->(dh:Commit)<-[:CONTAINS]-(b) "
        + "WHERE NOT exists(ch.mergeID) AND exists(dh.mergeID) "
        +
        "RETURN target.spaceID as spaceID, hb.branchID as headBranchID, lc.commitID as latestCommitID, ID(lc) as latestCopyIndex, b.branchID as branchID, "
        +
        "ch.commitID as tailID, ID(ch) as tailCopyIndex, dh.commitID + dh.mergeID as headID, ID(dh) as headCopyIndex")
    List<Map<String, Object>> mapBranches(@Param("targetSpaceID")
                                          String targetSpaceID);

    @Query(
        "MATCH (target:DesignSpace)-[:CONTAINS]->(m:Node)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(target:DesignSpace) "
        + "WHERE target.spaceID = {targetSpaceID} "
        +
        "RETURN target.spaceID as spaceID, m.nodeID as tailID, m.nodeTypes as tailTypes, e.componentRoles as componentRoles, "
        + "e.componentIDs as componentIDs, n.nodeID as headID, n.nodeTypes as headTypes")
    List<Map<String, Object>> mapDesignSpace(@Param("targetSpaceID") String targetSpaceID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(bi:Branch {branchID: {inputBranchID}}) "
        + "WITH target, bi "
        +
        "MERGE (target)-[:ARCHIVES]->(bo:Branch {branchID: {outputBranchID}}) "
        + "ON CREATE SET bo.idIndex = bi.idIndex "
        +
        "FOREACH(ignoreMe IN CASE WHEN bo.idIndex < bi.idIndex THEN [1] ELSE [] END | "
        + "SET bo.idIndex = bi.idIndex) "
        + "WITH bi, bo "
        + "MATCH (bi)-[:LATEST]->(ci:Commit)<-[:CONTAINS]-(bi) "
        + "CREATE UNIQUE (bo)-[:LATEST]->(ci)<-[:CONTAINS]-(bo) "
        + "WITH bi, bo "
        + "MATCH (bi)-[:CONTAINS]->(ci:Commit) "
        + "WHERE NOT (bi)-[:LATEST]->(ci) "
        + "CREATE UNIQUE (bo)-[:CONTAINS]->(ci)")
    void mergeBranch(@Param("targetSpaceID") String targetSpaceID,
                     @Param("inputBranchID") String inputBranchID,
                     @Param("outputBranchID") String outputBranchID);

    @Query(
        "MATCH (:DesignSpace {spaceID: {inputSpaceID}})-[:ARCHIVES]->(bi:Branch {branchID: {inputBranchID}}) "
        + "WITH bi "
        + "MATCH (output:DesignSpace {spaceID: {outputSpaceID}}) "
        + "WITH output, bi "
        +
        "MERGE (output)-[:ARCHIVES]->(bo:Branch {branchID: {outputBranchID}}) "
        + "ON CREATE SET bo.idIndex = bi.idIndex "
        +
        "FOREACH(ignoreMe IN CASE WHEN bo.idIndex < bi.idIndex THEN [1] ELSE [] END | "
        + "SET bo.idIndex = bi.idIndex) "
        + "WITH output, bi, bo "
        +
        "OPTIONAL MATCH (bi)-[:CONTAINS]->(c:Commit), (output)-[:ARCHIVES]->(:Branch)-[:CONTAINS]->(co:Commit {copyIndex: ID(c)}) "
        + "OPTIONAL MATCH (bi)-[:CONTAINS]->(ci:Commit) "
        +
        "WHERE NOT (output)-[:ARCHIVES]->(:Branch)-[:CONTAINS]->(:Commit {copyIndex: ID(ci)}) AND NOT exists(ci.mergeID) "
        + "OPTIONAL MATCH (bi)-[:CONTAINS]->(cim:Commit) "
        +
        "WHERE NOT (output)-[:ARCHIVES]->(:Branch)-[:CONTAINS]->(:Commit {copyIndex: ID(cim)}) AND exists(cim.mergeID) "
        +
        "WITH bi, bo, collect(distinct ci) as cis, collect(distinct cim) as cims, collect(distinct co) as cos "
        + "FOREACH(ci IN cis| "
        +
        "CREATE (bo)-[:CONTAINS]->(:Commit {commitID: ci.commitID, copyIndex: ID(ci)})-[:CONTAINS]->(:Snapshot {idIndex: 0})) "
        + "FOREACH(cim IN cims| "
        +
        "CREATE (bo)-[:CONTAINS]->(:Commit {commitID: cim.commitID, mergeID: cim.mergeID, copyIndex: ID(cim)})-[:CONTAINS]->(:Snapshot {idIndex: 0})) "
        + "FOREACH(co IN cos| "
        + "CREATE (bo)-[:CONTAINS]->(co)) "
        + "WITH bi, bo "
        + "MATCH (bi)-[:LATEST]->(ci:Commit)<-[:CONTAINS]-(bi) "
        +
        "CREATE UNIQUE (bo)-[:LATEST]->(:Commit {copyIndex: ID(ci)})<-[:CONTAINS]-(bo) "
        + "WITH bi, bo "
        +
        "MATCH (bi)-[:CONTAINS]->(ci:Commit)-[:SUCCEEDS]->(di:Commit)<-[:CONTAINS]-(bi)"
        +
        "CREATE UNIQUE (bo)-[:CONTAINS]->(:Commit {copyIndex: ID(ci)})-[:SUCCEEDS]->(:Commit {copyIndex: ID(di)})<-[:CONTAINS]-(bo)")
    void mergeBranch(@Param("inputSpaceID") String inputSpaceID,
                     @Param("inputBranchID") String inputBranchID,
                     @Param("outputSpaceID") String outputSpaceID,
                     @Param("outputBranchID") String outputBranchID);

    @Query("MATCH (target:DesignSpace {spaceID: {targetSpaceID}}) "
           + "SET target.spaceID = {outputSpaceID}")
    void renameDesignSpace(@Param("targetSpaceID") String targetSpaceID,
                           @Param("outputSpaceID") String outputSpaceID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:CONTAINS]->(n:Node) "
        + "RETURN n")
    Set<Node> getNodes(@Param("targetSpaceID") String targetSpaceID);

    //	@Query("MATCH (target:DesignSpace {spaceID:
    //{targetSpaceID}})-[:SELECTS]->(hb:Branch)-[:CONTAINS]->(tc:Commit
    //{commitID:
    //{targetCommitID}}), (target)-[:ARCHIVES]->(hb) "
    //			+ "MATCH
    //(hb)-[:CONTAINS]->(c:Commmit)-[:SUCCEEDS*]->(tc)"
    //			+ "DETACH DELETE c "
    //			+ "CREATE UNIQUE (hb)-[:LATEST]->(tc)")
    //	void resetHeadBranch(@Param("targetSpaceID") String targetSpaceID,
    //@Param("targetCommitID") String targetCommitID);

    @Query("MATCH (n:DesignSpace) RETURN n.spaceID;")
    List<String> listDesignSpaces();
    
    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:SELECTS]->(hb:Branch)-[:CONTAINS]->(tc:Commit {commitID: {targetCommitID}}), (target)-[:ARCHIVES]->(hb) "
        + "DETACH DELETE lc")
    void revertHeadBranch(@Param("targetSpaceID") String targetSpaceID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(b:Branch {branchID: {targetBranchID}}) "
        + "CREATE (target)-[:SELECTS]->(b)")
    void selectHeadBranch(@Param("targetSpaceID") String targetSpaceID,
                          @Param("targetBranchID") String targetBranchID);

    @Query(
        "MATCH (target:DesignSpace {spaceID: {targetSpaceID}})-[:ARCHIVES]->(bi:Branch {branchID: {inputBranchID}})-[:LATEST]->(ci:Commit)-[:CONTAINS]->(si:Snapshot)-[:CONTAINS]->(n:Node), (bi)-[:CONTAINS]->(ci) "
        + "WITH target, si, collect(n) as nodes "
        +
        "MATCH (target)-[:ARCHIVES]->(bo:Branch {branchID: {outputBranchID}})-[:LATEST]->(co:Commit)-[:CONTAINS]->(so:Snapshot), (bo)-[:CONTAINS]->(co) "
        + "SET so.idIndex = so.idIndex + size(nodes) "
        + "WITH si, nodes, so "
        + "UNWIND range(0, size(nodes) - 1) as nodeIndex "
        + "WITH si, nodeIndex, nodes[nodeIndex] as n, so "
        +
        "FOREACH(ignoreMe IN CASE WHEN NOT exists(n.nodeType) THEN [1] ELSE [] END | "
        +
        "CREATE (so)-[:CONTAINS]->(:Node {nodeID: 'n' + (so.idIndex - nodeIndex - 1), copyIndex: ID(n)})) "
        +
        "FOREACH(ignoreMe IN CASE WHEN exists(n.nodeType) THEN [1] ELSE [] END | "
        +
        "CREATE (so)-[:CONTAINS]->(:Node {nodeID: 'n' + (so.idIndex - nodeIndex - 1), copyIndex: ID(n), nodeType: n.nodeType})) "
        + "WITH si, n as m, so "
        + "MATCH (m)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(si) "
        +
        "FOREACH(ignoreMe IN CASE WHEN NOT exists(e.componentIDs) AND NOT exists(e.componentRoles) THEN [1] ELSE [] END | "
        +
        "CREATE UNIQUE (so)-[:CONTAINS]->(:Node {copyIndex: ID(m)})-[:PRECEDES]->(:Node {copyIndex: ID(n)})<-[:CONTAINS]-(so)) "
        +
        "FOREACH(ignoreMe IN CASE WHEN exists(e.componentIDs) AND exists(e.componentRoles) THEN [1] ELSE [] END | "
        +
        "CREATE UNIQUE (so)-[:CONTAINS]->(:Node {copyIndex: ID(m)})-[:PRECEDES {componentIDs: e.componentIDs, componentRoles: e.componentRoles}]->(:Node {copyIndex: ID(n)})<-[:CONTAINS]-(so))")
    void unionSnapshot(@Param("targetSpaceID") String targetSpaceID,
                       @Param("inputBranchID") String inputBranchID,
                       @Param("outputBranchID") String outputBranchID);

    @Query(
        "MATCH (input:DesignSpace {spaceID: {inputSpaceID}})-[:CONTAINS]->(n:Node) "
        + "WITH input, collect(n) as nodes "
        + "MERGE (output:DesignSpace {spaceID: {outputSpaceID}}) "
        +
        "ON CREATE SET output.idIndex = size(nodes), output.mergeIndex = input.mergeIndex "
        + "ON MATCH SET output.idIndex = output.idIndex + size(nodes) "
        +
        "FOREACH(ignoreMe IN CASE WHEN output.mergeIndex < input.mergeIndex THEN [1] ELSE [] END | "
        + "SET output.mergeIndex = input.mergeIndex) "
        + "WITH input, nodes, output "
        + "UNWIND range(0, size(nodes) - 1) as nodeIndex "
        + "WITH input, nodeIndex, nodes[nodeIndex] as n, output "
        +
        "FOREACH(ignoreMe IN CASE WHEN NOT exists(n.nodeType) THEN [1] ELSE [] END | "
        +
        "CREATE (output)-[:CONTAINS]->(:Node {nodeID: 'n' + (output.idIndex - nodeIndex - 1), copyIndex: ID(n)})) "
        +
        "FOREACH(ignoreMe IN CASE WHEN exists(n.nodeType) THEN [1] ELSE [] END | "
        +
        "CREATE (output)-[:CONTAINS]->(:Node {nodeID: 'n' + (output.idIndex - nodeIndex - 1), copyIndex: ID(n), nodeType: n.nodeType})) "
        + "WITH input, n as m, output "
        + "MATCH (m)-[e:PRECEDES]->(n:Node)<-[:CONTAINS]-(input) "
        +
        "FOREACH(ignoreMe IN CASE WHEN NOT exists(e.componentIDs) AND NOT exists(e.componentRoles) THEN [1] ELSE [] END | "
        +
        "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {copyIndex: ID(m)})-[:PRECEDES]->(:Node {copyIndex: ID(n)})<-[:CONTAINS]-(output)) "
        +
        "FOREACH(ignoreMe IN CASE WHEN exists(e.componentIDs) AND exists(e.componentRoles) THEN [1] ELSE [] END | "
        +
        "CREATE UNIQUE (output)-[:CONTAINS]->(:Node {copyIndex: ID(m)})-[:PRECEDES {componentIDs: e.componentIDs, componentRoles: e.componentRoles}]->(:Node {copyIndex: ID(n)})<-[:CONTAINS]-(output))")
    void unionDesignSpace(@Param("inputSpaceID") String inputSpaceID,
                          @Param("outputSpaceID") String outputSpaceID);
}
