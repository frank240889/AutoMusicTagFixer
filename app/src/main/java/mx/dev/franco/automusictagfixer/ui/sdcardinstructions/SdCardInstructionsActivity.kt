package mx.dev.franco.automusictagfixer.ui.sdcardinstructions

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils
import mx.dev.franco.automusictagfixer.utilities.RequiredPermissions

class SdCardInstructionsActivity : AppCompatActivity() {

    private var mSuccess = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sd_card_instructions)
        val button = findViewById<MaterialButton>(R.id.request_permission_button)
        button.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, RequiredPermissions.REQUEST_PERMISSION_SAF)
        }
        val toolbar = findViewById<MaterialToolbar>(R.id.sd_card_instructions_toolbar)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar!!.setTitle(R.string.sd_card_detected)
        actionBar.setDisplayHomeAsUpEnabled(true)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == RequiredPermissions.REQUEST_PERMISSION_SAF && resultCode == RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"
            val res = AndroidUtils.grantPermissionSD(applicationContext, resultData)
            mSuccess = res
            setResult(RESULT_OK, resultData)
        } else {
            mSuccess = false
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    public override fun onDestroy() {
        super.onDestroy()
        val toast = AndroidUtils.getToast(this)
        toast.duration = Toast.LENGTH_LONG
        val msg: String = if (mSuccess) {
            getString(R.string.permission_granted)
        } else {
            getString(R.string.permission_granted_fail)
        }
        toast.setText(msg)
        toast.show()
    }
}