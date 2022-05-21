package de.androidcrypto.emvreaderpro100svitlolistview;

import androidx.appcompat.app.AppCompatActivity;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.EditText;
import android.widget.TextView;

import com.pro100svitlo.creditCardNfcReader.CardNfcReaderTask;
import com.pro100svitlo.creditCardNfcReader.enums.EmvCardScheme;
import com.pro100svitlo.creditCardNfcReader.iso7816emv.EmvTags;
import com.pro100svitlo.creditCardNfcReader.model.EmvCard;
import com.pro100svitlo.creditCardNfcReader.parser.EmvParser;
import com.pro100svitlo.creditCardNfcReader.utils.Provider;
import com.pro100svitlo.creditCardNfcReader.utils.ResponseUtils;
import com.pro100svitlo.creditCardNfcReader.utils.TlvUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import fr.devnied.bitlib.BytesUtils;

public class VerifyPin extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    TextView nfcaContent;
    EditText pinToVerify;
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_pin);
        nfcaContent = findViewById(R.id.tvNfcaContentVerify);
        pinToVerify = findViewById(R.id.etPinToVerify);
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

        // first check that a digit number was entered in EditText pinToVerify
        // if not stop the complete process !!!
        String pin = pinToVerify.getText().toString();
        if (pin.length() != 4) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //UI related things, not important for NFC
                    nfcaContent.setText("entered PIN has to be exact 4 digits long !");
                }
            });
            return;
        }
        // now check for digits only, should not happen because EditText is of type number
        if (!pin.matches("[0-9]+")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //UI related things, not important for NFC
                    nfcaContent.setText("entered PIN has to be digits and nothing else !");
                }
            });
            return;
        }
        byte[] pinBytes = hexStringToByteArray(pin);



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
            idContentString = idContentString + "\n" + "pin to verify is: " + bytesToHex(pinBytes);
            EmvCard emvCard = new EmvCard();
            Provider mProvider = new Provider();
            mProvider.setmTagCom(isoDep);
            EmvParser emvParser = new EmvParser(mProvider, true);
            List<byte[]> aidsOnCard = emvParser.readWithPSEAlone();

            // check what the aid to use is
            EmvCard emvCardAid = emvParser.getCard();
            String aidToUse = emvCardAid.getAid();
            idContentString = idContentString + "\n" + "aid to use: " + aidToUse;

            // see: https://stackoverflow.com/questions/21019137/emv-verify-command-returning-69-85/21056054#21056054
            // first select the aid
            response = emvParser.selectAIDAlone(hexStringToByteArray(aidToUse));
            idContentString = idContentString + "\n" + "response after selectAidAlone: " + BytesUtils.bytesToString(response);
            if (ResponseUtils.isSucceed(response)) {
                idContentString = idContentString + "\n" + "response succed";
            } else {
                idContentString = idContentString + "\n" + "response NOT succed, stopping here";
                String finalIdContentString = idContentString;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //UI related things, not important for NFC
                        nfcaContent.setText(finalIdContentString);
                    }
                });
                return;
            }
            // second: get processing options

            // Get PDOL
            //byte[] pdol = TlvUtil.getValue(response, EmvTags.PDOL);
            // Send GPO Command
            //byte[] gpo = emvParser.getGetProcessingOptions(pdol, pProvider);


            byte[] responseGetProcessingOptions = emvParser.getProcessingOptionsForPinVerificationAlone();
            idContentString = idContentString + "\n" + "response after GetProcOptForPin: " + BytesUtils.bytesToString(responseGetProcessingOptions);
            System.out.println("* RESPONSE GPO:\n" + bytesToHex(responseGetProcessingOptions));
            if (ResponseUtils.isSucceed(responseGetProcessingOptions)) {
                idContentString = idContentString + "\n" + "response GPO Pin succed";
            } else {
                idContentString = idContentString + "\n" + "response GPO Pin NOT succed, stopping here";
                String finalIdContentString = idContentString;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //UI related things, not important for NFC
                        nfcaContent.setText(finalIdContentString);
                    }
                });
                return;
            }
            // gpo response bei AAB: 771282021980940c0801010010010101200102009000
            // 82 Application Interchange Profile 1980
            // gpo response bei Lloyds: 7716820219809410080101001001020118010200200102009000
            // 82 Application Interchange Profile 1980
            // gpo response bei dkb visa


            // third: check left pin try before verifying
            int leftPinTry = emvParser.getLeftPinTryAlone();
            idContentString = idContentString + "\n" + "leftPinTry: " + leftPinTry;

            // fourth: verify the pin
            idContentString = idContentString + "\n" + "now we are verifying the entered PIN";
            byte[] responsePinVerification = emvParser.verifyAPinAlone(pin);
            idContentString = idContentString + "\n" + "response after PinVerification: " + BytesUtils.bytesToString(responsePinVerification);
            if (ResponseUtils.isSucceed(responsePinVerification)) {
                idContentString = idContentString + "\n" + "response Pin verification succed";
            } else {
                idContentString = idContentString + "\n" + "response Pin verification NOT succed";
                String finalIdContentString = idContentString;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //UI related things, not important for NFC
                        nfcaContent.setText(finalIdContentString);
                    }
                });
            }
            // third: check left pin try after verifying
            int leftPinTryAfter = emvParser.getLeftPinTryAlone();
            idContentString = idContentString + "\n" + "leftPinTryAfter: " + leftPinTryAfter;


/*
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
*/
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
