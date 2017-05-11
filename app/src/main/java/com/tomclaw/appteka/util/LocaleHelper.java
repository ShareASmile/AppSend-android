package com.tomclaw.appteka.util;

import android.text.TextUtils;

import com.tomclaw.appteka.main.item.StoreItem;

import java.util.Locale;
import java.util.Map;

/**
 * Created by solkin on 14.03.17.
 */
public class LocaleHelper {

    public static String getLocalizedLabel(StoreItem item) {
        String label = item.getLabel();
        Locale locale = Locale.getDefault();
        String country = locale.getCountry();
        Map<String, String> labels = item.getLabels();
        if (labels != null) {
            String localizedLabel = labels.get(country.toLowerCase());
            if (!TextUtils.isEmpty(localizedLabel)) {
                label = localizedLabel;
            }
        }
        return label;
    }
}
