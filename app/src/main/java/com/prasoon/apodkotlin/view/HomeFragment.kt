package com.prasoon.apodkotlin.view

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.WallpaperManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.work.*
import com.google.android.material.navigation.NavigationView
import com.prasoon.apodkotlin.R
import com.prasoon.apodkotlin.model.ApodModel
import com.prasoon.apodkotlin.services.ImageDownloadWorker
import com.prasoon.apodkotlin.utils.Constants
import com.prasoon.apodkotlin.utils.Constants.CURRENT_DATE
import com.prasoon.apodkotlin.utils.Constants.IMAGE_HD_URL
import com.prasoon.apodkotlin.utils.Constants.IMAGE_NAME
import com.prasoon.apodkotlin.utils.Constants.IMAGE_URL
import com.prasoon.apodkotlin.utils.Constants.NIGHT_MODE
import com.prasoon.apodkotlin.utils.Constants.STORAGE_DIRECTORY_PATH
import com.prasoon.apodkotlin.utils.Constants.STORAGE_PERMISSION_CODE
import com.prasoon.apodkotlin.utils.DateInput
import com.prasoon.apodkotlin.utils.ImageUtils.saveImage
import com.prasoon.apodkotlin.utils.loadImage
import com.prasoon.apodkotlin.viewmodel.ApodViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_home.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(), NavigationView.OnNavigationItemSelectedListener {
    private val TAG = "HomeFragment"
    private val viewModel: ApodViewModel by viewModels()

    @Inject
    lateinit var currentApod: ApodModel
    //private var currentApod: ApodModel = ApodModel("", "", "", "", "", "", "")
    var apodDateListDb: List<String> = listOf()

    lateinit var toggle: ActionBarDrawerToggle

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toggle = ActionBarDrawerToggle(
            activity,
            drawer_layout,
            home_toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
        nav_view.bringToFront()     // Needed for buttons to be clickable

        Log.i(TAG, "apodDateListDb: $apodDateListDb")

        // Link corresponding ViewModel to View(this)
        //viewModel = ViewModelProviders.of(this).get(ApodViewModel::class.java)
        viewModel.refresh(DateInput.currentDate)

        home_swipe_refresh_layout.setOnRefreshListener {
            Log.i(TAG, "swipe_refresh_layout date: ${DateInput.currentDate}")
            viewModel.refresh(DateInput.currentDate)
            home_swipe_refresh_layout.isRefreshing = false
        }

        if (DateInput.simpleDateFormat != null)
            home_text_view_date_picker.text = DateInput.simpleDateFormat
        else
            home_text_view_date_picker.text = "Select Date to get today's picture!"

        // Enable scrolling for explanation
        home_text_view_explanation.setMovementMethod(ScrollingMovementMethod())

        home_select_list_fragment.setOnClickListener {
            Log.i(TAG, "selectListFragment")

            val action = HomeFragmentDirections.actionHomeFragmentToListFragment()
            // Avoid below for compile time safety
            // findNavController().navigate(R.id.action_homeFragment_to_listFragment)
            findNavController().navigate(action)
        }


        // Wallpaper
        val wallpaperManager = WallpaperManager.getInstance(context)
        home_set_wallpaper.setOnClickListener {
            Log.i(TAG, "set Wallpaper")
            //val bitmap = home_image_view_result.buildDrawingCache()
            val bitmap = (home_image_view_result.drawable as BitmapDrawable).bitmap
            val bmpImg = (home_image_view_result.getDrawable() as BitmapDrawable).bitmap


            try {
                // Set on Home screen
                wallpaperManager.setBitmap(bitmap)
                // Set on Lock Screen
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                } else {
                    Toast.makeText(requireContext(), "Wallpaper can't be set on devices running below Nougat!", Toast.LENGTH_SHORT).show()
                }
                Toast.makeText(requireContext(), "Wallpaper Set Successfully!!", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(requireContext(), "Setting WallPaper Failed!!", Toast.LENGTH_SHORT).show()
            }
        }

        val appSettingsPrefs: SharedPreferences = requireContext().getSharedPreferences(
            NIGHT_MODE,
            MODE_PRIVATE
        )
        val sharedPreferencesEditor: SharedPreferences.Editor = appSettingsPrefs.edit()
        val isNightMode: Boolean = appSettingsPrefs.getBoolean(NIGHT_MODE, false)

        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            // Change images
            home_dark_mode.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_light_mode
                )
            )

        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            home_dark_mode.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_dark_mode
                )
            )
        }
        // Night Mode preferences
        home_dark_mode.setOnClickListener {
            if (isNightMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                sharedPreferencesEditor.putBoolean(NIGHT_MODE, false)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                sharedPreferencesEditor.putBoolean(NIGHT_MODE, true)
            }
            sharedPreferencesEditor.apply()
        }

        val cal = Calendar.getInstance()

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                Log.i(
                    TAG,
                    "calendar default format: YEAR: $year MONTH: $monthOfYear DAY: $dayOfMonth"
                )

                // val myFormat = "dd.MM.yyyy" // mention the format you need
                // val myFormat2 = "dd LLL yyyy HH:mm:ss aaa z" // mention the format you need
                val format = "LLL d, yyyy" // mention the format you need

                val simpleDateFormat = SimpleDateFormat(format, Locale.US)
                DateInput.simpleDateFormat = simpleDateFormat.format(cal.time)
                home_text_view_date_picker.text = DateInput.simpleDateFormat
                Log.i(TAG, "calendar simpleDateFormat: ${DateInput.simpleDateFormat}")

                val monthOfYearString =
                    if (monthOfYear + 1 < 10) "0" + (monthOfYear + 1) else (monthOfYear + 1).toString()
                val dayOfMonthString =
                    if (dayOfMonth < 10) "0$dayOfMonth" else dayOfMonth.toString()

                DateInput.currentDate = "$year-$monthOfYearString-$dayOfMonthString"
                Log.i(TAG, "calendar date: ${DateInput.currentDate}")
                viewModel.refresh(DateInput.currentDate)
            }

        home_select_date_button.setOnClickListener {
            activity?.let { it1 ->
                val datePickerDialog = DatePickerDialog(
                    it1, dateSetListener, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(
                        Calendar.DAY_OF_MONTH
                    )
                )

                // Account for Date Picker to show till today's date.
                val halfDayBefore = 1 * 12 * 60 * 60 * 1000L
                val minimumRange = Calendar.getInstance()
                minimumRange.set(Calendar.YEAR, 1995)
                minimumRange.set(Calendar.MONTH, 5)
                minimumRange.set(Calendar.DAY_OF_MONTH, 16)
                datePickerDialog.datePicker.setMinDate(minimumRange.getTimeInMillis());

                datePickerDialog.datePicker.maxDate = System.currentTimeMillis() - halfDayBefore
                datePickerDialog.show()
            }
        }

        home_video_view_button.setOnClickListener {
            performActionIntent(requireContext(), currentApod.url, Constants.INTENT_ACTION_VIEW)
        }

        // todo: when apod is fully loaded, then enable the buttons, using loading from viewmodel
        home_add_to_favorites.setOnClickListener {
            Log.i(TAG, "apodDateListDb: addIntoFavorites $apodDateListDb")
            if (apodDateListDb.contains(DateInput.currentDate)) {
                Toast.makeText(activity, "Already saved in DB!", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.saveApod(currentApod)
                Toast.makeText(activity, "Added to Favorites!", Toast.LENGTH_SHORT).show()
                home_add_to_favorites.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorAddToFavorites
                    )
                )
            }
            /*addIntoFavorites.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_favorite_fill))*/
        }

        // Request Permission
        home_download_image.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permissions already granted
                // start to download file and save in external storage
                if (!currentApod.mediaType.equals("video")) {
                    context?.let { it1 -> //saveImage(it1, currentApod.url, currentApod.hdurl, DateInput.currentDate)
/*                        runBlocking {
                            WorkScheduler.scheduleWorkToSaveImage(it1, currentApod.url, currentApod.hdurl, DateInput.currentDate)
                        }
                        WorkScheduler.getWorkInfoAfterSaveImage(it1, viewLifecycleOwner)*/
                        ///////// Work Manager
                        val passDataToWorker = Data.Builder()
                            .putString(IMAGE_URL, currentApod.url)
                            .putString(IMAGE_HD_URL, currentApod.hdurl)
                            .putString(CURRENT_DATE, DateInput.currentDate)
                            .build()

                        // todo: context should be made private
                        // todo: disable the download button to avoid multiple attempts

                        // Constraints
                        val constraints =
                            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()

                        val imageDownloaderWorkRequest =
                            OneTimeWorkRequestBuilder<ImageDownloadWorker>()
                                .setConstraints(constraints)
                                .setInputData(passDataToWorker) // Pass data to worker
                                .build()
                        // Start the worker with enqueue
                        WorkManager.getInstance(it1).enqueue(imageDownloaderWorkRequest)
                        Toast.makeText(context, "Starting download...", Toast.LENGTH_SHORT).show()

                        WorkManager.getInstance(it1)
                            .getWorkInfoByIdLiveData(imageDownloaderWorkRequest.id)
                            .observe(viewLifecycleOwner) {
                                if (it.state == WorkInfo.State.SUCCEEDED) {
                                    // Get data from worker
                                    val imageName = it.outputData.getString(IMAGE_NAME)
                                    val storageDirectoryPath =
                                        it.outputData.getString(STORAGE_DIRECTORY_PATH)
                                    Log.i(
                                        TAG,
                                        "doWork result: \n File name: $imageName \n File path: $storageDirectoryPath"
                                    )
                                    Toast.makeText(
                                        context,
                                        "Saved as $imageName.jpg in $storageDirectoryPath",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        val wallpaperScheduleWorkRequest = PeriodicWorkRequest.Builder(
                            ImageDownloadWorker::class.java,
                            10,
                            TimeUnit.HOURS
                        )
                            .setConstraints(constraints)
                            .setInputData(passDataToWorker) // Pass data to worker
                            .build()

                    }
                }
            } else {
                requestStoragePermission(requireActivity())
            }
        }

        home_image_view_result.setOnClickListener {
            Log.i(TAG, "imageViewResult")
            if (currentApod.mediaType == "image") {
                val action = HomeFragmentDirections.actionHomeFragmentToViewFragment(currentApod.hdurl)
                findNavController().navigate(action)
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        // Observe when loading is successful
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            isLoading?.let {
                /* todo: isLoading should be changed to integer, and each value should account for errors received.
                    For ex: 503 server error, Image loading failed.*/
            }
        }

        viewModel.apodDateList.observe(viewLifecycleOwner) {
            apodDateListDb = it
            Log.i(TAG, "apodDateList from live data: $it")
            if (it.contains(DateInput.currentDate)) {
                Log.i(TAG, "apodDateList from live data: ${currentApod.date}")
                home_add_to_favorites.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorAddToFavorites
                    )
                )
            } else {
                home_add_to_favorites.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.gray
                    )
                )
            }
        }

        // Observe apod title from viewModel
        viewModel.apodModel.observe(viewLifecycleOwner, Observer { apodModel ->
            apodModel?.let {
                currentApod = apodModel
                DateInput.currentDate = apodModel.date
                Log.i(TAG, "observeViewModel apodDetail: $currentApod")
                Log.i(TAG, "observeViewModel apodDetail url: ${currentApod.url}")
                Log.i(
                    TAG,
                    "observeViewModel apodDetail modified: ${createApodUrl(currentApod.date)}"
                )
                if (currentApod.mediaType == "video") {
                    // Fit center for maintaining YouTube video's aspect ratio
                    home_image_view_result.scaleType = ImageView.ScaleType.FIT_CENTER

                    home_video_view_button.visibility = View.VISIBLE
                    home_download_image.visibility = View.INVISIBLE
                    home_add_to_favorites.visibility = View.INVISIBLE

                    // var videoId = extractYoutubeId(currentApod.url)
                    // loadVideo(videoId)
                    if (currentApod.url.contains("youtube")) {
                        home_add_to_favorites.visibility = View.VISIBLE
                        val thumbnailUrl = getYoutubeThumbnailUrlFromVideoUrl(currentApod.url)
                        Log.i(TAG, "observeViewModel apodDetail thumbnailUrl: $thumbnailUrl")
                        home_image_view_result.loadImage(thumbnailUrl, false, home_progress_image_view)
                    } else {
                        // Handling for Apods which are not image or a YouTube video.
                        // Open links with browser
                        performActionIntent(
                            requireContext(),
                            currentApod.url,
                            Constants.INTENT_ACTION_VIEW
                        )
                        home_image_view_result.setImageResource(R.drawable.handle_another_app)
                    }

                } else {
                    // Fit center crop to fit aspect ratio of imageview
                    home_image_view_result.scaleType = ImageView.ScaleType.CENTER_CROP
                    home_download_image.visibility = View.VISIBLE
                    home_image_view_result.visibility = View.VISIBLE
                    home_video_view_button.visibility = View.INVISIBLE
                    home_add_to_favorites.visibility = View.VISIBLE
                    home_image_view_result.loadImage(currentApod.url, false, home_progress_image_view)
                }

                home_text_view_title.text = currentApod.title
                home_text_view_metadata_date.text = currentApod.date
                home_text_view_explanation.text = currentApod.explanation
            }
        })
    }

    private fun requestStoragePermission(activity: Activity) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            // Add rational for requesting permission
            AlertDialog.Builder(requireContext())
                .setTitle("Permission needed... ")
                .setMessage("Grant Storage Permissions to Save Image")
                .setPositiveButton("Okay") { p0, p1 ->
                    Log.i(TAG, "requestStoragePermission: ")
                    requestPermissions(
                        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                        STORAGE_PERMISSION_CODE
                    )
                }
                .setNegativeButton("Cancel", null)
                .create()
                .show()
        } else {
            requestPermissions(
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "onRequestPermissionsResult: granted")
                Toast.makeText(activity, "Permission granted!", Toast.LENGTH_SHORT).show()

                // start to download file and save in external storage
                if (!currentApod.mediaType.equals("video")) {
                    context?.let { it1 ->
                        saveImage(
                            it1,
                            currentApod.url,
                            currentApod.hdurl,
                            DateInput.currentDate
                        )
                    }
                }
            } else {
                Log.i(TAG, "onRequestPermissionsResult: denied")
                Toast.makeText(activity, "Permission denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getApodDatesFromDb()
    }

    // Cancel any work if needed
    fun cancelWork(context: Context, workRequest: WorkRequest) {
        WorkManager.getInstance(context).cancelWorkById(workRequest.id)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                Toast.makeText(activity, "settings clicked!", Toast.LENGTH_SHORT).show()
                val action = HomeFragmentDirections.actionHomeFragmentToSettingsFragment()
                findNavController().navigate(action)
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}