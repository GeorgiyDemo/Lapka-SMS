/*
 * Copyright 2016 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Inlined from io.realm:android-adapters:3.1.0 since the library is no longer
 * available on public Maven repositories.
 */
package io.realm

import androidx.recyclerview.widget.RecyclerView

/**
 * The RealmRecyclerViewAdapter class is an abstract utility class for binding RecyclerView UI
 * elements to Realm data.
 *
 * This adapter will automatically handle any updates to its data and call
 * notifyDataSetChanged() as appropriate.
 */
abstract class RealmRecyclerViewAdapter<T : RealmModel, VH : RecyclerView.ViewHolder>(
    private var adapterData: OrderedRealmCollection<T>?,
    private val hasAutoUpdates: Boolean
) : RecyclerView.Adapter<VH>() {

    private val listener: OrderedRealmCollectionChangeListener<OrderedRealmCollection<T>>? =
        if (hasAutoUpdates) {
            OrderedRealmCollectionChangeListener { _, changeSet ->
                if (changeSet.state == OrderedCollectionChangeSet.State.INITIAL) {
                    notifyDataSetChanged()
                    return@OrderedRealmCollectionChangeListener
                }

                val deletions = changeSet.deletionRanges
                for (i in deletions.indices.reversed()) {
                    notifyItemRangeRemoved(deletions[i].startIndex, deletions[i].length)
                }

                val insertions = changeSet.insertionRanges
                for (range in insertions) {
                    notifyItemRangeInserted(range.startIndex, range.length)
                }

                val modifications = changeSet.changeRanges
                for (range in modifications) {
                    notifyItemRangeChanged(range.startIndex, range.length)
                }
            }
        } else {
            null
        }

    init {
        if (hasAutoUpdates && adapterData != null) {
            addListener(adapterData!!)
        }
    }

    private fun addListener(data: OrderedRealmCollection<T>) {
        @Suppress("UNCHECKED_CAST")
        when (data) {
            is RealmResults<T> -> data.addChangeListener(listener as OrderedRealmCollectionChangeListener<RealmResults<T>>)
            is RealmList<T> -> data.addChangeListener(listener as OrderedRealmCollectionChangeListener<RealmList<T>>)
            else -> throw IllegalArgumentException("RealmCollection not supported: " + data.javaClass)
        }
    }

    private fun removeListener(data: OrderedRealmCollection<T>) {
        @Suppress("UNCHECKED_CAST")
        when (data) {
            is RealmResults<T> -> data.removeChangeListener(listener as OrderedRealmCollectionChangeListener<RealmResults<T>>)
            is RealmList<T> -> data.removeChangeListener(listener as OrderedRealmCollectionChangeListener<RealmList<T>>)
            else -> throw IllegalArgumentException("RealmCollection not supported: " + data.javaClass)
        }
    }

    override fun getItemCount(): Int {
        return if (isDataValid) {
            adapterData!!.size
        } else {
            0
        }
    }

    /**
     * Returns the item associated with the specified position.
     */
    open fun getItem(index: Int): T? {
        if (!isDataValid) return null
        return adapterData?.get(index)
    }

    /**
     * Returns data associated with this adapter.
     */
    fun getData(): OrderedRealmCollection<T>? {
        return adapterData
    }

    /**
     * Updates the data associated to the Adapter. Useful when the query has been changed.
     * If the query does not change you might consider using the automaticUpdate feature.
     */
    open fun updateData(data: OrderedRealmCollection<T>?) {
        if (hasAutoUpdates) {
            if (adapterData != null) {
                removeListener(adapterData!!)
            }
            if (data != null) {
                addListener(data)
            }
        }
        adapterData = data
        notifyDataSetChanged()
    }

    private val isDataValid: Boolean
        get() = adapterData != null && adapterData!!.isValid
}
