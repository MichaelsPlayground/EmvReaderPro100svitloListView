package de.androidcrypto.emvreaderpro100svitlolistview;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.pro100svitlo.creditCardNfcReader.CardNfcReaderTask;
import com.pro100svitlo.creditCardNfcReader.model.EmvCard;
import com.pro100svitlo.creditCardNfcReader.parser.EmvParser;
import com.pro100svitlo.creditCardNfcReader.utils.Provider;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import fr.devnied.bitlib.BytesUtils;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    TextView nfcaContent;
    private NfcAdapter mNfcAdapter;
    Button readSpecial, verifyPin , verifyPin2;
    Intent readSpecialIntent, verifyPinIntent, verifyPin2Intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfcaContent = findViewById(R.id.tvNfcaContent);
        readSpecial = findViewById(R.id.btnReadSpecial);
        readSpecialIntent = new Intent(MainActivity.this, ReadSpecial.class);
        verifyPin = findViewById(R.id.btnVerifyPin);
        verifyPinIntent = new Intent(MainActivity.this, VerifyPin.class);
        verifyPin2 = findViewById(R.id.btnVerifyPin2);
        verifyPin2Intent = new Intent(MainActivity.this, VerifyPin2.class);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        readSpecial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(readSpecialIntent);
            }
        });
        verifyPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(verifyPinIntent);
            }
        });
        verifyPin2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(verifyPin2Intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {
            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag after reading
            mNfcAdapter.enableReaderMode(this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(this);
    }

    // This method is run in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {

        IsoDep isoDep = null;

        // Whole process is put into a big try-catch trying to catch the transceive's IOException
        try {
            isoDep = IsoDep.get(tag);
            if (isoDep != null) {
                ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, 10));
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //UI related things, not important for NFC
                    nfcaContent.setText("");
                }
            });
            //isoDep.connect();
            byte[] response;
            String idContentString = "Content of ISO-DEP tag";


            CardNfcReaderTask cardNfcReaderTask = new CardNfcReaderTask();
            cardNfcReaderTask.doInBackground(tag);
            idContentString = idContentString + "\n" + "card number: " + prettyPrintCardNumber(cardNfcReaderTask.getCardNumber());
            idContentString = idContentString + "\n" + "card type: " + cardNfcReaderTask.getCardType();
            idContentString = idContentString + "\n" + "card expiration date (MM/YY): " + cardNfcReaderTask.getCardExpireDate();
            idContentString = idContentString + "\n" + "card left pin try: " + cardNfcReaderTask.getLeftPinTry();
            idContentString = idContentString + "\n" + "card AID used: " + cardNfcReaderTask.getAid();
            // now checking the aids available on card
            List<byte[]> aidsOnCard = cardNfcReaderTask.getAids();
            idContentString = idContentString + "\n" + "number of aids on card: " + aidsOnCard.size();
            for (int i = 0; i < aidsOnCard.size(); i++) {
                idContentString = idContentString + "\n" + "aid " + i + " data: " + BytesUtils.bytesToStringNoSpace(aidsOnCard.get(i));

/*
                EmvCard emvCard = null;
                Provider mProvider = new Provider();
                mProvider.setmTagCom(isoDep);
                EmvParser emvParser = new EmvParser(mProvider, true);
                emvCard = emvParser.extractPublicDataAlone(aidsOnCard.get(i), "ALONE");
                if (emvCard != null) {
                    idContentString = idContentString + "\n" + "card number: " + prettyPrintCardNumber(emvCard.getCardNumber());
                    idContentString = idContentString + "\n" + "card type: " + emvCard.getType();
                    idContentString = idContentString + "\n" + "card expiration date (MM/YY): " + emvCard.getExpireDate();
                }*/
            }
            idContentString = idContentString + "\n" + "";
            idContentString = idContentString + "\n" + "";
            String finalIdContentString = idContentString;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //UI related things, not important for NFC
                    nfcaContent.setText(finalIdContentString);
                }
            });


            try {
                isoDep.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            //Trying to catch any exception that may be thrown
            e.printStackTrace();
        }

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    public static String prettyPrintCardNumber(String cardNumber) {
        if (cardNumber == null) return null;
        char delimiter = ' ';
        return cardNumber.replaceAll(".{4}(?!$)", "$0" + delimiter);
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
