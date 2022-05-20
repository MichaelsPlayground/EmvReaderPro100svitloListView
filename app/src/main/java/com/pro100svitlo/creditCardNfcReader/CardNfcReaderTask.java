package com.pro100svitlo.creditCardNfcReader;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;

import com.pro100svitlo.creditCardNfcReader.enums.EmvCardScheme;
import com.pro100svitlo.creditCardNfcReader.model.EmvCard;
import com.pro100svitlo.creditCardNfcReader.parser.EmvParser;
import com.pro100svitlo.creditCardNfcReader.utils.Provider;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class CardNfcReaderTask {

    public final static String CARD_UNKNOWN = EmvCardScheme.UNKNOWN.toString();
    public final static String CARD_VISA = EmvCardScheme.VISA.toString();
    public final static String CARD_NAB_VISA = EmvCardScheme.NAB_VISA.toString();
    public final static String CARD_MASTER_CARD = EmvCardScheme.MASTER_CARD.toString();
    public final static String CARD_AMERICAN_EXPRESS = EmvCardScheme.AMERICAN_EXPRESS.toString();
    public final static String CARD_CB = EmvCardScheme.CB.toString();
    public final static String CARD_LINK = EmvCardScheme.LINK.toString();
    public final static String CARD_JCB = EmvCardScheme.JCB.toString();
    public final static String CARD_DANKORT = EmvCardScheme.DANKORT.toString();
    public final static String CARD_COGEBAN = EmvCardScheme.COGEBAN.toString();
    public final static String CARD_DISCOVER = EmvCardScheme.DISCOVER.toString();
    public final static String CARD_BANRISUL = EmvCardScheme.BANRISUL.toString();
    public final static String CARD_SPAN = EmvCardScheme.SPAN.toString();
    public final static String CARD_INTERAC = EmvCardScheme.INTERAC.toString();
    public final static String CARD_ZIP = EmvCardScheme.ZIP.toString();
    public final static String CARD_UNIONPAY = EmvCardScheme.UNIONPAY.toString();
    public final static String CARD_EAPS = EmvCardScheme.EAPS.toString();
    public final static String CARD_VERVE = EmvCardScheme.VERVE.toString();
    public final static String CARD_TENN = EmvCardScheme.TENN.toString();
    public final static String CARD_RUPAY = EmvCardScheme.RUPAY.toString();
    public final static String CARD_ПРО100 = EmvCardScheme.ПРО100.toString();
    public final static String CARD_ZKA = EmvCardScheme.ZKA.toString();
    public final static String CARD_BANKAXEPT = EmvCardScheme.BANKAXEPT.toString();
    public final static String CARD_BRADESCO = EmvCardScheme.BRADESCO.toString();
    public final static String CARD_MIDLAND = EmvCardScheme.MIDLAND.toString();
    public final static String CARD_PBS = EmvCardScheme.PBS.toString();
    public final static String CARD_ETRANZACT = EmvCardScheme.ETRANZACT.toString();
    public final static String CARD_GOOGLE = EmvCardScheme.GOOGLE.toString();
    public final static String CARD_INTER_SWITCH = EmvCardScheme.INTER_SWITCH.toString();
    public final static String CARD_MIR = EmvCardScheme.MIR.toString();
    public final static String CARD_PROSTIR = EmvCardScheme.PROSTIR.toString();

    private final static String NFC_A_TAG = "TAG: Tech [android.nfc.tech.IsoDep, android.nfc.tech.NfcA]";
    private final static String NFC_B_TAG = "TAG: Tech [android.nfc.tech.IsoDep, android.nfc.tech.NfcB]";
    private final String UNKNOWN_CARD_MESS =
            "===========================================================================\n\n" +
                    "Hi! This library is not familiar with your credit card. \n " +
                    "Please, write me an email with information of your bank: \n" +
                    "country, bank name, card type, etc) and i will try to do my best, \n" +
                    "to add your bank as a known one into this lib. \n" +
                    "Great thanks for using and reporting!!! \n" +
                    "Here is my email: pro100svitlo@gmail.com. \n\n" +
                    "===========================================================================";


    private static final Logger LOGGER = LoggerFactory.getLogger(CardNfcReaderTask.class);

    private Provider mProvider = new Provider();
    private boolean mException;
    private EmvCard mCard;
    private CardNfcAsyncTask.CardNfcInterface mInterface;
    private Tag mTag;
    private String mCardNumber;
    private String mExpireDate;
    private String mCardType;
    private String mLeftPinTry;
    private String mAid;
    public static List<byte[]> mAids; // get it from EmvParser.readWithPSE()

    public String getCardNumber() {
        return mCardNumber;
    }

    public String getCardExpireDate() {
        return mExpireDate;
    }

    public String getCardType() {
        return mCardType;
    }

    public String getLeftPinTry() {
        return mLeftPinTry;
    }

    public String getAid() { return mAid; }

    public List<byte[]> getAids() { return mAids; }

    public void doInBackground(Tag mTag) {
        IsoDep mIsoDep = IsoDep.get(mTag);
        if (mIsoDep == null) {
            mInterface.doNotMoveCardSoFast();
            return;
        }
        mException = false;

        try {
            // Open connection
            mIsoDep.connect();

            mProvider.setmTagCom(mIsoDep);

            EmvParser parser = new EmvParser(mProvider, true);
            mCard = parser.readEmvCard();
            String aid = mCard.getAid();
            System.out.println("mCard1 aid: " + aid);
            System.out.println("mCard1 cardNumber: " + mCard.getCardNumber());
            onPostExecute();
        } catch (IOException e) {
            mException = true;
        } finally {
            IOUtils.closeQuietly(mIsoDep);
        }

    }

    void onPostExecute() {
            if (mCard != null) {
                System.out.println("mCard is NOT NULL ");
                if (StringUtils.isNotBlank(mCard.getCardNumber())) {
                    System.out.println("mCard cardNumber is NOT BLANK");
                    mCardNumber = mCard.getCardNumber();
                    mExpireDate = mCard.getExpireDate();
                    mCardType = mCard.getType().toString();
                    mLeftPinTry = String.valueOf(mCard.getLeftPinTry());
                    mAid = mCard.getAid();
                    if (mCardType.equals(EmvCardScheme.UNKNOWN.toString())) {
                        LOGGER.debug(UNKNOWN_CARD_MESS);
                    }
                } else {
                    System.out.println("mCard is NULL");
                }
            } else {
                System.out.println("mCard StringUtils.isNotBlank is FALSE");
            }
        //clearAll();
    }

    private void clearAll() {
        mInterface = null;
        mProvider = null;
        mCard = null;
        mTag = null;
        mCardNumber = null;
        mExpireDate = null;
        mCardType = null;
    }
}
