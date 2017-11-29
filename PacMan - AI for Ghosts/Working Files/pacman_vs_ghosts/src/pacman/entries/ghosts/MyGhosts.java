package pacman.entries.ghosts;

import java.util.EnumMap;
import pacman.controllers.Controller;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.internal.Node;
import pacman.game.Game;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getActions() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.ghosts.mypackage).
 */
public class MyGhosts extends Controller<EnumMap<GHOST,MOVE>>
{
	private EnumMap<GHOST, MOVE> myMoves=new EnumMap<GHOST, MOVE>(GHOST.class);
	private final static int PILL_PROXIMITY=15;

	// Constant of specific node indexes
	int topLeft = 0;
	int topRight = 78;
	int botLeft = 1191;
	int botRight = 1291;

	// Constant for chase time control
	boolean chase = true;
	int time = 0;

	public EnumMap<GHOST, MOVE> getMove(Game game, long timeDue) {
		myMoves.clear();

		// Reset chase time if player is eaten
		if (game.wasPacManEaten()) {
			time = 0;
		}

		// Increment time only when the ghost is in chase mode
		if (chase) {
			time++;
		}

		// Place your game logic here to play the game as the ghosts
		for (GHOST ghostType : GHOST.values()) {				// For each ghost
			if (game.doesGhostRequireAction(ghostType)) {		// If it requires an action
				// Check if ghost is vulnerable
				// If true, turn off chase and retreat
				// If false, continue chasing
				if (game.getGhostEdibleTime(ghostType) > 0 || closeToPower(game)) {
					chase = false;
					myMoves.put(ghostType, game.getApproximateNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghostType), game.getPacmanCurrentNodeIndex(), 
							game.getGhostLastMoveMade(ghostType), DM.PATH));
					return myMoves;
				} else chase = true;

				// Determine time interval to put the ghost into Scatter Mode
				if (time < 200 || (time >= 600 && time < 800) || (time >= 1200 && time < 1400 ) || (time >=1800 && time < 2000)) {
					scatterMode(game, ghostType, myMoves);
				}
				// If not in specific interval, proceed to Chase Mode
				else {
					// BLINKY - Chase by moving to PacMan location
					if (ghostType == GHOST.BLINKY) {
						myMoves.put(ghostType, game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghostType), game.getPacmanCurrentNodeIndex(), 
								game.getGhostLastMoveMade(ghostType), DM.PATH));
					}

					// PINKY - Chase by moving 4 tiles ahead in front of PacMan
					if (ghostType == GHOST.PINKY) {
						int targetNode = game.getPacmanCurrentNodeIndex();
						MOVE lastMove = game.getPacmanLastMoveMade();

						// 4 tiles = 24 nodes
						// We do the predicted movement 24 nodes ahead of PacMan as the target node for PINKY
						for (int i = 0; i < 24; i++) {
							MOVE[] possibleMove = game.getPossibleMoves(targetNode);
							boolean sameDirection = false;
							// Go through possible move and find whether it share same direction with last move
							for (int j = 0; j < possibleMove.length; j++) {
								if (possibleMove[j] == lastMove) {
									sameDirection = true;
								}
							}

							// If the next move has same direction, make the move
							if (sameDirection) {
								targetNode = game.getNeighbour(targetNode, lastMove);
							} else {	// Case where no same direction move is possible, pick the first possible neighboring node
								int proposedNode = game.getNeighbouringNodes(targetNode, lastMove)[0];
								lastMove = game.getApproximateNextMoveTowardsTarget(targetNode, proposedNode, lastMove, DM.PATH);
								targetNode = proposedNode;
							}
						}

						// Move ghost towards target node
						myMoves.put(ghostType, game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghostType), targetNode, 
								game.getGhostLastMoveMade(ghostType), DM.PATH));
					}

					// INKY - Chase by first targeting 2 tiles ahead in front of PacMan
					// and use BLINKY's position to extend his target node
					if (ghostType == GHOST.INKY) {
						int targetNode = game.getPacmanCurrentNodeIndex();
						MOVE lastMove = game.getPacmanLastMoveMade();

						// 2 tiles = 12 nodes
						// We do the predicted movement 12 nodes ahead of PacMan as the initial target node for INKY
						// This is similar to PINKY
						for (int i = 0; i < 12; i++) {
							MOVE[] possibleMove = game.getPossibleMoves(targetNode);
							boolean sameDirection = false;
							// Go through possible move and find whether it share same direction with last move
							for (int j = 0; j < possibleMove.length; j++) {
								if (possibleMove[j] == lastMove) {
									sameDirection = true;
								}
							}

							// If the next move has same direction, make the move
							if (sameDirection) {
								targetNode = game.getNeighbour(targetNode, lastMove);
							} else {	// Case where no same direction move is possible, pick the first possible neighboring node
								int proposedNode = game.getNeighbouringNodes(targetNode, lastMove)[0];
								lastMove = game.getApproximateNextMoveTowardsTarget(targetNode, proposedNode, lastMove, DM.PATH);
								targetNode = proposedNode;
							}
						}

						// Resolve the vector by getting the x and y distances between BLINKY and the initial target node
						int xDist = game.getNodeXCood(targetNode) - game.getNodeXCood(game.getGhostCurrentNodeIndex(GHOST.BLINKY));
						int yDist = game.getNodeYCood(targetNode) - game.getNodeYCood(game.getGhostCurrentNodeIndex(GHOST.BLINKY));
						targetNode = findClosestNode(game.getNodeXCood(targetNode) + xDist, game.getNodeYCood(targetNode) + yDist, game);

						// Move ghost towards target node
						myMoves.put(ghostType, game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghostType), targetNode, 
								game.getGhostLastMoveMade(ghostType), DM.PATH));
					}

					// SUE - vary between chasing and wandering off
					// If the distance is greater than 8 tiles, he will chase PacMan
					// If the distance is less than or equal to 8 tiles, he will wander off to his corner
					if (ghostType == GHOST.SUE) {
						// Get a next move of ghost by moving 1 node forward
						int initNode = game.getGhostCurrentNodeIndex(ghostType);
						MOVE lastMove = game.getGhostLastMoveMade(ghostType);
						int nextNode;
						MOVE[] possibleMove = game.getPossibleMoves(initNode);
						boolean sameDirection = false;
						for (int j = 0; j < possibleMove.length; j++) {
							if (possibleMove[j] == lastMove) {
								sameDirection = true;
							}
						}
						if (sameDirection) {
							nextNode = game.getNeighbour(initNode, lastMove);
						} else nextNode = game.getNeighbouringNodes(initNode, lastMove)[0];

						// Calculate the distance of moving 1 node
						double nodeDist = game.getDistance(initNode, nextNode, DM.PATH);
						// Calculate the distance of moving 48 nodes (8 tiles)
						double baseDist = nodeDist * 48;

						// Initialize target node
						int targetNode = 0;
						// Get the distance between the ghost and PacMan
						double dist = game.getDistance(game.getGhostCurrentNodeIndex(ghostType), game.getPacmanCurrentNodeIndex(), DM.PATH);
						// Determine behavior of SUE
						// If greater than 8 tiles, set targetNode as PacMan location
						if (dist > baseDist) {
							targetNode = game.getPacmanCurrentNodeIndex();
						} 
						// If less than or equal to 8 tiles, retreat to corner
						else {
							targetNode = botLeft;
						}

						// Move ghost towards target node
						myMoves.put(ghostType, game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghostType), targetNode, 
								game.getGhostLastMoveMade(ghostType), DM.PATH));
					}
				}
			}
		}
		return myMoves;
	}

	// Function to check whether PacMan is close to the power pill
	// Based on the example controller "StarterGhosts"
	private boolean closeToPower(Game game)
	{
		int[] powerPills=game.getPowerPillIndices();

		for(int i=0;i<powerPills.length;i++)
			if(game.isPowerPillStillAvailable(i) && game.getShortestPathDistance(powerPills[i],game.getPacmanCurrentNodeIndex())<PILL_PROXIMITY)
				return true;

		return false;
	}
		
	// Give a coordination to find the closest node available in the maze
	private int findClosestNode(int xCood, int yCood, Game game) {
		// Initialize variable for comparison
		int closestNode = -1;
		int minDist = 999;

		// Run through maze to look for destination node
		for (int i = 0; i < game.getCurrentMaze().graph.length;i++) {
			Node tempNode = game.getCurrentMaze().graph[i];
			int xCoodTemp = game.getNodeXCood(tempNode.nodeIndex);
			int yCoodTemp = game.getNodeYCood(tempNode.nodeIndex);

			if (xCoodTemp == xCood && yCoodTemp == yCood) {		// Matching node with same coordinate is found
				return tempNode.nodeIndex;
			} else {											// Compare with current minimum distance to determine closest node
				int dist = Math.abs(xCoodTemp - xCood) + Math.abs(yCoodTemp - yCood);
				if (dist < minDist) {				// Case of distance less than current minimum distance
					closestNode = tempNode.nodeIndex;	// Update closest node
					minDist = dist;					// Update minimum distance
				}
			}
		}
		return closestNode;
	}

	// Make the respective ghost go to its respective corner of the maze
	private void scatterMode(Game game, GHOST ghostType,EnumMap<GHOST, MOVE> myMoves) {
		int targetNode= 0 ;
		if (ghostType == GHOST.BLINKY) {
			targetNode = topRight;
		}
		if (ghostType == GHOST.PINKY) {
			targetNode = topLeft;
		}
		if (ghostType == GHOST.INKY) {
			targetNode = botRight;
		}
		if (ghostType == GHOST.SUE) {
			targetNode = botLeft;
		}
		myMoves.put(ghostType,game.getNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghostType), targetNode, 
				game.getGhostLastMoveMade(ghostType),DM.EUCLID));
	}
}