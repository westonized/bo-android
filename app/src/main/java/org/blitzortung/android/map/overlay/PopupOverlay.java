package org.blitzortung.android.map.overlay;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import org.blitzortung.android.app.R;
import org.blitzortung.android.map.OwnMapActivity;
import org.blitzortung.android.map.OwnMapView;

public abstract class PopupOverlay<Item extends OverlayItem> extends ItemizedOverlay<Item> {

    private final OwnMapActivity activity;

    public PopupOverlay(OwnMapActivity activity, Drawable defaultMarker) {
        super(defaultMarker);
        this.activity = activity;
        popupShown = false;
    }

    boolean popupShown;

    protected void showPopup(GeoPoint location, String text) {

        OwnMapView map = activity.getMapView();
        View popUp = map.getPopup();
        map.removeView(popUp);

        TextView statusText = (TextView) popUp.findViewById(R.id.popup_text);
        statusText.setBackgroundColor(0x88000000);
        statusText.setPadding(5, 5, 5, 5);
        statusText.setText(text);

        MapView.LayoutParams mapParams = new MapView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                location, 0, 0, MapView.LayoutParams.BOTTOM_CENTER);

        map.addView(popUp, mapParams);

        popupShown = true;
    }

    public boolean clearPopup() {
        OwnMapView map = activity.getMapView();
        View popUp = map.getPopup();

        map.removeView(popUp);

        boolean popupShownStatus = popupShown;
        popupShown = false;
        return popupShownStatus;
    }

    public OwnMapActivity getActivity() {
        return activity;
    }

    @Override
    public boolean onTap(GeoPoint arg0, MapView arg1) {
        boolean eventHandled = super.onTap(arg0, arg1);

        if (!eventHandled) {
            clearPopup();
        }

        return eventHandled;
    }
}
