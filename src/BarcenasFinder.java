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
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
//import java.io.;
import static java.lang.System.exit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.sat4j.core.VecInt;

import org.sat4j.specs.*;
import org.sat4j.minisat.*;
import org.sat4j.reader.*;
import org.sat4j.reader.DimacsReader;

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
            } else {
                // Get answer from Enviroment Agent for query SMELLAT X Y
                // Content should be: X Y YES/NO
                String[] smellresult = msg.getContent().split(" ");
                int sx, sy;
                sx = Integer.parseInt(smellresult[0]);
                sy = Integer.parseInt(smellresult[1]);
                if (smellresult[2].equals("YES")) {
                    // HERE you should perform the reasoning associated with discovering
                    // that it smells at position sx sy
                    System.out.println("FINDER: It smells at " + smellresult[0] + " " + smellresult[1]);
                } else {
                    // HERE you should perform the reasoning associated with discovering
                    // that it DOES NOT smell at position sx sy
                    System.out.println("FINDER: It DOES NOT smell at " + smellresult[0] + " " + smellresult[1]);
                }
                try {
                    // Get answer from the formula adding the evidence
                    ((BarcenasFinder) myAgent).smellAt(sx, sy, smellresult[2]);
                } catch (ParseFormatException ex) {
                    Logger.getLogger(FinderBehaviour.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(FinderBehaviour.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ContradictionException ex) {
                    Logger.getLogger(FinderBehaviour.class.getName()).log(Level.SEVERE, null, ex);
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
//
public class BarcenasFinder extends Agent {
    //  In the setup, perform first move of Agent to initial
    //  position (1,1) of the world, but first add behaviour
    //  of the Agent
    //

    ArrayList<Position> listOfSteps;
    int idNextStep, numMovements;
    int agentX, agentY;
    String EnvironmentAgentNickName = "BarcenasWorld";
    AID EnvironmentAgentID;
    int WorldDim;
    String StepsFile;
    ISolver solver = SolverFactory.newDefault(); // defaultSolver();
    IProblem problem;
    int BarcenasPastOffset;
    int BarcenasFutureOffset;
    int ScopeOffset;
    Path gammaPath = Paths.get("./gamma.cnf");

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
            System.out.println("FINDER: Not enough args");
        }
        idNextStep = 0;
        System.out.println("Starting Finder Agent !!! " + args.length);

        try {
            buildGamma(solver);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BarcenasFinder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BarcenasFinder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ContradictionException ex) {
            Logger.getLogger(BarcenasFinder.class.getName()).log(Level.SEVERE, null, ex);
        }
        // Testing here the use of the SAT solver sat4j
        /*
        ISolver solver = SolverFactory.newDefault(); // defaultSolver();
        IProblem problem;
        solver.setTimeout(3600); // 1 hour timeout
        Reader reader = new DimacsReader(solver);
        PrintWriter out = new PrintWriter(System.out, true);

        try {
            problem = reader.parseInstance("test2.cnf");

            if (problem.isSatisfiable()) {
                System.out.println("TEST CNF Satisfiable !");
                reader.decode(problem.model(), out);
            } else {
                System.out.println("Unsatisfiable !");
            }
        } catch (ParseFormatException e) {
            System.out.println("Parse format error, sorry!");
        } catch (IOException e) {
            System.out.println("IO error, sorry!");
        } catch (ContradictionException e) {
            System.out.println("Unsatisfiable (trivial)!");
        } catch (TimeoutException e) {
            System.out.println("Timeout, sorry!");
        }
         */
        // Prepare a list of movements to try with the FINDER
        // Agent
        String steps = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(StepsFile));
            steps = br.readLine();
            br.close();
            String[] stepsList = steps.split(" ");

        } catch (FileNotFoundException ex) {
            System.out.println("Steps file not found");
            exit(1);
        } catch (IOException ex) {
            Logger.getLogger(BarcenasFinder.class.getName()).log(Level.SEVERE, null, ex);
            exit(2);
        }

        String[] stepsList = steps.split(" ");
        listOfSteps = new ArrayList<Position>(stepsList.length);
        for (int i = 0; i < stepsList.length; i++) {
            String[] coords = stepsList[i].split(",");
            listOfSteps.add(new Position(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
            System.out.println(listOfSteps.get(i).x + "  -  " + listOfSteps.get(i).y);
        }
        /* Manual
        listOfSteps.add(new Position(1, 1));
        listOfSteps.add(new Position(1, 2));
        listOfSteps.add(new Position(1, 3));
        listOfSteps.add(new Position(2, 3));
         */
        numMovements = listOfSteps.size();
        if (args != null && args.length > 0) {
            EnvironmentAgentNickName = (String) args[0];
        } else {
            System.out.println("WARNING, using default Environment nick name !!");
        }
        System.out.println("Getting name of World Agent !!" + EnvironmentAgentNickName);
        EnvironmentAgentID = new AID(EnvironmentAgentNickName, AID.ISLOCALNAME);
        addBehaviour(new FinderBehaviour());
        moveToNext();
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
        System.out.println(getLocalName() + " moving to : (" + x + "," + y + ")");
    }

    public void movedTo(int x, int y) {
        agentX = x;
        agentY = y;
        System.out.println(getLocalName() + " moved to : (" + x + "," + y + ")");
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

    public void smellAt(int x, int y, String smells) throws ParseFormatException, IOException, ContradictionException {
        solver.clone();
        smells.equals("YES");
        for (int i = 1; i < WorldDim + 1; i++) {
            for (int j = 1; j < WorldDim + 1; j++) {
               
            }
        }

    }

    public void buildGamma(ISolver solver) throws UnsupportedEncodingException, FileNotFoundException, IOException, ContradictionException {
        solver.setTimeout(3600);
        int worldLinealDim = WorldDim * WorldDim;
        solver.newVar(worldLinealDim * 3);
        solver.setExpectedNumberOfClauses(2 + worldLinealDim + worldLinealDim * 2 + 2);

        int actualLiteral = 1;

        // Barcenas t-1, from 1,1 to n,n (nxn clauses)
        BarcenasPastOffset = actualLiteral;
        VecInt pastClause = new VecInt();
        for (int i = 0; i < worldLinealDim; i++) {
            pastClause.insertFirst(actualLiteral);
            actualLiteral++;
        }
        solver.addClause(pastClause);

        // Barcenas t+1, from 1,1 to n,n (nxn clauses)
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

        // Scope implications (nxnxnxn clauses)
        ScopeOffset = actualLiteral;
        for (int y = 0; y < worldLinealDim; y++) {
            int scope_x = (actualLiteral - 2 * worldLinealDim - 1) / WorldDim + 1;
            int scope_y = actualLiteral % WorldDim;
            if (scope_y == 0) {
                scope_y = WorldDim;
            }
            for (int i = 0; i < WorldDim; i++) {
                for (int j = 0; j < WorldDim; j++) {
                    // if in range of the scope
                    if (((i + 1) == scope_x && (j + 1) == scope_y)
                            || ((i + 2) == scope_x && (j + 1) == scope_y)
                            || ((i) == scope_x && (j + 1) == scope_y)
                            || ((i + 1) == scope_x && (j + 2) == scope_y)
                            || ((i + 1) == scope_x && (j) == scope_y)) {
                        VecInt clause = new VecInt();
                        clause.insertFirst(-actualLiteral);
                        clause.insertFirst(-(BarcenasFutureOffset + i * WorldDim + j));
                    } else {
                        VecInt clause = new VecInt();
                        clause.insertFirst(actualLiteral);
                        clause.insertFirst(-(BarcenasFutureOffset + i * WorldDim + j));
                    }
                }
            }
            actualLiteral++;

        }

        // Not in the 1,1 clauses (2 clauses)
        String notInPast = "-" + String.valueOf(BarcenasPastOffset) + " 0\n";
        String notInFuture = "-" + String.valueOf(BarcenasFutureOffset) + " 0\n";
        Files.write(gammaPath, notInPast.getBytes(), StandardOpenOption.APPEND);
        Files.write(gammaPath, notInFuture.getBytes(), StandardOpenOption.APPEND);

    }

    protected void takeDown() {
        System.out.println("Agent " + getAID().getName() + " terminating ");
    }

}
