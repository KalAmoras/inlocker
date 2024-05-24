package com.example.inlocker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val context: Context,
    private val installedApps: List<ApplicationInfo>,
    private var selectedPasswordItem: PasswordItem?,
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    private val packageManager: PackageManager = context.packageManager

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.app_list_item, parent, false)
        return AppViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val appInfo = installedApps[position]
        holder.bind(appInfo)
        holder.selectPasswordButton.tag = appInfo
    }

    override fun getItemCount(): Int = installedApps.size

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appNameTextView: TextView = itemView.findViewById(R.id.appNameTextView)
        private val appIconImageView: ImageView = itemView.findViewById(R.id.appIconImageView)
        val selectPasswordButton: Button = itemView.findViewById(R.id.selectPasswordButton)
        private val selectedPasswordTextView: TextView = itemView.findViewById(R.id.selectedPasswordTextView)

        fun bind(appInfo: ApplicationInfo) {
            appNameTextView.text = appInfo.loadLabel(packageManager)
            appIconImageView.setImageDrawable(appInfo.loadIcon(packageManager))

            selectedPasswordTextView.text = selectedPasswordItem?.password ?: "Choose your password"
        }
    }

    fun setSelectedPasswordItem(passwordItem: PasswordItem?) {
        selectedPasswordItem = passwordItem
        notifyDataSetChanged()
    }
}
