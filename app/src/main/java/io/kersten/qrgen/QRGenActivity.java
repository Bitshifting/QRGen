package io.kersten.qrgen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.Encoder;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;
import java.util.UUID;


public class QRGenActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrgen);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_qrgen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void generateDatCode(View view) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();

            BitMatrix bm = writer.encode(((EditText) findViewById(R.id.qrText)).getText().toString(), BarcodeFormat.QR_CODE, 150, 150);
            Bitmap ImageBitmap = Bitmap.createBitmap(144, 144, Bitmap.Config.ARGB_8888);

            for (int i = 0; i < 144; i++) {//width
                for (int j = 0; j < 144; j++) {//height
                    ImageBitmap.setPixel(i, j, bm.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }

            if (ImageBitmap != null) {
                ((ImageView) findViewById(R.id.qrView)).setImageBitmap(ImageBitmap);
            } else {
                Toast.makeText(getApplicationContext(), "blame tito",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (WriterException e) {
            Toast.makeText(getApplicationContext(), "tito broke it",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private final static UUID PEBBLE_APP_UUID = UUID.fromString("4407f2c4-e8ba-4c55-afd8-1938c3c619d8");

    public void checkForDatPebble(View view) {
        boolean connected = PebbleKit.isWatchConnected(getApplicationContext());
        //Toast.makeText(getApplicationContext(), connected ? "Connected AF!" : "hook me up yo", Toast.LENGTH_SHORT).show();
        if (!connected) {
            Toast.makeText(getApplicationContext(), "not connected ):", Toast.LENGTH_SHORT).show();
            return;
        }

        PebbleKit.startAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);

        if (!PebbleKit.areAppMessagesSupported(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "API messages not supported!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Register some callbacks
        PebbleKit.registerReceivedAckHandler(getApplicationContext(), new PebbleKit.PebbleAckReceiver(PEBBLE_APP_UUID) {

            @Override
            public void receiveAck(Context context, int transactionId) {
                Log.i(getLocalClassName(), "Received ack for transaction " + transactionId);
                bumpQueue();
            }

        });

        PebbleKit.registerReceivedNackHandler(getApplicationContext(), new PebbleKit.PebbleNackReceiver(PEBBLE_APP_UUID) {

            @Override
            public void receiveNack(Context context, int transactionId) {
                Log.i(getLocalClassName(), "Received nack for transaction " + transactionId);
                if (retryStack.peek().tries < 3) {
                    try {
                        Thread.sleep(50);
                    }catch(InterruptedException e){

                    }
                    Log.i(getLocalClassName(), "Retrying...");
                    retryStack.peek().tries++;
                    retryStack.peek().send();
                } else {
                    Log.i(getLocalClassName(), "Giving up on this packet...");
                    bumpQueue();
                }
            }

        });

        PebbleKit.registerReceivedDataHandler(this, new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                Log.i(getLocalClassName(), "Received value=" + data.getUnsignedIntegerAsLong(0) + " for key: 0");

                PebbleKit.sendAckToPebble(getApplicationContext(), transactionId);

                if (data.contains(1)) {
                    Log.i(getLocalClassName(), "Got the chunk length! " + data.getUnsignedIntegerAsLong(1));
                    chunkSize = Integer.parseInt("" + data.getUnsignedIntegerAsLong(1));

                    Toast.makeText(getApplicationContext(), "transferring tito", Toast.LENGTH_SHORT).show();
                    transferImageBytes(TITO, chunkSize);
                }
            }

        });

        // We need to determine the chunk size..

        // transfer the current qr code by reading bytes out from the image
        PebbleDictionary data = new PebbleDictionary();

        // 1768777475 = what is your chunk size?
        data.addUint8(1, (byte) 0);
        addToQueue(new PebbleMsg(data));
        //PebbleKit.sendDataToPebble(getApplicationContext(), PEBBLE_APP_UUID, data);

        // chunk size will come back in a callback, then we can start the transfer...


        return;
        //transferImageBytes(TITO, chunkSize);
    }


    /**
     * QR CODE TRANSFER THINGS
     */

    class PebbleMsg {
        private PebbleDictionary data;
        int tries = 0;

        PebbleMsg(PebbleDictionary data) {
            this.data = data;
        }

        void send() {
            PebbleKit.sendDataToPebble(getApplicationContext(), PEBBLE_APP_UUID, data);

        }
    }


    private static byte[] TITO = {
            (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55,
            (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA,
            (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55, (byte)0x55,
            (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA
    };

    // Only send these after the last one has been ack'd
    private static ArrayDeque<PebbleMsg> messageQueue = new ArrayDeque<>();
    private static Stack<PebbleMsg> retryStack = new Stack<>();
    private static boolean waiting = false;

    private static void addToQueue(PebbleMsg msg) {
        if (!waiting) {
            Log.i("Static", "Immediate dispatch.");
            msg.send();
            retryStack.push(msg);
            waiting = true;
        } else {
            Log.i("Static", "Message queued.");
            messageQueue.addLast(msg);
        }
    }

    private static void bumpQueue() {
        if (messageQueue.peek() != null) {
            Log.i("Static", "Dispatch from bump.");
            PebbleMsg p =messageQueue.removeFirst();
            retryStack.push(p);
            p.send();
            waiting = true;
        } else {
            Log.i("Static", "Nothing left.");
            waiting = false;
        }
    }


    static int chunkSize = 0;


    private void transferImageBytes(byte[] bytes, int chunkSize) {

        // Let the pebble know how much data is incoming...
        PebbleDictionary data = new PebbleDictionary();
        data.addUint32(2, bytes.length);
        //PebbleKit.sendDataToPebble(getApplicationContext(), PEBBLE_APP_UUID, data);
        addToQueue(new PebbleMsg(data));

        byte txbuf[] = new byte[chunkSize];
        int start = 0;

        while (start < bytes.length) {
            Log.i(getLocalClassName(), "Sending " + bytes.length + " bytes, buffer size " + txbuf.length + " offset " + start);

            System.arraycopy(bytes, start, txbuf, 0, Math.min(txbuf.length, bytes.length - start));
            data = new PebbleDictionary();
            data.addBytes(3, txbuf);
            addToQueue(new PebbleMsg(data));
            start += txbuf.length;
        }

        Log.i(getLocalClassName(), "alright done sending data...");
        // Done sending...
        data = new PebbleDictionary();
        data.addUint32(4, 0);
        addToQueue(new PebbleMsg(data));

    }
}

