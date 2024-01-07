package lv.chi.photopicker

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import lv.chi.photopicker.adapter.SelectableImage
import lv.chi.photopicker.utils.SingleLiveEvent

internal class PickerViewModel : ViewModel() {

    private val hasContentData = MutableLiveData<Boolean>(false)
    private val inProgressData = MutableLiveData<Boolean>(false)
    private val hasPermissionData = MutableLiveData<Boolean>(false)
    private val selectedData = MutableLiveData<ArrayList<Uri>>(arrayListOf())
    private val photosData = MutableLiveData<ArrayList<SelectableImage>>(arrayListOf())
    private val maxSelectionReachedData = SingleLiveEvent<Unit>()

    private var maxSelectionCount = SELECTION_UNDEFINED

    val hasContent: LiveData<Boolean> = Transformations.distinctUntilChanged(hasContentData)
    val inProgress: LiveData<Boolean> = inProgressData
    val hasPermission: LiveData<Boolean> = hasPermissionData
    val selected: LiveData<ArrayList<Uri>> = selectedData
    val photos: LiveData<ArrayList<SelectableImage>> = photosData
    val maxSelectionReached: LiveData<Unit> = maxSelectionReachedData

    fun setHasPermission(hasPermission: Boolean) = hasPermissionData.postValue(hasPermission)

    fun setMaxSelectionCount(count: Int) {
        maxSelectionCount = count
    }

    fun clearSelected() {
        GlobalScope.launch {
            val photos = requireNotNull(photosData.value).map { it.copy(selected = false) }
            val array = arrayListOf<SelectableImage>()
            array.addAll(photos)
            photosData.postValue(array)
            selectedData.postValue(arrayListOf())
        }
    }

    fun setPhotos(photos: List<SelectableImage>) {
        hasContentData.postValue(photos.isNotEmpty())
        photosData.postValue(photos as ArrayList<SelectableImage>?)
    }

    fun setInProgress(progress: Boolean) {
        inProgressData.postValue(progress)
    }

    fun toggleSelected(photo: SelectableImage) {
        GlobalScope.launch(Dispatchers.IO) {
            val selected = requireNotNull(selectedData.value)

            when {
                photo.selected -> selected.remove(photo.uri)
                canSelectMore(selected.size) -> photo.uri?.let { selected.add(it) }
                else -> {
                    maxSelectionReachedData.postValue(Unit)
                    return@launch
                }
            }

            val photos = requireNotNull(photosData.value)
            photos.indexOfFirst { item -> item.id == photo.id }
                .takeIf { pos -> pos != -1 }
                ?.let { pos -> photos[pos] = photo.copy(selected = !photo.selected) }

            selectedData.postValue(selected)
            photosData.postValue(photos)
        }
    }

    private fun canSelectMore(size: Int) =
        maxSelectionCount == SELECTION_UNDEFINED || maxSelectionCount > size

    companion object {
        const val SELECTION_UNDEFINED = -1
    }
}