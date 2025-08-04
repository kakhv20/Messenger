package ge.kakhvlediani.messenger.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import androidx.core.graphics.drawable.toDrawable
import ge.kakhvlediani.messenger.R

class LoadingDialog(context: Context) : Dialog(context) {

    init {
        setContentView(R.layout.dialog_loading)
        setCancelable(false)
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
    }
}