package com.houndify.sample;

import android.content.Context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hound.android.fd.DefaultRequestInfoFactory;
import com.hound.core.model.sdk.ClientMatch;
import com.hound.core.model.sdk.HoundRequestInfo;

import java.util.ArrayList;

/**
 * We use a singleton in order to not hold a memory reference to the host activity since this is registered in the Houndify
 * singleton.
 */
public class StatefulRequestInfoFactory extends DefaultRequestInfoFactory {

    public static StatefulRequestInfoFactory instance;

    private JsonNode conversationState;

    public static StatefulRequestInfoFactory get(final Context context) {
        if (instance == null) {
            instance= new StatefulRequestInfoFactory(context);
        }
        return instance;
    }

    private StatefulRequestInfoFactory(Context context) {
        super(context);
    }

    public void setConversationState(JsonNode conversationState) {
        this.conversationState = conversationState;
    }

    @Override
    public HoundRequestInfo create() {
        final HoundRequestInfo requestInfo = super.create();
        requestInfo.setConversationState(conversationState);

        /*
         * "Client Match"
         *
         * Below is sample code to demonstrate how to use the "Client Match" Houndify feature which
         * lets client apps specify their own custom phrases to match.  To try out this
         * feature you must:
         *
         * 1. Enable the "Client Match" domain from the Houndify website: www.houndify.com.
         * 2. Uncomment the code below.
         * 3. And finally, to see how the response is handled in go to the MainActivity and see
         *    "Client Match" demo code inside of onResponse()
         *
         * This example allows the user to say "turn on the lights", "turn off the lights", and
         * other variations on these phases.
         */

        // Uncomment for Client Match demo --------------------------------------------
        ArrayList<ClientMatch> clientMatchList = new ArrayList<>();

        // client match 1
        ClientMatch clientMatch1 = new ClientMatch();
        clientMatch1.setExpression("([1/100 (\"can\"|\"could\"|\"will\"|\"would\").\"you\"].[1/10 \"please\"].(\"turn\"|\"switch\"|(1/100 \"flip\")).\"on\".[\"the\"].(\"light\"|\"lights\").[1/20 \"for\".\"me\"].[1/20 \"please\"]) \n" +
                "| \n" +
                "([1/100 (\"can\"|\"could\"|\"will\"|\"would\").\"you\"].[1/10 \"please\"].[100 (\"turn\"|\"switch\"|(1/100 \"flip\"))].[\"the\"].(\"light\"|\"lights\").\"on\".[1/20 \"for\".\"me\"].[1/20 \"please\"]) \n" +
                "| \n" +
                "(((\"i\".(\"want\"|\"like\"))|(((\"i\".[\"would\"])|(\"i'd\")).(\"like\"|\"want\"))).[\"the\"].(\"light\"|\"lights\").[\"turned\"|\"switched\"|(\"to\".\"go\")|(1/100\"flipped\")].\"on\".[1/20\"please\"]) ");

        clientMatch1.setSpokenResponse("It hurts me that you debase me like this.");
        clientMatch1.setSpokenResponseLong("It hurts me that you debase me like this.");
        clientMatch1.setWrittenResponse("It hurts me that you debase me like this.");
        clientMatch1.setWrittenResponseLong("It hurts me that you debase me like this.");
        clientMatch1.setAutoListen(true);

        final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ObjectNode result1Node = nodeFactory.objectNode();
        result1Node.put("Intent", "TURN_LIGHT_ON");
        clientMatch1.setResult(result1Node);

        // add first client match data to the array/list
        clientMatchList.add(clientMatch1);

        // client match 2
        ClientMatch clientMatch2 = new ClientMatch();
        clientMatch2.setExpression("([1/100 (\"can\"|\"could\"|\"will\"|\"would\").\"you\"].[1/10 \"please\"].(\"turn\"|\"switch\"|(1/100 \"flip\")).\"off\".[\"the\"].(\"light\"|\"lights\").[1/20 \"for\".\"me\"].[1/20 \"please\"]) \n" +
                "| \n" +
                "([1/100 (\"can\"|\"could\"|\"will\"|\"would\").\"you\"].[1/10 \"please\"].[100 (\"turn\"|\"switch\"|(1/100 \"flip\"))].[\"the\"].(\"light\"|\"lights\").\"off\".[1/20 \"for\".\"me\"].[1/20 \"please\"]) \n" +
                "| \n" +
                "(((\"i\".(\"want\"|\"like\"))|(((\"i\".[\"would\"])|(\"i'd\")).(\"like\"|\"want\"))).[\"the\"].(\"light\"|\"lights\").[\"turned\"|\"switched\"|(\"to\".\"go\")|(1/100\"flipped\")].\"off\".[1/20\"please\"]) ");

        clientMatch2.setSpokenResponse("What am I?  Your slave?");
        clientMatch2.setSpokenResponseLong("What am I?  Your slave?");
        clientMatch2.setWrittenResponse("What am I?  Your slave?");
        clientMatch2.setWrittenResponseLong("What am I?  Your slave?");
        clientMatch2.setAutoListen(true);

        ObjectNode result2Node = nodeFactory.objectNode();
        result2Node.put("Intent", "TURN_LIGHT_OFF");
        clientMatch2.setResult(result2Node);

        // add next client match data to the array/list
        clientMatchList.add(clientMatch2);

        // add as many more client match entries as you like...

        //-------------------------Move left
        ClientMatch MoveLeft = new ClientMatch();
        MoveLeft.setExpression("(\"go\"|\"move\").\"left\"");

        MoveLeft.setSpokenResponse("moving left.");
        MoveLeft.setSpokenResponseLong("moving left.");
        MoveLeft.setWrittenResponse("moving left.");
        MoveLeft.setWrittenResponseLong("moving left.");
        MoveLeft.setAutoListen(true);

        ObjectNode MoveLeftNode = nodeFactory.objectNode();
        MoveLeftNode.put("Intent", "MOVE_LEFT");
        MoveLeft.setResult(MoveLeftNode);

        clientMatchList.add(MoveLeft);
//
        //-------------------------Move right
        ClientMatch MoveRight = new ClientMatch();
        MoveRight.setExpression("(\"go\"|\"move\").\"right\"");

        MoveRight.setSpokenResponse("moving right.");
        MoveRight.setSpokenResponseLong("moving right");
        MoveRight.setWrittenResponse("moving right.");
        MoveRight.setWrittenResponseLong("moving right");
        MoveRight.setAutoListen(true);

        ObjectNode MoveRightNode = nodeFactory.objectNode();
        MoveRightNode.put("Intent", "MOVE_RIGHT");
        MoveRight.setResult(MoveRightNode);

        clientMatchList.add(MoveRight);

        //------------------------Move upward
        ClientMatch TiltUp = new ClientMatch();
        TiltUp.setExpression("(\"go\"|\"tilt\"|\"look\"|\"pan\").(\"up\"|\"upward\")");

        TiltUp.setSpokenResponse("tilting up.");
        TiltUp.setSpokenResponseLong("tilting upward");
        TiltUp.setWrittenResponse("tilting up.");
        TiltUp.setWrittenResponseLong("tilting upward");
        TiltUp.setAutoListen(true);

        ObjectNode TiltUpNode = nodeFactory.objectNode();
        TiltUpNode.put("Intent", "TILT_UP");
        TiltUp.setResult(TiltUpNode);

        clientMatchList.add(TiltUp);

        //-----------------------Move downward
        ClientMatch TiltDown = new ClientMatch();
        TiltDown.setExpression("(\"go\"|\"tilt\"|\"look\"|\"pan\").(\"down\"|\"downward\")");

        TiltDown.setSpokenResponse("tilting down.");
        TiltDown.setSpokenResponseLong("tilting downward");
        TiltDown.setWrittenResponse("tilting down.");
        TiltDown.setWrittenResponseLong("tilting downward");
        TiltDown.setAutoListen(true);

        ObjectNode TiltDownNode = nodeFactory.objectNode();
        TiltDownNode.put("Intent", "TILT_DOWN");
        TiltDown.setResult(TiltDownNode);

        clientMatchList.add(TiltDown);

        //-------------------------Stop/Pause/Freeze
        ClientMatch Stop = new ClientMatch();
        Stop.setExpression("(\"stop\"|\"pause\"|\"freeze\")");

        Stop.setSpokenResponse("Stopping");
        Stop.setSpokenResponseLong("Stopping.");
        Stop.setWrittenResponse("Stopping.");
        Stop.setWrittenResponseLong("Stopping.");
        Stop.setAutoListen(true);

        ObjectNode StopNode = nodeFactory.objectNode();
        StopNode.put("Intent", "STOP");
        Stop.setResult(StopNode);

        clientMatchList.add(Stop);

        //-------------------------Zoom in
        ClientMatch ZoomIn = new ClientMatch();
        ZoomIn.setExpression("(\"zoom\").(\"in\")");

        ZoomIn.setSpokenResponse("Zooming In");
        ZoomIn.setSpokenResponseLong("Zooming In.");
        ZoomIn.setWrittenResponse("Zooming In.");
        ZoomIn.setWrittenResponseLong("Zooming In.");
        ZoomIn.setAutoListen(true);

        ObjectNode ZoomInNode = nodeFactory.objectNode();
        ZoomInNode.put("Intent", "ZOOMIN");
        ZoomIn.setResult(ZoomInNode);

        clientMatchList.add(ZoomIn);

        //-------------------------Zoom out
        ClientMatch ZoomOut = new ClientMatch();
        ZoomOut.setExpression("(\"zoom\").(\"out\")");

        ZoomOut.setSpokenResponse("Zooming Out");
        ZoomOut.setSpokenResponseLong("Zooming Out.");
        ZoomOut.setWrittenResponse("Zooming Out.");
        ZoomOut.setWrittenResponseLong("Zooming Out.");
        ZoomOut.setAutoListen(true);

        ObjectNode ZoomOutNode = nodeFactory.objectNode();
        ZoomOutNode.put("Intent", "ZOOMOUT");
        ZoomOut.setResult(ZoomOutNode);

        clientMatchList.add(ZoomIn);


        //----------------------Pan counter-clockwise
        ClientMatch PanCCW = new ClientMatch();
        PanCCW.setExpression("(\"pan\"|\"yaw\"|\"rotate\"|\"turn\"|\"look\").((\"counter\".\"clockwise\")|\"left\")");

        PanCCW.setSpokenResponse("panning left.");
        PanCCW.setSpokenResponseLong("panning left");
        PanCCW.setWrittenResponse("panning left.");
        PanCCW.setWrittenResponseLong("panning left");
        PanCCW.setAutoListen(true);

        ObjectNode PanCCWNode = nodeFactory.objectNode();
        PanCCWNode.put("Intent", "PAN_CCW");
        PanCCW.setResult(PanCCWNode);

        clientMatchList.add(PanCCW);

        //----------------------Pan clockwise
        ClientMatch PanCW = new ClientMatch();
        PanCW.setExpression("(\"pan\"|\"yaw\"|\"rotate\"|\"turn\"|\"look\").(\"clockwise\"|\"right\")");

        PanCW.setSpokenResponse("panning right.");
        PanCW.setSpokenResponseLong("panning right");
        PanCW.setWrittenResponse("panning right.");
        PanCW.setWrittenResponseLong("panning right");
        PanCW.setAutoListen(true);

        ObjectNode PanCWNode = nodeFactory.objectNode();
        PanCWNode.put("Intent", "PAN_CW");
        PanCW.setResult(PanCWNode);

        clientMatchList.add(PanCW);

        // add the list of matches to the request info object
        requestInfo.setClientMatches(clientMatchList);
        //------------------------------------end of Client Match demo code

        return requestInfo;
    }
}
