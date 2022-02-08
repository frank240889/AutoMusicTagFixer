package mx.dev.franco.automusictagfixer.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.app.ShareCompat
import com.android.billingclient.api.*
import com.google.android.material.button.MaterialButton
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.ui.BaseFragment
import mx.dev.franco.automusictagfixer.ui.main.MainActivity
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils
import mx.dev.franco.automusictagfixer.utilities.Constants
import java.util.*

class AboutFragment : BaseFragment() {
    private var billingClient: BillingClient? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingClient = BillingClient.newBuilder(requireActivity()).enablePendingPurchases()
            .setListener { billingResult: BillingResult?, list: List<Purchase?>? -> }
            .build()
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.e(javaClass.name, billingResult.responseCode.toString() + "")
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //Set an action bar
        //Set UI elements
        val shareButton: MaterialButton = view.findViewById(R.id.mb_share)
        val rateButton: MaterialButton = view.findViewById(R.id.mb_rate)
        val contactButton: MaterialButton = view.findViewById(R.id.mb_bug_report)
        val githubButton: MaterialButton = view.findViewById(R.id.mb_github)
        val monetizationButton: MaterialButton = view.findViewById(R.id.mb_donations)
        //Set listener for UI elements
        shareButton.setOnClickListener { v: View? ->
            val shareSubText = getString(R.string.app_name) + " " + getString(R.string.share)
            val shareBodyText = playStoreLink
            val shareIntent =
                ShareCompat.IntentBuilder.from(requireActivity()).setType("text/plain").setText(
                    """
    $shareSubText
    $shareBodyText
    """.trimIndent()
                ).intent
            shareIntent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            startActivity(shareIntent)
        }
        rateButton.setOnClickListener { v: View? -> rateApp() }
        contactButton.setOnClickListener { v: View? ->
            AndroidUtils.openInExternalApp(
                Intent.ACTION_SENDTO,
                "mailto: dark.yellow.studios@gmail.com", activity
            )
        }
        githubButton.setOnClickListener { v: View? ->
            val uri = Uri.parse("https://github.com/frank240889/AutoMusicTagFixer")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
        monetizationButton.setOnClickListener { v: View? ->
            val skuList: MutableList<String> = ArrayList()
            skuList.add("automusictagfixer2008350092")
            val params = SkuDetailsParams.newBuilder().setSkusList(skuList)
                .setType(BillingClient.SkuType.INAPP)
            billingClient!!.querySkuDetailsAsync(params.build()) { billingResult: BillingResult?, list: List<SkuDetails>? ->
                for (skuDetails in list!!) {
                    Log.e(billingClient!!.javaClass.name, skuDetails.sku)
                }
                val flowParams = BillingFlowParams.newBuilder().setSkuDetails(list[0]).build()
                billingClient!!.launchBillingFlow(requireActivity(), flowParams)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as MainActivity?)!!.actionBar!!.title = getString(R.string.about)
        //((MainActivity)getActivity()).startTaskFab.hide();
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        var animation = super.onCreateAnimation(transit, enter, nextAnim)
        if (animation == null && nextAnim != 0) {
            animation = AnimationUtils.loadAnimation(activity, nextAnim)
        }
        if (animation != null && view != null) requireView().setLayerType(View.LAYER_TYPE_HARDWARE, null)
        return animation
    }

    private val playStoreLink: String
        private get() = Constants.PLAY_STORE_URL + requireActivity().packageName

    private fun rateApp() {
        val packageName = requireActivity().packageName
        val uri = Uri.parse("market://details?id=$packageName")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        // To count with Play market backstack, after pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(Constants.PLAY_STORE_URL + packageName)
                )
            )
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): AboutFragment {
            return AboutFragment()
        }
    }
}