package com.houndify.sample;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.hound.android.fd.HoundSearchResult;
import com.hound.android.libphs.PhraseSpotterReader;
import com.hound.android.fd.Houndify;
import com.hound.android.sdk.VoiceSearchInfo;
import com.hound.android.sdk.audio.SimpleAudioByteStreamSource;
import com.hound.core.model.sdk.CommandResult;
import com.hound.core.model.sdk.HoundResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class MainActivity extends Activity  {
    private TextView textView;
    private TextView msgText;
    private PhraseSpotterReader phraseSpotterReader;
    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    TextToSpeechMgr textToSpeechMgr;
//    private static final String ACTION_USB_PERMISSION =
//            "com.android.example.USB_PERMISSION";
//    private static final int INTERFACE = 1;
//    private static final int ENDPOINT = 0;
//    private static final boolean USE_FORCE = true;
//    private UsbManager manager;
//    private UsbInterface inter;
//    private UsbEndpoint end;
//    private UsbDeviceConnection connection;
//    private boolean gotPermission = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The activity_main layout contains the com.hound.android.fd.HoundifyButton which is displayed
        // as the black microphone. When press it will load the HoundifyVoiceSearchActivity.
        setContentView( R.layout.activity_main );

        // Text view for displaying written result
        textView = (TextView)findViewById(R.id.textView);
        msgText = (TextView)findViewById(R.id.msgText);
        // Setup TextToSpeech
        textToSpeechMgr = new TextToSpeechMgr( this );

        // Normally you'd only have to do this once in your Application#onCreate
        Houndify.get(this).setClientId( Constants.CLIENT_ID );
        Houndify.get(this).setClientKey(Constants.CLIENT_KEY);
        Houndify.get(this).setRequestInfoFactory(StatefulRequestInfoFactory.get(this));

        String s = "USB devices:\n";

//        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
//        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
//        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
//        registerReceiver(mUsbReceiver, filter);
//
//        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
//        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPhraseSpotting();
    }

    /**
     * Called to start the Phrase Spotter
     */
    private void startPhraseSpotting() {
        if ( phraseSpotterReader == null ) {
            phraseSpotterReader = new PhraseSpotterReader(new SimpleAudioByteStreamSource());
            phraseSpotterReader.setListener( phraseSpotterListener );
            phraseSpotterReader.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // if we don't, we must still be listening for "ok hound" so teardown the phrase spotter
        if ( phraseSpotterReader != null ) {
            stopPhraseSpotting();
        }
    }

    /**
     * Called to stop the Phrase Spotter
     */
    private void stopPhraseSpotting() {
        if ( phraseSpotterReader != null ) {
            phraseSpotterReader.stop();
            phraseSpotterReader = null;
        }
    }

    /**
     * Implementation of the PhraseSpotterReader.Listener interface used to handle PhraseSpotter
     * call back.
     */
    private final PhraseSpotterReader.Listener phraseSpotterListener = new PhraseSpotterReader.Listener() {
        @Override
        public void onPhraseSpotted() {

            // It's important to note that when the phrase spotter detects "Ok Hound" it closes
            // the input stream it was provided.
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    stopPhraseSpotting();
                    // Now start the HoundifyVoiceSearchActivity to begin the search.
                    Houndify.get( MainActivity.this ).voiceSearch( MainActivity.this );
                }
            });
        }

        @Override
        public void onError(final Exception ex) {

            // for this sample we don't care about errors from the "Ok Hound" phrase spotter.

        }
    };

    /**
     * The HoundifyVoiceSearchActivity returns its result back to the calling Activity
     * using the Android's onActivityResult() mechanism.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Houndify.REQUEST_CODE) {
            final HoundSearchResult result = Houndify.get(this).fromActivityResult(resultCode, data);

            if (result.hasResult()) {
                onResponse( result.getResponse() );
            }
            else if (result.getErrorType() != null) {
                onError(result.getException(), result.getErrorType());
            }
            else {
                textView.setText("Aborted search");
            }
        }
    }
    /**
     * Send Message via USB for now
     */
//    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (ACTION_USB_PERMISSION.equals(action)) {
//                synchronized (this) {
//                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//
//                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
//                        if(device != null){
//                            inter = device.getInterface(INTERFACE);
//                            end = inter.getEndpoint(ENDPOINT);
//                            connection = manager.openDevice(device);
//                            connection.claimInterface(inter, USE_FORCE);
//                        }
//                        gotPermission = true;
//                    }
//                    else {
//                        Log.d("PERMISSIONS", "permission denied for device " + device);
//                        ((TextView)findViewById(R.id.textView)).setText("permission denied");
//                    }
//                }
//            }
//        }
//    };
    static int crc16(final byte[] buffer) {
        int crc = 0xFFFF;

        for (int j = 0; j < buffer.length ; j++) {
            crc = ((crc  >>> 8) | (crc  << 8) )& 0xffff;
            crc ^= (buffer[j] & 0xff);//byte to int, trunc sign
            crc ^= ((crc & 0xff) >> 4);
            crc ^= (crc << 12) & 0xffff;
            crc ^= ((crc & 0xFF) << 5) & 0xffff;
        }
        crc &= 0xffff;
        return crc;
    }
//  truck position, truck velocity, pan, pan velocity, tilt, tilt velocity
    public void sendMessage(float[] data){
       final byte[] message = new byte[29];
//        short[] trpy = {thrust, roll, pitch, yaw, currentYaw};


        // 85 85 02 10 19 00 73 248
        // 55 55 02 0a 13 00 49 F8

        //gains (type=0x67)
        // kp(int16) kd(int16) kpyaw(int16) kdyaw(int16)
        // meat: 700 100 300 150
        // pwm ticks per radian
        // pwm ticks per radian per second

        // trpy (type=0x70)
        // thrust(int16) roll(int16) pitch(int16) yaw(int16) currentyaw(int16)
        // enablemotors(uint8) = 1 or 0, need to disable then enable if quadrotor went >60 degrees
        // usexternalyaw(unti8) = 0 (unless vicon)

        // return trpy (type = char 't')
        message[0] = 0x55;  // starting bytes
        message[1] = 12;    // length of data
        message[2] = 0x70;  // type
        int message_index = 3;
        for( float var : data){
//            message[message_index+1] = (byte)((var >> 8) & 0xff);
//            message[message_index] = (byte)(var & 0xff);
//            message_index+=2;
            byte[] float_bytes= ByteBuffer.allocate(4).putFloat(var).array();
            for (int i = 0; i< 4; i ++) {
                message[message_index+i] = float_bytes[i];
            }
            message_index = message_index + 4;
        }

        short crc = (short)crc16(Arrays.copyOfRange(message, 1, 27));
        message[27] = (byte)(crc & 0xff);
        message[28] = (byte)((crc >> 8) & 0xff);

//        TextView text = (TextView)findViewById(R.id.text);
        msgText.setText("message :" +Arrays.toString(message));
//        msgText.setText("message : testing");

//        new Thread(new Runnable() {
//            public void run() {
//                connection.bulkTransfer(end, message, message.length, 1000);
//            }
//        }).start();

    }


    /**
     * Called from onActivityResult() above
     *
     * @param response
     */
    private void onResponse(final HoundResponse response) {
        if (response.getResults().size() > 0) {
            // Required for conversational support
            StatefulRequestInfoFactory.get(this).setConversationState(response.getResults().get(0).getConversationState());

            textView.setText("Received response\n\n" + response.getResults().get(0).getWrittenResponse());
            textToSpeechMgr.speak(response.getResults().get(0).getSpokenResponse());
            /**
             * "Client Match" demo code.
             *
             * Houndify client apps can specify their own custom phrases which they want matched using
             * the "Client Match" feature. This section of code demonstrates how to handle
             * a "Client Match phrase".  To enable this demo first open the
             * StatefulRequestInfoFactory.java file in this project and and uncomment the
             * "Client Match" demo code there.
             *
             * Example for parsing "Client Match"
             */
            if ( response.getResults().size() > 0 ) {
                CommandResult commandResult = response.getResults().get( 0 );
                if ( commandResult.getCommandKind().equals("ClientMatchCommand")) {
                    JsonNode matchedItemNode = commandResult.getJsonNode().findValue("MatchedItem");
                    String intentValue = matchedItemNode.findValue( "Intent").textValue();

                    if ( intentValue.equals("TURN_LIGHT_ON") ) {
//                        textToSpeechMgr.speak("Client match TURN LIGHT ON successful");
                        float[] msg_data = {1.5f,0,10,22,33.4f, 99.2f};
                        sendMessage(msg_data);

                    }
                    else if ( intentValue.equals("TURN_LIGHT_OFF") ) {
//                        textToSpeechMgr.speak("Client match TURN LIGHT OFF successful");
                        float[] msg_data = {1.5f,0,0,0,2,5};
                        sendMessage(msg_data);

                    }
                    else if ( intentValue.equals("TURN_LEFT") ) {
                        float[] msg_data = {1.1f,1.1f,1.1f,1.2f,1.3f,1.0f};
                        sendMessage(msg_data);

                    }
                }
            }
        }
        else {
            textView.setText("Received empty response!");
        }
    }

    /**
     * Called from onActivityResult() above
     *
     * @param ex
     * @param errorType
     */
    private void onError(final Exception ex, final VoiceSearchInfo.ErrorType errorType) {
        textView.setText(errorType.name() + "\n\n" + exceptionToString(ex));
    }

    private static String exceptionToString(final Exception ex) {
        try {
            final StringWriter sw = new StringWriter(1024);
            final PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            pw.close();
            return sw.toString();
        }
        catch (final Exception e) {
            return "";
        }
    }


    /**
     * Helper class used for managing the TextToSpeech engine
     */
    class TextToSpeechMgr implements TextToSpeech.OnInitListener {
        private TextToSpeech textToSpeech;

        public TextToSpeechMgr( Activity activity ) {
            textToSpeech = new TextToSpeech( activity, this );
        }

        @Override
        public void onInit( int status ) {
            // Set language to use for playing text
            if ( status == TextToSpeech.SUCCESS ) {
                int result = textToSpeech.setLanguage(Locale.US);
            }
        }

        /**
         * Play the text to the device speaker
         *
         * @param textToSpeak
         */
        public void speak( String textToSpeak ) {
            textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_ADD, null);
        }
    }
}
