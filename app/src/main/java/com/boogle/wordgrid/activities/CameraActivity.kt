package com.boogle.wordgrid.activities

import android.R
import android.content.ContentValues
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import com.boogle.wordgrid.dao.IntervalNode
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.googlecode.tesseract.android.TessBaseAPI
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import com.boogle.wordgrid.dao.Pair

class CameraActivity : ComponentActivity() {
    val tag = "CAMERA"
    private var imageUri: Uri? = null
    private var croppedImageUri: Uri? = null
    private val cropImage = registerForActivityResult(CropImageContract()) { result ->

    }
    private val captureImage = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        handleCaptureImageResult(success)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val resolver = contentResolver
        val photoDetails = ContentValues().apply {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_$timeStamp.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WordGridApp")
        }
        imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photoDetails)
        imageUri?.let {
            captureImage.launch(it)
        }
    }

    private fun handleCaptureImageResult(success: Boolean) {
        if (success) {
            cropImage.launch(
                options(uri = imageUri)
            )
        } else {
            Toast.makeText(this, "Photograph failed...", Toast.LENGTH_SHORT).show()
        }
    }
    private fun handleCropImageResult(result: CropImageView.CropResult) {
        if (result.isSuccessful) {
            croppedImageUri = result.uriContent
            var image: Mat? = null
            try {
                image = convertBitmapToMat()
            } catch (t: Throwable) {
                Toast.makeText(this, "Couldn't find image file: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    val CAMERA_CAPTURE = 1024 // Code for activity resolution

    val BOARD_SIZE = 4

    val VALID_CHARS = HashSet(
        mutableListOf(
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n",
            "o", "p", "qu", "r", "u", "s", "t", "v", "w", "x", "y", "z"
        )
    )
    val VALID_CHARS_PRETTY = arrayOf(
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
        "M", "N", "O", "P", "Qu", "U", "R", "S", "T", "W", "X", "Y", "Z"
    )

    private val randomBtn: Button? = null
    private val solveBtn: Button? = null
    private val populateBtn: Button? = null
    private val clearBtn: Button? = null
    private val cameraBtn: ImageButton? = null
    private val easyEntry: EditText? = null
    private val solutionsListView: ListView? = null
    private val maxScore: TextView? = null

    private val mPhotoUri: Uri? = null
    private val mCroppedUri: Uri? = null


    private val ASSET_TESS_DIR = "tessdata"

    private val TESS_DATA = "/tessdata"

    private val DATA_PATH: String? = null

    private val EXTERNAL_DIR: String? = null

    private var tessAPI: TessBaseAPI? = null

    private val OpenCVSetup = false // Set to true once OpenCV has been setup


    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    Log.i("Open CV", "OpenCV loaded successfully")
                }

                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }
    private fun convertBitmapToMat(): Mat {
        var ims: InputStream? = null
        ims = contentResolver.openInputStream(mCroppedUri!!)
        val bitmap = BitmapFactory.decodeStream(ims)
        val img = Mat()
        Utils.bitmapToMat(bitmap, img)
        return img
    }

    private fun downsizeMat(mat: Mat, w: Double, h: Double) {
        var width = mat.size().width
        var height = mat.size().height
        val scaleFactor = 0.9
        var matToResize = mat.clone()
        while (width > w || height > h) {
            val resizedMat = Mat()
            val newSize = Size(width * 0.9, height * 0.9)

            Imgproc.resize(matToResize, resizedMat, newSize, 0.0, 0.0, Imgproc.INTER_CUBIC)
            matToResize = resizedMat.clone()
            width = matToResize.size().width
            height = matToResize.size().height
        }
    }

    private fun preprocessImage(img: Mat): Mat {
        Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY)
        Imgproc.threshold(img, img, 180.0, 255.0, Imgproc.THRESH_BINARY_INV)
        val kernelOne = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, Size(3.0, 3.0))
        Imgproc.morphologyEx(img, img, Imgproc.MORPH_CLOSE, kernelOne, Point(-1.0, -1.0), 2)
        return img
    }

    private fun fitToGrid(img: Mat): ArrayList<Mat> {
        val imgHeight = img.size().height
        val imgWidth = img.size().width
        Log.d("Open CV", "Cropped image size: $imgHeight x $imgWidth")
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        // Find all contours on board
        Imgproc.findContours(img, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE)
        Log.d("Open CV", contours.size.toString() + " contours found.")
        val letterContours = ArrayList<Mat>()

        // used to build BSTs based on y-coordinate of contour start, and x-coordinate respectively
        var rootY: IntervalNode<Pair>? = null
        var rootX: IntervalNode<Pair>? = null
        for (contour in contours) {
            val contourRect: Rect =
                Imgproc.boundingRect(contour) // Bounding rectangle around contour
            val contourWidth: Double = contourRect.width.toDouble()
            val contourHeight: Double = contourRect.height.toDouble()
            val contourX: Double = contourRect.x.toDouble()
            val contourY: Double = contourRect.y.toDouble()
            if (contourRect.area() < imgHeight * imgWidth * 1 / 550) { // small enough to capture I contours
                continue
            } else if (contourRect.area() > imgHeight * imgWidth * 1 / 60) { // big enough to capture all letters, but not complete tiles.
                continue
            } else if (contourWidth < imgWidth * 1 / 50 || contourHeight < imgHeight * 1 / 50) { // extremely skinny contours are not okay
                continue
            } else if (contourWidth < imgWidth * 1 / 15 && contourHeight < imgHeight * 1 / 15) { // both dimensions cannot be small
                continue
            } else if (contourX <= 5 || contourY <= 5 || contourX + contourWidth >= imgWidth - 6 || contourY + contourHeight >= imgHeight - 6) {
                continue  // contour not at very edge of image
            }
            val letterContour: Mat =
                Mat(img, contourRect).clone() // contourRect is ROI of original MAT
            letterContours.add(letterContour) // Identified as a contour of appropriate size
            Log.d("Open CV", "Contour of size $contourHeight x $contourWidth ACCEPTED.")
            Log.d(
                "Open CV",
                "Relative size by area of " + contourRect.area() / (imgHeight * imgWidth)
            )


            // draw rectangle on originla image (for testing)
            Imgproc.rectangle(
                img, Point(contourRect.x.toDouble(), contourRect.y.toDouble()), Point(
                    contourRect.x.toDouble() + contourRect.width.toDouble(),
                    contourRect.y.toDouble() + contourRect.height.toDouble()
                ), Scalar(200.0), 15
            )
            val yInterval: IntervalNode<Pair> = IntervalNode(
                contourY,
                contourY + contourHeight, Pair(contourX, letterContour)
            )
            // BST of intervals organized by y-coordinate. Stores x-coordinate for grid formulation later.
            val xInterval: IntervalNode<Pair> = IntervalNode(
                contourX,
                contourX + contourWidth, Pair(contourY, letterContour)
            )
            // BST of intervals organized by x-coordinate
            if (rootY == null) {
                rootY = yInterval // Initialize tree
            } else {
                rootY.add(yInterval)
            }
            if (rootX == null) {
                rootX = xInterval
            } else {
                rootX.add(xInterval)
            }
        }
        val contourBitmap = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(img, contourBitmap)

//        testImage.setImageBitmap(contourBitmap);
        Log.d("Open CV", letterContours.size.toString() + " letter-sized contours found.")
        if (letterContours.size == 0) { // No letters found.
            throw Error("Failed to localize Boggle board")
        }

        // Collapse to all exclusive intervals contained in tree.
        var mergedY: ArrayList<IntervalNode<ArrayList<Pair?>>> = rootY!!.merge()
        var mergedX: ArrayList<IntervalNode<ArrayList<Pair?>>> = rootX!!.merge()
        var outliersExist = true
        Log.d("Open CV", "Grid before pruning: " + mergedY.size + ", " + mergedX.size)
        while (outliersExist) { // outliers are any contours which don't overlap with any other contours in at least one dimension
            outliersExist = false //  assume good until proved wrong
            if (mergedY.size == 0) {
                Log.d("Open CV", "No rows left.")
                throw Error("Failed to find board!")
            }
            for (i in mergedY.indices) {
                val y_interval: IntervalNode<ArrayList<Pair?>> = mergedY[i]

                if (y_interval.data!!.size === 1) {
                    Log.d("Open CV", "Outlier row removed. Reconstructing grid estimation...")
                    rootY.removeByData(y_interval.data!!.get(0))
                    rootX.removeByData(y_interval.data!!.get(0)) // remove outlier from both trees
                    mergedY = rootY.merge() // reconstruct intervals
                    mergedX = rootX.merge()
                    Log.d(
                        "Open CV",
                        "Grid after reconstruction: " + mergedY.size + ", " + mergedX.size
                    )
                    outliersExist = true // we found an outlier
                    break
                }
            }
            if (mergedX.size == 0) {
                Log.d("Open CV", "No columns left.")
                throw Error("Failed to find board!")
            }
            for (i in mergedX.indices) {
                val x_interval: IntervalNode<ArrayList<Pair?>> = mergedX[i]

                if (x_interval.data!!.size === 1) { // outlier merged group with one element
                    Log.d("Open CV", "Outlier column removed. Reconstructing grid estimation...")
                    rootY.removeByData(x_interval.data!!.get(0))
                    rootX.removeByData(x_interval.data!!.get(0))
                    mergedY = rootY.merge()
                    mergedX = rootX.merge()
                    Log.d(
                        "Open CV",
                        "Grid after reconstruction: " + mergedY.size.toString() + ", " + mergedX.size.toString()
                    )
                    outliersExist = true
                    break
                }
            }
        }
        val orderedLetterContours = ArrayList<Mat>()
        Log.d(
            "Open CV",
            "Pruned grid of size: " + mergedY.size.toString() + ", " + mergedX.size.toString()
        )
        if (mergedY.size != 4 || mergedX.size != 4) { // Expectation of four rows and four columns after pruning
            throw Error("Failed to localize Boggle board.")
        } else {
            Log.d("Open CV", "Board localized. Fitting to grid...")
            Collections.sort(mergedY) // Sort by y-coordinate (4 rows)
            Collections.sort(mergedX) // Sort by x-coordiante (4 columns)
            val grid = Mat.zeros(Size(4.0, 4.0), CvType.CV_8UC1)
            for (i in mergedY.indices) { // Over each row
                Log.d(
                    "Open CV",
                    "Fitting over row $i starting at position " + java.lang.String.valueOf(
                        mergedY[i].start
                    )
                )
                val row: IntervalNode<ArrayList<Pair?>> = mergedY[i]
                val rowData: ArrayList<Pair?>? =
                    row.data // Contains x-coordinate and contour pair
                Collections.sort(rowData) // Sort by x-coordinate (i.e gives contours in 1 row, from left to right)
                for (rowDatum in rowData!!) {
                    for (j in mergedX.indices) { // Over each column
                        Log.d("Open CV", "Checking column $j")
                        val col: IntervalNode<ArrayList<Pair?>> = mergedX[j]
                        if (col.overlapsPoint(rowDatum!!.pos)) { // Grid position found
                            Log.d("Open CV", "Element matched to column $j")
                            if (grid[i, j][0] == 1.0) {
                                Log.d("Open CV", "Position already occupied. Ignoring.")
                            } else {
                                Log.d("Open CV", "Populating grid position at $i $j")
                                grid.put(i, j, 1.0)
                                orderedLetterContours.add(rowDatum.contour)
                            }
                            break
                        }
                    }
                }
            }
            for (i in 0..3) {
                for (j in 0..3) {
                    if (grid[i, j][0] == 0.0) {
                        Log.d(
                            "Open CV",
                            "Grid position at " + i.toString() + ", " + j.toString() + "unoccupied. Filling with blank."
                        )
                        val blank = Mat.zeros(Size(100.0, 100.0), CvType.CV_8UC1)
                        orderedLetterContours.add(i * 4 + j, blank) // unrecognized grid component
                    }
                }
            }
        }
        return orderedLetterContours
    }

    /**
     * Make a directory in external storage
     * @param path directory to create
     */
    private fun prepareDirectory(path: String) {
        val dir = File(path)
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.d("Tesseract", "ERROR: Creation of directory $path failed.")
            }
        } else {
            Log.d("Tesseract", "Created directory $path")
        }
    }

    /**
     * Prepare Tesseract directory and move over training file.
     */
    private fun prepareTesseract() {
        Log.d("Tesseract", "Preparing directory at $DATA_PATH$TESS_DATA")
        prepareDirectory(DATA_PATH + TESS_DATA)
        copyTessDataFiles()
    }


    /**
     * Copy eng.traineddata file from assets to device storage
     */
    private fun copyTessDataFiles() {
        try {
            val fileList = assets.list(ASSET_TESS_DIR) // eng.traineddata stored in assets/TESS_DATA
            Log.d("Tesseract", "Files in assets$TESS_DATA: $fileList")
            for (fileName in fileList!!) {
                Log.d("Tesseract", "Copying file $fileName")

                // open file within the assets folder and if it is not already there, copy it to the SD card
                val pathToDataFile =
                    "$DATA_PATH$TESS_DATA/$fileName" // external_storage/tessdata/eng.traineddata
                if (!File(pathToDataFile).exists()) {
                    val `in` = assets.open("$ASSET_TESS_DIR/$fileName")
                    val out: OutputStream = FileOutputStream(pathToDataFile)

                    // Transfer bytes from in to out
                    val buf = ByteArray(1024)
                    var len: Int
                    while (`in`.read(buf).also { len = it } > 0) {
                        out.write(buf, 0, len)
                    }
                    `in`.close()
                    out.close()
                    Log.d("Tesseract", "Copied $fileName to SD card.")
                }
            }
        } catch (e: IOException) {
            Log.d("Tesseract", "Unable to copy files to tessdata $e")
        }
    }

    /**
     * Given an ordered list of the contour Mats (L to R, top to bottom), use OCR to try and
     * recognize each character. Try all four orientations and use prediction with highest confidence.
     *
     * @param letterContours ordered letter contours Mat objects
     * @return predictions from L to R, top to bottom
     */
    private fun getBoardCharacters(letterContours: ArrayList<Mat>): ArrayList<String> {
        val symbols = ArrayList<String>()
        tessAPI = TessBaseAPI()
        if (tessAPI == null) {
            Log.e("Tesseract", "TessBaseAPI failed ot initialize.")
        } else {
            tessAPI!!.init(DATA_PATH, "eng")
            tessAPI!!.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_CHAR) // Individual characters
            tessAPI!!.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "ABCDEFGHJKLMNOPQWRSTUVWXYZ")
            // only single uppercase letters can be returned
            // NOTE: I is excluded as it's recognized geometrically
            Log.d("Tesseract", "Tesseract API loaded.")
            for (letterContour in letterContours) {
                if (letterContour.width() < letterContour.height() * 1 / 2.5 || letterContour.height() < letterContour.width() * 1 / 2.5) {
                    // has trouble with I so pinpoint geometrically
                    Log.d("Tesseract", "Identified I geometrically!")
                    symbols.add("I")
                    continue
                }
                var rotConfFactor: Double
                var confFactor: Double
                if (letterContour.width() < letterContour.height()) {
                    rotConfFactor = 1.0
                    confFactor =
                        1.05 // usually the letter orientation has the longest side as the height
                    // inflate the confidence of those predictions
                } else {
                    rotConfFactor = 1.05
                    confFactor = 1.0
                }

                // provide black border around contour
                val borderedLetterContour = Mat()
                Core.copyMakeBorder(
                    letterContour,
                    borderedLetterContour,
                    200,
                    200,
                    200,
                    200,
                    Core.BORDER_CONSTANT,
                    Scalar(0.0, 0.0, 0.0)
                )
                val borderedLetterContourFlipped = borderedLetterContour.clone()
                val borderedLetterContourRotCW = borderedLetterContour.clone()
                val borderedLetterContourRotCCW = borderedLetterContour.clone()

                // rotations to all other orientations
                rotateMAT(borderedLetterContourFlipped, 3) // 180 deg
                rotateMAT(borderedLetterContourRotCW, 1) // 90 CW
                rotateMAT(borderedLetterContourRotCCW, 2) // 90 CCW


                // convert Mats to Bitmaps
                val letterBmp = Bitmap.createBitmap(
                    borderedLetterContour.cols(), borderedLetterContour.rows(),
                    Bitmap.Config.ARGB_8888
                )
                val letterBmpFlipped = Bitmap.createBitmap(
                    borderedLetterContourFlipped.cols(),
                    borderedLetterContourFlipped.rows(), Bitmap.Config.ARGB_8888
                )
                val letterBmpCW = Bitmap.createBitmap(
                    borderedLetterContourRotCW.cols(),
                    borderedLetterContourRotCW.rows(), Bitmap.Config.ARGB_8888
                )
                val letterBmpCCW = Bitmap.createBitmap(
                    borderedLetterContourRotCCW.cols(),
                    borderedLetterContourRotCCW.rows(), Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(borderedLetterContour, letterBmp)
                Utils.matToBitmap(borderedLetterContourFlipped, letterBmpFlipped)
                Utils.matToBitmap(borderedLetterContourRotCW, letterBmpCW)
                Utils.matToBitmap(borderedLetterContourRotCCW, letterBmpCCW)


                // make four predictions with confidence
                tessAPI!!.setImage(letterBmp)
                val predictedChar = tessAPI!!.getUTF8Text()
                val confidence = tessAPI!!.meanConfidence()
                tessAPI!!.setImage(letterBmpFlipped)
                val predictedCharFlipped = tessAPI!!.getUTF8Text()
                val confidenceFlipped = tessAPI!!.meanConfidence()
                tessAPI!!.setImage(letterBmpCW)
                val predictedCharCW = tessAPI!!.getUTF8Text()
                val confidenceCW = tessAPI!!.meanConfidence()
                tessAPI!!.setImage(letterBmpCCW)
                val predictedCharCCW = tessAPI!!.getUTF8Text()
                val confidenceCCW = tessAPI!!.meanConfidence()


                // best prediction for upright orientations
                var finalPred: String
                var finalPredConfidence: Int
                Log.d(
                    "Tesseract",
                    "$predictedChar|$predictedCharFlipped|$predictedCharCW|$predictedCharCCW"
                )
                if (predictedChar == " " && predictedCharFlipped == " ") {
                    finalPred = "" // Empty prediction
                    finalPredConfidence = 0
                } else if (predictedChar == " ") {
                    finalPred = predictedCharFlipped
                    finalPredConfidence = confidenceFlipped
                } else if (predictedCharFlipped == " ") {
                    finalPred = predictedChar
                    finalPredConfidence = confidence
                } else {
                    if (confidence >= confidenceFlipped) {
                        finalPred = predictedChar
                        finalPredConfidence = confidence
                    } else {
                        finalPred = predictedCharFlipped
                        finalPredConfidence = confidenceFlipped
                    }
                }

                // best prediction for sideways orientation
                var finalPredRot: String
                var finalPredRotConfidence: Int
                if (predictedCharCW == " " && predictedCharCCW == " ") {
                    finalPredRot = "" // Empty prediction
                    finalPredRotConfidence = 0
                } else if (predictedCharCW == " ") {
                    finalPredRot = predictedCharCCW
                    finalPredRotConfidence = confidenceCCW
                } else if (predictedCharCCW == " ") {
                    finalPredRot = predictedCharCW
                    finalPredRotConfidence = confidenceCW
                } else {
                    if (confidenceCW >= confidenceCCW) {
                        finalPredRot = predictedCharCW
                        finalPredRotConfidence = confidenceCW
                    } else {
                        finalPredRot = predictedCharCCW
                        finalPredRotConfidence = confidenceCCW
                    }
                }
                Log.d(
                    "Tesseract",
                    "Prediction: $predictedChar with confidence $confidence"
                )
                Log.d(
                    "Tesseract",
                    "Flipped Prediction: $predictedCharFlipped with confidence $confidenceFlipped"
                )
                Log.d(
                    "Tesseract",
                    "Prediction (CW): $predictedCharCW with confidence $confidenceCW"
                )
                Log.d(
                    "Tesseract",
                    "Prediction (CCW): $predictedCharCCW with confidence $confidenceCCW"
                )


                // apply weighting to confidence
                finalPredConfidence = Math.round(finalPredConfidence * confFactor).toInt()
                finalPredRotConfidence = Math.round(finalPredRotConfidence * rotConfFactor).toInt()
                Log.d(
                    "Tesseract",
                    "After rotation factor, confidence of: $finalPredConfidence for $finalPred"
                )
                Log.d(
                    "Tesseract",
                    "After rotation factor, confidence of: $finalPredRotConfidence for $finalPredRot"
                )
                if (finalPredConfidence < finalPredRotConfidence) {
                    finalPred = finalPredRot
                }
                Log.d("Tesseract", "Final Prediction: $finalPred")
                symbols.add(finalPred)
            }
        }

        return symbols // return symbols
    }

    /**
     * Efficient rotation of Mat 90, 180, or 270 degrees
     * @param toRotate Mat to rotate
     * @param rotateCode 1 if 90 CW, 2, if 90 CCW, 3 if 270.
     */
    private fun rotateMAT(toRotate: Mat, rotateCode: Int) {
        // rotateCode: 1 -> 90 CW
        //             2 -> 90 CCW
        //             3 -> 180
        if (rotateCode == 1) {
            Core.transpose(toRotate, toRotate)
            Core.flip(toRotate, toRotate, 1) // flip on y-axis
        } else if (rotateCode == 2) {
            Core.transpose(toRotate, toRotate)
            Core.flip(toRotate, toRotate, 0) // x-axis
        } else if (rotateCode == 3) {
            Core.flip(toRotate, toRotate, -1) // both axes
        }
    }
}