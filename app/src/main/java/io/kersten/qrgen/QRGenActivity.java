package io.kersten.qrgen;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.Encoder;


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

            BitMatrix bm = writer.encode(((EditText)findViewById(R.id.qrText)).getText().toString(), BarcodeFormat.QR_CODE, 150, 150);
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
}
