package com.app.vanillacamera

import android.content.Context
import android.content.Intent
import android.hardware.camera2.*
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.io.IOException
import kotlin.math.max


class VanillaScannerViewModel : ViewModel() {

    private var flashOn = false
    var manager: CameraManager? = null
    var cameraId: String? = null
    var captureRequestBuilder: CaptureRequest.Builder? = null
    var cameraCaptureSession: CameraCaptureSession? = null
    var cameraCharacteristics: CameraCharacteristics? = null
    var textureView: AutoFitTextureView? = null
    var backgroundHandler: Handler? = null
    var captureCallback: CameraCaptureSession.CaptureCallback? = null


    fun setFlash(flashOn: Boolean) {
        if (manager != null && cameraId != null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                toggleFlashMode(flashOn)
            }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun toggleFlash() {

        if (manager != null && cameraId != null)

            try {
                this.flashOn = !flashOn
                toggleFlashMode(flashOn)
            } catch (e: Exception) {
               e.printStackTrace()
            }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun toggleFlashMode(enable: Boolean) {
        try {
            if (enable) {
                captureRequestBuilder?.set(
                    CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_TORCH
                )
//                    captureRequestBuilder?.set(
//                        CaptureRequest.CONTROL_AE_MODE,
//                        CaptureRequest.CONTROL_AE_MODE_ON
//                    )
            } else {
                captureRequestBuilder?.set(
                    CaptureRequest.FLASH_MODE,
                    CaptureRequest.FLASH_MODE_OFF
                )
//                    captureRequestBuilder?.set(
//                        CaptureRequest.CONTROL_AE_MODE,
//                        CaptureRequest.CONTROL_AE_MODE_OFF
//                    )
            }
            cameraCaptureSession?.setRepeatingRequest(captureRequestBuilder?.build()!!, null, null)
        } catch (e: CameraAccessException) {
          e.printStackTrace()
        } catch (e: Exception) {
          e.printStackTrace()
        }

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun touchToFocus(event: MotionEvent?) {
        var maskedAction = event?.actionMasked
        if (!cameraId.isNullOrEmpty()) {
            try {

                var sensorArraySize =
                    cameraCharacteristics?.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)

                val y = event?.x?.toInt()?.div(textureView?.width?.toFloat()!!)
                    ?.times(sensorArraySize?.height()?.toFloat()!!)
                val x = event?.y?.toInt()?.div(textureView?.height?.toFloat()!!)
                    ?.times(sensorArraySize?.width()?.toFloat()!!)

                val halfTouchWidth = 150
                val halfTouchHeight = 150

                val focusArea = MeteringRectangle(
                    max(x?.minus(halfTouchWidth)!!, 0.0f).toInt(),
                    max(y?.minus(halfTouchHeight)!!, 0.0f).toInt(),
                    halfTouchWidth * 2,
                    halfTouchHeight * 2,
                    MeteringRectangle.METERING_WEIGHT_MAX - 1
                )


                captureCallback = object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)

                        captureRequestBuilder?.set(CaptureRequest.CONTROL_AF_TRIGGER, null)
                        cameraCaptureSession?.setRepeatingRequest(
                            captureRequestBuilder?.build()!!,
                            null,
                            null
                        )
                    }

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {
                        super.onCaptureFailed(session, request, failure)
                    }
                }

                cameraCaptureSession?.stopRepeating()


                captureRequestBuilder?.set(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
                )
                captureRequestBuilder?.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF
                )

                cameraCaptureSession?.capture(
                    captureRequestBuilder?.build()!!, captureCallback,
                    Handler()
                )

                if (isMeteringAreaAFSupported()) {
                    captureRequestBuilder?.set(
                        CaptureRequest.CONTROL_AF_REGIONS,
                        arrayOf(MeteringRectangle(focusArea.rect, 1))
                    )
                }
                captureRequestBuilder?.set(
                    CaptureRequest.CONTROL_MODE,
                    CameraMetadata.CONTROL_MODE_AUTO
                );
                captureRequestBuilder?.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_AUTO
                );
                captureRequestBuilder?.set(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START
                );
                captureRequestBuilder?.setTag("FOCUS_TAG"); //we'll capture this later for resuming the preview

                //then we ask for a single request (not repeating!)
                cameraCaptureSession?.capture(
                    captureRequestBuilder?.build()!!,
                    captureCallback,
                    backgroundHandler
                )


            } catch (e: Exception) {
              e.printStackTrace()
            }
        } else {

        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun isMeteringAreaAFSupported(): Boolean {

        var result = false
        try {
            manager?.getCameraCharacteristics(cameraId!!)
                ?.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)!! >= 1
        } catch (e: Exception) {
          e.printStackTrace()
        }
        return result
    }

    fun scanQrFromGallery(context: Context, data: Intent): String {
        var text = ""
        val uri = data.data
        Log.d("BarCode", uri!!.toString())
        try {
            val generatedQRCode = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            val width = generatedQRCode.width
            val height = generatedQRCode.height
            val pixels = IntArray(width * height)
            generatedQRCode.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader()
            var result: Result? = null
            try {
                result = reader.decode(binaryBitmap)
            } catch (e: NotFoundException) {
              e.printStackTrace()
            } catch (e: ChecksumException) {
              e.printStackTrace()
            } catch (e: FormatException) {
              e.printStackTrace()
            }

            text = result?.text!!

            Log.d("barcode_string", text)

        } catch (e: IOException) {
          e.printStackTrace()
        } catch (e: KotlinNullPointerException) {
          e.printStackTrace()
        } catch (e: Exception) {
          e.printStackTrace()
        }

        return text
    }


}
