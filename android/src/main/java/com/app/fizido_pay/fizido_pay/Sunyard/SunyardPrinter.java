package com.app.fizido_pay.fizido_pay.Sunyard;

import android.content.Context;

import androidx.annotation.NonNull;

import com.socsi.exception.SDKException;
import com.socsi.smartposapi.printer.Align;
import com.socsi.smartposapi.printer.FontLattice;
import com.socsi.smartposapi.printer.FontType;
import com.socsi.smartposapi.printer.Printer2;
import com.socsi.smartposapi.printer.TextEntity;

import io.flutter.plugin.common.MethodCall;


public class SunyardPrinter {

    private Printer2 print;
    private FontType mCh = FontType.SIMSUM, mEn = FontType.AVENIR_NEXT_CONDENSED_BLOD;

    public SunyardPrinter(Context context) {
        initPrinter(context);
    }


    private void initPrinter(Context context){
         print = Printer2.getInstance(context);
         boolean havePrinter = false;
         try {
             havePrinter = print.havePrinter();
         } catch (SDKException e) {
             e.printStackTrace();
         }
         if (havePrinter) {
            // Utils.popToast(context, "pos Supports printing");

         }
     }

    public void startPrint(@NonNull MethodCall call) {
        //String stan, int originalMinorAmount, String terminalId, String merchantId, String transmissionDate, String transactionComment
//        Bitmap bitmap = BitmapFactory.decodeResource();
//        Bitmap bitmapForWatermark  = print.createBitmapForWatermark(bitmap);
//        print.appendImage(bitmap);
//        print.appendImage(bitmapForWatermark, Align.CENTER);

        print.appendTextEntity2(new TextEntity("Transaction Receipt\n", mCh, mEn, FontLattice.THIRTY, false, Align.CENTER,true));
        print.appendTextEntity2(print.getSeparatorLinetEntity());
        print.appendTextEntity2(new TextEntity("AMOUNT: NGN" +call.argument("originalMinorAmount").toString() + "\n", mCh, mEn, FontLattice.THIRTY, false, Align.CENTER,true));
        print.appendTextEntity2(print.getSeparatorLinetEntity());
        print.appendTextEntity2(new TextEntity("TerminalID: "+call.argument("terminalId")+ "\n", mCh, mEn, FontLattice.TWENTY_FOUR, false, Align.LEFT,true));
        print.appendTextEntity2(new TextEntity("MerchantID: "+call.argument("merchantId")+ "\n", mCh, mEn, FontLattice.TWENTY_FOUR, false, Align.LEFT,true));
        print.appendTextEntity2(new TextEntity("Stan:   "+call.argument("originalTransStan")+ "\n", mCh, mEn, FontLattice.TWENTY_FOUR, false, Align.LEFT,true));
        print.appendTextEntity2(new TextEntity("Date:   "+ call.argument("transmissionDate")+ "\n", mCh, mEn, FontLattice.TWENTY_FOUR, false, Align.LEFT,true));
        print.appendTextEntity2(new TextEntity("Card PAN:    "+call.argument("cardPan")+ "\n", mCh, mEn, FontLattice.TWENTY_FOUR, false, Align.LEFT,true));
        print.appendTextEntity2(new TextEntity("Card Expiry: "+call.argument("expiryDate")+ "\n", mCh, mEn, FontLattice.TWENTY_FOUR, false, Align.LEFT,true));
        print.appendTextEntity2(new TextEntity("Ref: "+call.argument("authCode")+ "\n\n", mCh, mEn, FontLattice.TWENTY_FOUR, false, Align.LEFT,true));
        print.appendTextEntity2(print.getSeparatorLinetEntity());
        print.appendTextEntity2(new TextEntity(call.argument("transactionComment")+ "\n", mCh, mEn, FontLattice.THIRTY, false, Align.CENTER,true));
        print.appendTextEntity2(print.getSeparatorLinetEntity());
        print.limitTimePrint(10, print.getPrintBuffer());
        print.startPrint();
    }

}
