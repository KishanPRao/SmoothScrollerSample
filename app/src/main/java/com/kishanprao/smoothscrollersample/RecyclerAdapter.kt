package com.kishanprao.smoothscrollersample

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecyclerAdapter(private var context: Context, private var dataList: ArrayList<String>) :
    RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {
    override fun getItemCount(): Int {
        return dataList.size;
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.list_item_view, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = dataList.get(position);
    }


    class ViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!) {
        var textView: TextView = itemView!!.findViewById(R.id.title)
    }
}