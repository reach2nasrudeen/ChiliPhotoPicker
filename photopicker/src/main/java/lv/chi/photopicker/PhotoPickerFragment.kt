package lv.chi.photopicker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupMenu
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_photo_picker.*
import kotlinx.android.synthetic.main.fragment_photo_picker.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lv.chi.photopicker.PickerViewModel.Companion.SELECTION_UNDEFINED
import lv.chi.photopicker.adapter.SelectableImage
import lv.chi.photopicker.ext.isPermissionGranted
import lv.chi.photopicker.ext.parentAs
import lv.chi.photopicker.utils.NonDismissibleBehavior
import lv.chi.photopicker.utils.SpacingItemDecoration

class PhotoPickerFragment : DialogFragment() {

    private val selectorAdapter = SelectorAdapter(::onImageClicked)

    private var behavior: BottomSheetBehavior<FrameLayout>? = null

    private var snackBar: Snackbar? = null

    private val cornerRadiusOutValue = TypedValue()

    private lateinit var contextWrapper: ContextThemeWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contextWrapper = ContextThemeWrapper(context, getTheme(requireArguments()))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return PickerDialog(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return LayoutInflater.from(contextWrapper).inflate(
            R.layout.fragment_photo_picker,
            container,
            false
        )
            .apply {
                contextWrapper.theme.resolveAttribute(
                    R.attr.pickerCornerRadius,
                    cornerRadiusOutValue,
                    true
                )

                photos.apply {
                    adapter = selectorAdapter
                    val margin = context.resources.getDimensionPixelSize(R.dimen.margin_2dp)
                    addItemDecoration(SpacingItemDecoration(margin, margin, margin, margin))
                    layoutManager = GridLayoutManager(
                        requireContext(),
                        if (orientation() == Configuration.ORIENTATION_LANDSCAPE) 5 else 3,
                        RecyclerView.VERTICAL,
                        false
                    )
                }

//                findViewById<AppCompatTextView>(R.id.grant).setOnClickListener { grantPermissions() }

                pickerBottomSheetCallback.setMargin(
                    requireContext().resources.getDimensionPixelSize(
                        cornerRadiusOutValue.resourceId
                    )
                )
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        coordinator.doOnLayout {
            behavior = BottomSheetBehavior.from<FrameLayout>(design_bottom_sheet).apply {
                addBottomSheetCallback(pickerBottomSheetCallback)
                isHideable = true
                skipCollapsed = false
                peekHeight =
                    if (orientation() == Configuration.ORIENTATION_LANDSCAPE) it.measuredHeight / 2
                    else BottomSheetBehavior.PEEK_HEIGHT_AUTO
            }
        }

        val limit = getMaxSelection(arguments)
        println("limit====>$limit")
        selectorAdapter.limit = limit
        selectorAdapter.onItemClick = {
            handleSelected()
        }

        if (savedInstanceState == null) updateState()

//        vm.photos.observe(viewLifecycleOwner, Observer { handlePhotos(it) })
        /*  vm.inProgress.observe(viewLifecycleOwner, Observer {
              photos.visibility = if (it) View.INVISIBLE else View.VISIBLE
              progressbar.visibility = if (it) View.VISIBLE else View.GONE
          })
          vm.hasContent.observe(viewLifecycleOwner, Observer {
              pickerBottomSheetCallback.setNeedTransformation(it)
              if (it) remeasureContentDialog()
          })*/
        /*vm.maxSelectionReached.observe(viewLifecycleOwner, Observer {
            val max = getMaxSelection(requireArguments())
            Toast.makeText(
                requireContext(),
                resources.getQuantityString((R.plurals.picker_max_selection_reached), max, max),
                Toast.LENGTH_SHORT
            ).show()
        })*/


    }

    private fun onImageClicked(state: SelectableImage) {
//        if (getAllowMultiple(requireArguments())) {
//            vm.toggleSelected(state)
//        }
    }

    private val popupMenu by lazy {
        PopupMenu(context, this.view?.findViewById(R.id.tv_folder_name))
    }

    private fun buildMenus(images: List<SelectableImage>) {
        val tvFolderName = this.view?.findViewById<AppCompatTextView>(R.id.tv_folder_name)

        val imageIsNotEmpty = !images.isNullOrEmpty()
        pickerBottomSheetCallback.setNeedTransformation(imageIsNotEmpty)

        if (imageIsNotEmpty) {

            tvFolderName?.setOnClickListener { popupMenu.show() }

            remeasureContentDialog()
            val data = mutableListOf<Pair<String, List<SelectableImage>>>()
            data.clear()
            data.add(Pair("Gallery", images))
            val columns = images.groupBy { et ->
                et.dirName ?: "Unknown"
            }.toList().sortedByDescending { p -> p.second.size }
            data.addAll(columns)
            popupMenu.menu.clear()
            data.forEachIndexed { index, pair ->
                val str = "${pair.first}  (${pair.second.size})"
                popupMenu.menu.add(0, index, index, str)
            }
            popupMenu.setOnMenuItemClickListener {
                tvFolderName?.text = data[it.itemId].first
                selectorAdapter.submitList(data[it.itemId].second)
                true
            }
            tvFolderName?.text = "Gallery"
            tvFolderName?.visibility = View.VISIBLE
        } else {
            tvFolderName?.visibility = View.INVISIBLE
        }
    }

    private fun remeasureContentDialog() {
        coordinator.doOnLayout {
            val heightLp = design_bottom_sheet.layoutParams
            heightLp.height =
                coordinator.measuredHeight + requireContext().resources.getDimensionPixelSize(
                    cornerRadiusOutValue.resourceId
                )
            design_bottom_sheet.layoutParams = heightLp
        }
    }


    @SuppressLint("InflateParams")
    private fun handleSelected() {
        selectorAdapter.selectedList.let { selected ->
            if (selected.isEmpty()) {
                snackBar?.dismiss()
                snackBar = null
            } else {
                val count = selected.count()
                if (snackBar == null) {
                    val view =
                        LayoutInflater.from(contextWrapper).inflate(R.layout.view_snackbar, null)
                    snackBar = Snackbar.make(coordinator, "", Snackbar.LENGTH_INDEFINITE)
                        .setBehavior(NonDismissibleBehavior())
                    (snackBar?.view as? ViewGroup)?.apply {
                        setPadding(0, 0, 0, 0)
                        removeAllViews()
                        addView(view)
                        findViewById<AppCompatImageView>(R.id.cancel).setOnClickListener {/* vm.clearSelected()*/ }
                        findViewById<AppCompatTextView>(R.id.select).setOnClickListener {
                            parentAs<Callback>()?.onImagesPicked(selected)
                            dismiss()
                        }
                    }
                    snackBar?.show()
                }

                snackBar?.view?.findViewById<AppCompatTextView>(R.id.count)?.text =
                    resources.getQuantityString(
                        R.plurals.picker_selected_count,
                        count,
                        count
                    )
            }
        }
    }
/*
    private fun handlePhotos(photos: List<SelectableImage>) {
//        vm.setInProgress(false)
        selectorAdapter.submitList(photos.toMutableList())
        empty_text.visibility =
            if (photos.isEmpty() && vm.hasPermission.value == true) View.VISIBLE else View.GONE
    }*/

    private fun loadPhotos() {

        GlobalScope.launch {
            context?.let { ImageSource.getMediaSource(it) }.orEmpty().let {
                withContext(Dispatchers.Main) {
                    progressbar.visibility = View.GONE
                    empty_text.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
                    buildMenus(it)
                    selectorAdapter.submitList(it)
                }
            }
        }
//        vm.setInProgress(true)
        /*GlobalScope.launch(Dispatchers.IO) {
            context?.let { ImageSource.getMediaSource(it) }.orEmpty().let {
                withContext(Dispatchers.Main) {
                    empty_text.visibility = if (it.isEmpty() && vm.hasPermission.value == true) View.VISIBLE else View.GONE
                    buildMenus(it)
                    selectorAdapter.submitList(it)
                }
//                vm.setPhotos(it)
            }
        }*/
    }
//
//    private fun grantPermissions() {
//        if (!isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE))
//            requestPermissions(
//                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
//                Request.MEDIA_ACCESS_PERMISSION
//            )
//    }

    private fun updateState() {
        if (isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            loadPhotos()
        }
    }

    private val pickerBottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        private var margin = 0
        private var needTransformation = false
        override fun onStateChanged(bottomSheet: View, @BottomSheetBehavior.State newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            if (!needTransformation) return
            val calculatedSpacing = calculateSpacing(slideOffset)
            design_bottom_sheet.translationY = -calculatedSpacing
            design_bottom_sheet.setPadding(0, calculatedSpacing.toInt(), 0, 0)
        }

        fun setMargin(margin: Int) {
            this.margin = margin
        }

        fun setNeedTransformation(need: Boolean) {
            needTransformation = need
        }

        private fun calculateSpacing(progress: Float) = margin * progress
    }

    private fun orientation() = requireContext().resources.configuration.orientation

    interface Callback {
        fun onImagesPicked(photos: ArrayList<SelectableImage>)
    }

    companion object {
        private const val KEY_MULTIPLE = "KEY_MULTIPLE"
        private const val KEY_ALLOW_CAMERA = "KEY_ALLOW_CAMERA"
        private const val KEY_THEME = "KEY_THEME"
        private const val KEY_MAX_SELECTION = "KEY_MAX_SELECTION"

        fun newInstance(
            multiple: Boolean = false,
            allowCamera: Boolean = false,
            maxSelection: Int = SELECTION_UNDEFINED,
            @StyleRes theme: Int = R.style.ChiliPhotoPicker_Light
        ) = PhotoPickerFragment().apply {
            arguments = bundleOf(
                KEY_MULTIPLE to multiple,
                KEY_ALLOW_CAMERA to allowCamera,
                KEY_MAX_SELECTION to maxSelection,
                KEY_THEME to theme
            )
        }

        private fun getTheme(args: Bundle) = args.getInt(KEY_THEME)
        private fun getAllowCamera(args: Bundle) = args.getBoolean(KEY_ALLOW_CAMERA)
        private fun getAllowMultiple(args: Bundle) = args.getBoolean(KEY_MULTIPLE)
        private fun getMaxSelection(args: Bundle?) = args?.getInt(KEY_MAX_SELECTION, 5) ?: 5
    }
}