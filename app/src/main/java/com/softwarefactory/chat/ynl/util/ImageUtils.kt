package com.softwarefactory.chat.ynl.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.graphics.drawable.RoundedBitmapDrawable
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.util.Base64

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream


object ImageUtils {

    val AVATAR_WIDTH = 128
    val AVATAR_HEIGHT = 128

    /**
     * Bo tròn ảnh avatar
     * @param context
     * @param src ảnh dạng bitmap
     * @return RoundedBitmapDrawable là đầu vào cho hàm setImageDrawable()
     */
    fun roundedImage(context: Context, src: Bitmap): RoundedBitmapDrawable {
        /*Bo tròn avatar*/
        val res = context.resources
        val dr = RoundedBitmapDrawableFactory.create(res, src)
        dr.cornerRadius = Math.max(src.width, src.height) / 2.0f

        return dr
    }

    /**
     * Đối với ảnh hình chữ nhật thì cần cắt ảnh theo hình vuông và lấy phần tâm
     * ảnh để khi đặt làm avatar sẽ không bị méo
     * @param srcBmp
     * @return
     */
    fun cropToSquare(srcBmp: Bitmap): Bitmap? {
        var dstBmp: Bitmap? = null
        if (srcBmp.width >= srcBmp.height) {

            dstBmp = Bitmap.createBitmap(
                srcBmp,
                srcBmp.width / 2 - srcBmp.height / 2,
                0,
                srcBmp.height,
                srcBmp.height
            )

        } else {
            dstBmp = Bitmap.createBitmap(
                srcBmp,
                0,
                srcBmp.height / 2 - srcBmp.width / 2,
                srcBmp.width,
                srcBmp.width
            )
        }

        return dstBmp
    }

    /**
     * Convert ảnh dạng bitmap ra String base64
     * @param imgBitmap
     * @return
     */
    fun encodeBase64(imgBitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        imgBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Làm giảm số điểm ảnh xuống để tránh lỗi Firebase Database OutOfMemory
     * @param is anh dau vao
     * @param reqWidth kích thước chiều rộng sau khi giảm
     * @param reqHeight kích thước chiều cao sau khi giảm
     * @return
     */
    fun makeImageLite(
        `is`: InputStream, width: Int, height: Int,
        reqWidth: Int, reqHeight: Int
    ): Bitmap? {
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        // Calculate inSampleSize
        val options = BitmapFactory.Options()
        options.inSampleSize = inSampleSize

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeStream(`is`, null, options)
    }


    fun convertBitmapToInputStream(bitmap: Bitmap): InputStream {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos)
        val bitmapdata = bos.toByteArray()
        return ByteArrayInputStream(bitmapdata)
    }

}
