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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.TextView;
import java.lang.Math;

import com.fasterxml.jackson.databind.JsonNode;
import com.hound.android.fd.HoundSearchResult;
import com.hound.android.libphs.PhraseSpotterReader;
import com.hound.android.fd.Houndify;
import com.hound.android.sdk.VoiceSearchInfo;
import com.hound.android.sdk.audio.SimpleAudioByteStreamSource;
import com.hound.core.model.sdk.CommandResult;
import com.hound.core.model.sdk.HoundResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class MainActivity extends Activity  {
    private static final float PI = 3.1415926f;
    private TextView textView;
    private TextView msgText;
    private TextView msgText2;
    private  float pan_rad = PI/2.0f;
    private  float tilt_rad = 0.0f;
    private  float lin_pos = 0.0f;  //0 - 125 radians  or 0 - .8meters
    private  float slow_vel = 0.75f; // rad /second
    private  float lin_slow_vel = 7.0f;

    private PhraseSpotterReader phraseSpotterReader;
    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    TextToSpeechMgr textToSpeechMgr;

    private static final String TAG = "Main Activity";
    private MjpegView mv;

    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private static final int INTERFACE = 1;
    private static final int ENDPOINT = 0;
    private static final boolean USE_FORCE = true;
    private UsbManager manager;
    private UsbInterface inter;
    private UsbEndpoint end;
    private UsbDeviceConnection connection;
    private boolean gotPermission = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The activity_main layout contains the com.hound.android.fd.HoundifyButton which is displayed
        // as the black microphone. When press it will load the HoundifyVoiceSearchActivity.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView( R.layout.activity_main );
        // Text view for displaying written result
        textView = (TextView)findViewById(R.id.textView);
        msgText = (TextView)findViewById(R.id.msgText);
        msgText2 = (TextView)findViewById(R.id.msgText2);

        // Setup TextToSpeech
        textToSpeechMgr = new TextToSpeechMgr( this );

        // Normally you'd only have to do this once in your Application#onCreate
        Houndify.get(this).setClientId(Constants.CLIENT_ID);
        Houndify.get(this).setClientKey(Constants.CLIENT_KEY);
        Houndify.get(this).setRequestInfoFactory(StatefulRequestInfoFactory.get(this));

        String s = "USB devices:\n";


        /**
         * ip streaming
          */

//        String URL = "http://158.130.109.221:8080/video";
//        String URL = "http://158.130.110.0:8080/video";
         String URL = "http://158.130.109.54:8080/video";

        /**
         * usb sending
         */

        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        if (deviceIterator.hasNext()){
            UsbDevice device  = deviceIterator.next();
            manager.requestPermission(device, mPermissionIntent);
            s += device.toString() + "\n";
            s += "num usbInterfaces: " + String.valueOf(device.getInterfaceCount()) + "\n";
            msgText2.setText(s);
        }
        else {
            msgText2.setText("There is nothing connected");
        }

        mv = (MjpegView) findViewById(R.id.mv);
        new DoRead().execute(URL);

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
    public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... url) {
            //TODO: if camera has authentication deal with it and don't just not work
            HttpResponse res = null;
            DefaultHttpClient httpclient = new DefaultHttpClient();
            Log.d(TAG, "1. Sending http request");
            try {
                res = httpclient.execute(new HttpGet(URI.create(url[0])));
                Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                if(res.getStatusLine().getStatusCode()==401){
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(res.getEntity().getContent());
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                Log.d(TAG, "Request failed-ClientProtocolException", e);
                //Error connecting to camera
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Request failed-IOException", e);
                //Error connecting to camera
            }

            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            mv.setSource(result);
            mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
//            mv.setDisplayMode(R.layout.main);

            mv.showFps(true);
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
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            inter = device.getInterface(INTERFACE);
                            end = inter.getEndpoint(ENDPOINT);
                            connection = manager.openDevice(device);
                            connection.claimInterface(inter, USE_FORCE);
                        }
                        gotPermission = true;
                    }
                    else {
                        Log.d("PERMISSIONS", "permission denied for device " + device);
                        ((TextView)findViewById(R.id.msgText)).setText("permission denied");
                    }
                }
            }
        }
    };
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
    public void sendMessage(byte msgtype, float[] data){
        final byte[] message = new byte[13];
        message[0] = 0x55;  // starting bytes
        message[1] = 8;    // length of data
        message[2] = msgtype;  // type
        int message_index = 3;
        for( float var : data){
//            message[message_index+1] = (byte)((var >> 8) & 0xff);
//            message[message_index] = (byte)(var & 0xff);
//            message_index+=2;
            byte[] float_bytes= ByteBuffer.allocate(4).putFloat(var).array();
            for (int i = 0; i< 4; i ++) {
                message[message_index+i] = float_bytes[3-i];
            }
            message_index = message_index + 4;
        }

        short crc = (short)crc16(Arrays.copyOfRange(message, 1, 11));
        message[11] = (byte)(crc & 0xff);
        message[12] = (byte)((crc >> 8) & 0xff);

        msgText.setText("message :" +Arrays.toString(message));

        new Thread(new Runnable() {
            public void run() {
                connection.bulkTransfer(end, message, message.length, 1000);
            }
        }).start();

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

                    if ( intentValue.equals("MOVE_LEFT") ) {
                        if( lin_pos >40.0f) {
                            lin_pos  = lin_pos - 40.0f; //rads  0- 125
                        }
                        msgText2.setText(Float.toString(lin_pos));

                        float[] msg_data_96 = {lin_slow_vel, lin_pos}; //linear
                        float[] msg_data_97 = {slow_vel, tilt_rad}; //pan
                        float[] msg_data_98 = {slow_vel, pan_rad}; //tilt
                        sendMessage((byte)170, msg_data_96); //linear
//                        sendMessage((byte)171, msg_data_97); //pan
//                        sendMessage((byte)172, msg_data_98); //tilt

                    }
                    else if( intentValue.equals("MOVE_RIGHT") ) {
                        if( lin_pos+40.0f <110.0f) {
                            lin_pos  = lin_pos +40.0f; //rads  0- 125
                        }
                        msgText2.setText(Float.toString(lin_pos));
                        float[] msg_data_96 = {lin_slow_vel, lin_pos}; //linear
                        float[] msg_data_97 = {slow_vel, tilt_rad}; //pan
                        float[] msg_data_98 = {slow_vel, pan_rad}; //tilt
                        sendMessage((byte)170, msg_data_96); //linear
//                        sendMessage((byte)171, msg_data_97); //pan
//                        sendMessage((byte)172, msg_data_98); //tilt

                    }
                    else if( intentValue.equals("PAN_CCW") ) {
                        if (pan_rad+0.25f*PI < PI/2.0f) {
                            pan_rad = pan_rad + (0.25f * PI); //45 degrees
                        }
                        msgText2.setText(Float.toString(pan_rad));

                        float[] msg_data_96 = {lin_slow_vel, lin_pos}; //linear
                        float[] msg_data_97 = {slow_vel, tilt_rad}; //pan
                        float[] msg_data_98 = {slow_vel, pan_rad}; //tilt
//                        sendMessage((byte)170, msg_data_96); //linear
                        sendMessage((byte)172, msg_data_97); //pan
//                        sendMessage((byte)172, msg_data_98); //tilt

                    }
                    else if( intentValue.equals("PAN_CW") ) {
                        if (pan_rad-(0.25f*PI) > -PI/2.0f) {
                            pan_rad = pan_rad - (0.25f * PI); //45 degrees
                        }
                        msgText2.setText(Float.toString(pan_rad));
                        float[] msg_data_96 = {lin_slow_vel, lin_pos}; //linear
                        float[] msg_data_97 = {slow_vel, tilt_rad}; //pan
                        float[] msg_data_98 = {slow_vel, pan_rad}; //tilt
//                        sendMessage((byte)170, msg_data_96); //linear
                        sendMessage((byte)172, msg_data_97); //pan
//                        sendMessage((byte)172, msg_data_98); //tilt

                    }
                    else if( intentValue.equals("TILT_UP") ) {
                        if ((tilt_rad + 0.25f*PI) < PI  ){
                            tilt_rad = tilt_rad + (0.25f * PI); //45 degrees
                        }
                        msgText2.setText(Float.toString(tilt_rad));

                        float[] msg_data_96 = {lin_slow_vel, lin_pos}; //linear
                        float[] msg_data_97 = {slow_vel, tilt_rad}; //pan
                        float[] msg_data_98 = {slow_vel, pan_rad}; //tilt
//                        sendMessage((byte)170, msg_data_96); //linear
//                        sendMessage((byte)171, msg_data_97); //pan
                        sendMessage((byte)171, msg_data_98); //tilt

                    }
                    else if( intentValue.equals("TILT_DOWN") ) {
                        if ((tilt_rad - 0.25f*PI) > 0.0f) {
                            tilt_rad = tilt_rad - (0.25f * PI); //45 degrees
                        }
                        msgText2.setText(Float.toString(tilt_rad));

                        float[] msg_data_96 = {lin_slow_vel, lin_pos}; //linear
                        float[] msg_data_97 = {slow_vel, tilt_rad}; //pan
                        float[] msg_data_98 = {slow_vel, pan_rad}; //tilt
//                        sendMessage((byte)170, msg_data_96); //linear
//                        sendMessage((byte)171, msg_data_97); //pan
                        sendMessage((byte)171, msg_data_98); //tilt

                    }

                    else if( intentValue.equals("STOP") ) {

                        float[] msg_data_96 = {0.0f, lin_pos}; //linear
                        float[] msg_data_97 = {0.0f, tilt_rad}; //pan
                        float[] msg_data_98 = {0.0f, pan_rad}; //tilt
                        sendMessage((byte) 170, msg_data_96); //linear
                        sendMessage((byte) 171, msg_data_97); //pan
                        sendMessage((byte) 172, msg_data_98); //tilt
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
    public void onEstop(View v)
    {
        float[] msg_data_96 = {0.0f, lin_pos}; //linear
        float[] msg_data_97 = {0.0f, tilt_rad}; //pan
        float[] msg_data_98 = {0.0f, pan_rad}; //tilt
        sendMessage((byte) 170, msg_data_96); //linear
        sendMessage((byte) 171, msg_data_97); //pan
        sendMessage((byte) 172, msg_data_98); //tilt
        msgText2.setText("STOPPING NOW!");
    }
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
