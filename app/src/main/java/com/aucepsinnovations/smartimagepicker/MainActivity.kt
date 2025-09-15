package com.aucepsinnovations.smartimagepicker

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aucepsinnovations.smart_image_picker.core.api.CountMode
import com.aucepsinnovations.smart_image_picker.core.api.PickerConfig
import com.aucepsinnovations.smart_image_picker.core.api.PickerResult
import com.aucepsinnovations.smart_image_picker.core.contract.SmartImagePickerContract
import com.aucepsinnovations.smart_image_picker.core.data.SmartImagePicker
import com.aucepsinnovations.smartimagepicker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMainBinding

    private val pickImages = registerForActivityResult(SmartImagePickerContract()) { result ->
        when (result) {
            is PickerResult.Single -> {
                println(result.uri)
                SmartImagePicker.uriToFile(this, result.uri, 1024)
            }

            is PickerResult.Multiple -> {
                println(result.uris)
                SmartImagePicker.urisToFiles(this, result.uris, 1024)
            }

            is PickerResult.Canceled -> {
                Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT)
            }

            is PickerResult.Error -> {
                println(result.message)
                Log.ERROR
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initUI()
    }

    private fun initUI() {
        with(binding) {
            btnExactOne.setOnClickListener(this@MainActivity)
            btnExact.setOnClickListener(this@MainActivity)
            btnMax.setOnClickListener(this@MainActivity)
            btnMin.setOnClickListener(this@MainActivity)
            btnRange.setOnClickListener(this@MainActivity)
            btnUnlimited.setOnClickListener(this@MainActivity)
        }
    }

    override fun onClick(view: View?) {
        with(binding) {
            when (view) {
                btnExactOne -> {
                    pickImages.launch(
                        PickerConfig(
                            countMode = CountMode.EXACT_ONE,
                            accentColor = Color.WHITE,
                            backgroundColor = Color.DKGRAY,
                            buttonColor = Color.WHITE,
                            titleColor = Color.BLACK,
                            textColor = Color.BLACK
                        )
                    )
                }

                btnExact -> {
                    pickImages.launch(
                        PickerConfig(
                            countMode = CountMode.EXACT(5),
                            accentColor = Color.WHITE,
                            backgroundColor = Color.DKGRAY,
                            buttonColor = Color.WHITE,
                            titleColor = Color.BLACK,
                            textColor = Color.BLACK
                        )
                    )
                }

                btnMax -> {
                    pickImages.launch(
                        PickerConfig(
                            countMode = CountMode.MAX(5),
                            accentColor = Color.WHITE,
                            backgroundColor = Color.DKGRAY,
                            buttonColor = Color.WHITE,
                            titleColor = Color.BLACK,
                            textColor = Color.BLACK
                        )
                    )
                }

                btnMin -> {
                    pickImages.launch(
                        PickerConfig(
                            countMode = CountMode.MIN(2),
                            accentColor = Color.WHITE,
                            backgroundColor = Color.DKGRAY,
                            buttonColor = Color.WHITE,
                            titleColor = Color.BLACK,
                            textColor = Color.BLACK
                        )
                    )
                }

                btnRange -> {
                    pickImages.launch(
                        PickerConfig(
                            countMode = CountMode.RANGE(2, 5),
                            accentColor = Color.WHITE,
                            backgroundColor = Color.DKGRAY,
                            buttonColor = Color.WHITE,
                            titleColor = Color.BLACK,
                            textColor = Color.BLACK
                        )
                    )
                }

                btnUnlimited -> {
                    pickImages.launch(
                        PickerConfig(
                            countMode = CountMode.UNLIMITED,
                            accentColor = Color.WHITE,
                            backgroundColor = Color.DKGRAY,
                            buttonColor = Color.WHITE,
                            titleColor = Color.BLACK,
                            textColor = Color.BLACK
                        )
                    )
                }
            }
        }
    }
}