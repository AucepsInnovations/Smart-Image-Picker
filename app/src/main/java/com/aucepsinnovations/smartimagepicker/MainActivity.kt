package com.aucepsinnovations.smartimagepicker

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aucepsinnovations.smart_image_picker.core.api.CountMode
import com.aucepsinnovations.smart_image_picker.core.api.PickerConfig
import com.aucepsinnovations.smart_image_picker.core.api.PickerResult
import com.aucepsinnovations.smart_image_picker.core.contract.SmartImagePickerContract
import com.aucepsinnovations.smartimagepicker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMainBinding

    private val pickImages = registerForActivityResult(SmartImagePickerContract()) { result ->
        when (result) {
            is PickerResult.Success -> {}
            is PickerResult.Canceled -> {}
            is PickerResult.Error -> {}
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
            btnPicker.setOnClickListener(this@MainActivity)
        }
    }

    override fun onClick(view: View?) {
        with(binding) {
            when (view) {
                btnPicker -> {
                    pickImages.launch(
                        PickerConfig(
                            countMode = CountMode.UNLIMITED
                        )
                    )
                }
            }
        }
    }
}