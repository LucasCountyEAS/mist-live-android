package com.mist.streaming

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/**
 * Entry point for the Mist TV app.
 * Hosts the [BrowseFragment] which renders the TV Guide.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, BrowseFragment())
                .commitNow()
        }
    }
}
