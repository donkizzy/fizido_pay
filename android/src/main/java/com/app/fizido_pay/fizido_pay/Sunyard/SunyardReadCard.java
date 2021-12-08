package com.app.fizido_pay.fizido_pay.Sunyard;

import static com.socsi.smartposapi.icc.Icc.IC_CARD_ON;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;


import com.app.fizido_pay.fizido_pay.FizidoPayPlugin;
import com.app.fizido_pay.fizido_pay.Sunyard.bean.TlvBean;
import com.app.fizido_pay.fizido_pay.Sunyard.util.TlvUtils;
import com.pos.empressa.empressa_pos.KSNUtilities;
import com.socsi.aidl.pinservice.OperationPinListener;
import com.socsi.exception.SDKException;
import com.socsi.smartposapi.card.CardReader;
import com.socsi.smartposapi.card.IcCardSearchCallback;
import com.socsi.smartposapi.card.MagCardSearchCallback;
import com.socsi.smartposapi.card.RfSearchCallback;
import com.socsi.smartposapi.card.rf.CardReaderConst;
import com.socsi.smartposapi.card.rf.RFSearchResultInfo;
import com.socsi.smartposapi.card.rf.UltralightDriver;
import com.socsi.smartposapi.emv2.AsyncEmvCallback;
import com.socsi.smartposapi.emv2.EmvL2;
import com.socsi.smartposapi.icc.Icc;
import com.socsi.smartposapi.magcard.CardInfo;
import com.socsi.smartposapi.magcard.Magcard;
import com.socsi.smartposapi.ped.Ped;
import com.socsi.smartposapi.terminal.TerminalManager;
import com.socsi.utils.DateUtil;
import com.socsi.utils.HexUtil;
import com.socsi.utils.Log;
import com.socsi.utils.StringUtil;
import com.socsi.utils.TlvUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodChannel;
import socsi.middleware.emvl2lib.EmvAidCandidate;
import socsi.middleware.emvl2lib.EmvApi;
import socsi.middleware.emvl2lib.EmvCallback;
import socsi.middleware.emvl2lib.EmvCallbackGetPinResult;
import socsi.middleware.emvl2lib.EmvErrorCode;
import socsi.middleware.emvl2lib.EmvStartProcessParam;
import socsi.middleware.emvl2lib.EmvTermConfig;

public class SunyardReadCard {


    private int channelType;
    private final Context mContext;
    String panNumber ;
    String cardPin = "";

    public SunyardReadCard(Context mContext) {
        this.mContext = mContext;
    }


    public void searchCard(@NonNull MethodChannel.Result result,int transactionAmount) {
        try {
            CardReader cardReader = CardReader.getInstance();
            cardReader.setMagCardSearchCallback(new MagCardSearchCallback() {

                @Override
                public void onSearchResult(int ret) {
                    switch (ret) {
                        case 0:
                            CardInfo cardInfo = null;
                            try {
                                cardInfo = Magcard.getInstance().readUnEncryptTrack();
                            } catch (SDKException e) {
                                e.printStackTrace();
                            }
                            //updateResult(cardInfo.toString());
                            break;
                        case 1:
                            //updateResult("read timeout");
                            break;
                        default:
                            //updateResult("read fail:" + ret);
                            break;
                    }
                }
            });
            cardReader.setIcCardSearchCallback(new IcCardSearchCallback() {
                @Override
                public void onSearchResult(int ret) {
                    switch (ret) {
                        case 0:
                            channelType = EmvStartProcessParam.EMV_API_CHANNEL_FROM_ICC;
                            startProcess(result,transactionAmount);
                            return;
                        case 1:
                            //updateResult("read timeout");
                            break;
                        case 2:
                            //updateResult("read fail: unknown card type");
                            break;
                        default:
                            //updateResult("read fail:" + ret);
                            break;
                    }
                }
            });
            cardReader.setRfSearchCallback(new RfSearchCallback() {
                @Override
                public void onSearchResult(int ret, int cardType, byte[] SerialNumber, byte[] ATQA) {
                    Log.i("cardType", String.valueOf(cardType));
                    if (ret == 0) {
                        byte isICC = Icc.getInstance().checkCardOn((byte) 0x00);
                        byte isNFC = Icc.getInstance().checkCardOn((byte) 0x01);
                        Log.i("test", isICC + " " + isNFC);
                        channelType = EmvStartProcessParam.EMV_API_CHANNEL_FORM_PICC;
                        startProcess(result,transactionAmount);
                        return;
                    } else if (ret == 1) {
                        //updateResult("read timeout");
                    } else {
                        //updateResult("read fail:" + ret);
                    }
                }
            });
            byte type = 0x00;
            type = (byte) (type | 0x01);    //support magnetic card
            type = (byte) (type | 0x02);    //support ic card
            type = (byte) (type | 0x04);    //support non-contact card
            cardReader.searchCard(type, 20 * 1000);
        } catch (Exception e) {
            e.printStackTrace();
            stopSearch();
            //updateResult("search card fail");
        }
    }

    private void startProcess(@NonNull MethodChannel.Result result,int transactionAmount) {
        com.sunyard.smartposapi.emv2.EmvL2 emvL2 = com.sunyard.smartposapi.emv2.EmvL2.getInstance(mContext, FizidoPayPlugin.class.getSimpleName());
        emvL2.init();
        com.sunyard.middleware.emvl2lib.EmvStartProcessParam param = new com.sunyard.middleware.emvl2lib.EmvStartProcessParam();
        param.mTransAmt = transactionAmount;
        param.mTransDate = StringUtil.hexStr2Bytes(DateUtil.getCurDateStr("yyMMdd"));
        param.mTransTime = StringUtil.hexStr2Bytes(DateUtil.getCurDateStr("HHmmss"));
        param.mChannelType = channelType;
        param.mTag9CValue = 0;

        if (channelType == EmvStartProcessParam.EMV_API_CHANNEL_FORM_PICC) {
            param.mProcType = EmvStartProcessParam.EMV_API_PROC_QPBOC;
        } else {
            param.mProcType = EmvStartProcessParam.EMV_API_PROC_PBOC_FULL;
        }
        param.mTransType = EmvStartProcessParam.EMV_API_TRANS_TYPE_NORMAL;
        param.mTermCountryCode = "566".getBytes();
        param.mTransCurrCode = "566".getBytes();
        emvL2.startProcess(param, new com.sunyard.smartposapi.emv2.AsyncEmvCallback() {
            @Override
            public void panConfirm(final byte[] pan, final com.sunyard.smartposapi.emv2.EmvL2.PanConfirmHandler handler) {
                panNumber = StringUtil.byte2HexStr(pan) ;
                //updateResult("Na The Pan be this"+panNumber);
                Log.d("FizidoPayPlugin", "EMVDevice process startProcess panCofirm");
                handler.onPanConfirm(0);

            }

            @Override
            public void getPin(final int type, final int retryTimes, final com.sunyard.smartposapi.emv2.EmvL2.GetPinHandler handler) {
                //updateResult("[getPin]type:" + type + ", retryTimes:" + retryTimes);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Boolean isOnlinePin = true;
                        if (type == EmvCallback.EMV_ONLINEPIN || type == EmvCallback.EMV_EC_ONLINEPIN) {
                            isOnlinePin = true;
                        } else if (type == EmvCallback.EMV_OFFLINEPIN || type == EmvCallback.EMV_OFFLINE_ONLY) {
                            isOnlinePin = false;
                        } else {
                            Log.d("FizidoPayPlugin", "EMVDevice SyncEmvCallback getPin type error: type" + type);
                        }
                        Bundle param = new Bundle();
                        param.putBoolean("isOnline", false);
                        param.putString("pan", getpanData());//"6274311520010841"
                        param.putString("promptString", "Enter Card PIN");
                        param.putIntArray("pinLimit", new int[]{0, 4, 6});
                        param.putInt("pinAlgMode", Ped.DES_TYPE_DES);
                        param.putInt("keysType", Ped.DES_TYPE_DES);
                        param.putInt("desType", Ped.DES_TYPE_DES);
                        param.putInt("timeout", 60);
                        try {
                            Ped.getInstance().startPinInput(mContext, 0x01, param, new OperationPinListener() {

                                @Override
                                public void onInput(int len, int key) {
                                    Log.d("FizidoPayPlugin", "onInput  len:" + len + "  key:" + key);
                                }

                                @Override
                                public void onError(int errorCode) {
                                    stopSearch();
                                    Log.d("FizidoPayPlugin", "onError   errorCode:" + errorCode);
                                }

                                @Override
                                public void onConfirm(final byte[] data, boolean isNonePin) {

                                    Log.d("FizidoPayPlugin", "onConfirm   data:" + HexUtil.toString(data) + "  isNonePin:" + isNonePin);
                                    if (!isNonePin) {
                                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                                            @Override
                                            public void run() {
                                                getClearPin(data);
                                                Toast.makeText(mContext, "Pin Ok"  , Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                    handler.onGetPin(com.sunyard.middleware.emvl2lib.EmvCallbackGetPinResult.CV_PIN_SUCC, data);
                                }

                                @Override
                                public void onCancel()
                                {
                                    stopSearch();
                                    initEmv();
                                    Log.d("FizidoPayPlugin", "onCancel");
                                }
                            });

                        } catch (SDKException e) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    result.error("1-0-0","An error happened",e.toString());
                                }
                            });
                            e.printStackTrace();
                        }
                    }
                }).start();

            }

            @Override
            public void aidSelect(final String[] aidNames, final com.sunyard.smartposapi.emv2.EmvL2.AidSelectHandler handler) {
                Log.d("FizidoPayPlugin", "EMVDevice process startProcess aidSelect"+ Arrays.toString(aidNames));
                handler.onAidSelect(0);
            }

            @Override
            public void termRiskManager(byte[] pan, int panSn, com.sunyard.smartposapi.emv2.EmvL2.TermRiskManageHandler handler) {
                //updateResult("[termRiskManager]pan:" + new String(pan) + ", panSn:" + panSn);
                handler.onTermRiskManager(0, 0);
            }

            @Override
            public void issuerReference(byte[] pan, com.sunyard.smartposapi.emv2.EmvL2.IssuerReferenceHandler handler) {
                //updateResult("[issuerReference]pan:" + new String(pan));
                handler.onIssuerReference(0);
            }

            @Override
            public void accountTypeSelect(com.sunyard.smartposapi.emv2.EmvL2.AccountTypeSelectHandler handler) {
                //updateResult("[accountTypeSelect]");
                handler.onAccountType(1);
            }

            @Override
            public void certConfirm(byte[] type, byte[] certNum, com.sunyard.smartposapi.emv2.EmvL2.CertConfirmHandler handler) {
                //updateResult("[certConfirm]type:" + new String(type) + ", certNum:" + new String(certNum));
                handler.onCertConfirm(1);
            }

            @Override
            public void lcdMsg(byte[] title, byte[] msg, boolean isYesNo, int timeout, com.sunyard.smartposapi.emv2.EmvL2.LcdMsgHandler handler) {
                //updateResult("[lcdMsg]title:" + new String(title) + ", msg:" + new String(msg) + ", isYesNo:" + isYesNo + ", timeout:" + timeout);
                handler.onLcdMsg(1);
            }

            @Override
            public void confirmEC(com.sunyard.smartposapi.emv2.EmvL2.ConfirmEcHandler handler) {
                //updateResult("[accountTypeSelect]");
                handler.onConfirmEc(0);
            }

            @Override
            public void processResult(int ret) {
                Log.d("jp", new String("身份证".getBytes()));
                Log.d("FizidoPayPlugin", "[processResult]ret:" + ret);
                byte[] tag = StringUtil.hexStr2Bytes("579F269F279F109F379F36959A9F219C9F025F2A829F1A9F039F339F349F359F1E9F06849F099F419F535F345A575F249F079B9F119F125F20995F255F30509F4000");
                if (ret == 0 || ret == 1 || ret == 2) {
                    byte[] tlvData = emvL2.getTLVData(tag);

                    HashMap<String, String> cardDataMap = (HashMap<String, String>) TlvUtil.tlvToMap(tlvData);


                    KSNUtilities ksnUtilitites = new KSNUtilities();
                    String workingKey = ksnUtilitites.getWorkingKey("3F2216D8297BCE9C",getInitialKSN()) ;
                    String pinBlock =  ksnUtilitites.DesEncryptDukpt(workingKey , getpanData(),cardPin);
                    cardDataMap.put("CardPin",pinBlock);
                    cardDataMap.put("ksn",ksnUtilitites.getLatestKsn());
                    cardDataMap.put("pan",panNumber);

                    Log.d("Trying Something",cardDataMap.toString());
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {

                            result.success(cardDataMap);
                        }
                    });

                    if (tlvData != null) {
                        List<TlvBean> tlvList = TlvUtils.builderTlvList(StringUtil.byte2HexStr(tlvData));
                        String result = "";
                        if (tlvList != null) {
                            for (TlvBean tlv : tlvList) {
                                result += "\ntag:" + tlv.getTag();
                                result += " value:" + tlv.getValue();
                            }
                        }
                        Log.d("FizidoPayPlugin", "[getTLVData]" + StringUtil.byte2HexStr(tlvData));
                    } else {
                    }
                } else if (ret == 3) {
                    Log.d("FizidoPayPlugin", "Magnetic card");
                    //updateResult("Magnetic card");
                } else if (ret == -1334) {
                    readcard(result);
                }

            }
        });
    }

    private void getClearPin(byte[] data) {
        String pinHex =  HexUtil.toString(data) ;
        String[] pinArray = pinHex.split("3");

        for (int i=0; i < pinArray.length; i++)
        {
            cardPin = cardPin + pinArray[i] ;
        }
    }

    private void readcard( @NonNull MethodChannel.Result result) {
        EmvL2.getInstance(mContext, FizidoPayPlugin.class.getSimpleName()).resetProcess();
        initEmv();
        EmvTermConfig termConfig = EmvL2.getInstance(mContext, FizidoPayPlugin.class.getSimpleName()).getTermConfig();
        termConfig.setTermType(22);
        EmvL2.getInstance(mContext, FizidoPayPlugin.class.getSimpleName()).setTermConfig(termConfig);
        EmvStartProcessParam emvStartProcessParam = new EmvStartProcessParam();
        String SN = null;
        try {
            SN = TerminalManager.getInstance().getSN();
        } catch (SDKException e) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    result.error("1-0-1","An error happened",e.toString());        }
            });
            e.printStackTrace();
        }
        emvStartProcessParam.mIfdSerialNum = SN.getBytes();
        if (channelType == EmvStartProcessParam.EMV_API_CHANNEL_FORM_PICC) {
            emvStartProcessParam.mProcType = EmvStartProcessParam.EMV_API_PROC_QPBOC;
        } else {
            emvStartProcessParam.mProcType = EmvStartProcessParam.EMV_API_PROC_PBOC_FULL;
        }
        emvStartProcessParam.mTransType = EmvStartProcessParam.EMV_API_TRANS_TYPE_NORMAL;
        //交易计数器  Transaction counter
        emvStartProcessParam.mSeqNo = 100;
        //交易金额,单位分  Transaction amount, unit cent
        emvStartProcessParam.mTransAmt = 1000;
        //返现金额  Cash back amount
        emvStartProcessParam.mCashbackAmt = 0;
        emvStartProcessParam.mTransDate = StringUtil.hexStr2Bytes(DateUtil.getCurDateStr("yyMMdd"));
        emvStartProcessParam.mTransTime = StringUtil.hexStr2Bytes(DateUtil.getCurDateStr("HHmmss"));
        //交易类型，TAG 9C的值   Transaction type, the value of TAG 9C
        emvStartProcessParam.mTag9CValue = 0x00;
        //是否支持电子现金   Whether to support electronic cash
        emvStartProcessParam.mIsSupportEC = true;
        //通道类型,EMV_API_CHANNEL_FROM_ICC表示接触卡 EMV_API_CHANNEL_FORM_PICC表示非接触卡  Channel type, EMV_API_CHANNEL_FROM_ICC means contact card EMV_API_CHANNEL_FORM_PICC means contactless card
        emvStartProcessParam.mChannelType = channelType;
        //非电子现金接口，强制联机  Non-electronic cash interface, mandatory online
        emvStartProcessParam.mIsQpbocForceOnline = true;
        //init first
        EmvL2.getInstance(mContext, FizidoPayPlugin.class.getSimpleName()).init();
        EmvL2.getInstance(mContext, FizidoPayPlugin.class.getSimpleName()).startProcess(emvStartProcessParam, new AsyncEmvCallback() {

            @Override
            public void confirmTransType(byte[] aid, int oldTransType, EmvL2.ConfirmTransTypeHandler handler) {
                handler.onConfirmTransType(oldTransType);
            }

            @Override
            public void panCofirm(byte[] pan, EmvL2.PanConfirmHandler panConfirmHandler) {
                Log.d("FizidoPayPlugin", "EMVDevice process startProcess panCofirm");
                panNumber = StringUtil.byte2HexStr(pan) ;
                //0: success, others: fail
                panConfirmHandler.onPanConfirm(0);
            }

            @Override
            public void getPin(int type, int retryTimes, final EmvL2.GetPinHandler getPinHandler) {

                Log.d("FizidoPayPlugin", "EMVDevice process startProcess getPin");
                Log.d("FizidoPayPlugin", "type:" + type + ", retryTimes:" + retryTimes);
                boolean isOnlinePin = true;
                if (type == 1) {
                    isOnlinePin = false;
                }

                final boolean finalIsOnlinePin = isOnlinePin;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Bundle param = new Bundle();
                        param.putBoolean("isOnline", finalIsOnlinePin);
                        param.putString("pan", getpanData());//"6274311520010841"
                        param.putString("promptString", "Enter Card PIN");
                        param.putIntArray("pinLimit", new int[]{4, 6});
                        param.putInt("pinAlgMode", Ped.DES_TYPE_DES);//ALGORITHMTYPE_USE_PAN_SUPPLY_F
                        param.putInt("keysType", Ped.DES_TYPE_DES);//KEYS_TYPE_DUKPT
                        param.putInt("desType",Ped.DES_TYPE_DES);
                        param.putInt("timeout", 60);
                        try {
                            Ped.getInstance().startPinInput(mContext, 0x01, param, new OperationPinListener() {

                                @Override
                                public void onInput(int len, int key) {
                                    Log.e("FizidoPayPlugin", "onInput  len:" + len + "  key:" + key);
                                }

                                @Override
                                public void onError(int errorCode) {
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            result.error("1-0-3","onError   errorCode:",errorCode);                    }
                                    });


                                    Log.e("FizidoPayPlugin", "onError   errorCode:" + errorCode);
                                    getPinHandler.onGetPin(EmvCallbackGetPinResult.CV_PIN_FAIL, null);
                                }

                                @Override
                                public void onConfirm(byte[] data, boolean isNonePin) {
                                    getClearPin(data);
                                    getPinHandler.onGetPin(EmvCallbackGetPinResult.CV_PIN_SUCC, data);
                                    Log.e("FizidoPayPlugin", "onConfirm   data:"  + "  isNonePin:" + isNonePin);
                                }

                                @Override
                                public void onCancel() {
                                    stopSearch();
                                    initEmv();
                                    Log.e("FizidoPayPlugin", "onCancel");
                                    getPinHandler.onGetPin(EmvCallbackGetPinResult.CV_PIN_CANCLE, new byte[]{0x00, 0x00});
                                }
                            });
                        } catch (SDKException e) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    result.error("1-0-4","An error happened",e.toString());

                                }
                            });

                            e.printStackTrace();
                        }
                    }
                }).start();

                Log.i("FizidoPayPlugin", "getpin success");
            }

            @Override
            public int onVerifyOfflinePin(byte b, int i) {

                return EmvCallbackGetPinResult.CV_PIN_SUCC;
            }

            @Override
            public void aidSelect(EmvAidCandidate[] emvAidCandidates, int times, EmvL2.
                    AidSelectHandler aidSelectHandler) {
                Log.d("FizidoPayPlugin", "EMVDevice process startProcess aidSelect");
                aidSelectHandler.onAidSelect(0);
            }

            @Override
            public void termRiskManager(byte[] pan, int panSn, EmvL2.
                    TermRiskManageHandler termRiskManageHandler) {
                Log.d("FizidoPayPlugin", "EMVDevice process startProcess termRiskManager");
                termRiskManageHandler.onTermRiskManager(0, 0);
            }

            @Override
            public void issuerReference(byte[] bytes, EmvL2.
                    IssuerReferenceHandler issuerReferenceHandler) {
                Log.d("FizidoPayPlugin", "EMVDevice process startProcess issuerReference");
                issuerReferenceHandler.onIssuerReference(0);
            }

            @Override
            public void accountTypeSelect(EmvL2.AccountTypeSelectHandler accountTypeSelectHandler) {
                accountTypeSelectHandler.onAccountType(EmvCallback.EMV_ACCOUNT_TYPE_DEFAULT);
            }

            @Override
            public void certConfirm(int type, byte[] certNum, EmvL2.
                    CertConfirmHandler certConfirmHandler) {
                Log.d("FizidoPayPlugin", "EMVDevice process startProcess certConfirm");
                certConfirmHandler.onCertConfirm(1);
            }

            @Override
            public void lcdMsg(byte[] bytes, byte[] bytes1, boolean b, int i, EmvL2.
                    LcdMsgHandler lcdMsgHandler) {
                Log.d("FizidoPayPlugin", "EMVDevice process startProcess lcdMsg");
                lcdMsgHandler.onLcdMsg(1);
            }

            @Override
            public void confirmEC(EmvL2.ConfirmEcHandler confirmEcHandler) {
                Log.d("FizidoPayPlugin", "EMVDevice process startProcess confirmEC");
                confirmEcHandler.onConfirmEc(-1);
            }

            @Override
            public void processResult(int ret) {
                Log.d("FizidoPayPlugin", "EMVDevice process startProcess processResult:" + ret);
                int code = 1;
                String desc = "";
                switch (ret) {
                    case EmvApi.EMV_TRANS_FALLBACK:
                        desc = "EMV_TRANS_FALLBACK";
                        break;
                    case EmvApi.EMV_TRANS_TERMINATE:
                        desc = "EMV_TRANS_TERMINATE";
                        break;
                    case EmvApi.EMV_TRANS_ACCEPT:
                        code = 0;
                        desc = "EMV_TRANS_ACCEPT";
                        break;
                    case EmvApi.EMV_TRANS_DENIAL:
                        desc = "EMV_TRANS_DENIAL";
                        break;
                    case EmvApi.EMV_TRANS_GOONLINE:
                        updateICCardData(result);
                        return;
                    case EmvApi.EMV_TRANS_QPBOC_ACCEPT:
                        code = 0;
                        desc = "EMV_TRANS_QPBOC_ACCEPT";
                        break;
                    case EmvApi.EMV_TRANS_QPBOC_DENIAL:
                    case EmvApi.EMV_TRANS_QPBOC_GOONLINE:
                        code = 0;
                        desc = "EMV_TRANS_QPBOC_GOONLINE";
                        updateICCardData(result);
                        return;
                    case EmvErrorCode.EMV_ERR_BASE:
                        desc = "EMV_ERR_BASE";
                        break;
                    case 7: //utral light
                        readUtralLight();
                }
                //updateResult("code:" + code + ",desc:" + (desc.isEmpty() ? ret : desc));
            }
        });
    }

    /**
     * 读取Utral Light Card
     */
    private void readUtralLight(){
        UltralightDriver.getInstance().searchCard(10, new UltralightDriver.OnSearchCardCallback() {
            @Override
            public void onSearchResult(int i, RFSearchResultInfo rfSearchResultInfo) {
                String des="";
                switch (i) {
                    case CardReaderConst.SearchResult.RESULT_SUCCESS:
                        try {
                            byte[] read =UltralightDriver.getInstance().read((byte)0x05);
                            des="read:"+StringUtil.byte2HexStr(read);
                        } catch (SDKException e) {
                            e.printStackTrace();
                        }
                        break;
                    case CardReaderConst.SearchResult.RESULT_TIMEOUT:
                        des="寻卡超时";
                        break;
                    case CardReaderConst.SearchResult.RESULT_FIND_UNKONW_CARD:
                        des="卡片已插入但是未知卡片类型";
                        break;
                    case CardReaderConst.SearchResult.RESULT_FAIL:
                        des="寻卡失败";
                        break;
                    case -2:
                        des="该卡不是Ultralight卡";
                        break;
                }
                Log.d(des);
                //updateResult(des);
            }
        });
    }
    private void updateICCardData( @NonNull MethodChannel.Result methodResult) {
        byte[] cardData = getTlvData(mContext, FizidoPayPlugin.class.getSimpleName(), "5A575F345F20");

        if (cardData == null) {
            return;
        }
        HashMap<String, String> cardDataMap = (HashMap<String, String>) TlvUtil.tlvToMap(cardData);
        KSNUtilities ksnUtilitites = new KSNUtilities();
        String workingKey = ksnUtilitites.getWorkingKey("3F2216D8297BCE9C",getInitialKSN()) ;
        String pinBlock =  ksnUtilitites.DesEncryptDukpt(workingKey , getpanData(),cardPin);
        cardDataMap.put("CardPin",pinBlock);
        cardDataMap.put("ksn",ksnUtilitites.getLatestKsn());
        cardDataMap.put("pan",panNumber);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                methodResult.success(cardDataMap);
            }
        });
        String pan = cardDataMap.get("57");
        if (!StringUtil.isEmpty(pan)) {
            pan = pan.replace("F", "");//card no
        }
        String cardHolderName = cardDataMap.get("5F20");
        String track2data = cardDataMap.get("57");
        String result = "card no: " + pan.substring(0, 16) + "\ncardHolderName:" + cardHolderName
                + "\ntrack 2:" + track2data + "\ntlv data:";
        byte[] sendF55Data = getTlvData(mContext, FizidoPayPlugin.class.getSimpleName(), "9F269F279F109F379F36959A9C9F025F2A829F1A9F039F339F349F359F1E849F099F419F639F6C9F66917172DF32DF33DF34"); //without: 5F24 + 9F12 + 9F53
        List<TlvBean> tlvList = TlvUtils.builderTlvList(StringUtil.byte2HexStr(sendF55Data));
        if (tlvList != null) {
            for (TlvBean tlv : tlvList) {
                result += "\ntag:" + tlv.getTag();
                result += " value:" + tlv.getValue();
            }
        }
    }

    private byte[] getTlvData(Context context, String packageName, String tlvString) {
        try {
            Map<String, String> mMap = new HashMap<String, String>();
            mMap.put("DF35", tlvString);
            return EmvL2.getInstance(context, packageName).getTLVData(TlvUtil.mapToTlv(mMap));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void stopSearch() {
        CardReader.getInstance().stopSearch(7);
    }

//    private String  getpanData() {
//        String sPan = null;
//        com.sunyard.smartposapi.emv2.EmvL2 emvL2 = com.sunyard.smartposapi.emv2.EmvL2.getInstance(mContext, FizidoPayPlugin.class.getSimpleName());
//        byte[] sendF55Data = emvL2.getTLVData(StringUtil.hexStr2Bytes("5A57"));
//        List<TlvBean> tlvList = TlvUtils.builderTlvList(StringUtil.byte2HexStr(sendF55Data));
//        for (TlvBean tlvBean:tlvList) {
//            if (tlvBean.getTag().equals("5A")) sPan = tlvBean.getValue();
//            if (tlvBean.getTag().equals("57")) sPan = tlvBean.getValue().substring(0, tlvBean.getLength());
//            Log.d("FizidoPayPlugin", "sPan is:" + sPan);
//        }
//        return sPan;
//    }
    private String getpanData() {
        String panRegx="F";
        String sPan = null;
        com.sunyard.smartposapi.emv2.EmvL2 emvL2 = com.sunyard.smartposapi.emv2.EmvL2.getInstance(mContext, FizidoPayPlugin.class.getSimpleName());
        byte[] iccData =  emvL2.getTag(new byte[]{0x5A,0x00}, 1);
        if (iccData==null || iccData.length==0){
            iccData=emvL2.getTag(new byte[]{0x57,0x00},1);
            panRegx="D";
        }
        byte[] cardNO = new byte[iccData[1]];
        System.arraycopy(iccData, 2, cardNO, 0, iccData[1]);
        sPan=StringUtil.byte2HexStr(cardNO);
        if (sPan.contains("D")||sPan.contains("F")) {
            sPan = sPan.substring(0, sPan.indexOf(panRegx));
        }
        Log.d("FizidoPayPlugin", "card number = " + sPan);
        return sPan;
    }

    private void initEmv() {
        EmvL2.getInstance(mContext, FizidoPayPlugin.class.getSimpleName()).init();
        String[] aids = new String[]{
                // Verve
                "9F0607A0000003710001DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0F0C89F6604620000805F2A0208409F1A020418",
                "9F0607A0000003710002DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0F0C89F6604620000805F2A0208409F1A020418",

                //VISA
                "9F0608A000000003010101DF0101009F08020000DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0608A000000003010102DF0101009F08020000DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0607A0000000031010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0607A0000000032010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0607A0000000033010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0609A00000000305076010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0608A000000003101001DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0608A000000003101002DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0607A0000000032020DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0607A0000000034010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0607A0000000035010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0607A0000000036010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0607A0000000036020DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0607A0000000038002DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0607A0000000038010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0607A0000000039010DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0608A000000003101008DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0608A000000003101009DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                "9F0608A000000003101012DF0101009F08020140DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418",
                //masterCard
                //9F0607A0000000041010DF0101009F08020002DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B0C89F6604620000805F2A0208409F1A020418
                "9F0607A0000000041010DF010100DF1105FC50BC2000DF1205FC50BCF800DF130500000000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B8C89F6604620000805F2A0208409F1D086E7A000000000000",
                "9F0607A0000000043060DF010100DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000010000DF2006001000000000DF21060000000300009F3303E0B8C89F6604620000805F2A0208409F1D086E7A000000000000",
                //JCB
                "9F0607A0000000651010DF010100DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000000000DF2006001000000000DF21060000000300009F3303E0B0C89F660462000080",
                //UnionPay
                "9F0608A000000333010101DF010100DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF1801009F7B06000000100000DF1906001000000000DF2006001000000000DF21060010000000009F3303E0B8C89F660422000080",
//                    "9F0608A000000333010102DF010100DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF1801019F7B06000000100000DF1906000000100000DF2006001000000000DF21061000001000009F3303E0F0C8",
                "9F0608A000000333010102DF010100DF11050000000000DF12050000000000DF130500000000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF1801019F7B06000000100000DF1906001000000000DF2006001000000000DF21060010000000009F3303E028C89F660426000080",
                "9F0608A000000333010103DF010100DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF1801019F7B06000000100000DF1906001000000000DF2006001000000000DF21060010000000009F3303E028C89F660426000080",
                "9F0608A000000333010106DF010100DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF1801019F7B06000000100000DF1906001000000000DF2006001000000000DF21060010000000009F3303E028C89F660426000080",
                //JCB
                "9F0607A0000000651010DF0101009F09020200DF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF1504F0F0F0F0DF160100DF170100DF14039F3704DF180101DF1906000000000000DF2006000010000000DF21060000000000009F3303E040C89F6604640000805F2A0204189F1A020418",
                //Rupay
                "9f0607A0000005241010df1105fc5080a000df1205f85080f800df130504000000009f1b0400000000df150400000000df160199df170199df14039f3704df1801009f7b06000000100000df1906000000200000df2006000000500001df21060000005000015f2a0203569f1a0203569f090200029f3303e0d9c89f660436004000"
        };


        int ret = EmvL2.getInstance(mContext, FizidoPayPlugin.class.getSimpleName()).clearAids();
        if (ret != 0) {
            Log.d("FizidoPayPlugin","clear aids fail");
        }
        ret = EmvL2.getInstance(mContext, FizidoPayPlugin.class.getSimpleName()).addAids(aids);
        if (ret != 0) {
            Log.e("FizidoPayPlugin", "add aids fail");
        }
        String[] capks = new String[]{

                // Verve Capk
                "9F0607A00000037100019F220109DF05083230323431323331DF060101DF070101DF0281B0B036A8CAE0593A480976BFE84F8A67759E52B3D9F4A68CCC37FE720E594E5694CD1AE20E1B120D7A18FA5C70E044D3B12E932C9BBD9FDEA4BE11071EF8CA3AF48FF2B5DDB307FC752C5C73F5F274D4238A92B4FCE66FC93DA18E6C1CC1AA3CFAFCB071B67DAACE96D9314DB494982F5C967F698A05E1A8A69DA931B8E566270F04EAB575F5967104118E4F12ABFF9DEC92379CD955A10675282FE1B60CAD13F9BB80C272A40B6A344EA699FB9EFA6867DF040103DF0314319F3C608B67F1118C729B0E1516EAB07CB290C8",
                "9F0607A00000037100019F220104DF05083230313731323331DF060101DF070101DF028190D13CD5E1B921E4E0F0D40E2DE14CCE73E3A34ED2DCFA826531D8195641091E37C8474D19B686E8243F089A69F7B18D2D34CB4824F228F7750F96D1EFBDFF881F259A8C04DE64915A3A3D7CB846135F4083C93CDE755BC808886F600542DFF085558D5EA7F45CB15EC835064AA856D602A0A44CD021F54CF8EC0CC680B54B3665ABE74A7C43D02897FF84BB4CB98BC91DDF040103DF03148B36A3E3D814CE6C6EBEAAF27674BB7BC67275B1",
                "9F0607A00000037100019F220103DF05083230313731323331DF060101DF070101DF028190d06238b856cf2c8890a7f668ca17c19247498d193a7c11e7105dedeee6a873e8189e50493e9b17547c42ea4fa88bbef30bb6bc2409246ccc95f36622a7f4d92d46444f20b1b24bf63c5b28395d8ef18c23205c2119dfe5fba2fbfc311b2fe8a6a75b35a7dab72d421792a500cdfd8133b8a97d84a49c0bd22d52d06ea5e0ef3e471d47d8370c37aa48b564689d0035d9DF040103DF0314319F3C608B67F1118C729B0E1516EAB07CB290C8",
                "9F0607A00000037100019F220106DF05083230323831323238DF060101DF070101DF0281F8D2DA0134B4DFC93A75EE8960C99896D50A91527B87BA7B16CDB77E5B6FDB750EB70B54026CADDA1D562C77A2C6DA541E94BC415D43E68489B16980F2E887C09E4CF90E2E639B179277BBA0E982CCD1F80521D1457209125B3ABCD309E1B92B5AEDA2EB1CBF933BEAD9CE7365E52B7D17FCB405AA28E5DE6AA3F08E764F745E70859ABCBA41E570A6E4367B3D6FECE723B73ABF3EB53DCDE3816E8A813460447021509D0DFDF2EEEE74CC35485FB55C26836EB3BF9C7DEBEE6C0B77B7BE059233801CF76B321FCA25FB1E63117AE1865E23161EC39D7B1FB84256C2BE72BF8EC771548DB9F00BEF77C509FADA15E2B53FF950D383F96211D3DF040103DF0314F5BAB84ECE5F8BD45511E5CA861B80C7E6C51F55",
                "9F0607A00000037100029F220105DF05083230323431323331DF060101DF070101DF0281B0B036A8CAE0593A480976BFE84F8A67759E52B3D9F4A68CCC37FE720E594E5694CD1AE20E1B120D7A18FA5C70E044D3B12E932C9BBD9FDEA4BE11071EF8CA3AF48FF2B5DDB307FC752C5C73F5F274D4238A92B4FCE66FC93DA18E6C1CC1AA3CFAFCB071B67DAACE96D9314DB494982F5C967F698A05E1A8A69DA931B8E566270F04EAB575F5967104118E4F12ABFF9DEC92379CD955A10675282FE1B60CAD13F9BB80C272A40B6A344EA699FB9EFA6867DF040103DF0314676822D335AB0D2C3848418CB546DF7B6A6C32C0",
                "9F0607A00000037100029F220104DF05083230313731323331DF060101DF070101DF028190D13CD5E1B921E4E0F0D40E2DE14CCE73E3A34ED2DCFA826531D8195641091E37C8474D19B686E8243F089A69F7B18D2D34CB4824F228F7750F96D1EFBDFF881F259A8C04DE64915A3A3D7CB846135F4083C93CDE755BC808886F600542DFF085558D5EA7F45CB15EC835064AA856D602A0A44CD021F54CF8EC0CC680B54B3665ABE74A7C43D02897FF84BB4CB98BC91DDF040103DF0314676822D335AB0D2C3848418CB546DF7B6A6C32C0",
                "9F0607A00000037100029F220103DF05083230313731323331DF060101DF070101DF028190d06238b856cf2c8890a7f668ca17c19247498d193a7c11e7105dedeee6a873e8189e50493e9b17547c42ea4fa88bbef30bb6bc2409246ccc95f36622a7f4d92d46444f20b1b24bf63c5b28395d8ef18c23205c2119dfe5fba2fbfc311b2fe8a6a75b35a7dab72d421792a500cdfd8133b8a97d84a49c0bd22d52d06ea5e0ef3e471d47d8370c37aa48b564689d0035d9DF040103DF0314319F3C608B67F1118C729B0E1516EAB07CB290C8",
                "9F0607A00000037100029F220106DF05083230323831323238DF060101DF070101DF0281F8D2DA0134B4DFC93A75EE8960C99896D50A91527B87BA7B16CDB77E5B6FDB750EB70B54026CADDA1D562C77A2C6DA541E94BC415D43E68489B16980F2E887C09E4CF90E2E639B179277BBA0E982CCD1F80521D1457209125B3ABCD309E1B92B5AEDA2EB1CBF933BEAD9CE7365E52B7D17FCB405AA28E5DE6AA3F08E764F745E70859ABCBA41E570A6E4367B3D6FECE723B73ABF3EB53DCDE3816E8A813460447021509D0DFDF2EEEE74CC35485FB55C26836EB3BF9C7DEBEE6C0B77B7BE059233801CF76B321FCA25FB1E63117AE1865E23161EC39D7B1FB84256C2BE72BF8EC771548DB9F00BEF77C509FADA15E2B53FF950D383F96211D3DF040103DF0314F5BAB84ECE5F8BD45511E5CA861B80C7E6C51F55",
                
                
                "9F0605A0000000659F220109DF05083230303931323331DF060101DF070101DF028180B72A8FEF5B27F2B550398FDCC256F714BAD497FF56094B7408328CB626AA6F0E6A9DF8388EB9887BC930170BCC1213E90FC070D52C8DCD0FF9E10FAD36801FE93FC998A721705091F18BC7C98241CADC15A2B9DA7FB963142C0AB640D5D0135E77EBAE95AF1B4FEFADCF9C012366BDDA0455C1564A68810D7127676D493890BDDF040103DF03144410C6D51C2F83ADFD92528FA6E38A32DF048D0A",
                "9F0605A0000000659F220110DF05083230313231323331DF060101DF070101DF02819099B63464EE0B4957E4FD23BF923D12B61469B8FFF8814346B2ED6A780F8988EA9CF0433BC1E655F05EFA66D0C98098F25B659D7A25B8478A36E489760D071F54CDF7416948ED733D816349DA2AADDA227EE45936203CBF628CD033AABA5E5A6E4AE37FBACB4611B4113ED427529C636F6C3304F8ABDD6D9AD660516AE87F7F2DDF1D2FA44C164727E56BBC9BA23C0285DF040103DF0314C75E5210CBE6E8F0594A0F1911B07418CADB5BAB",
                "9F0605A0000000659F220112DF05083230313431323331DF060101DF070101DF0281B0ADF05CD4C5B490B087C3467B0F3043750438848461288BFEFD6198DD576DC3AD7A7CFA07DBA128C247A8EAB30DC3A30B02FCD7F1C8167965463626FEFF8AB1AA61A4B9AEF09EE12B009842A1ABA01ADB4A2B170668781EC92B60F605FD12B2B2A6F1FE734BE510F60DC5D189E401451B62B4E06851EC20EBFF4522AACC2E9CDC89BC5D8CDE5D633CFD77220FF6BBD4A9B441473CC3C6FEFC8D13E57C3DE97E1269FA19F655215B23563ED1D1860D8681DF040103DF0314874B379B7F607DC1CAF87A19E400B6A9E25163E8",
                "9F0605A0000000659F220114DF05083230313631323331DF060101DF070101DF0281F8AEED55B9EE00E1ECEB045F61D2DA9A66AB637B43FB5CDBDB22A2FBB25BE061E937E38244EE5132F530144A3F268907D8FD648863F5A96FED7E42089E93457ADC0E1BC89C58A0DB72675FBC47FEE9FF33C16ADE6D341936B06B6A6F5EF6F66A4EDD981DF75DA8399C3053F430ECA342437C23AF423A211AC9F58EAF09B0F837DE9D86C7109DB1646561AA5AF0289AF5514AC64BC2D9D36A179BB8A7971E2BFA03A9E4B847FD3D63524D43A0E8003547B94A8A75E519DF3177D0A60BC0B4BAB1EA59A2CBB4D2D62354E926E9C7D3BE4181E81BA60F8285A896D17DA8C3242481B6C405769A39D547C74ED9FF95A70A796046B5EFF36682DC29DF040103DF0314C0D15F6CD957E491DB56DCDD1CA87A03EBE06B7B",
                "9F0605A0000000659F220111DF05083230323531323331DF060101DF070101DF0281B0A2583AA40746E3A63C22478F576D1EFC5FB046135A6FC739E82B55035F71B09BEB566EDB9968DD649B94B6DEDC033899884E908C27BE1CD291E5436F762553297763DAA3B890D778C0F01E3344CECDFB3BA70D7E055B8C760D0179A403D6B55F2B3B083912B183ADB7927441BED3395A199EEFE0DEBD1F5FC3264033DA856F4A8B93916885BD42F9C1F456AAB8CFA83AC574833EB5E87BB9D4C006A4B5346BD9E17E139AB6552D9C58BC041195336485DF040103DF0314D9FD62C9DD4E6DE7741E9A17FB1FF2C5DB948BCB",
                "9F0605A0000000659F220113DF05083230323531323331DF060101DF070101DF0281F8A3270868367E6E29349FC2743EE545AC53BD3029782488997650108524FD051E3B6EACA6A9A6C1441D28889A5F46413C8F62F3645AAEB30A1521EEF41FD4F3445BFA1AB29F9AC1A74D9A16B93293296CB09162B149BAC22F88AD8F322D684D6B49A12413FC1B6AC70EDEDB18EC1585519A89B50B3D03E14063C2CA58B7C2BA7FB22799A33BCDE6AFCBEB4A7D64911D08D18C47F9BD14A9FAD8805A15DE5A38945A97919B7AB88EFA11A88C0CD92C6EE7DC352AB0746ABF13585913C8A4E04464B77909C6BD94341A8976C4769EA6C0D30A60F4EE8FA19E767B170DF4FA80312DBA61DB645D5D1560873E2674E1F620083F30180BD96CA589DF040103DF031454CFAE617150DFA09D3F901C9123524523EBEDF3",
                "9f0605a0000000039f220101df05083230313231323331df060101df070101df028180c696034213d7d8546984579d1d0f0ea519cff8deffc429354cf3a871a6f7183f1228da5c7470c055387100cb935a712c4e2864df5d64ba93fe7e63e71f25b1e5f5298575ebe1c63aa617706917911dc2a75ac28b251c7ef40f2365912490b939bca2124a30a28f54402c34aeca331ab67e1e79b285dd5771b5d9ff79ea630b75df040103df0314d34a6a776011c7e7ce3aec5f03ad2f8cfc5503cc",
                "9f0605a0000000049f220103df05083230313231323331df060101df070101df028180c2490747fe17eb0584c88d47b1602704150adc88c5b998bd59ce043edebf0ffee3093ac7956ad3b6ad4554c6de19a178d6da295be15d5220645e3c8131666fa4be5b84fe131ea44b039307638b9e74a8c42564f892a64df1cb15712b736e3374f1bbb6819371602d8970e97b900793c7c2a89a4a1649a59be680574dd0b60145df040103df03145addf21d09278661141179cbeff272ea384b13bb",
                "9f0605a0000003339f220109df05083230313231323331df060101df070101df0281b0eb374dfc5a96b71d2863875eda2eafb96b1b439d3ece0b1826a2672eeefa7990286776f8bd989a15141a75c384dfc14fef9243aab32707659be9e4797a247c2f0b6d99372f384af62fe23bc54bcdc57a9acd1d5585c303f201ef4e8b806afb809db1a3db1cd112ac884f164a67b99c7d6e5a8a6df1d3cae6d7ed3d5be725b2de4ade23fa679bf4eb15a93d8a6e29c7ffa1a70de2e54f593d908a3bf9ebbd760bbfdc8db8b54497e6c5be0e4a4dac29e5df040103df0314a075306eab0045baf72cdd33b3b678779de1f527",
                "9f0605a0000003339f220104df05083230313431323331df060101df070101df0281f8bc853e6b5365e89e7ee9317c94b02d0abb0dbd91c05a224a2554aa29ed9fcb9d86eb9ccbb322a57811f86188aac7351c72bd9ef196c5a01acef7a4eb0d2ad63d9e6ac2e7836547cb1595c68bcbafd0f6728760f3a7ca7b97301b7e0220184efc4f653008d93ce098c0d93b45201096d1adff4cf1f9fc02af759da27cd6dfd6d789b099f16f378b6100334e63f3d35f3251a5ec78693731f5233519cdb380f5ab8c0f02728e91d469abd0eae0d93b1cc66ce127b29c7d77441a49d09fca5d6d9762fc74c31bb506c8bae3c79ad6c2578775b95956b5370d1d0519e37906b384736233251e8f09ad79dfbe2c6abfadac8e4d8624318c27daf1df040103df0314f527081cf371dd7e1fd4fa414a665036e0f5e6e5",
                "9f0605a0000000659f220109df05083230313431323331df060101df070101df028180b72a8fef5b27f2b550398fdcc256f714bad497ff56094b7408328cb626aa6f0e6a9df8388eb9887bc930170bcc1213e90fc070d52c8dcd0ff9e10fad36801fe93fc998a721705091f18bc7c98241cadc15a2b9da7fb963142c0ab640d5d0135e77ebae95af1b4fefadcf9c012366bdda0455c1564a68810d7127676d493890bddf040103df03144410c6d51c2f83adfd92528fa6e38a32df048d0a",
                "9f0605a0000000659f220110df05083230313231323331df060101df070101df02819099b63464ee0b4957e4fd23bf923d12b61469b8fff8814346b2ed6a780f8988ea9cf0433bc1e655f05efa66d0c98098f25b659d7a25b8478a36e489760d071f54cdf7416948ed733d816349da2aadda227ee45936203cbf628cd033aaba5e5a6e4ae37fbacb4611b4113ed427529c636f6c3304f8abdd6d9ad660516ae87f7f2ddf1d2fa44c164727e56bbc9ba23c0285df040103df0314c75e5210cbe6e8f0594a0f1911b07418cadb5bab",
                "9f0605a0000003339f22010adf05083230313431323331df060101df070101df028180b2ab1b6e9ac55a75adfd5bbc34490e53c4c3381f34e60e7fac21cc2b26dd34462b64a6fae2495ed1dd383b8138bea100ff9b7a111817e7b9869a9742b19e5c9dac56f8b8827f11b05a08eccf9e8d5e85b0f7cfa644eff3e9b796688f38e006deb21e101c01028903a06023ac5aab8635f8e307a53ac742bdce6a283f585f48efdf040103df0314c88be6b2417c4f941c9371ea35a377158767e4e3",
                "9f0605a0000000039f220108df05083230313431323331df060101df070101df0281b0d9fd6ed75d51d0e30664bd157023eaa1ffa871e4da65672b863d255e81e137a51de4f72bcc9e44ace12127f87e263d3af9dd9cf35ca4a7b01e907000ba85d24954c2fca3074825ddd4c0c8f186cb020f683e02f2dead3969133f06f7845166aceb57ca0fc2603445469811d293bfefbafab57631b3dd91e796bf850a25012f1ae38f05aa5c4d6d03b1dc2e568612785938bbc9b3cd3a910c1da55a5a9218ace0f7a21287752682f15832a678d6e1ed0bdf040103df031420d213126955de205adc2fd2822bd22de21cf9a8",
                "9f0605a0000003339f220102df05083230313431323331df060101df070101df028190a3767abd1b6aa69d7f3fbf28c092de9ed1e658ba5f0909af7a1ccd907373b7210fdeb16287ba8e78e1529f443976fd27f991ec67d95e5f4e96b127cab2396a94d6e45cda44ca4c4867570d6b07542f8d4bf9ff97975db9891515e66f525d2b3cbeb6d662bfb6c3f338e93b02142bfc44173a3764c56aadd202075b26dc2f9f7d7ae74bd7d00fd05ee430032663d27a57df040103df031403bb335a8549a03b87ab089d006f60852e4b8060",
                "9f0605a0000000659f220112df05083230313431323331df060101df070101df0281b0adf05cd4c5b490b087c3467b0f3043750438848461288bfefd6198dd576dc3ad7a7cfa07dba128c247a8eab30dc3a30b02fcd7f1c8167965463626feff8ab1aa61a4b9aef09ee12b009842a1aba01adb4a2b170668781ec92b60f605fd12b2b2a6f1fe734be510f60dc5d189e401451b62b4e06851ec20ebff4522aacc2e9cdc89bc5d8cde5d633cfd77220ff6bbd4a9b441473cc3c6fefc8d13e57c3de97e1269fa19f655215b23563ed1d1860d8681df040103df0314874b379b7f607dc1caf87a19e400b6a9e25163e8",
                "9f0605a0000000039f220107df05083230313231323331df060101df070101df028190a89f25a56fa6da258c8ca8b40427d927b4a1eb4d7ea326bbb12f97ded70ae5e4480fc9c5e8a972177110a1cc318d06d2f8f5c4844ac5fa79a4dc470bb11ed635699c17081b90f1b984f12e92c1c529276d8af8ec7f28492097d8cd5becea16fe4088f6cfab4a1b42328a1b996f9278b0b7e3311ca5ef856c2f888474b83612a82e4e00d0cd4069a6783140433d50725fdf040103df0314b4bc56cc4e88324932cbc643d6898f6fe593b172",
                "9f0605a0000000049f220163df05083230313231323331df060101df070101df028190cf71f040528c9af2bf4341c639b7f31be1abff269633542cf22c03ab51570402c9cafc14437ae42f4e7cad00c9811b536dff3792facb86a0c7fae5fa50ae6c42546c534ea3a11fbd2267f1cf9ac68874dc221ecb3f6334f9c0bb832c075c2961ca9bbb683bec2477d12344e1b7d6dbe07b286fcf41a0f7f1f6f248a8c86398b7fa1c115111051dd01df3ed08985705fddf040103df03146e5ff80cd0a1cc2e3249b9c198d43427ce874013",
                "9f0605a0000000049f220104df05083230313231323331df060101df070101df028190a6da428387a502d7ddfb7a74d3f412be762627197b25435b7a81716a700157ddd06f7cc99d6ca28c2470527e2c03616b9c59217357c2674f583b3ba5c7dcf2838692d023e3562420b4615c439ca97c44dc9a249cfce7b3bfb22f68228c3af13329aa4a613cf8dd853502373d62e49ab256d2bc17120e54aedced6d96a4287acc5c04677d4a5a320db8bee2f775e5fec5df040103df0314381a035da58b482ee2af75f4c3f2ca469ba4aa6c",
                "9f0605a0000000039f220109df05083230313631323331df060101df070101df0281f89d912248de0a4e39c1a7dde3f6d2588992c1a4095afbd1824d1ba74847f2bc4926d2efd904b4b54954cd189a54c5d1179654f8f9b0d2ab5f0357eb642feda95d3912c6576945fab897e7062caa44a4aa06b8fe6e3dba18af6ae3738e30429ee9be03427c9d64f695fa8cab4bfe376853ea34ad1d76bfcad15908c077ffe6dc5521ecef5d278a96e26f57359ffaeda19434b937f1ad999dc5c41eb11935b44c18100e857f431a4a5a6bb65114f174c2d7b59fdf237d6bb1dd0916e644d709ded56481477c75d95cdd68254615f7740ec07f330ac5d67bcd75bf23d28a140826c026dbde971a37cd3ef9b8df644ac385010501efc6509d7a41df040103df03141ff80a40173f52d7d27e0f26a146a1c8ccb29046",
                "9f0605a0000000039f220163df05083230313231323331df060101df070101df028190cf71f040528c9af2bf4341c639b7f31be1abff269633542cf22c03ab51570402c9cafc14437ae42f4e7cad00c9811b536dff3792facb86a0c7fae5fa50ae6c42546c534ea3a11fbd2267f1cf9ac68874dc221ecb3f6334f9c0bb832c075c2961ca9bbb683bec2477d12344e1b7d6dbe07b286fcf41a0f7f1f6f248a8c86398b7fa1c115111051dd01df3ed08985705fddf040103df0314b2f6af1ddc393be17525d0ea7bf568bed5b71167",
                "9f0605a0000000049f220106df05083230313631323331df060101df070101df0281f8cb26fc830b43785b2bce37c81ed334622f9622f4c89aae641046b2353433883f307fb7c974162da72f7a4ec75d9d657336865b8d3023d3d645667625c9a07a6b7a137cf0c64198ae38fc238006fb2603f41f4f3bb9da1347270f2f5d8c606e420958c5f7d50a71de30142f70de468889b5e3a08695b938a50fc980393a9cbce44ad2d64f630bb33ad3f5f5fd495d31f37818c1d94071342e07f1bec2194f6035ba5ded3936500eb82dfda6e8afb655b1ef3d0d7ebf86b66dd9f29f6b1d324fe8b26ce38ab2013dd13f611e7a594d675c4432350ea244cc34f3873cba06592987a1d7e852adc22ef5a2ee28132031e48f74037e3b34ab747fdf040103df0314f910a1504d5ffb793d94f3b500765e1abcad72d9",
                "9f0605a0000000049f220105df05083230313431323331df060101df070101df0281b0b8048abc30c90d976336543e3fd7091c8fe4800df820ed55e7e94813ed00555b573feca3d84af6131a651d66cff4284fb13b635edd0ee40176d8bf04b7fd1c7bacf9ac7327dfaa8aa72d10db3b8e70b2ddd811cb4196525ea386acc33c0d9d4575916469c4e4f53e8e1c912cc618cb22dde7c3568e90022e6bba770202e4522a2dd623d180e215bd1d1507fe3dc90ca310d27b3efccd8f83de3052cad1e48938c68d095aac91b5f37e28bb49ec7ed597df040103df0314ebfa0d5d06d8ce702da3eae890701d45e274c845",
                "9f0605a0000003339f220101df05083230313431323331df060101df070101df028180bbe9066d2517511d239c7bfa77884144ae20c7372f515147e8ce6537c54c0a6a4d45f8ca4d290870cda59f1344ef71d17d3f35d92f3f06778d0d511ec2a7dc4ffeadf4fb1253ce37a7b2b5a3741227bef72524da7a2b7b1cb426bee27bc513b0cb11ab99bc1bc61df5ac6cc4d831d0848788cd74f6d543ad37c5a2b4c5d5a93bdf040103df0314e881e390675d44c2dd81234dce29c3f5ab2297a0",
                "9f0605a0000003339f220108df05083230323031323331df060101df070101df028190b61645edfd5498fb246444037a0fa18c0f101ebd8efa54573ce6e6a7fbf63ed21d66340852b0211cf5eef6a1cd989f66af21a8eb19dbd8dbc3706d135363a0d683d046304f5a836bc1bc632821afe7a2f75da3c50ac74c545a754562204137169663cfcc0b06e67e2109eba41bc67ff20cc8ac80d7b6ee1a95465b3b2657533ea56d92d539e5064360ea4850fed2d1bfdf040103df0314ee23b616c95c02652ad18860e48787c079e8e85a",
                "9f0605a0000003339f220103df05083230313431323331df060101df070101df0281b0b0627dee87864f9c18c13b9a1f025448bf13c58380c91f4ceba9f9bcb214ff8414e9b59d6aba10f941c7331768f47b2127907d857fa39aaf8ce02045dd01619d689ee731c551159be7eb2d51a372ff56b556e5cb2fde36e23073a44ca215d6c26ca68847b388e39520e0026e62294b557d6470440ca0aefc9438c923aec9b2098d6d3a1af5e8b1de36f4b53040109d89b77cafaf70c26c601abdf59eec0fdc8a99089140cd2e817e335175b03b7aa33ddf040103df031487f0cd7c0e86f38f89a66f8c47071a8b88586f26",
                "9f0605a0000003339f22010bdf05083230313631323331df060101df070101df0281f8cf9fdf46b356378e9af311b0f981b21a1f22f250fb11f55c958709e3c7241918293483289eae688a094c02c344e2999f315a72841f489e24b1ba0056cfab3b479d0e826452375dcdbb67e97ec2aa66f4601d774feaef775accc621bfeb65fb0053fc5f392aa5e1d4c41a4de9ffdfdf1327c4bb874f1f63a599ee3902fe95e729fd78d4234dc7e6cf1ababaa3f6db29b7f05d1d901d2e76a606a8cbffffecbd918fa2d278bdb43b0434f5d45134be1c2781d157d501ff43e5f1c470967cd57ce53b64d82974c8275937c5d8502a1252a8a5d6088a259b694f98648d9af2cb0efd9d943c69f896d49fa39702162acb5af29b90bade005bc157df040103df0314bd331f9996a490b33c13441066a09ad3feb5f66c",
                "9f0605a0000000659f220114df05083230313631323331df060101df070101df0281f8aeed55b9ee00e1eceb045f61d2da9a66ab637b43fb5cdbdb22a2fbb25be061e937e38244ee5132f530144a3f268907d8fd648863f5a96fed7e42089e93457adc0e1bc89c58a0db72675fbc47fee9ff33c16ade6d341936b06b6a6f5ef6f66a4edd981df75da8399c3053f430eca342437c23af423a211ac9f58eaf09b0f837de9d86c7109db1646561aa5af0289af5514ac64bc2d9d36a179bb8a7971e2bfa03a9e4b847fd3d63524d43a0e8003547b94a8a75e519df3177d0a60bc0b4bab1ea59a2cbb4d2d62354e926e9c7d3be4181e81ba60f8285a896d17da8c3242481b6c405769a39d547c74ed9ff95a70a796046b5eff36682dc29df040103df0314c0d15f6cd957e491db56dcdd1ca87a03ebe06b7b"
        };
        ret = EmvL2.getInstance(mContext, FizidoPayPlugin.class.getSimpleName()).clearCapks();
        if (ret != 0) {
            Log.d("FizidoPayPlugin","clear capks fail");
        }
        ret = EmvL2.getInstance(mContext, FizidoPayPlugin.class.getSimpleName()).addCapks(capks);
        if (ret != 0) {
            Log.d("FizidoPayPlugin","add capks fail");
        }
        Log.d("FizidoPayPlugin","init success");
    }

    private String getInitialKSN(){
        SharedPreferences sharedPref = mContext.getSharedPreferences( "KSNCOUNTER", Context.MODE_PRIVATE);
        int ksn = sharedPref.getInt("KSN",00001);
        if(ksn > 9999){
            ksn = 00000 ;
        }
        int latestKSN = ksn + 1 ;
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("KSN",latestKSN);
        editor.apply();
        return  "0000000002DDDDE"+ String.format("%05d", latestKSN);
    }


    public void checkCard( @NonNull MethodChannel.Result result){
        boolean isCardInserted = false ;
      if(Icc.IC_CARD_OFF == Icc.getInstance().checkCardOn(Icc.IC_CARD)){
          Log.d("Empressa","Card is NOT INSERTED");
          isCardInserted =  false ;

      }else if(IC_CARD_ON == Icc.getInstance().checkCardOn(Icc.IC_CARD)){
          Log.d("Empressa","Card is INSERTED");
          isCardInserted =  true ;
      }
        Log.d("Empressa","Card NO EVEN DEY AT ALL");
      result.success(isCardInserted);
    }

}
