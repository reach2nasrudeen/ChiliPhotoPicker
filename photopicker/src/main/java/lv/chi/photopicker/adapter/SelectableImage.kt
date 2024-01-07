package lv.chi.photopicker.adapter

import android.net.Uri
import android.provider.MediaStore

data class SelectableImage(
    val id: String,
    val name: String,
    val size: Long = 0,
    val orientation: Int,
    val createTime: String?,
    val dirId: String?,
    val dirName: String?,
    val path: String?, //路径
    var selected: Boolean = false
) {
    var uri: Uri? = null
        get() {
            if (field == null) {
                val baseUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                field = Uri.withAppendedPath(baseUri, id)
            }
            return field
        }


}