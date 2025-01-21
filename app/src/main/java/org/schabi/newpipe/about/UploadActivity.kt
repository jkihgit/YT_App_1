package org.schabi.newpipe.about

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.ui.AppBarConfiguration
import org.schabi.newpipe.R
import org.schabi.newpipe.databinding.ActivityUploadBinding
import org.schabi.newpipe.databinding.FragmentUploaderBinding
import org.schabi.newpipe.util.Localization
import org.schabi.newpipe.util.ThemeHelper
import org.schabi.newpipe.util.TimeLogger
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class UploadActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityUploadBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Localization.assureCorrectAppLanguage(this)
        super.onCreate(savedInstanceState)
        ThemeHelper.setTheme(this)
        title = getString(R.string.title_activity_upload)

        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.uploaderToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

class UploaderFragment : Fragment() {

    private var _binding: FragmentUploaderBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val logType = "text/plain"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUploaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.uploaderBtn.setOnClickListener {
            saveToFile()
        }
    }

    private fun saveToFile() {
        val tl = TimeLogger.getInstance(requireContext())
        val data = tl.getLogContents()
        Log.d("timeLogger", "log output:\n$data")
        val uri = writeFile(data)
        uploadFile(uri)
    }

    private fun uploadFile(uri: Uri) {
        val suff = "ist.ac.kr"
        val mid = "ng@ka"
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
//            android specifications are silly sometimes
            putExtra(Intent.EXTRA_EMAIL, arrayOf("kh_ju$mid$suff"))
            putExtra(Intent.EXTRA_TITLE, getString(R.string.upload_dialog_title))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body))
            type = logType
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.upload_dialog_title)))
    }

    @SuppressLint("ServiceCast")
    private fun writeFile(data: String): Uri {
        val retval: Uri
        val dirPath = requireContext()
            .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath
        val dir = File(dirPath, "TimeLogger")
        if (!(dir.exists())) {
            dir.mkdir()
        }
        val f = File(dir, getString(R.string.timelog_file))
        if (f.exists()) {
            f.delete()
        }

        val outputStream: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues()
            values.put(MediaStore.Downloads.DISPLAY_NAME, getString(R.string.timelog_file))
            values.put(MediaStore.Downloads.DATE_ADDED, System.currentTimeMillis() / 1000)
            values.put(MediaStore.Downloads.DATE_TAKEN, System.currentTimeMillis() / 1000)
            values.put(MediaStore.Downloads.DATE_MODIFIED, System.currentTimeMillis() / 1000)
            values.put(MediaStore.MediaColumns.MIME_TYPE, logType)
            values.put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS +
                    File.separator + getString(R.string.timelog_dir)
            )
            val extVolumeUri: Uri? = MediaStore.Files.getContentUri("external")
//            returns null if file already exists ... or for whatever reason you cant write there
            retval = requireContext().contentResolver.insert(extVolumeUri!!, values)!!
            requireContext().contentResolver.openOutputStream(retval)
        } else {
            @Suppress("DEPRECATION") val path =
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                ).toString()
            val file = File(path, getString(R.string.timelog_file) + ".log")
            retval = Uri.fromFile(file)
            FileOutputStream(file)
        }

        outputStream?.write(data.toByteArray())
        outputStream?.close()
        return retval
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
