package br.pizao.copiloto.view

import android.widget.ImageView
import androidx.databinding.BindingAdapter

@BindingAdapter("source")
fun setSource(imageView: ImageView, resId: Int) {
    imageView.setImageResource(resId)
}