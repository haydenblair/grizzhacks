package org.zumh.android.grizzhacks;

import android.support.v4.app.Fragment;

public class RecognitionActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return RecognitionFragment.newInstance();
    }
}
