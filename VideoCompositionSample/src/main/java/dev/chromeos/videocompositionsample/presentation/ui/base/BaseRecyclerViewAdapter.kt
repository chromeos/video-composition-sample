/*
 * Copyright (c) 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.chromeos.videocompositionsample.presentation.ui.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

abstract class BaseRecyclerViewAdapter<T>(var data: MutableList<T> = ArrayList(),
                                          open var layoutRes: Int,
                                          open var onItemClickListener: ((T) -> Unit?)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val items: MutableList<T>
        get() = data

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return getViewHolder(view)
    }

    abstract fun getViewHolder(view: View): RecyclerView.ViewHolder

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = data[position]
        holder.itemView.setOnClickListener { onItemClickListener?.invoke(item) }
        onBindViewHolder(holder, item, position)
    }

    abstract fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: T, position: Int)

    override fun getItemCount(): Int {
        return data.size
    }

    open fun getItem(position: Int): T {
        return data[position]
    }

    open fun getPosition(item: T): Int {
        return data.indexOf(item)
    }

    open fun add(data: T) {
        this.data.add(data)
        notifyItemInserted(this.data.size - 1)
    }

    open fun add(position: Int, data: T) {
        this.data.add(position, data)
        notifyItemInserted(position)
    }

    open fun add(list: List<T>) {
        val startPosition = data.size
        data.addAll(list)
        notifyItemRangeChanged(startPosition, list.size)
    }

    open fun replace(oldData: T, newData: T) {
        val position = data.indexOf(oldData)
        if (position != -1) {
            data.removeAt(position)
            data.add(position, newData)
            notifyItemChanged(position)
        }
    }

    open fun replaceAll(list: List<T>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    open fun remove(data: T) {
        val position = this.data.indexOf(data)
        if (position != -1)
            remove(position)
    }

    open fun remove(position: Int) {
        data.removeAt(position)
        notifyItemRemoved(position)
    }

    open fun clear() {
        val size = data.size
        data.clear()
        notifyItemRangeRemoved(0, size)
    }
}