package com.meeting.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 二维码工具类
 */
public class QrcodeUtil {

    /**
     * 生成唯一Token
     */
    public static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    /**
     * 生成二维码图片（返回BufferedImage）
     */
    public static BufferedImage generateQrcodeImage(String content, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 2);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * 生成二维码图片（返回Base64编码字符串）
     */
    public static String generateQrcodeBase64(String content, int width, int height) throws Exception {
        BufferedImage image = generateQrcodeImage(content, width, height);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] bytes = baos.toByteArray();
        return java.util.Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 生成二维码图片并保存到文件
     */
    public static String generateQrcodeFile(String content, int width, int height, String filePath) throws Exception {
        BufferedImage image = generateQrcodeImage(content, width, height);
        File file = new File(filePath);
        ImageIO.write(image, "png", file);
        return file.getAbsolutePath();
    }

    /**
     * 生成会议二维码内容
     */
    public static String generateMeetingQrcodeContent(String token) {
        return "MEETING_CHECKIN:" + token;
    }
}
