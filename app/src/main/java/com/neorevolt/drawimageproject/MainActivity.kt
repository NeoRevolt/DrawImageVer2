package com.neorevolt.drawimageproject

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.neorevolt.drawimageproject.databinding.ActivityMainBinding
import com.neorevolt.drawimageproject.reflection.MainViewModel

private const val modulePackageName = "com.neorevolt.drawimage"
private const val editImageClassname = "$modulePackageName.EditImageActivity"
private const val showDataLibClassname = "$modulePackageName.ShowDataLibActivity"
private const val showDataClassname = "$modulePackageName.ShowDataActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var manager: SplitInstallManager
    private val moduleDrawImage by lazy { getString(R.string.module_drawimage) }
    private var destinationClass: String? = null

    private var photoUrl: Uri? = null

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        manager = SplitInstallManagerFactory.create(this)

        binding.apply {
            btnToDrawImage.setOnClickListener {
                destinationClass = editImageClassname
                loadAndLaunchModule(moduleDrawImage, destinationClass!!)
            }

            btnSendData.setOnClickListener {
                destinationClass = showDataClassname
                loadAndLaunchModule(moduleDrawImage, destinationClass!!)
            }

            viewModel = ViewModelProviders.of(this@MainActivity).get(MainViewModel::class.java)

            //Get String from DFM via Reflection
            viewModel.refData.observe(this@MainActivity, Observer {
                binding.tvResultFromFeature.text = it
            })
            viewModel.loadReflectionData()
        }

        // Get Image from DFM
        val extras = intent.data
        Log.d(null, "INI YANG DIDAPET : $extras")
        photoUrl = extras
        if (extras != null && photoUrl != null) {
            binding.ivBaseActivity.setImageURI(photoUrl)
        }

    }

    private val listener = SplitInstallStateUpdatedListener { state ->
        val names = state.moduleNames().joinToString(" - ")
        when (state.status()) {
            SplitInstallSessionStatus.DOWNLOADING -> {
                displayLoadingState(state, "Downloading $names")
            }

            SplitInstallSessionStatus.INSTALLED -> {
                destinationClass?.let { onSuccessfulLoad(names, it) }
            }

            SplitInstallSessionStatus.INSTALLING -> displayLoadingState(state, "Installing $names")
            SplitInstallSessionStatus.FAILED -> {
                Toast.makeText(this, "Error to install", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onResume() {
        manager.registerListener(listener)
        super.onResume()
    }

    override fun onPause() {
        manager.unregisterListener(listener)
        super.onPause()
    }

    // Request to install feature
    //TODO (NeoRevolt) : Find a way to show error when theres no internet.
    private fun loadAndLaunchModule(moduleName: String, className: String) {
        updateProgressMessage("Loading module $moduleName")

        if (manager.installedModules.contains(moduleName)) {
            updateProgressMessage("Already Installed")
            onSuccessfulLoad(moduleName, className)
            return
        }

        val request = SplitInstallRequest.newBuilder()
            .addModule(moduleName)
            .build()

        manager.startInstall(request)

        updateProgressMessage("Starting install for $moduleName")
    }

    private fun onSuccessfulLoad(moduleName: String, className: String) {
        when (moduleName) {
            moduleDrawImage -> {
                //TODO (NeoRevolt) : Change destination class in DFM
                when (className) {
                    editImageClassname -> launchActivity(editImageClassname)
                    showDataLibClassname -> launchActivity(showDataLibClassname)
                    showDataClassname -> launchActivity(showDataClassname)

                }
//                viewModel.setReflectionData()
                Toast.makeText(this, "$moduleName Successfully Installed", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        displayButtons()
    }

    //Note (NeoRevolt) : Find a way to send data between Base and Dynamic Module (COMPLETED)
    private fun launchActivity(className: String) {

        when (className) {
            editImageClassname -> {
                Intent().setClassName(packageName, className)
                    .also {
                        // Send Image URL to DFM
                        val photoUrl =
                            "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9a/Sample_Floorplan.jpg/640px-Sample_Floorplan.jpg"
//                            "https://w0.peakpx.com/wallpaper/837/383/HD-wallpaper-ocean-android-beach-blue-ocean-sea-water.jpg"
//                            "https://w0.peakpx.com/wallpaper/205/460/HD-wallpaper-sea-beach-beautiful-scene-clouds-deep-sea-natural-graphy-nature-beauty-graphy-lover-portrait.jpg"
                        it.putExtra("extra_req", "remote")
                        it.putExtra("extra_photo", photoUrl)
                        startActivity(it)
                    }
            }
            showDataClassname -> {
                viewModel.setReflectionData("Ini Pesan Dari Base")
                Intent().setClassName(packageName, className).also { startActivity(it) }
            }
            else -> Intent().setClassName(packageName, className).also { startActivity(it) }
        }
    }

    private fun displayLoadingState(state: SplitInstallSessionState, message: String) {
        displayProgress()
        binding.apply {
            progressBar.max = state.totalBytesToDownload().toInt()
            progressBar.progress = state.bytesDownloaded().toInt()

            updateProgressMessage(message)
        }
    }

    private fun updateProgressMessage(message: String) {
        if (binding.progress.visibility != View.VISIBLE) {
            displayProgress()
        }
        binding.progressText.text = message

    }

    private fun displayProgress() {
        binding.progress.visibility = View.VISIBLE
        binding.button.visibility = View.GONE
    }

    private fun displayButtons() {
        binding.progress.visibility = View.GONE
        binding.button.visibility = View.VISIBLE
    }


    companion object {
        val EXTRA_PHOTO = "extra_photo"
    }
}