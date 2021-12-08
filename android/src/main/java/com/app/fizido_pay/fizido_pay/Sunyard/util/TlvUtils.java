package com.app.fizido_pay.fizido_pay.Sunyard.util;


import com.app.fizido_pay.fizido_pay.Sunyard.bean.LPositon;
import com.app.fizido_pay.fizido_pay.Sunyard.bean.TlvBean;
import com.socsi.utils.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将字符串转换为TLV对象
 *
 * Convert string to TLV object
 */
public abstract class TlvUtils {

    /**
     * 将16进制字符串转换为TLV对象列表
     * Convert a hexadecimal string to a list of TLV objects
     *
     * @param hexString
     * @return
     */
    public static List<TlvBean> builderTlvList(String hexString) {
        try {
            List<TlvBean> tlvs = new ArrayList<TlvBean>();

            int position = 0;
//			while (position != StringUtils.length(hexString)) {
            while (position != hexString.length()) {
                String _hexTag = getTag(hexString, position);
                position += _hexTag.length();

                LPositon l_position = getLengthAndPosition(hexString, position);
                int _vl = l_position.get_vL();

                position = l_position.get_position();

//				String _value = StringUtils.substring(hexString, position, position + _vl * 2);
                String _value = hexString.substring(position, position + _vl * 2);

                position = position + _value.length();

                tlvs.add(new TlvBean(_hexTag, _vl, _value));
            }
            return tlvs;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将16进制字符串转换为TLV对象MAP
     *
     * @param hexString
     * @return
     */
    public static Map<String, TlvBean> builderTlvMap(String hexString) {
        try {
            Map<String, TlvBean> tlvs = new HashMap<String, TlvBean>();

            int position = 0;
            while (position != hexString.length()) {
                String _hexTag = getTag(hexString, position);

                position += _hexTag.length();

                LPositon l_position = getLengthAndPosition(hexString, position);

                int _vl = l_position.get_vL();
                position = l_position.get_position();
                String _value = hexString.substring(position, position + _vl * 2);
                position = position + _value.length();

                tlvs.put(_hexTag, new TlvBean(_hexTag, _vl, _value));
            }
            return tlvs;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 返回最后的Value的长度
     *
     * @param hexString
     * @param position
     * @return
     */
    private static LPositon getLengthAndPosition(String hexString, int position) {
        String firstByteString = hexString.substring(position, position + 2);
        int i = Integer.parseInt(firstByteString, 16);
        String hexLength = "";

        if (((i >>> 7) & 1) == 0) {
            hexLength = hexString.substring(position, position + 2);
            position = position + 2;
        } else {
            // 当最左侧的bit位为1的时候，取得后7bit的值，
            int _L_Len = i & 127;
            position = position + 2;
            hexLength = hexString.substring(position, position + _L_Len * 2);
            // position表示第一个字节，后面的表示有多少个字节来表示后面的Value值
            position = position + _L_Len * 2;
        }
        return new LPositon(Integer.parseInt(hexLength, 16), position);

    }

    /**
     * 取得子域Tag标签
     *
     * @param hexString
     * @param position
     * @return
     */
    private static String getTag(String hexString, int position) {
//		String firstByte = StringUtils.substring(hexString, position, position + 2);
        String firstByte = hexString.substring(position, position + 2);
        int i = Integer.parseInt(firstByte, 16);
        if ((i & 0x1f) == 0x1f) {
            return hexString.substring(position, position + 4);

        } else {
            return hexString.substring(position, position + 2);
        }
    }

    public static TlvBean creatTlv(String tag, String value) {
        TlvBean tlv = new TlvBean(tag, value.length(), value);
        return tlv;
    }

    public static byte[] builderTlvListToByte(List<TlvBean> tlvList) {
        String dataString = "";
        int length;
        String lengthString = "";
//		byte length1, length2;
        for (int i = 0; i < tlvList.size(); i++) {
            TlvBean tlv = tlvList.get(i);
            dataString += tlv.getTag();
            length = tlv.getLength();
            if (length > 127) {
                if (length > 256) {
                    dataString += "82";
                    dataString += String.format("%02x", length / 256);
                    dataString += String.format("%02x", length % 256);
                } else {
                    dataString += "81";
                    dataString += String.format("%02x", length % 256);
                }
            } else {
                dataString += String.format("%02x", length % 256);
            }
//			lengthString = String.format("%02x", length);
            dataString += lengthString;
            dataString += tlv.getValue();
//			tlv.getValue();
        }
        byte[] data = StringUtil.hexStr2Bytes(dataString);
        return data;
    }

    public static String toHexString(byte[] data) {
        if (data == null) {
            return "";
        } else {
            StringBuilder stringBuilder = new StringBuilder();

            for(int i = 0; i < data.length; ++i) {
                String string = Integer.toHexString(data[i] & 255);
                if (string.length() == 1) {
                    stringBuilder.append("0");
                }

                stringBuilder.append(string.toUpperCase());
            }

            return stringBuilder.toString();
        }
    }
}