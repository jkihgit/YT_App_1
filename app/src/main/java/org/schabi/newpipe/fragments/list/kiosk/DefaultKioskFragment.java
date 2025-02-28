package org.schabi.newpipe.fragments.list.kiosk;

import android.os.Bundle;

import org.schabi.newpipe.error.ErrorInfo;
import org.schabi.newpipe.error.UserAction;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.util.KioskTranslator;

public class DefaultKioskFragment extends KioskFragment {

    public DefaultKioskFragment() {
        super("unknown");
    }

    public DefaultKioskFragment(final String ingress) {
        super(ingress);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (serviceId < 0) {
            updateSelectedDefaultKiosk();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (serviceId != 0) {
            if (currentWorker != null) {
                currentWorker.dispose();
            }
            updateSelectedDefaultKiosk();
            reloadContent();
        }
    }

    private void updateSelectedDefaultKiosk() {
        try {
            serviceId = 0;

            final KioskList kioskList = NewPipe.getService(serviceId).getKioskList();
            kioskId = kioskList.getDefaultKioskId();
            url = kioskList.getListLinkHandlerFactoryByType(kioskId).fromId(kioskId).getUrl();

            kioskTranslatedName = KioskTranslator.getTranslatedKioskName(kioskId, requireContext());
            name = kioskTranslatedName;

            currentInfo = null;
            currentNextPage = null;
        } catch (final ExtractionException e) {
            showError(new ErrorInfo(e, UserAction.REQUESTED_KIOSK,
                    "Loading default kiosk for selected service"));
        }
    }
}
