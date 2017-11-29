Name: Hung Hong
Class: IMGD 4100
Assignment: AI Implementation for PacMan VS Ghosts

The folder contains the following files:
- README.txt
- Report on AI Implementation.pdf - The project write-up that also includes playtesting report
- MyGhosts.java - The Controller file that mimics the AI of Ghosts

The implemented AI is based on the following specification:
http://gameinternals.com/post/2072558330/understanding-pac-man-ghost-behavior

=======================================================================================================
Instruction on how to use the .java file:
- Get the package for PacMan VS Ghosts (available on InstructAssist)
- Add MyGhosts.java to the following directory: pacman_vs_ghosts > src > pacman > entries > ghosts
- Open Executor.java and include the following line on top to import the java file to the executor:
	import pacman.entries.ghosts.MyGhosts;
- Scroll down in Executor.java to find the exec command, change it to the following:
	exec.runGameTimed(new HumanController(new KeyBoardInput()),new MyGhosts(),visual);
- Run the Executor.java file to play the game with the controller MyGhosts.java 
=======================================================================================================