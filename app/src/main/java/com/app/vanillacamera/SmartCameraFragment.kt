package com.app.vanillacamera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.test.vanillacamera.R
import com.test.vanillacamera.databinding.VanillaScannerFragmentBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException

class SmartCameraFragment : Fragment() {

    private var flash: Boolean = false
    private var clickOnScanQR: Boolean = false
    private var isFirstTime = false
    private val PICK_IMAGE_REQUEST = 1447
    private val REQUEST_STORAGE_PERMISSION = 1335
    private lateinit var reader: QRCodeReader
    private lateinit var binaryBitmap: BinaryBitmap
    private lateinit var source: RGBLuminanceSource
    private  var pixels: IntArray? = null
    private var height: Int = 0
    private var result: Result? = null
    private var lastMessage = ""
    private var width : Int = 0
    private lateinit var output : String
    private var aspectRatio = 0.0f
    private lateinit var scannerSharedViewModel : ScannerSharedViewModel


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)

    private  var bitmap: Bitmap?=null

    //Callback defined to find the various states of the camera
    private var stateCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object  : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            try {
                //When camera is opened we initialise the surface opreview with the camera else we close the camera

                cameraDevice = camera

                var cameraRequestBuilder =
                    camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                cameraRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                cameraRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                cameraRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
                cameraRequestBuilder.addTarget(surface!!)

                viewModel.captureRequestBuilder = cameraRequestBuilder

                camera.createCaptureSession(
                    listOf(surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigureFailed(session: CameraCaptureSession) {

                        }

                        override fun onConfigured(session: CameraCaptureSession) {
                            try {

                                var captureRequest = cameraRequestBuilder.build()
                                cameraCaptureSession = session
                                viewModel.cameraCaptureSession = cameraCaptureSession

                                cameraCaptureSession?.setRepeatingRequest(
                                    captureRequest,
                                    captureCallback, backgroundHandler
                                );
                            } catch (e: CameraAccessException) {
                                e.printStackTrace()

                                initiateCameraOnResume()

                            }
                            catch (e : Exception)
                            {
                                e.printStackTrace()
                            }
                        }

                    },
                    backgroundHandler
                )
            }catch (e : Exception)
            {
                e.printStackTrace()
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    private var captureCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object  : CameraCaptureSession.CaptureCallback(){
        override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int) {
            super.onCaptureSequenceAborted(session, sequenceId)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)

            try {
                bitmap = surfaceView?.bitmap
                if(bitmap!=null) {
                    output = readQRCode(surfaceView?.bitmap!!)
                    if (!output.isNullOrEmpty() && output != lastMessage) {

                        var vibrator =
                            context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                VibrationEffect.createOneShot(
                                    500,
                                    VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            );
                        } else {
                            //deprecated in API 26
                            vibrator.vibrate(500)
                        }
                        lastMessage = output


                        if(scannerSharedViewModel!=null)
                            scannerSharedViewModel.scanResult.postValue(output)

                        Log.e("Scanresult", output)
                    }
                }
            }
            catch (e : Exception)
            {
                e.printStackTrace()
            }

        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            super.onCaptureFailed(session, request, failure)
        }

        override fun onCaptureSequenceCompleted(
            session: CameraCaptureSession,
            sequenceId: Int,
            frameNumber: Long
        ) {
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber)

        }

        override fun onCaptureStarted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            timestamp: Long,
            frameNumber: Long
        ) {
            super.onCaptureStarted(session, request, timestamp, frameNumber)
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            super.onCaptureProgressed(session, request, partialResult)
        }

        override fun onCaptureBufferLost(
            session: CameraCaptureSession,
            request: CaptureRequest,
            target: Surface,
            frameNumber: Long
        ) {
            super.onCaptureBufferLost(session, request, target, frameNumber)
        }
    }



    private var manager: CameraManager? = null
    private lateinit var viewModel: VanillaScannerViewModel
    private lateinit var dataBinding: VanillaScannerFragmentBinding
    private  var surfaceView: AutoFitTextureView? = null
    private val CAMERA_PERMISSION=123
    private lateinit var holder : SurfaceHolder
    private var surface : Surface? = null
    private  var backgroundThread : HandlerThread? = null
    private lateinit var backgroundHandler : Handler
    private var cameraCaptureSession : CameraCaptureSession? = null
    private var cameraId = ""
    private  var cameraDevice : CameraDevice? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProviders.of(this).get(VanillaScannerViewModel::class.java)
        dataBinding = DataBindingUtil.inflate(inflater,
            R.layout.vanilla_scanner_fragment,container,false)
        scannerSharedViewModel = ViewModelProviders.of(requireActivity()).get(ScannerSharedViewModel::class.java)
        dataBinding.vanillaScannerViewModel = viewModel
        viewModel.textureView = dataBinding.svCamFragment
        return dataBinding.root
    }

    private fun checkCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onResume() {
        super.onResume()
        onClickScanner()
    }

    private fun initiateCameraOnResume()
    {

        try {
            openBackgroundThread()
            if(surface!=null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setUpCamera()
                    openCamera()
                }
            }
        }
        catch (e : Exception)
        {
            e.printStackTrace()
        }


        var listner = object :  TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, p1: Int, p2: Int) {

            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {

            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
                closeCamera()
                closeBackgroundThread()
                return true
            }

            override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
                surface = Surface(p0)

                openBackgroundThread()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setUpCamera()
                    openCamera()
                }

            }

        }


        if(ContextCompat.checkSelfPermission(requireContext(),android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

            if(checkCameraHardware(requireContext())) {
                try {
                    if(surfaceView == null) {
                        surfaceView = dataBinding.svCamFragment
                        surfaceView?.surfaceTextureListener = listner
                    }
                    else
                    {
                        surfaceView?.surfaceTextureListener = listner
                    }

                }
                catch (e : Exception)
                {
                    e.printStackTrace()
                }
            }
            else
            {
                Toast.makeText(requireContext(),"Camera not accessable", Toast.LENGTH_SHORT).show()
            }
        }
        else
        {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA),CAMERA_PERMISSION)
        }

        dataBinding.ivFlash.setOnClickListener {
            if(requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
            {
                if(!flash)
                {
                    try{
                        viewModel.setFlash(true)
                        dataBinding.ivFlash.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.upi_ic_flash))
                        flash = true
                    }
                    catch (e : Exception)
                    {
                        e.printStackTrace()
                    }
                }
                else
                {
                    try{
                        viewModel.setFlash(false)
                        dataBinding.ivFlash.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.upi_ic_flash_disable))
                        flash = false
                    }
                    catch (e : Exception)
                    {
                        e.printStackTrace()
                    }
                }
            }
            else
            {
               Toast.makeText(requireContext(),"Your device does not support flashlight", Toast.LENGTH_SHORT).show()
            }
        }

        dataBinding.ivGallery.setOnClickListener {
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED)
            {
                val myIntent = Intent().apply {
                    type = "image/*"
                    action = Intent.ACTION_GET_CONTENT
                }
                startActivityForResult(Intent.createChooser(myIntent, "Select Picture"), PICK_IMAGE_REQUEST)
            }
            else
            {
                Toast.makeText(requireContext(),"Storage Permission not granted",Toast.LENGTH_SHORT).show()
                // requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),REQUEST_STORAGE_PERMISSION)
            }
        }

        val displayMetrics = DisplayMetrics()
        var windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        aspectRatio = (width/height).toFloat()



    }




    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode)
        {
            CAMERA_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    initiateCameraOnResume()
                } else {
                    requestPermission()
                }
            }


            REQUEST_STORAGE_PERMISSION ->
            {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {

                }
                else
                {
                    requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),REQUEST_STORAGE_PERMISSION)
                }
            }
            else ->
            {

            }
        }
    }

    private fun openBackgroundThread() {
        if(backgroundThread!=null)
        {
            backgroundThread?.quitSafely()
        }
        backgroundThread = HandlerThread("camera_background_thread")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread?.looper)
        viewModel.backgroundHandler = backgroundHandler
    }

    private fun closeCamera() {

        if(cameraCaptureSession!=null && cameraDevice!=null) {
            cameraCaptureSession?.close();
            cameraDevice?.close()
        }
    }

    private fun closeBackgroundThread() {
        if(backgroundThread!=null)
            backgroundThread?.quitSafely()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun openCamera() {
        try {

            //This method opens the camera
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED ) {
                manager?.openCamera(cameraId, stateCallback, backgroundHandler);
            }
        } catch (e : Exception) {
            e.printStackTrace();
        }
    }

    override fun onPause() {
        super.onPause()
        closeCamera()
        closeBackgroundThread()
    }


    override fun onStop() {
        super.onStop()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setUpCamera() {

        //This method creates a camera manager object from camera2 api followed by finding the id og the back facing camera
        if(manager == null)
        {
            manager = activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            viewModel.manager = manager
            var cameraList = manager?.cameraIdList
            for(item in cameraList?.iterator()!!)
            {
                var characteristics = manager?.getCameraCharacteristics(item)
                viewModel.cameraCharacteristics = characteristics
                var sensorArraySize = characteristics?.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                surfaceView?.dim = sensorArraySize
                var orientation = characteristics?.get(CameraCharacteristics.LENS_FACING)
                if(orientation == CameraCharacteristics.LENS_FACING_BACK && characteristics?.get(
                        CameraCharacteristics.FLASH_INFO_AVAILABLE)!!)
                {
                    cameraId = item
                    viewModel.cameraId = cameraId
                    break
                }
            }
        }
    }



    fun readQRCode(bitmap: Bitmap): String {
        var text = ""
        try {
            var mini_bit: Bitmap? = null
            var displayMetrics = DisplayMetrics()
            (context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(displayMetrics)
            width = 1000
            height = 1000
            mini_bit = Bitmap.createScaledBitmap(bitmap,width,height,false)
            pixels = IntArray(width * height)
            mini_bit.getPixels(pixels, 0, width, 0, 0, width, height)
            source = RGBLuminanceSource(width, height, pixels)
            binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            reader = QRCodeReader()
            try {
                result = reader.decode(binaryBitmap)
            } catch (e: NotFoundException) {
                e.printStackTrace()
            } catch (e: ChecksumException) {
                e.printStackTrace()
            } catch (e: FormatException) {
                e.printStackTrace()
            }catch (e: Exception)
            {
                e.printStackTrace()
            }

            bitmap.recycle()
            mini_bit.recycle()

            if (result != null)
                text = result?.text!!
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return text
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                if (data != null && data.data != null) {
                    var dataString = viewModel.scanQrFromGallery(requireContext(),data)
                    if(!dataString.isNullOrEmpty())
                    {


                        if(scannerSharedViewModel!=null)
                            scannerSharedViewModel.scanResult.postValue(dataString)


//                        scannerSharedViewModel.scanResult.value = dataString
                    }
                }
            }
        }
    }

    fun onClickScanner() {

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermission()

        }
        else
        {
            initiateCameraOnResume()

        }


    }

    private fun requestPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION)
    }

}