package lv.chi.photopicker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import lv.chi.photopicker.adapter.SelectableImage


/**
 * create by wzq on 2020/7/16
 *
 */
class SelectorAdapter(
    private val onImageClick: (SelectableImage) -> Unit) :
    ListAdapter<SelectableImage, SelectorAdapter.Holder>(Diff()) {

    val selectedList = arrayListOf<SelectableImage>()

    var limit = 0

    var onItemClick: ((Int) -> Unit)? = null

    var canPreview = false

    private val onItemStateChange = fun(itemData: SelectableImage): Boolean {
        if (limit > 0 && selectedList.size >= limit) {
            return false
        }
        if (itemData.selected) {
            selectedList.remove(itemData)
        } else {
            if (selectedList.contains(itemData)) {
                selectedList.remove(itemData)
            }
            selectedList.add(itemData)
        }
        onItemClick?.invoke(selectedList.size)
        return true
    }

    /* private val onItemPreview = fun(context: Context, position: Int) {
         if (canPreview) {
             val intent = Intent(context, PreviewActivity::class.java)
             val data = arrayListOf<MediaData>()
             data.addAll(currentList)
             intent.putParcelableArrayListExtra("data", data)
             intent.putExtra("position", position)
             context.startActivity(intent)
         }
     }*/

    class Diff : DiffUtil.ItemCallback<SelectableImage>() {
        override fun areItemsTheSame(oldItem: SelectableImage, newItem: SelectableImage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: SelectableImage,
            newItem: SelectableImage
        ): Boolean {
            return oldItem.name == newItem.name
        }
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): Holder {
        val root = LayoutInflater.from(p0.context).inflate(R.layout.item_selector, p0, false)
        return Holder(root, onItemStateChange)
    }

    override fun onBindViewHolder(holder: Holder, p1: Int) {
        val item = getItem(p1)
        Glide.with(holder.img).load(item.uri).apply(RequestOptions().skipMemoryCache(true))
            .into(holder.img)
        holder.checkBox.isSelected = item.selected
        holder.checkBox.tag = item
    }

    class Holder(
        root: View,
        onStateChange: (SelectableImage) -> Boolean
    ) :
        RecyclerView.ViewHolder(root) {
        val img: ImageView = root.findViewById(R.id.img)
        val checkBox: View = root.findViewById(R.id.checkbox)

        init {
            checkBox.setOnClickListener {
                val itemData = (it.tag as? SelectableImage) ?: return@setOnClickListener
                if (onStateChange(itemData)) {
                    itemData.selected = !itemData.selected
                    checkBox.isSelected = itemData.selected
                }
            }
            /*img.setOnClickListener {
                onPreview(it.context, bindingAdapterPosition)
            }*/
        }
    }
}