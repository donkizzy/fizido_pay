package com.app.fizido_pay.fizido_pay.Sunyard.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BmpUtil {
	private static final String TAG = "BmpUtil";

	public static byte[] getMonoData(Bitmap bitmap) {
		byte[] buf = null;

		if (bitmap != null) {
			int height = bitmap.getHeight();
			int width = bitmap.getWidth();
			int pixels[] = new int[width * height];
			byte bpixels[] = new byte[width * height];
			int r = 0, g = 0, b = 0;
			bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

			for(int i = 0; i < height; i++){
				for(int j = 0; j<width; j++){
					r = Color.red(pixels[i*width+j]);
					g = Color.green(pixels[i*width+j]);
					b = Color.blue(pixels[i*width+j]);
					pixels[i*width+j] = (int) (r * 0.299 + g * 0.587 + b * 0.114);
					if (pixels[i*width+j] > 128)
						bpixels[i*width+j] = 1;
				}
			}

			int bytesPerRow = (width + 31) / 32 * 32 / 8;
			//int bytesPerRow = width / 8;
			buf = new byte[bytesPerRow * height];
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < bytesPerRow; j++) {
					int tmpBufIndex = i * bytesPerRow + j;
					int tmpPixelIndex = (height - 1 - i) * width + j * 8;
					for (int k = 0; k < 8; k++) {
						if(tmpPixelIndex + k < bpixels.length)
							buf[tmpBufIndex] |= bpixels[tmpPixelIndex + k] << (7 - k);
					}
				}
			}
		}

		return buf;
	}

	//将getMonoData后的数据还原成bitmap
	public static Bitmap getBitmapForMonoData(byte[] monoData){
		Boolean isIOS;
		int width;
		int height;

		if(monoData.length == 32*79){
			isIOS = true;
			width = 256;
			height = 79;
		}else{
			isIOS = false;
			width = 296;
			height = 100;
		}

		//因智能pos打印时图片宽度大了后会有黑块，故图片宽度设为签名时创建的bitmap的宽度
		Bitmap bitmap = Bitmap.createBitmap(237, height, Bitmap.Config.ARGB_4444);
//		Bitmap bitmap = Bitmap.createBitmap(296, height, Bitmap.Config.ARGB_4444);
		int[] oldPixels = new int[width * height];

		//将monoData还原成像素组成的数据
		for(int i = 0; i < height; i++){
			for(int j = 0; j < width/8; j++){
				int oldPixel = (height - 1 - i) * width/8 + j;
				int newPixel = i * width + j * 8;
				for(int k = 0;k < 8; k++){
					if(newPixel + k <oldPixels.length){
						if(monoData[oldPixel] < 0)
							oldPixels [newPixel + k] = (((256 + monoData[oldPixel]) >> (7 - k)) % 2) == 0 ? -16777216 : -1;
						else
							oldPixels [newPixel + k] = (monoData[oldPixel] >> (7 - k)) % 2 == 0 ? -16777216 : -1;
					}
				}
			}
		}

		if(isIOS){
			int[] pixels = new int[237 * height];
			int curr = 0;
			for(int i =0 ;i < oldPixels.length ;i++){
				if((i+1)%width > 237 || (i+1)%width == 0){

				}else{
					pixels[curr++] = oldPixels[i];
				}
			}
			bitmap.setPixels(pixels, 0,237, 0, 0, 237, height);
		}else{
			bitmap.setPixels(oldPixels, 0,296, 0, 0, 296, height);
		}
		return bitmap;
	}


	// 保存单色位图数据
	public static void saveMonoData(Bitmap bitmap, Context context) {
		if (bitmap != null) {
			FileOutputStream fos = null;
			try {
				fos = context.openFileOutput("bmp.data", Context.MODE_PRIVATE);
				int height = bitmap.getHeight();
				int width = bitmap.getWidth();
				Log.i(TAG, "height = " + height + ", width = " + width);
				int pixels[] = new int[width * height];
				byte bpixels[] = new byte[width * height];
				int r = 0, g = 0, b = 0;
				bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

				int count = width * height;
				for (int i = 0; i < count; i++) {
					r = Color.red(pixels[i]);
					g = Color.green(pixels[i]);
					b = Color.blue(pixels[i]);
					pixels[i] = (int) (r * 0.299 + g * 0.587 + b * 0.114);
					if (pixels[i] > 128)
						bpixels[i] = 1;
				}

				// int bytesPerRow = (width+31)/32*32/8;
				int bytesPerRow = width / 8;
				byte[] buf = new byte[bytesPerRow * height];
				for (int i = 0; i < height; i++) {
					for (int j = 0; j < bytesPerRow; j++) {
						int tmpBufIndex = i * bytesPerRow + j;
						int tmpPixelIndex = (height - 1 - i) * width + j * 8;
						for (int k = 0; k < 8; k++) {
							buf[tmpBufIndex] |= bpixels[tmpPixelIndex + k] << (7 - k);
						}
					}
				}
				fos.write(buf);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (fos != null)
						fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// 保存为JPG或PNG格式
	public static void saveJpg(Bitmap bitmap) {
		if (bitmap != null) {
			ByteArrayOutputStream baos = null;
			try {
				String path = Environment.getExternalStorageDirectory()
						.getAbsolutePath() + File.separator + "util.jpg";
				baos = new ByteArrayOutputStream();
				// bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
				byte[] photoBytes = baos.toByteArray();
				if (photoBytes != null) {
					new FileOutputStream(new File(path)).write(photoBytes);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (baos != null)
						baos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// 保存为24位位图
	public static void saveBmp(Bitmap bitmap) {
		if (bitmap != null) {
			// 只能转成PNG、JPEG
			// ByteArrayOutputStream bos = new ByteArrayOutputStream();
			// bitmap.compress(CompressFormat.PNG, 90, bos);
			// byte[] data = bos.toByteArray();
			// img.setImageBitmap(BitmapFactory.decodeByteArray(data, 0,
			// data.length));

			// ByteBuffer dst = ByteBuffer.allocate(bitmap.getRowBytes()* h);
			// bitmap.copyPixelsToBuffer(dst);
			// IntBuffer dst=IntBuffer.allocate(w*h);
			// bitmap.copyPixelsToBuffer(dst);

			// calculate how many bytes our image consists of.
			// int bytes = bitmap.getByteCount();
			// or we can calculate bytes this way. Use a different value than 4
			// if you don't use 32bit images.
			// int bytes = bitmap.getWidth()*bitmap.getHeight()*4;
			// ByteBuffer buffer = ByteBuffer.allocate(bytes); //Create a new
			// buffer
			// bitmap.copyPixelsToBuffer(buffer); //Move the byte data to the
			// buffer
			// byte[] array = buffer.array(); //Get the underlying array
			// containing the data.

			int w = bitmap.getWidth(), h = bitmap.getHeight();
			int[] pixels = new int[w * h];
			bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
			byte[] rgb = getBmpRGB888(pixels, w, h);
			byte[] header = getBmpHeader(rgb.length);
			byte[] infos = getBmpInfosHeader(w, h);

			byte[] buffer = new byte[54 + rgb.length];
			System.arraycopy(header, 0, buffer, 0, header.length);
			System.arraycopy(infos, 0, buffer, 14, infos.length);
			System.arraycopy(rgb, 0, buffer, 54, rgb.length);

			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(Environment
						.getExternalStorageDirectory().getPath()
						+ File.separator + "util.bmp");
				fos.write(buffer);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (fos != null)
						fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// BMP文件头
	private static byte[] getBmpHeader(int size) {
		byte[] buffer = new byte[14];
		buffer[0] = 0x42;
		buffer[1] = 0x4D;
		buffer[2] = (byte) (size >> 0);
		buffer[3] = (byte) (size >> 8);
		buffer[4] = (byte) (size >> 16);
		buffer[5] = (byte) (size >> 24);
		buffer[6] = 0x00;
		buffer[7] = 0x00;
		buffer[8] = 0x00;
		buffer[9] = 0x00;
		buffer[10] = 0x36;
		buffer[11] = 0x00;
		buffer[12] = 0x00;
		buffer[13] = 0x00;

		return buffer;
	}

	// BMP文件信息头
	private static byte[] getBmpInfosHeader(int w, int h) {
		byte[] buffer = new byte[40];
		buffer[0] = 0x28;
		buffer[1] = 0x00;
		buffer[2] = 0x00;
		buffer[3] = 0x00;
		buffer[4] = (byte) (w >> 0);
		buffer[5] = (byte) (w >> 8);
		buffer[6] = (byte) (w >> 16);
		buffer[7] = (byte) (w >> 24);
		buffer[8] = (byte) (h >> 0);
		buffer[9] = (byte) (h >> 8);
		buffer[10] = (byte) (h >> 16);
		buffer[11] = (byte) (h >> 24);
		buffer[12] = 0x01;
		buffer[13] = 0x00;
		buffer[14] = 0x18;
		buffer[15] = 0x00;
		buffer[16] = 0x00;
		buffer[17] = 0x00;
		buffer[18] = 0x00;
		buffer[19] = 0x00;
		buffer[20] = 0x00;
		buffer[21] = 0x00;
		buffer[22] = 0x00;
		buffer[23] = 0x00;
		// buffer[24] = (byte) 0xE0;
		buffer[24] = 0x00;
		buffer[25] = 0x00;
		// buffer[25] = 0x01;
		buffer[26] = 0x00;
		buffer[27] = 0x00;
		buffer[28] = 0x00;
		// buffer[28] = 0x02;
		buffer[29] = 0x00;
		// buffer[29] = 0x03;
		buffer[30] = 0x00;
		buffer[31] = 0x00;
		buffer[32] = 0x00;
		buffer[33] = 0x00;
		buffer[34] = 0x00;
		buffer[35] = 0x00;
		buffer[36] = 0x00;
		buffer[37] = 0x00;
		buffer[38] = 0x00;
		buffer[39] = 0x00;

		return buffer;
	}

	private static byte[] getBmpRGB888(int[] b, int w, int h) {
		int len = b.length;
		System.out.println(b.length);
		byte[] buffer = new byte[w * h * 3];
		int offset = 0;
		for (int i = len - 1; i >= w; i -= w) {// DIB文件格式最后一行为第一行，每行按从左到右顺序
			int end = i, start = i - w + 1;
			for (int j = start; j <= end; j++) {
				buffer[offset] = (byte) (b[j] >> 0);
				buffer[offset + 1] = (byte) (b[j] >> 8);
				buffer[offset + 2] = (byte) (b[j] >> 16);
				offset += 3;
			}
		}

		return buffer;
	}

	public static Bitmap decodeFile(String fileName, int height) {
		Bitmap b = null;
		BitmapFactory.Options o2 = null;
		try {
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;

			FileInputStream fis;

			fis = new FileInputStream(fileName);

			BitmapFactory.decodeStream(fis, null, o);
			fis.close();

			int scale = o.outHeight / height;

			o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			fis = new FileInputStream(fileName);
			b = BitmapFactory.decodeStream(fis, null, o2);
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();

		} catch (OutOfMemoryError e) {
			e.printStackTrace();
			System.gc();
		}
		return b;
	}

}

// (biWidth* biBitCount+31)/8*biHeight 4字节对齐

/*
 * 位图文件格式 圖像文件頭
 * 　　1）1-2：(這裏的數字代表的是"字",即兩個字節，下同）圖像文件頭。0x4d42=’BM’，表示是Windows支持的BMP格式
 * 。(注意：查ascii表B 0x42,M0x4d,bfType
 * 为兩個字節，B为low字節，M为high字節所以bfType=0x4D42，而不是0x424D，但注意) 　　2）3-6：整個文件大小。4690
 * 0000，为00009046h=36934。 　　3）7-8：保留，必須設置为0。 　　4）9-10：保留，必須設置为0。
 * 　　5）11-14：從文件開始到位圖數據之間的偏移量(14+40+4*（2^biBitCount）)。4600
 * 0000，为00000046h=70，上面的文件頭就是35字=70字節。 位圖信息頭 　　6）15-18：位圖圖信息頭長度。
 * 　　7）19-22：位圖寬度，以像素为單位。8000 0000，为00000080h=128。 　　8）23-26：位圖高度，以像素为單位。9000
 * 0000，为00000090h=144。 　　9）27-28：位圖的位面數，該值總是1。0100，为0001h=1。
 * 　　10）29-30：每個像素的位數。
 * 有1（單色），4（16色），8（256色），16（64K色，高彩色），24（16M色，真彩色），32（4096M色，增強型真彩色
 * ）。1000为0010h=16。 　　11）31-34：壓縮說明：有0（不壓縮），1（RLE 8，8位RLE壓縮），2（RLE
 * 4，4位RLE壓縮，3（Bitfields
 * ，位域存放）。RLE簡單地說是采用像素數+像素值的方式進行壓縮。T408采用的是位域存放方式，用兩個字節表示一個像素，位域分配为r5b6g5。圖中0300
 * 0000为00000003h=3。
 * 　　12）35-38：用字節數表示的位圖數據的大小，該數必須是4的倍數，數值上等於（≥位圖寬度的最小的4的倍數）×位圖高度×每個像素位數。0090
 * 0000为00009000h=80×90×2h=36864。对于BI_RGB必须设置为0 　　13）39-42：用象素/米表示的水平分辨率。A00F
 * 0000为0000 0FA0h=4000。 一般不用关心，设为0 　　14）43-46：用象素/米表示的垂直分辨率。A00F 0000为0000
 * 0FA0h=4000。 一般不用关心，设为0 　　15）47-50：位圖使用的顏色索引數。設为0的話，則說明使用所有調色板項。 一般不用关心，设为0
 * 　　16）51-54：對圖象顯示有重要影響的顏色索引的數目。如果是0，表示都重要。 调试板： 位图数据：
 */