package com.mytracker.gpstracker.familytracker.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.view.View
import android.widget.*
import com.mytracker.gpstracker.familytracker.model.repository.Authentication
import com.mytracker.gpstracker.familytracker.modelview.AuthModelView
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.mytracker.gpstracker.familytracker.BuildConfig
import com.mytracker.gpstracker.familytracker.MyNavigationTutorial
import com.mytracker.gpstracker.familytracker.R
import com.mytracker.gpstracker.familytracker.model.CountryData
import timber.log.Timber

class VerificationActivity : AppCompatActivity() {

    private val VERIFICATION_TIMEOUT: Long = 60 * 1000
    private val PREF_VERIFICATION: String = "pref_verification"
    private val VERIFICATION_CODE: String = "ver_code"
    lateinit var phoneEt : EditText
    lateinit var verifyBtn : Button
    lateinit var checkBtn: Button
    lateinit var countDownTimer: TextView
    lateinit var code: String
    lateinit var id: String
    lateinit var countryCode: String

    lateinit var auth: FirebaseAuth

    lateinit var spinner: Spinner
    lateinit var spinnerAdapter: ArrayAdapter<String>

    private lateinit var verifyCallback: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var authInterface: AuthModelView.AuthInterface
    private val authentication = Authentication(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)

        spinner = findViewById(R.id.country_code)
        phoneEt = findViewById(R.id.phone_et)
        verifyBtn = findViewById(R.id.verify_btn)
        countDownTimer = findViewById(R.id.count_down)
        checkBtn = findViewById(R.id.code_btn)

//        auth = FirebaseAuth.getInstance()

        spinnerAdapter = ArrayAdapter(this,
                R.layout.support_simple_spinner_dropdown_item,
                CountryData.countryNames)
        spinner.adapter = spinnerAdapter
        spinner.onItemSelectedListener = object :AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                countryCode = CountryData.countryAreaCodes[p2]
            }
        }
        spinner.setSelection(0)

        code = ""
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            phoneEt.setText("+1 650-555-3434")
            code = "123456"
        } else {
            code = ""
        }
        id = ""

        countDownTimer.visibility = View.GONE

        authentication.init()

        updateUI(false)
        verifyBtn.setOnClickListener {
            if (checkInput()) {
                verifyPhone()
                holdVerification()
            } else {
                Toast.makeText(this, "Please enter your phone", Toast.LENGTH_SHORT).show()
            }
        }
        checkBtn.setOnClickListener {
            code = phoneEt.text.toString()
            authentication.signIn(PhoneAuthProvider.getCredential(id, code))
        }

        authInterface = object: AuthModelView.AuthInterface {
            override fun onCompleted(p0: PhoneAuthCredential?) {
                Timber.d("onCompleted: ")
                holdVerification()
                updateUI(true)
            }

            override fun onFailed(p0: FirebaseException?) {
                Timber.e(p0)
                updateUI(false)
                Toast.makeText(
                        this@VerificationActivity,
                        p0.let { it?.localizedMessage },
                        Toast.LENGTH_SHORT
                ).show()
            }

            override fun onCodeSent(p0: String?) {
                Timber.d("onCodeSent: ")
                checkBtn.visibility = View.VISIBLE
                id = p0!!
            }

            override fun onException(p0: FirebaseAuthInvalidCredentialsException) {
                Timber.e(p0)
                updateUI(false)
            }
        }
        authentication.addListener(authInterface)
    }

    private fun updateUI(isChecking: Boolean) {
        if (isChecking) {
            phoneEt.hint = getString(R.string.ver_code)
            phoneEt.inputType = InputType.TYPE_CLASS_NUMBER
//            checkBtn.visibility = View.VISIBLE
            verifyBtn.visibility = View.GONE
            phoneEt.setText("")
            if(BuildConfig.DEBUG){
                phoneEt.setText("123456")
//                checkBtn.visibility = View.VISIBLE
            }
        } else {
            verifyBtn.isEnabled = true
            if(BuildConfig.DEBUG) phoneEt.setText("+1 650-555-3434")
            phoneEt.hint = getString(R.string.enter_your_phone)
            phoneEt.setText("")
            phoneEt.inputType = InputType.TYPE_CLASS_PHONE
            checkBtn.visibility = View.GONE
            verifyBtn.visibility = View.VISIBLE
            countDownTimer.visibility = View.GONE
        }
    }
    private fun verifyPhone() {
        val phone = "+" + countryCode + phoneEt.text.toString()
        Timber.d("phoone: %s", phone)
        authentication.verify(phone)
        updateUI(true)
    }

    private fun holdVerification() {
        verifyBtn.isEnabled = false
        countDownTimer.visibility = View.VISIBLE
        startTimer()
    }

    private fun startTimer() {
        object :CountDownTimer(VERIFICATION_TIMEOUT, 1000){
            override fun onFinish() {
                verifyBtn.isEnabled = true
                countDownTimer.visibility = View.GONE
            }

            override fun onTick(p0: Long) {
                countDownTimer.text = ((p0/1000).toString())
            }
        }.start()
    }

    private fun checkInput(): Boolean {
        return !phoneEt.text.equals("")
    }
}
