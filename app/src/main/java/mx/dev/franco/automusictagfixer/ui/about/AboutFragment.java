package mx.dev.franco.automusictagfixer.ui.about;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ShareCompat;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.ui.BaseFragment;
import mx.dev.franco.automusictagfixer.ui.main.MainActivity;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;

public class AboutFragment extends BaseFragment {
    BillingClient billingClient;
    public AboutFragment(){}

    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        billingClient = BillingClient.
                newBuilder(requireActivity()).
                enablePendingPurchases().
                setListener((billingResult, list) -> {

                }).
                build();


        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                Log.e(getClass().getName(), billingResult.getResponseCode()+"");
            }

            @Override
            public void onBillingServiceDisconnected() {

            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //Set an action bar
        //Set UI elements
        MaterialButton shareButton = view.findViewById(R.id.mb_share);
        MaterialButton rateButton = view.findViewById(R.id.mb_rate);
        MaterialButton contactButton = view.findViewById(R.id.mb_bug_report);
        MaterialButton githubButton = view.findViewById(R.id.mb_github);
        MaterialButton monetizationButton = view.findViewById(R.id.mb_donations);
        //Set listener for UI elements
        shareButton.setOnClickListener(v -> {
            String shareSubText = getString(R.string.app_name) + " " + getString(R.string.share);
            String shareBodyText = getPlayStoreLink();

            Intent shareIntent = ShareCompat.IntentBuilder.from(getActivity()).
                    setType("text/plain").
                    setText(shareSubText +"\n"+ shareBodyText).getIntent();
            shareIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            startActivity(shareIntent);
        });

        rateButton.setOnClickListener(v -> rateApp());

        contactButton.setOnClickListener(v ->
                AndroidUtils.openInExternalApp(Intent.ACTION_SENDTO,
                        "mailto: dark.yellow.studios@gmail.com", getActivity()));

        githubButton.setOnClickListener(v -> {
            Uri uri = Uri.parse("https://github.com/frank240889/AutoMusicTagFixer");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        });

        monetizationButton.setOnClickListener(v -> {
            List<String> skuList = new ArrayList<>();
            skuList.add("automusictagfixer2008350092");
            SkuDetailsParams.Builder params =  SkuDetailsParams.
                    newBuilder().
                    setSkusList(skuList).
                    setType(BillingClient.SkuType.INAPP);
            billingClient.querySkuDetailsAsync(params.build(), (billingResult, list) -> {
                for (SkuDetails skuDetails : list) {
                    Log.e(billingClient.getClass().getName(), skuDetails.getSku());
                }
                BillingFlowParams flowParams = BillingFlowParams.newBuilder().
                        setSkuDetails(list.get(0)).
                        build();
                billingClient.launchBillingFlow(requireActivity(), flowParams);
            });
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((MainActivity)getActivity()).actionBar.setTitle(getString(R.string.about));
        ((MainActivity)getActivity()).startTaskFab.hide();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        Animation animation = super.onCreateAnimation(transit, enter, nextAnim);

        if (animation == null && nextAnim != 0) {
            animation = AnimationUtils.loadAnimation(getActivity(), nextAnim);
        }

        if (animation != null && getView() != null)
            getView().setLayerType(View.LAYER_TYPE_HARDWARE, null);

        return animation;
    }

    private String getPlayStoreLink(){
        return Constants.PLAY_STORE_URL + getActivity().getPackageName();
    }

    private void rateApp(){
        String packageName = getActivity().getPackageName();
        Uri uri = Uri.parse("market://details?id=" + packageName);
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, after pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(Constants.PLAY_STORE_URL + packageName)));
        }
    }
}
