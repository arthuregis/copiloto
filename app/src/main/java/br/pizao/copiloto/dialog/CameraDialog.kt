package br.pizao.copiloto.dialog

import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.IBinder
import android.view.*
import androidx.fragment.app.DialogFragment
import br.pizao.copiloto.R
import br.pizao.copiloto.service.CopilotoService
import br.pizao.copiloto.service.CopilotoService.Companion.CAMERA_START_ACTION
import br.pizao.copiloto.overlay.GraphicOverlay
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CameraDialog : DialogFragment() {

    lateinit var textureView: TextureView
    lateinit var graphicOverlay: GraphicOverlay

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as CopilotoService.CameraBinder
            val cameraService = binder.getService()
            cameraService.startPreview(textureView, graphicOverlay)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.CameraDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.camera_dialog, container)
        textureView = root.findViewById(R.id.texPreview)

        graphicOverlay = root.findViewById(R.id.graphic_overlay)
        graphicOverlay.clear()

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)

        root.findViewById<FloatingActionButton>(R.id.close_button).setOnClickListener {
            dismiss()
        }

        startCamera()

        return root
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        context?.unbindService(connection)
        startBackgroundCamera()
    }

    private fun startCamera() {
        Intent(context, CopilotoService::class.java).also {
            context?.bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun startBackgroundCamera() {
        Intent(context, CopilotoService::class.java).apply {
            action = CAMERA_START_ACTION
        }.also {
            context?.startService(it)
        }
    }
}