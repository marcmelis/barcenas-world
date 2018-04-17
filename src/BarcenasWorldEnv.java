
import jade.core.Agent;
import jade.core.behaviours.*;

import java.util.ArrayList;
import jade.core.AID;
import jade.lang.acl.*;

// The cyclic behaviour of the BarcenasWorld Environment:
//
//
class EnvBehaviour extends CyclicBehaviour {

    // int state = 1;
    public void action() {

        ACLMessage msg = myAgent.receive();

        if (msg != null) {
            String content = msg.getContent();
            String[] tokens = content.split(" ");
            String nx;
            String ny;
            int inx, iny;

            System.out.println(" WORLD:received message with content: " + tokens[0] + " " + tokens[1] + " " + tokens[2]);
            // myAgent.getLocalName()
            if (tokens[0].equals("MOVETO")) {
                ACLMessage reply = msg.createReply();

                // Content should be: MOVEDTO X Y
                nx = tokens[1];
                ny = tokens[2];
                inx = Integer.parseInt(nx);
                iny = Integer.parseInt(ny);
                System.out.println(" WORLD: Checking right position ");
                if (((BarcenasWorldEnv) myAgent).withinLimits(inx, iny)) {
                    reply.setContent("MOVEDTO " + nx + " " + ny);
                    System.out.println(" World: moving agent to " + nx + "," + ny);
                } else {
                    reply.setContent("NOTMOVEDTO " + nx + " " + ny);
                    System.out.println(" World: NOT moving agent to " + nx + "," + ny);
                }
                System.out.println(" WORLD:sending reply message with content: " + reply.getContent());
                myAgent.send(reply);
            } else if (tokens[0].equals("SMELLAT")) {
                ACLMessage reply = msg.createReply();
                nx = tokens[1];
                ny = tokens[2];
                inx = Integer.parseInt(nx);
                iny = Integer.parseInt(ny);

                if (((BarcenasWorldEnv) myAgent).isBarcenasAround(inx, iny)) {
                    reply.setContent(nx + " " + ny + " " + "YES");
                    System.out.println("WORLD: It smells at " + nx + " " + ny);
                } else {
                    reply.setContent(nx + " " + ny + " " + "NO");
                    System.out.println("WORLD: It DOES not smell at " + nx + " " + ny);
                }
                myAgent.send(reply);
            }

        } else {
            block();
        }

    }

}

public class BarcenasWorldEnv extends Agent {

    int BarcenasX, BarcenasY, WorldDim;

    // Check if position x,y is within the limits of the
    // WorldDim x WorldDim   world
    public boolean withinLimits(int x, int y) {

        return (x >= 1 && x <= WorldDim && y >= 1 && y <= WorldDim);
    }
    // Check if Barcenas is around position x,y
    // (or in the same position x,y)
    //       B
    //    B x,y B
    //       B

    public boolean isBarcenasAround(int x, int y) {
        boolean isx = true, isy = true;

        if (x >= 2) {
            isx = (BarcenasX >= (x - 1));
        }
        if (x <= (WorldDim - 1)) {
            isx = isx && (BarcenasX <= (x + 1));
        }
        if (y >= 2) {
            isy = (BarcenasY >= (y - 1));
        }
        if (y <= (WorldDim - 1)) {
            isy = isy && (BarcenasY <= (y + 1));
        }

        return (isx && (y==BarcenasY)) || ((x==BarcenasX) && isy);
    }

    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 2) {
            WorldDim = Integer.parseInt((String) args[0]);
            BarcenasX = Integer.parseInt((String) args[1]);
            BarcenasY = Integer.parseInt((String) args[2]);
        } else {
            System.out.println("WORLD: Not enough args");
        }

        addBehaviour(new EnvBehaviour());
    }

    protected void takeDown() {
        System.out.println("Agent " + getAID().getName() + " terminating ");
    }

}
