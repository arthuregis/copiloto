package br.pizao.copiloto.service.impl

import android.graphics.SurfaceTexture
import android.view.TextureView

interface SurfaceTextureListenerImpl : TextureView.SurfaceTextureListener {

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
    }

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        return true
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
}