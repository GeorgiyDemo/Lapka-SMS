/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.moez.QKSMS.feature.gallery

import android.Manifest
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.moez.QKSMS.R
import com.moez.QKSMS.common.base.QkActivity
import com.moez.QKSMS.common.util.DateFormatter
import com.moez.QKSMS.common.util.extensions.setVisible
import com.moez.QKSMS.common.widget.QkTextView
import com.moez.QKSMS.model.MmsPart
import com.moez.QKSMS.util.Preferences
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class GalleryActivity : QkActivity(), GalleryView {

    @Inject lateinit var dateFormatter: DateFormatter
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var pagerAdapter: GalleryPagerAdapter
    @Inject lateinit var prefs: Preferences

    // View properties
    private val pager: ViewPager2 by lazy { findViewById(R.id.pager) }
    private val toolbar: Toolbar by lazy { findViewById(R.id.toolbar) }
    private val toolbarTitle: QkTextView by lazy { findViewById(R.id.toolbarTitle) }
    private val toolbarSubtitle: QkTextView by lazy { findViewById(R.id.toolbarSubtitle) }

    val partId by lazy { intent.getLongExtra("partId", 0L) }

    private val optionsItemSubject: Subject<Int> = PublishSubject.create()
    private val pageChangedSubject: Subject<MmsPart> = PublishSubject.create()
    private val viewModel by lazy { ViewModelProvider(this, viewModelFactory)[GalleryViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gallery_activity)
        showBackButton(true)
        viewModel.bindView(this)

        pager.adapter = pagerAdapter
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                this@GalleryActivity.onPageSelected(position)
            }
        })

        pagerAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                pagerAdapter.getData()?.takeIf { pagerAdapter.itemCount > 0 }
                        ?.indexOfFirst { part -> part.id == partId }
                        ?.let { index ->
                            onPageSelected(index)
                            pager.setCurrentItem(index, false)
                            pagerAdapter.unregisterAdapterDataObserver(this)
                        }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (prefs.showInTaskSwitcher.get()) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    fun onPageSelected(position: Int) {
        toolbarSubtitle.text = pagerAdapter.getItem(position)?.messages?.firstOrNull()?.date
                ?.let(dateFormatter::getDetailedTimestamp)
        toolbarSubtitle.isVisible = toolbarTitle.text.isNotBlank()

        pagerAdapter.getItem(position)?.run(pageChangedSubject::onNext)
    }

    override fun render(state: GalleryState) {
        toolbar.setVisible(state.navigationVisible)

        title = state.title
        pagerAdapter.updateData(state.parts)
    }

    override fun optionsItemSelected(): Observable<Int> = optionsItemSubject

    override fun screenTouched(): Observable<*> = pagerAdapter.clicks

    override fun pageChanged(): Observable<MmsPart> = pageChangedSubject

    override fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.gallery, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> optionsItemSubject.onNext(item.itemId)
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        pagerAdapter.destroy()
    }

}