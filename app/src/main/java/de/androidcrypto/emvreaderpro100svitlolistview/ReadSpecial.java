package de.androidcrypto.emvreaderpro100svitlolistview;

import androidx.appcompat.app.AppCompatActivity;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.TextView;

import com.pro100svitlo.creditCardNfcReader.CardNfcReaderTask;
import com.pro100svitlo.creditCardNfcReader.enums.EmvCardScheme;
import com.pro100svitlo.creditCardNfcReader.model.EmvCard;
import com.pro100svitlo.creditCardNfcReader.parser.EmvParser;
import com.pro100svitlo.creditCardNfcReader.utils.Provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.devnied.bitlib.BytesUtils;

public class ReadSpecial extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    TextView nfcaContent;
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_special);
        nfcaContent = findViewById(R.id.tvNfcaContentSpecial);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
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
            isoDep.connect();
            byte[] response;
            String idContentString = "Content of ISO-DEP tag special";
            EmvCard emvCard = new EmvCard();
            Provider mProvider = new Provider();
            mProvider.setmTagCom(isoDep);
            EmvParser emvParser = new EmvParser(mProvider, true);
            List<byte[]> aidsOnCard = emvParser.readWithPSEAlone();
            // now the corresponding application labels are available
            List<String> applicationLabels = new ArrayList<>();
            applicationLabels = emvParser.getApplicationLabels();
            idContentString = idContentString + "\n" + "number of aids on card: " + aidsOnCard.size();
            idContentString = idContentString + "\n" + "number of application label on card: " + applicationLabels.size();
            System.out.println("number of application label on card: " + applicationLabels.size());
            idContentString = idContentString + "\n" + "------------------------";
            for (int j = 0; j < applicationLabels.size(); j++) {
                idContentString = idContentString + "\n" + "card applicationLabel " + j + " " + applicationLabels.get(j);
            }
            idContentString = idContentString + "\n" + "------------------------";
            idContentString = idContentString + "\n" + "------------------------";

            for (int i = 0; i < aidsOnCard.size(); i++) {
                idContentString = idContentString + "\n" + "aid " + i + " data: " + BytesUtils.bytesToStringNoSpace(aidsOnCard.get(i));
                System.out.println("** aid " + i + " : " + BytesUtils.bytesToString(aidsOnCard.get(i)));
                emvParser.readWithAIDAlone(emvCard, aidsOnCard.get(i), "ALONE");
                if (emvCard != null) {
                    idContentString = idContentString + "\n" + "card number: " + prettyPrintCardNumber(emvCard.getCardNumber());
                    idContentString = idContentString + "\n" + "card type: " + emvCard.getType();
                    idContentString = idContentString + "\n" + "card expiration date (MM/YY): " + emvCard.getExpireDate();
                }
                String aidString = BytesUtils.bytesToString(aidsOnCard.get(i));
                EmvCardScheme emvCardScheme = emvParser.findCardSchemeAlone(aidString, emvCard.getCardNumber());
                idContentString = idContentString + "\n" + "card scheme: " + emvCardScheme;
                idContentString = idContentString + "\n" + "card scheme: " + emvCardScheme.getName();
                idContentString = idContentString + "\n" + "------------------------";
            }

            // todo read list of transactions

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
