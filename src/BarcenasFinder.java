/*
TO DO:
Terminar la función buildGamma
Terminar la función smell at
Hacer el smell at grafico
Implementar a M.Rajoy
 */
import jade.core.Agent;
import jade.core.behaviours.*;

import java.util.ArrayList;
import jade.core.AID;
import jade.lang.acl.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import static java.lang.System.exit;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.sat4j.core.VecInt;

import org.sat4j.specs.*;
import org.sat4j.minisat.*;
import org.sat4j.reader.*;

// The cyclic behaviour of the Finder Agent:
//
//  Its member function action() will be executed
//  indefinitely, as far as the agent is active
class FinderBehaviour extends CyclicBehaviour {

    // We can be in two states:
    //
    //  1.  Waiting to get position information
    //  2.  Waiting to get smell sensor information (from current position)
    int state = 1;

    // Wait for next message from the Environment Agent,
    // to "sense" what is the current position.
    // Once current position is obtained,
    // Use the smell sensor to obtain from the Environment
    // the smell in the current position
    //
    public void action() {
        // Get next message from evironment Agent
        ACLMessage msg = myAgent.receive();

        if (msg != null) {
            // myAgent.getLocalName()
            if (state == 1) {
                ACLMessage reply = msg.createReply();
                String[] coords = msg.getContent().split(" ");
                String nx;
                String ny;
                // Content should be: MOVEDTO X Y
                nx = coords[1];
                ny = coords[2];
                if (coords[0].equals("MOVEDTO")) {
                    ((BarcenasFinder) myAgent).movedTo(Integer.parseInt(nx), Integer.parseInt(ny));

                    // Send message asking for smell info at current position
                    reply.setPerformative(ACLMessage.QUERY_IF);
                    reply.setContent("SMELLAT " + nx + " " + ny);
                    myAgent.send(reply);
                    state = 2;
                }
            } else if (state == 2) {
                ACLMessage reply = msg.createReply();
                // Get answer from Enviroment Agent for query SMELLAT X Y
                // Content should be: X Y YES/NO
                String[] smellresult = msg.getContent().split(" ");
                int sx, sy;
                sx = Integer.parseInt(smellresult[0]);
                sy = Integer.parseInt(smellresult[1]);
                if (smellresult[2].equals("YES")) {
                    // that it smells at position sx sy
                    System.out.println("FINDER => It smells at " + smellresult[0] + " " + smellresult[1]);
                } else {
                    // that it DOES NOT smell at position sx sy
                    System.out.println("FINDER => It DOESN'T smell at " + smellresult[0] + " " + smellresult[1]);
                }
                try {
                    // Get answer from the formula adding the evidence
                    ((BarcenasFinder) myAgent).smellAt(sx, sy, smellresult[2]);
                } catch (ParseFormatException | IOException | ContradictionException | TimeoutException ex) {
                    Logger.getLogger(FinderBehaviour.class.getName()).log(Level.SEVERE, null, ex);
                }
                //Ask if Mariano is here
                reply.setContent("ISMARIANOHERE " + sx + " " + sy);
                myAgent.send(reply);
                state = 3;
            } else {
                // Get answer from Enviroment Agent for query ISMARIANOHERE X Y
                // Content should be: X Y [ML|MR|NO]
                String[] marianoInfo = msg.getContent().split(" ");
                int mx, my;
                mx = Integer.parseInt(marianoInfo[0]);
                my = Integer.parseInt(marianoInfo[1]);
                if (!marianoInfo[2].equals("NO")) {
                    if (marianoInfo[2].equals("ML")) {
                        // that it smells at position sx sy
                        System.out.println("FINDER => Mariano says that Barcenas is at his left. From (" + marianoInfo[0] + "," + marianoInfo[1] + ")");
                    } else if (marianoInfo[2].equals("MR")) {
                        // that it DOES NOT smell at position sx sy
                        System.out.println("FINDER => Mariano says that Barcenas is at his right. From (" + marianoInfo[0] + "," + marianoInfo[1] + ")");
                    }
                    try {
                        // Get answer from the formula adding the wisdom of Mariano
                        ((BarcenasFinder) myAgent).marianoFound(mx, my, marianoInfo[2]);
                    } catch (ParseFormatException | IOException | ContradictionException | TimeoutException ex) {
                        Logger.getLogger(FinderBehaviour.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                ((BarcenasFinder) myAgent).moveToNext();
                state = 1;

            }
        } else {
            block();
        }
    }

}

class Position {

    public int x, y;

    public Position(int a, int b) {
        x = a;
        y = b;
    }
}

// This agent performs a sequence of movements, and after each
// move it "senses" from the evironment the resulting position
// and then the outcome from the smell sensor, to try to locate
// the position of Barcenas
public class BarcenasFinder extends Agent {
    //  In the setup, perform first move of Agent to initial
    //  position (1,1) of the world, but first add behaviour
    //  of the Agent

    ArrayList<Position> listOfSteps;
    ArrayList<VecInt> futureToPast = null;
    String[][] matrix;
    int idNextStep, numMovements;
    int agentX, agentY;
    int WorldDim;
    int BarcenasPastOffset;
    int BarcenasFutureOffset;
    int SmellsOffset;
    int MarianoOffset;
    Boolean BarcenasFound = false;
    ISolver solver;
    String EnvironmentAgentNickName = "BarcenasWorld";
    String StepsFile;
    Path gammaPath = Paths.get("./gamma.cnf");
    AID EnvironmentAgentID;

    // Get the nickname of the environment Agent from the arguments
    // to get its AgentID to be able to communicate with the environment
    // arg 0 = world agent name
    // arg 1 = world size
    // arg 2 = name of the steps file
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 2) {
            WorldDim = Integer.parseInt((String) args[1]);
            StepsFile = (String) args[2];

        } else {
            System.out.println("FINDER => Not enough args");
            System.exit(0);
        }
        try {
            solver = buildGamma();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BarcenasFinder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BarcenasFinder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ContradictionException ex) {
            Logger.getLogger(BarcenasFinder.class.getName()).log(Level.SEVERE, null, ex);
        }
        idNextStep = 0;
        System.out.println("STARTING FINDER AGENT...");

        // Prepare a list of movements to try with the FINDER Agent
        String steps = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(StepsFile));
            steps = br.readLine();
            br.close();
            String[] stepsList = steps.split(" ");

        } catch (FileNotFoundException ex) {
            System.out.println("MSG.   => Steps file not found");
            exit(1);
        } catch (IOException ex) {
            Logger.getLogger(BarcenasFinder.class.getName()).log(Level.SEVERE, null, ex);
            exit(2);
        }

        String[] stepsList = steps.split(" ");
        listOfSteps = new ArrayList<Position>(stepsList.length);
        initializeMatrix();

        for (int i = 0; i < stepsList.length; i++) {
            String[] coords = stepsList[i].split(",");
            listOfSteps.add(new Position(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
        }

        numMovements = listOfSteps.size();
        if (args != null && args.length > 0) {
            EnvironmentAgentNickName = (String) args[0];
        } else {
            System.out.println("MSG.   => WARNING, using default Environment nick name!");
        }

        System.out.println("MSG.   => Getting name of World Agent: " + EnvironmentAgentNickName);
        EnvironmentAgentID = new AID(EnvironmentAgentNickName, AID.ISLOCALNAME);
        addBehaviour(new FinderBehaviour());
        moveToNext();
    }

    public void initializeMatrix() {
        matrix = new String[WorldDim][WorldDim];
        for (int i = 0; i < WorldDim; i++) {
            for (int j = 0; j < WorldDim; j++) {
                matrix[i][j] = "?";
            }
        }
    }

    // Use agent "actuators" to move to (x,y)
    // We simulate this why telling to the World Agent (environment)
    // that we want to move, but we need the answer from it
    // to be sure that the movement was made with success
    public void moveTo(int x, int y) {
        // Tell the EnvironmentAgentID that we want  to move
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);

        msg.addReceiver(EnvironmentAgentID);
        msg.setLanguage("English");
        msg.setContent("MOVETO " + x + " " + y);
        send(msg);
        System.out.println("FINDER => moving to : (" + x + "," + y + ")");
    }

    public void movedTo(int x, int y) {
        agentX = x;
        agentY = y;
        System.out.println("FINDER => moved to : (" + x + "," + y + ")");
    }

    public void moveToNext() {

        Position nextPosition;

        if (idNextStep < numMovements) {
            nextPosition = listOfSteps.get(idNextStep);
            moveTo(nextPosition.x, nextPosition.y);
            idNextStep = idNextStep + 1;
        } else {
            doDelete();
        }
    }

    public void smellAt(int x, int y, String smells) throws ParseFormatException, IOException, ContradictionException, TimeoutException {
        //Add the evidence
        VecInt evidence = new VecInt();
        if (smells.equals("YES")) {
            evidence.insertFirst((x - 1) * WorldDim + y - 1 + SmellsOffset);
        } else {
            evidence.insertFirst(-((x - 1) * WorldDim + y - 1 + SmellsOffset));
        }
        solver.addClause(evidence);

        //Add the last future clauses to past clauses
        if (futureToPast != null) {
            Iterator it = futureToPast.iterator();
            while (it.hasNext()) {
                solver.addClause((VecInt) it.next());
            }
        }
        futureToPast = new ArrayList<VecInt>();

        for (int i = 1; i < WorldDim + 1; i++) {
            for (int j = 1; j < WorldDim + 1; j++) {
                int linealIndex = (i - 1) * WorldDim + j - 1 + BarcenasFutureOffset;
                VecInt variablePositive = new VecInt();
                VecInt variableNegative = new VecInt();
                variablePositive.insertFirst(linealIndex);
                variableNegative.insertFirst(-linealIndex);

                if (!(solver.isSatisfiable(variablePositive))) {
                    futureToPast.add(variableNegative);
                    //System.out.println("FINDER => Barcenas not found (" + i + "," + j + ")");
                    matrix[j - 1][i - 1] = "X";
                }

                if (!(solver.isSatisfiable(variableNegative))) {
                    System.out.println("FINDER => Barcenas Found at (" + i + "," + j + ")");
                    matrix[j - 1][i - 1] = "B";
                    BarcenasFound = true;
                }
            }
        }

        printMatrix();
        if (BarcenasFound) {
            takeDown();
        }

    }

    public void marianoFound(int x, int y, String marianoInfo) throws ParseFormatException, IOException, ContradictionException, TimeoutException {
        //Add the evidence
        VecInt evidence = new VecInt();
        if (marianoInfo.equals("ML")) {
            evidence.insertFirst(coordToLineal(x, y, MarianoOffset));
        } else {
            evidence.insertFirst(-coordToLineal(x, y, MarianoOffset));
        }
        solver.addClause(evidence);
        
        //Add the last future clauses to past clauses
        if (futureToPast != null) {
            Iterator it = futureToPast.iterator();
            while (it.hasNext()) {
                solver.addClause((VecInt) it.next());
            }
        }
        futureToPast = new ArrayList<VecInt>();

        for (int i = 1; i < WorldDim + 1; i++) {
            for (int j = 1; j < WorldDim + 1; j++) {
                int linealIndex = (i - 1) * WorldDim + j - 1 + BarcenasFutureOffset;
                VecInt variablePositive = new VecInt();
                VecInt variableNegative = new VecInt();
                variablePositive.insertFirst(linealIndex);
                variableNegative.insertFirst(-linealIndex);

                if (!(solver.isSatisfiable(variablePositive))) {
                    futureToPast.add(variableNegative);
                    //System.out.println("FINDER => Barcenas not found (" + i + "," + j + ")");
                    matrix[j - 1][i - 1] = "X";
                }

                if (!(solver.isSatisfiable(variableNegative))) {
                    System.out.println("FINDER => Barcenas Found at (" + i + "," + j + ")");
                    matrix[j - 1][i - 1] = "B";
                    BarcenasFound = true;
                }
            }
        }
        matrix[y - 1][x - 1] = "M";
        printMatrix();
    }

    public void printMatrix() {
        System.out.println("FINDER => Printing Barcenas world matrix");
        for (int i = 0; i < WorldDim; i++) {
            System.out.print("\t#\t");
            for (int j = 0; j < WorldDim; j++) {
                System.out.print(matrix[j][i] + " ");
            }
            System.out.println("\t#");
        }
    }

    public ISolver buildGamma() throws UnsupportedEncodingException, FileNotFoundException, IOException, ContradictionException {
        solver = SolverFactory.newDefault();
        solver.setTimeout(3600);
        int worldLinealDim = WorldDim * WorldDim;
        solver.newVar(worldLinealDim * 4);

        int actualLiteral = 1;

        // Barcenas t-1, from 1,1 to n,n (1 clause)
        BarcenasPastOffset = actualLiteral;
        VecInt pastClause = new VecInt();
        for (int i = 0; i < worldLinealDim; i++) {
            pastClause.insertFirst(actualLiteral);
            actualLiteral++;
        }
        solver.addClause(pastClause);

        // Barcenas t+1, from 1,1 to n,n (1 clause)
        BarcenasFutureOffset = actualLiteral;
        VecInt futureClause = new VecInt();
        for (int i = 0; i < worldLinealDim; i++) {
            futureClause.insertFirst(actualLiteral);
            actualLiteral++;
        }
        solver.addClause(futureClause);

        // Barcenas t-1 -> Barcenas t+1 (nxn clauses)
        for (int i = 0; i < worldLinealDim; i++) {
            VecInt clause = new VecInt();
            clause.insertFirst(i + 1);
            clause.insertFirst(-(i + BarcenasFutureOffset));
            solver.addClause(clause);
        }

        // Smells implications (nxnxnxn clauses)
        SmellsOffset = actualLiteral;
        for (int k = 0; k < worldLinealDim; k++) {
            int s_x = linealToCoord(actualLiteral, SmellsOffset)[0];
            int s_y = linealToCoord(actualLiteral, SmellsOffset)[1];
            for (int b_x = 1; b_x < WorldDim + 1; b_x++) {
                for (int b_y = 1; b_y < WorldDim + 1; b_y++) {
                    // if smells
                    if ((b_x == s_x && b_y == s_y)
                            || (b_x + 1 == s_x && b_y == s_y)
                            || (b_x - 1 == s_x && b_y == s_y)
                            || (b_y + 1 == s_y && b_x == s_x)
                            || (b_y - 1 == s_y && b_x == s_x)) {
                        VecInt clause = new VecInt();
                        clause.insertFirst(actualLiteral);
                        clause.insertFirst(-coordToLineal(b_x, b_y, BarcenasFutureOffset));
                        solver.addClause(clause);
                    } else {
                        VecInt clause = new VecInt();
                        clause.insertFirst(-actualLiteral);
                        clause.insertFirst(-coordToLineal(b_x, b_y, BarcenasFutureOffset));
                        solver.addClause(clause);
                    }
                }
            }
            actualLiteral++;

        }

        // Mariano implications (nxnxnxn clauses)
        MarianoOffset = actualLiteral;
        for (int k = 0; k < worldLinealDim; k++) {
            int m_x = linealToCoord(actualLiteral, MarianoOffset)[0];
            int m_y = linealToCoord(actualLiteral, MarianoOffset)[1];
            for (int b_x = 1; b_x <= WorldDim; b_x++) {
                for (int b_y = 1; b_y <= WorldDim; b_y++) {
                    // If left
                    if (b_y >= m_y) {
                        VecInt clause = new VecInt();
                        clause.insertFirst(-actualLiteral);
                        clause.insertFirst(-coordToLineal(b_x, b_y, BarcenasFutureOffset));
                        solver.addClause(clause);
                        // If right
                    } else {
                        VecInt clause = new VecInt();
                        clause.insertFirst(actualLiteral);
                        clause.insertFirst(-coordToLineal(b_x, b_y, BarcenasFutureOffset));
                        solver.addClause(clause);
                    }
                }
            }
            actualLiteral++;
        }

        // Not in the 1,1 clauses (2 clauses)
        VecInt notInFuture = new VecInt();
        VecInt notInPast = new VecInt();
        notInFuture.insertFirst(-BarcenasFutureOffset);
        notInPast.insertFirst(-BarcenasPastOffset);
        solver.addClause(notInFuture);
        solver.addClause(notInPast);

        return solver;
    }

    public int coordToLineal(int x, int y, int offset) {
        return ((x - 1) * WorldDim) + (y - 1) + offset;
    }

    public int[] linealToCoord(int lineal, int offset) {
        lineal = lineal - offset + 1;
        int[] coords = new int[2];
        coords[1] = lineal % WorldDim;
        if (coords[1] == 0) {
            coords[1] = WorldDim;
        }
        coords[0] = (lineal - 1) / WorldDim + 1;
        return coords;
    }

    protected void takeDown() {
        System.out.println("Agent " + getAID().getName() + " terminating ");
        System.exit(0);
    }

}
