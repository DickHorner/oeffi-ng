package de.schildbach.oeffi.network;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.EditText;

import de.schildbach.oeffi.Application;
import de.schildbach.oeffi.R;
import de.schildbach.oeffi.util.DialogBuilder;
import de.schildbach.pte.NetworkId;

public class NetworkCredentialsDialog {
    @SuppressLint("UnsafeImplicitIntentLaunch")
    public static void show(
            final Context context,
            final NetworkId networkId
            ) {
        final Application application = Application.getInstance();
        final DialogBuilder builder = DialogBuilder.get(context, R.layout.network_credentials_dialog);
        builder.setTitle(R.string.network_preferences_credentials_edit_title);
        final View contentView = builder.getView();
        final EditText editText = contentView.findViewById(R.id.credentials_text);
        editText.setHint(context.getString(R.string.network_preferences_credentials_edit_hint,
                NetworkResources.instance(context, networkId).label));
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            final String text = editText.getText().toString();
            NetworkProviderFactory.getInstance()
                    .setNetworkCredentials(application.prefsGetNetworkId(false), text);
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.setNeutralButton(R.string.help, (dialog, which) -> {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(context.getString(R.string.network_preferences_credentials_help_url))));
        });
        builder.setCancelable(true);
        builder.show();
    }
}
